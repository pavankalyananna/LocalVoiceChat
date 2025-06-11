package com.example.localvoicechat

data class Participant(
    val name: String,
    val isHost: Boolean = false,
    var isSpeaking: Boolean = false, // Changed to var
    var isMuted: Boolean = false     // Changed to var
)