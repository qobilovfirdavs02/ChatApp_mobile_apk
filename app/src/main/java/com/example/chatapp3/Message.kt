package com.example.chatapp3

data class Message(
    val id: Int,
    val sender: String,
    val content: String,
    val timestamp: String,
    val edited: Boolean,
    var reaction: String? = null, // Reaksiya uchun
    var replyToId: Int? = null    // Javob berilgan xabar IDsi uchun
)