package com.example.localvoicechat

data class Participant(
    val name: String,
    val isHost: Boolean = false,
    var isSpeaking: Boolean = false,
    var isMuted: Boolean = false
)