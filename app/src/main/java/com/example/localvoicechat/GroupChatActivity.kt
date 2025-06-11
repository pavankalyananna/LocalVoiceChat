package com.example.localvoicechat

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.localvoicechat.databinding.ActivityGroupChatBinding
import org.json.JSONObject
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
    private var hostIp = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("GroupChat", "Starting Group Chat Activity")

        userName = intent.getStringExtra("USER_NAME") ?: "User"
        isHost = intent.getBooleanExtra("IS_HOST", false)

        Log.d("GroupChat", "User: $userName, Host: $isHost")

        // Get hotspot IP
        hostIp = getHotspotIpAddress(this)
        Log.d("GroupChat", "Using host IP: $hostIp")

        // Initialize WebRTC
        webRTCClient = WebRTCClient(application, this, userName)
        signalingClient = SignalingClient(this, webRTCClient, userName, isHost, hostIp)

        // Initialize participants with current user
        participants.add(Participant(userName, isHost, false, false))
        Log.d("GroupChat", "Added self to participants: $userName")

        setupRecyclerView()
        setupMuteButton()
        updateParticipantTitle()

        // Start signaling
        signalingClient.connect()
        Log.d("GroupChat", "Signaling client started")

        // Set audio mode
        volumeControlStream = AudioManager.STREAM_VOICE_CALL
    }

    // GroupChatActivity.kt - Update getHotspotIpAddress()
    private fun getHotspotIpAddress(context: Context): String {
        return if (isHost) {
            "192.168.43.1" // Host device always uses this IP
        } else {
            try {
                val wifiManager = getSystemService(WIFI_SERVICE) as WifiManager
                val dhcpInfo = wifiManager.dhcpInfo
                Formatter.formatIpAddress(dhcpInfo.gateway)
            } catch (ex: Exception) {
                "192.168.43.1" // Fallback
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ParticipantAdapter(participants)
        binding.participantRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.participantRecyclerView.adapter = adapter
        Log.d("GroupChat", "RecyclerView setup complete")
    }

    private fun setupMuteButton() {
        binding.muteButton.setOnClickListener {
            isMuted = !isMuted
            webRTCClient.toggleAudio(!isMuted)
            updateMuteButtonState()

            // Update current user's mute status
            participants.firstOrNull { it.name == userName }?.isMuted = isMuted
            adapter.notifyDataSetChanged()
            Log.d("GroupChat", "Mute state toggled: $isMuted")
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
        Log.d("GroupChat", "Updated participant title: ${participants.size} participants")
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
        Log.d("GroupChat", "Adding remote stream from $userId")
        runOnUiThread {
            if (userId != userName) {
                val existing = participants.find { it.name == userId }
                if (existing == null) {
                    participants.add(Participant(userId, false, true, false))
                    Log.d("GroupChat", "Added new participant: $userId")
                } else {
                    existing.isSpeaking = true
                    Log.d("GroupChat", "Updated existing participant: $userId")
                }
                updateParticipantTitle()
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onRemoveRemoteStream(userId: String) {
        Log.d("GroupChat", "Removing remote stream from $userId")
        runOnUiThread {
            participants.removeIf { it.name == userId }
            updateParticipantTitle()
            adapter.notifyDataSetChanged()
        }
    }

    override fun onPeerConnected(userId: String) {
        Log.d("GroupChat", "Peer connected: $userId")
        runOnUiThread {
            if (userId != userName && participants.none { it.name == userId }) {
                participants.add(Participant(userId, false, false, false))
                updateParticipantTitle()
                adapter.notifyDataSetChanged()
                Log.d("GroupChat", "Added peer to UI: $userId")

                // Create peer connection
                webRTCClient.createPeerConnection(userId)

                // Host initiates call
                if (isHost) {
                    Log.d("GroupChat", "Host creating offer for $userId")
                    webRTCClient.createOffer(userId)
                }
            }
        }
    }

    override fun onPeerDisconnected(userId: String) {
        Log.d("GroupChat", "Peer disconnected: $userId")
        runOnUiThread {
            participants.removeIf { it.name == userId }
            updateParticipantTitle()
            adapter.notifyDataSetChanged()
        }
    }

    override fun onSendSignal(to: String, message: JSONObject) {
        Log.d("GroupChat", "Sending signal to $to: ${message.toString().take(50)}...")
        signalingClient.sendSignal(to, message)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("GroupChat", "Activity destroyed - cleaning up")
        webRTCClient.disconnect()
        signalingClient.disconnect()
    }
}