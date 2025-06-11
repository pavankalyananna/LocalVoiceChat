package com.example.localvoicechat

import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.localvoicechat.databinding.ActivityGroupChatBinding
import org.webrtc.*

class GroupChatActivity : AppCompatActivity(), WebRTCClient.Listener {

    private lateinit var binding: ActivityGroupChatBinding
    private lateinit var webRTCClient: WebRTCClient
    private lateinit var signalingClient: SignalingClient
    private lateinit var adapter: ParticipantAdapter
    private var isMuted = false
    private val participants = mutableListOf<Participant>()
    private var isHost = false
    private var userName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userName = intent.getStringExtra("USER_NAME") ?: "User"
        isHost = intent.getBooleanExtra("IS_HOST", false)

        // Check and request microphone permission
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                1
            )
        }

        // Initialize WebRTC
        webRTCClient = WebRTCClient(application, this, userName)
        signalingClient = SignalingClient(this, webRTCClient, userName, isHost)

        // Initialize participants
        participants.apply {
            add(Participant(userName, isHost, false, false)) // Current user
        }

        setupRecyclerView()
        setupMuteButton()
        updateParticipantTitle()

        // Start WebRTC connection
        if (isHost) {
            signalingClient.startSignalingServer()
        } else {
            signalingClient.connectToSignalingServer("192.168.43.1") // Host IP
        }

        // Set audio mode
        volumeControlStream = AudioManager.STREAM_VOICE_CALL
    }

    private fun setupRecyclerView() {
        adapter = ParticipantAdapter(participants)
        binding.participantRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.participantRecyclerView.adapter = adapter
    }

    private fun setupMuteButton() {
        binding.muteButton.setOnClickListener {
            isMuted = !isMuted
            webRTCClient.toggleAudio(!isMuted)
            updateMuteButtonState()

            // Update current user's mute status
            participants.firstOrNull { it.name == userName }?.isMuted = isMuted
            adapter.notifyDataSetChanged()
        }
        updateMuteButtonState()
    }

    private fun updateMuteButtonState() {
        binding.muteButton.text = getString(
            if (isMuted) R.string.mic_unmute else R.string.mic_mute
        )
        binding.muteButton.setIconResource(
            if (isMuted) R.drawable.ic_mic_off else R.drawable.ic_mic
        )
    }

    private fun updateParticipantTitle() {
        binding.participantTitle.text =
            resources.getString(R.string.participants_title, participants.size)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.e("GroupChat", "Microphone permission denied")
            }
        }
    }
    // WebRTCClient.Listener methods
    override fun onAddRemoteStream(stream: MediaStream, userId: String) {
        runOnUiThread {
            val existing = participants.find { it.name == userId }
            if (existing == null) {
                participants.add(Participant(userId, false, true, false))
            } else {
                existing.isSpeaking = true
            }
            updateParticipantTitle()
            adapter.notifyDataSetChanged()
        }
    }

    override fun onRemoveRemoteStream(userId: String) {
        runOnUiThread {
            participants.removeIf { it.name == userId }
            updateParticipantTitle()
            adapter.notifyDataSetChanged()
        }
    }

    override fun onPeerConnected(userId: String) {
        runOnUiThread {
            if (participants.none { it.name == userId }) {
                participants.add(Participant(userId, false, false, false))
                updateParticipantTitle()
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onPeerDisconnected(userId: String) {
        runOnUiThread {
            participants.removeIf { it.name == userId }
            updateParticipantTitle()
            adapter.notifyDataSetChanged()
        }
    }

    override fun onDestroy() {
        webRTCClient.disconnect()
        signalingClient.disconnect()
        super.onDestroy()
    }
}