package com.example.budgettracker.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity to persist chat messages.
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val message: String,
    val isUser: Boolean,
    val timestamp: Long
)
