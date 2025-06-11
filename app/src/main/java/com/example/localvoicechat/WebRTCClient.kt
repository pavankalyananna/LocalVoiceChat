package com.example.localvoicechat

import android.content.Context
import android.util.Log
import org.webrtc.*

class WebRTCClient(
    context: Context,
    private val listener: Listener,
    private val userId: String
) : PeerConnection.Observer {

    private val eglBase = EglBase.create()
    private val peerConnectionFactory: PeerConnectionFactory
    private val audioSource: AudioSource
    private val localAudioTrack: AudioTrack
    private val peerConnections = mutableMapOf<String, PeerConnection>()
    private val remoteStreams = mutableMapOf<String, MediaStream>()

    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .createInitializationOptions()
        )

        val options = PeerConnectionFactory.Options()
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .createPeerConnectionFactory()

        // Create audio source and track
        val audioSourceConstraints = MediaConstraints()
        audioSource = peerConnectionFactory.createAudioSource(audioSourceConstraints)
        localAudioTrack = peerConnectionFactory.createAudioTrack("audio_track_$userId", audioSource)
    }

    fun createPeerConnection(remoteUserId: String): PeerConnection? {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN

        val pc = peerConnectionFactory.createPeerConnection(rtcConfig, this) ?: return null
        pc.addTrack(localAudioTrack)
        peerConnections[remoteUserId] = pc
        return pc
    }

    fun createOffer(remoteUserId: String) {
        val pc = peerConnections[remoteUserId] ?: return
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        pc.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription) {
                pc.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() = Unit
                    override fun onSetFailure(error: String) {
                        Log.e("WebRTCClient", "SetLocalDescription error: $error")
                    }
                    // Changed to use non-nullable SessionDescription
                    override fun onCreateSuccess(desc: SessionDescription) = Unit
                    override fun onCreateFailure(error: String) = Unit
                }, desc)
            }

            override fun onSetSuccess() = Unit
            override fun onSetFailure(error: String) = Unit
            override fun onCreateFailure(error: String) {
                Log.e("WebRTCClient", "CreateOffer error: $error")
            }
        }, constraints)
    }

    fun toggleAudio(enable: Boolean) {
        localAudioTrack.setEnabled(enable)
    }

    fun disconnect() {
        peerConnections.values.forEach { it.close() }
        peerConnections.clear()
        remoteStreams.clear()
    }

    // PeerConnection.Observer methods
    override fun onIceCandidate(candidate: IceCandidate) = Unit
    override fun onAddStream(stream: MediaStream) {
        listener.onAddRemoteStream(stream, stream.id)
    }
    override fun onRemoveStream(stream: MediaStream) {
        listener.onRemoveRemoteStream(stream.id)
    }
    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) = Unit
    override fun onSignalingChange(state: PeerConnection.SignalingState?) = Unit
    override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) = Unit
    override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
    override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) = Unit
    override fun onAddTrack(rtpReceiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) = Unit
    override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) = Unit
    override fun onRenegotiationNeeded() = Unit
    override fun onDataChannel(dataChannel: DataChannel?) = Unit
    override fun onTrack(transceiver: RtpTransceiver?) = Unit

    interface Listener {
        fun onAddRemoteStream(stream: MediaStream, userId: String)
        fun onRemoveRemoteStream(userId: String)
        fun onPeerConnected(userId: String)
        fun onPeerDisconnected(userId: String)
    }
}