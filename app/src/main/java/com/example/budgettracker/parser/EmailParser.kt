package com.example.budgettracker.parser

import com.example.budgettracker.data.local.entities.TransactionEntity
import com.example.budgettracker.utils.Constants
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Data model for the parsed result.
 */
data class ParsedTransaction(
    val amount: Double,
    val type: String,
    val category: String,
    val merchant: String?,
    val timestamp: Long,
    val accountLast4: String? = null,
    val parsedDate: String? = null,
    val parsedTime: String? = null
)

/**
 * Extension function to convert parsed data into a Database Entity.
 */
fun ParsedTransaction.toTransactionEntity(): TransactionEntity {
    return TransactionEntity(
        amount = this.amount,
        type = this.type,
        category = this.category,
        accountId = Constants.DEFAULT_ACCOUNT_ID,
        accountName = Constants.DEFAULT_ACCOUNT_NAME,
        source = "EMAIL",
        timestamp = this.timestamp // Uses the parsed timestamp
    )
}

object EmailParser {
    
    // Regex Patterns
    private val amountRegex = Regex("(?i)(?:rs\\.?|inr|₹)\\s*([\\d,]+\\.?\\d*)")
    private val accountRegex = Regex("\\.\\.\\.(\\d{4})")
    private val dateTimeRegex = Regex("(\\d{2}-\\d{2}-\\d{4})\\s+(\\d{2}:\\d{2}:\\d{2})")
    
    // Merchant: Detect words after 'by', 'to', or 'at'
    private val merchantByRegex = Regex("(?i)by\\s+([a-zA-Z0-9\\s]{3,30})")
    private val merchantToRegex = Regex("(?i)to\\s+([a-zA-Z0-9\\s]{3,30})")
    private val merchantAtRegex = Regex("(?i)at\\s+([a-zA-Z0-9\\s]{3,30})")

    private val expenseKeywords = listOf("debited", "spent", "paid", "sent", "transaction at")
    private val incomeKeywords = listOf("credited", "received", "added")

    fun parseEmail(content: String): ParsedTransaction? {
        if (content.isBlank()) return null

        try {
            val amount = extractAmount(content) ?: return null
            val type = extractType(content)
            val account = extractAccount(content)
            val (date, time, timestamp) = extractDateTime(content)
            val merchant = extractMerchant(content, type)
            
            // Detect Category using smart classifier
            val category = CategoryClassifier.detectCategory(merchant)

            return ParsedTransaction(
                amount = amount,
                type = type,
                category = category,
                merchant = merchant,
                timestamp = timestamp,
                accountLast4 = account,
                parsedDate = date,
                parsedTime = time
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun extractAmount(content: String): Double? {
        return amountRegex.find(content)?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()
    }

    private fun extractType(content: String): String {
        val lowerContent = content.lowercase()
        return when {
            incomeKeywords.any { lowerContent.contains(it) } -> "INCOME"
            expenseKeywords.any { lowerContent.contains(it) } -> "EXPENSE"
            else -> "EXPENSE"
        }
    }

    private fun extractAccount(content: String): String? {
        return accountRegex.find(content)?.groupValues?.get(1)
    }

    private fun extractDateTime(content: String): Triple<String?, String?, Long> {
        val match = dateTimeRegex.find(content)
        if (match != null) {
            val dateStr = match.groupValues[1]
            val timeStr = match.groupValues[2]
            val fullDateTime = "$dateStr $timeStr"
            
            return try {
                val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
                val timestamp = sdf.parse(fullDateTime)?.time ?: System.currentTimeMillis()
                Triple(dateStr, timeStr, timestamp)
            } catch (e: Exception) {
                Triple(null, null, System.currentTimeMillis())
            }
        }
        return Triple(null, null, System.currentTimeMillis())
    }

    private fun extractMerchant(content: String, type: String): String? {
        // Try 'by' first (common in Indian bank SMS for credited by/spent at)
        var merchant = merchantByRegex.find(content)?.groupValues?.get(1)
        
        // If not found and it's an expense, try 'to' or 'at'
        if (merchant == null) {
            merchant = if (type == "EXPENSE") {
                merchantToRegex.find(content)?.groupValues?.get(1) ?: 
                merchantAtRegex.find(content)?.groupValues?.get(1)
            } else {
                null
            }
        }

        return merchant?.let { cleanMerchant(it) }
    }

    private fun cleanMerchant(merchant: String): String {
        return merchant
            .replace(Regex("(?i)UTR|NEFT|IMPS|RTGS|Ref|CR|A/c"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .uppercase()
    }
}
