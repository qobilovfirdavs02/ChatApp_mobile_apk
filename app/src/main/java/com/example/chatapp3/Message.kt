package com.example.chatapp3

data class Message(
    val id: Int,
    val sender: String,
    val content: String,
    val timestamp: String,
    val edited: Boolean
)