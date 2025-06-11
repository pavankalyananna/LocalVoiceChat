package com.example.localvoicechat

import android.content.Context
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONArray
import org.json.JSONObject
import java.net.URISyntaxException

class SignalingClient(
    context: Context,
    private val webRTCClient: WebRTCClient,
    private val userId: String,
    private val isHost: Boolean,
    private val hostIp: String
) {
    private var socket: Socket? = null
    private val appContext = context.applicationContext

    fun connect() {
        try {
            Log.d("SignalingClient", "Connecting to signaling server at http://$hostIp:3000")

            val options = IO.Options().apply {
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
                randomizationFactor = 0.5
                transports = arrayOf("websocket")
            }

            socket = IO.socket("http://$hostIp:3000", options)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("SignalingClient", "Connected to signaling server")
                joinRoom()
            }

            socket?.on("peer-list") { args ->
                try {
                    Log.d("SignalingClient", "Received peer-list event")
                    val peers = args[0] as JSONArray
                    Log.d("SignalingClient", "Peer list: $peers")

                    for (i in 0 until peers.length()) {
                        val peerId = peers.getString(i)
                        if (peerId != userId) {
                            Log.d("SignalingClient", "Adding peer from list: $peerId")
                            webRTCClient.listener.onPeerConnected(peerId)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SignalingClient", "Error handling peer-list", e)
                }
            }

            socket?.on("new-peer") { args ->
                try {
                    Log.d("SignalingClient", "Received new-peer event")
                    val peerId = args[0] as String
                    Log.d("SignalingClient", "New peer: $peerId")

                    if (peerId != userId) {
                        Log.d("SignalingClient", "Adding new peer: $peerId")
                        webRTCClient.listener.onPeerConnected(peerId)
                    }
                } catch (e: Exception) {
                    Log.e("SignalingClient", "Error handling new-peer", e)
                }
            }

            socket?.on("peer-left") { args ->
                try {
                    Log.d("SignalingClient", "Received peer-left event")
                    val peerId = args[0] as String
                    Log.d("SignalingClient", "Peer left: $peerId")

                    webRTCClient.listener.onPeerDisconnected(peerId)
                } catch (e: Exception) {
                    Log.e("SignalingClient", "Error handling peer-left", e)
                }
            }

            socket?.on("signal") { args ->
                try {
                    Log.d("SignalingClient", "Received signal event")
                    val data = args[0] as JSONObject
                    Log.d("SignalingClient", "Signal data: $data")

                    handleSignal(data)
                } catch (e: Exception) {
                    Log.e("SignalingClient", "Error handling signal", e)
                }
            }

            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.d("SignalingClient", "Disconnected from signaling server")
            }

            // Use string literals for error events
            socket?.on("error") { args ->
                val error = args[0]?.toString() ?: "Unknown error"
                Log.e("SignalingClient", "Socket error: $error")
            }

            Log.d("SignalingClient", "Attempting to connect to signaling server")
            socket?.connect()
        } catch (e: URISyntaxException) {
            Log.e("SignalingClient", "Invalid URI", e)
        } catch (e: Exception) {
            Log.e("SignalingClient", "Connection error", e)
        }

        socket?.on("connect_error") { args ->
            Log.e("SignalingClient", "Connect error: ${args[0]}")
        }

        socket?.on("error") { args ->
            Log.e("SignalingClient", "Error: ${args[0]}")
        }

        socket?.on("reconnect_attempt") {
            Log.d("SignalingClient", "Reconnect attempt")
        }

        socket?.on("reconnect_error") { args ->
            Log.e("SignalingClient", "Reconnect error: ${args[0]}")
        }
    }

    private fun joinRoom() {
        Log.d("SignalingClient", "Joining room as $userId")
        socket?.emit("join", userId)
    }

    private fun handleSignal(data: JSONObject) {
        try {
            val type = data.getString("type")
            val from = data.getString("from")
            Log.d("SignalingClient", "Handling signal type: $type from: $from")

            when (type) {
                "offer" -> {
                    val sdp = data.getString("sdp")
                    Log.d("SignalingClient", "Received offer SDP: $sdp")
                    webRTCClient.setRemoteDescription(from, sdp, "offer")
                }
                "answer" -> {
                    val sdp = data.getString("sdp")
                    Log.d("SignalingClient", "Received answer SDP: $sdp")
                    webRTCClient.setRemoteDescription(from, sdp, "answer")
                }
                "ice-candidate" -> {
                    val candidate = data.getString("candidate")
                    val sdpMid = data.getString("sdpMid")
                    val sdpMLineIndex = data.getInt("sdpMLineIndex")
                    Log.d("SignalingClient", "Received ICE candidate: $candidate")
                    webRTCClient.addIceCandidate(from, sdpMid, sdpMLineIndex, candidate)
                }
            }
        } catch (e: Exception) {
            Log.e("SignalingClient", "Error handling signal", e)
        }
    }

    fun sendSignal(to: String, message: JSONObject) {
        Log.d("SignalingClient", "Sending signal to $to: ${message.toString()}")
        socket?.emit("signal", to, message)
    }

    fun disconnect() {
        Log.d("SignalingClient", "Disconnecting from signaling server")
        socket?.disconnect()
        socket = null
    }
}
