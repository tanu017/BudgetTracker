package com.example.budgettracker.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.data.local.entities.ChatMessageEntity
import com.example.budgettracker.data.local.entities.TransactionEntity
import com.example.budgettracker.parser.ChatIntent
import com.example.budgettracker.parser.ChatParser
import com.example.budgettracker.repository.ChatRepository
import com.example.budgettracker.repository.TransactionRepository
import com.example.budgettracker.ui.chatbot.model.ChatMessage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ChatViewModel handles the state and logic for the Budget Bot.
 * Now persists messages in the Room database via ChatRepository.
 */
class ChatViewModel(
    private val transactionRepository: TransactionRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    // Internal list for immediate UI updates
    val messages = mutableStateListOf<ChatMessage>()

    init {
        // Load messages from database when ViewModel is created
        viewModelScope.launch {
            chatRepository.getMessages().collectLatest { entities ->
                messages.clear()
                if (entities.isEmpty()) {
                    // Initial greeting if history is empty
                    val greeting = "Hello! I'm your Budget Bot. How can I help you today?"
                    saveAndDisplayBotMessage(greeting)
                } else {
                    messages.addAll(entities.map { it.toUiModel() })
                }
            }
        }
    }

    /**
     * Processes a user message and persists it.
     */
    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val timestamp = System.currentTimeMillis()
        
        viewModelScope.launch {
            // 1. Save user message to DB
            chatRepository.insertMessage(
                ChatMessageEntity(message = text, isUser = true, timestamp = timestamp)
            )

            // 2. Parse and handle the intent
            val intent = ChatParser.parse(text)
            handleIntent(intent)
        }
    }

    private suspend fun handleIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.Greeting -> {
                val replies = listOf(
                    "Hey! How can I help you today?",
                    "Hi there! Want to add an expense or check your balance?",
                    "Hello! You can ask me things like 'how much did I spend on food?'"
                )
                saveAndDisplayBotMessage(replies.random())
            }

            is ChatIntent.AddIncome -> {
                val entity = TransactionEntity(
                    amount = intent.amount,
                    category = intent.category,
                    type = "INCOME",
                    accountName = "Cash",
                    source = "CHATBOT",
                    timestamp = intent.timestamp
                )
                transactionRepository.insertTransaction(entity)
                val dateStr = formatTimestamp(intent.timestamp)
                saveAndDisplayBotMessage("✅ Added ₹${intent.amount} as ${intent.category} on $dateStr.")
            }

            is ChatIntent.AddExpense -> {
                val entity = TransactionEntity(
                    amount = intent.amount,
                    category = intent.category,
                    type = "EXPENSE",
                    accountName = "Cash",
                    source = "CHATBOT",
                    timestamp = intent.timestamp
                )
                transactionRepository.insertTransaction(entity)
                val dateStr = formatTimestamp(intent.timestamp)
                saveAndDisplayBotMessage("✅ Recorded ₹${intent.amount} for ${intent.category} on $dateStr.")
            }
            
            is ChatIntent.GetSummary -> {
                val total = if (intent.category != null) {
                    transactionRepository.getTotalByTypeCategoryAndDateRange(
                        intent.type, 
                        "%${intent.category}%",
                        intent.startDate,
                        intent.endDate
                    ) ?: 0.0
                } else {
                    transactionRepository.getTotalByTypeAndDateRange(
                        intent.type,
                        intent.startDate,
                        intent.endDate
                    ) ?: 0.0
                }
                
                val action = if (intent.type == "INCOME") "earned" else "spent"
                val categorySuffix = if (intent.category != null) " on ${intent.category}" else ""
                
                if (total == 0.0) {
                    saveAndDisplayBotMessage("No records found for that period.")
                } else {
                    saveAndDisplayBotMessage("You have $action a total of ₹$total$categorySuffix ${intent.timeLabel}.")
                }
            }
            
            is ChatIntent.Unknown -> {
                saveAndDisplayBotMessage(
                    "I didn't understand. Try:\n" +
                    "• 'spend 100 on food'\n" +
                    "• 'earned 5000'\n" +
                    "• 'how much did I spend last month?'"
                )
            }
        }
    }

    private suspend fun saveAndDisplayBotMessage(text: String) {
        chatRepository.insertMessage(
            ChatMessageEntity(message = text, isUser = false, timestamp = System.currentTimeMillis())
        )
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    fun clearChat() {
        viewModelScope.launch {
            chatRepository.clearChat()
        }
    }

    // Mapper extension
    private fun ChatMessageEntity.toUiModel() = ChatMessage(
        message = message,
        isUser = isUser,
        timestamp = timestamp
    )
}
