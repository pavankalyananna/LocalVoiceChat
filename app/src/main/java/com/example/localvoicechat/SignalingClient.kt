package com.example.localvoicechat

import android.content.Context
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.net.URISyntaxException

class SignalingClient(
    context: Context,
    private val webRTCClient: WebRTCClient,
    private val userId: String,
    private val isHost: Boolean
) {
    private var socket: Socket? = null
    private val appContext = context.applicationContext

    fun startSignalingServer() {
        if (!isHost) return
        Log.d("SignalingClient", "Host started signaling server")
    }

    fun connectToSignalingServer(hostIp: String) {
        try {
            val options = IO.Options()
            options.forceNew = true
            socket = IO.socket("http://$hostIp:3000", options)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("SignalingClient", "Connected to signaling server")
                joinRoom()
            }

            socket?.on("message") { args ->
                val data = args[0] as JSONObject
                handleMessage(data)
            }

            socket?.connect()
        } catch (e: URISyntaxException) {
            Log.e("SignalingClient", "Invalid URI", e)
        }
    }

    private fun joinRoom() {
        socket?.emit("join", userId)
    }

    private fun handleMessage(data: JSONObject) {
        when (data.getString("type")) {
            "offer" -> handleOffer(data)
            // Add placeholder implementations
            "answer" -> Log.d("SignalingClient", "Received answer")
            "ice-candidate" -> Log.d("SignalingClient", "Received ICE candidate")
            "peer-joined" -> Log.d("SignalingClient", "Peer joined")
            "peer-left" -> Log.d("SignalingClient", "Peer left")
        }
    }

    private fun handleOffer(data: JSONObject) {
        val sdp = data.getString("sdp")
        val remoteUserId = data.getString("from")
        val description = SessionDescription(SessionDescription.Type.OFFER, sdp)

        // Create peer connection
        webRTCClient.createPeerConnection(remoteUserId)?.setRemoteDescription(
            object : SimpleSdpObserver() {
                override fun onSetSuccess() {
                    // Create answer
                    webRTCClient.createOffer(remoteUserId)
                }
            },
            description
        )
    }

    fun disconnect() {
        socket?.disconnect()
    }
}

open class SimpleSdpObserver : org.webrtc.SdpObserver {
    override fun onCreateSuccess(desc: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) {}
    override fun onSetFailure(error: String?) {}
}