package com.example.budgettracker.parser

import com.example.budgettracker.data.local.entities.TransactionEntity

/**
 * Data model for the parsed result.
 */
data class ParsedTransaction(
    val amount: Double,
    val type: String,
    val category: String,
    val merchant: String?,
    val timestamp: Long
)

/**
 * Extension function to convert parsed data into a Database Entity.
 */
fun ParsedTransaction.toTransactionEntity(): TransactionEntity {
    return TransactionEntity(
        amount = this.amount,
        type = this.type,
        category = this.category,
        accountName = "Primary Bank", // Default for parsed emails
        source = "EMAIL",
        timestamp = this.timestamp
    )
}

object EmailParser {
    // Regex Patterns
    // 1. Amount: Matches ₹100, Rs. 500, INR 2000 (handles decimals and commas)
    private val amountRegex = Regex("(?i)(?:rs\\.?|inr|₹)\\s*([\\d,]+\\.?\\d*)")
    
    // 2. Type: Identifies if money went out or came in
    private val expenseKeywords = listOf("debited", "spent", "paid", "sent", "transaction at")
    private val incomeKeywords = listOf("credited", "received", "added")

    // 3. Merchant: Looks for words after 'at', 'to', or 'from'
    private val merchantRegex = Regex("(?i)(?:at|to|from)\\s+([a-zA-Z0-9\\s]{3,15})")

    fun parseEmail(content: String): ParsedTransaction? {
        if (content.isBlank()) return null

        try {
            // Find Amount
            val amountMatch = amountRegex.find(content)
            val amount = amountMatch?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: return null

            // Determine Type
            val lowerContent = content.lowercase()
            val type = when {
                expenseKeywords.any { lowerContent.contains(it) } -> "EXPENSE"
                incomeKeywords.any { lowerContent.contains(it) } -> "INCOME"
                else -> "EXPENSE" // Default to expense for safety
            }

            // Extract Merchant
            val merchantMatch = merchantRegex.find(content)
            val merchant = merchantMatch?.groupValues?.get(1)?.trim()
            
            // Detect Category using smart classifier
            val category = CategoryClassifier.detectCategory(merchant)

            return ParsedTransaction(
                amount = amount,
                type = type,
                category = category,
                merchant = merchant,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            return null
        }
    }
}
