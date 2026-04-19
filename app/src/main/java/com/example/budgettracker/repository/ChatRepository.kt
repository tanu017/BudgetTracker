package com.example.budgettracker.repository

import com.example.budgettracker.data.local.dao.ChatDao
import com.example.budgettracker.data.local.entities.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository to manage chat message data.
 */
class ChatRepository(private val chatDao: ChatDao) {

    /**
     * Returns a Flow of all persisted chat messages.
     */
    fun getMessages(): Flow<List<ChatMessageEntity>> = chatDao.getAllMessages()

    /**
     * Inserts a message into the database.
     */
    suspend fun insertMessage(message: ChatMessageEntity) {
        chatDao.insertMessage(message)
    }

    /**
     * Clears all chat history.
     */
    suspend fun clearChat() {
        chatDao.clearMessages()
    }
}
