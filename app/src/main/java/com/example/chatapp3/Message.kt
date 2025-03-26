package com.example.chatapp3

data class Message(
    val id: Int,
    val sender: String,
    var content: String,
    val timestamp: String,
    var edited: Boolean = false,
    var deleted: Boolean = false,
    var reaction: String? = null, // Reaksiya uchun
    var replyToId: Int? = null    // Javob berilgan xabar IDsi uchun
)