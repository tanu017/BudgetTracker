package com.example.budgettracker.ui.chatbot.model

/**
 * Represents a single message in the chat.
 * @param message The text content
 * @param isUser True if sent by user, false if sent by bot
 * @param timestamp Time message was sent
 */
data class ChatMessage(
    val message: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
