package com.example.localvoicechat

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.webrtc.*

class WebRTCClient(
    context: Context,
    val listener: Listener,
    private val userId: String
) : PeerConnection.Observer {

    private val eglBase = EglBase.create()
    private val peerConnectionFactory: PeerConnectionFactory
    private val audioSource: AudioSource
    private val localAudioTrack: AudioTrack
    private val peerConnections = mutableMapOf<String, PeerConnection>()
    private val remoteStreams = mutableMapOf<String, MediaStream>()

    init {
        Log.d("WebRTCClient", "Initializing WebRTCClient for user: $userId")

        // Initialize PeerConnectionFactory
        val initializationOptions = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()

        PeerConnectionFactory.initialize(initializationOptions)
        Log.d("WebRTCClient", "PeerConnectionFactory initialized")

        val options = PeerConnectionFactory.Options()
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .createPeerConnectionFactory()
        Log.d("WebRTCClient", "PeerConnectionFactory created")

        // Create audio source and track
        val audioSourceConstraints = MediaConstraints()
        audioSource = peerConnectionFactory.createAudioSource(audioSourceConstraints)
        localAudioTrack = peerConnectionFactory.createAudioTrack("audio_track_$userId", audioSource)
        Log.d("WebRTCClient", "Audio track created")
    }

    fun createPeerConnection(remoteUserId: String): PeerConnection? {
        if (peerConnections.containsKey(remoteUserId)) {
            Log.d("WebRTCClient", "Peer connection already exists for $remoteUserId")
            return peerConnections[remoteUserId]
        }

        Log.d("WebRTCClient", "Creating peer connection for $remoteUserId")

        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
            keyType = PeerConnection.KeyType.ECDSA
        }

        val pc = peerConnectionFactory.createPeerConnection(rtcConfig, this) ?: run {
            Log.e("WebRTCClient", "Failed to create peer connection for $remoteUserId")
            return null
        }

        // Add local audio track
        pc.addTrack(localAudioTrack)
        peerConnections[remoteUserId] = pc
        Log.d("WebRTCClient", "Created peer connection for $remoteUserId")
        return pc
    }

    fun createOffer(remoteUserId: String) {
        Log.d("WebRTCClient", "Creating offer for $remoteUserId")

        val pc = peerConnections[remoteUserId] ?: run {
            Log.e("WebRTCClient", "No peer connection for $remoteUserId")
            return
        }

        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
        }

        pc.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription) {
                Log.d("WebRTCClient", "Offer created for $remoteUserId")

                pc.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        Log.d("WebRTCClient", "Set local description successfully for offer to $remoteUserId")
                    }

                    override fun onSetFailure(error: String) {
                        Log.e("WebRTCClient", "Set local description failed for offer: $error")
                    }
                    override fun onCreateSuccess(desc: SessionDescription?) {}
                    override fun onCreateFailure(error: String?) {}
                }, desc)

                // Create signal message
                val message = JSONObject().apply {
                    put("type", "offer")
                    put("sdp", desc.description)
                    put("from", userId)
                }
                listener.onSendSignal(remoteUserId, message)
                Log.d("WebRTCClient", "Sending offer to $remoteUserId")
            }

            override fun onSetSuccess() {}
            override fun onSetFailure(error: String) {
                Log.e("WebRTCClient", "Create offer failed: $error")
            }
            override fun onCreateFailure(error: String) {
                Log.e("WebRTCClient", "Create offer failed: $error")
            }
        }, constraints)
    }

    fun setRemoteDescription(remoteUserId: String, sdp: String, type: String) {
        Log.d("WebRTCClient", "Setting remote description for $remoteUserId, type: $type")

        val pc = peerConnections[remoteUserId] ?: run {
            Log.e("WebRTCClient", "No peer connection for $remoteUserId")
            return
        }

        val description = SessionDescription(
            when (type) {
                "offer" -> SessionDescription.Type.OFFER
                "answer" -> SessionDescription.Type.ANSWER
                else -> SessionDescription.Type.OFFER
            },
            sdp
        )

        pc.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                Log.d("WebRTCClient", "Set remote description successfully for $remoteUserId")
                if (type == "offer") {
                    Log.d("WebRTCClient", "Creating answer for $remoteUserId")
                    createAnswer(remoteUserId)
                }
            }

            override fun onSetFailure(error: String) {
                Log.e("WebRTCClient", "Set remote description failed: $error")
            }
            override fun onCreateSuccess(desc: SessionDescription?) {}
            override fun onCreateFailure(error: String?) {}
        }, description)
    }

    fun createAnswer(remoteUserId: String) {
        Log.d("WebRTCClient", "Creating answer for $remoteUserId")

        val pc = peerConnections[remoteUserId] ?: run {
            Log.e("WebRTCClient", "No peer connection for $remoteUserId")
            return
        }

        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
        }

        pc.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription) {
                Log.d("WebRTCClient", "Answer created for $remoteUserId")

                pc.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        Log.d("WebRTCClient", "Set local description successfully for answer to $remoteUserId")
                    }

                    override fun onSetFailure(error: String) {
                        Log.e("WebRTCClient", "Set local description failed for answer: $error")
                    }
                    override fun onCreateSuccess(desc: SessionDescription?) {}
                    override fun onCreateFailure(error: String?) {}
                }, desc)

                // Create signal message
                val message = JSONObject().apply {
                    put("type", "answer")
                    put("sdp", desc.description)
                    put("from", userId)
                }
                listener.onSendSignal(remoteUserId, message)
                Log.d("WebRTCClient", "Sending answer to $remoteUserId")
            }

            override fun onSetSuccess() {}
            override fun onSetFailure(error: String) {}
            override fun onCreateFailure(error: String) {
                Log.e("WebRTCClient", "Create answer failed: $error")
            }
        }, constraints)
    }

    fun addIceCandidate(remoteUserId: String, sdpMid: String, sdpMLineIndex: Int, candidate: String) {
        Log.d("WebRTCClient", "Adding ICE candidate for $remoteUserId: $candidate")

        val pc = peerConnections[remoteUserId] ?: run {
            Log.e("WebRTCClient", "No peer connection for $remoteUserId")
            return
        }

        val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex, candidate)
        pc.addIceCandidate(iceCandidate)
    }

    fun toggleAudio(enable: Boolean) {
        Log.d("WebRTCClient", "Toggling audio: $enable")
        localAudioTrack.setEnabled(enable)
    }

    fun disconnect() {
        Log.d("WebRTCClient", "Disconnecting all peer connections")
        peerConnections.values.forEach {
            it.close()
            Log.d("WebRTCClient", "Closed peer connection")
        }
        peerConnections.clear()
        remoteStreams.clear()
    }

    // PeerConnection.Observer methods
    override fun onIceCandidate(candidate: IceCandidate) {
        Log.d("WebRTCClient", "onIceCandidate: ${candidate.sdp}")

        val iceCandidate = JSONObject().apply {
            put("type", "ice-candidate")
            put("candidate", candidate.sdp)
            put("sdpMid", candidate.sdpMid)
            put("sdpMLineIndex", candidate.sdpMLineIndex)
            put("from", userId)
        }

        peerConnections.keys.forEach { remoteUserId ->
            Log.d("WebRTCClient", "Sending ICE candidate to $remoteUserId")
            listener.onSendSignal(remoteUserId, iceCandidate)
        }
    }

    override fun onAddStream(stream: MediaStream) {
        Log.d("WebRTCClient", "onAddStream: ${stream.id}")
        listener.onAddRemoteStream(stream, stream.id)
    }

    override fun onRemoveStream(stream: MediaStream) {
        Log.d("WebRTCClient", "onRemoveStream: ${stream.id}")
        listener.onRemoveRemoteStream(stream.id)
    }

    // Other required overrides
    override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>?) = Unit
    override fun onSignalingChange(state: PeerConnection.SignalingState?) = Unit
    override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
        Log.d("WebRTCClient", "onIceConnectionChange: $state")
    }
    override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
    override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
        Log.d("WebRTCClient", "onIceGatheringChange: $state")
    }
    override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<MediaStream>?) = Unit
    override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
        Log.d("WebRTCClient", "onConnectionChange: $newState")
    }
    override fun onRenegotiationNeeded() = Unit
    override fun onDataChannel(dataChannel: DataChannel?) = Unit
    override fun onTrack(transceiver: RtpTransceiver?) = Unit

    interface Listener {
        fun onAddRemoteStream(stream: MediaStream, userId: String)
        fun onRemoveRemoteStream(userId: String)
        fun onPeerConnected(userId: String)
        fun onPeerDisconnected(userId: String)
        fun onSendSignal(to: String, message: JSONObject)
    }
}