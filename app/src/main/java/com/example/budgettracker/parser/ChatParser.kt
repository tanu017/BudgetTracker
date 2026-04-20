package com.example.budgettracker.parser

import android.util.Log
import com.example.budgettracker.ui.transactions.utils.ChatDateUtils
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

/**
 * ChatIntent defines the actions the bot can perform.
 */
sealed class ChatIntent {
    data class AddExpense(val amount: Double, val category: String, val timestamp: Long) : ChatIntent()
    data class AddIncome(val amount: Double, val category: String, val timestamp: Long) : ChatIntent()
    data class GetSummary(
        val category: String?, 
        val type: String,
        val startDate: Long,
        val endDate: Long,
        val timeLabel: String
    ) : ChatIntent()
    object Greeting : ChatIntent()
    object Unknown : ChatIntent()
}

/**
 * Parser class for handling natural language inputs for budget tracking.
 */
class ChatParser {
    companion object {
        private const val TAG = "ChatParser"

        fun parse(input: String): ChatIntent {
            val normalized = normalizeInput(input)
            Log.d(TAG, "Normalized input: $normalized")

            if (isGreeting(normalized)) return ChatIntent.Greeting

            val timestamp = extractDate(input.lowercase())
            val amount = extractAmount(normalized)

            var cleanText = normalized
            val noiseWords = listOf("today", "yesterday", "tomorrow", "earlier", "now", "tonight", "already", "morning", "evening")
            noiseWords.forEach { cleanText = cleanText.replace(Regex("\\b$it\\b"), "") }
            
            cleanText = cleanText.replace(Regex("\\bon\\s+\\d+(?:st|nd|rd|th)?\\s+[a-z]+(?:\\s+\\d{4})?"), "")
            cleanText = cleanText.replace(Regex("\\s+"), " ").trim()

            val isIncome = listOf("earned", "received", "income", "salary", "got salary").any { normalized.contains(it) }
            val isExpense = listOf("spent", "spend", "paid", "pay", "buying", "bought", "add expense").any { normalized.contains(it) }

            if (amount != null) {
                if (isExpense) {
                    val category = extractCategory(cleanText, amount.toString(), listOf("on", "for"))
                    return ChatIntent.AddExpense(amount, category.ifEmpty { "General" }, timestamp)
                } else if (isIncome) {
                    val category = extractCategory(cleanText, amount.toString(), listOf("from", "as", "salary"))
                    return ChatIntent.AddIncome(amount, category.ifEmpty { "Salary" }, timestamp)
                }
            }

            if (normalized.contains("how much") || normalized.contains("total") || normalized.contains("summary")) {
                val type = if (normalized.contains("earn") || normalized.contains("income") || normalized.contains("received")) "INCOME" else "EXPENSE"
                val (range, label) = detectTimeRange(normalized)
                val category = extractCategory(cleanText, "", listOf("on", "for", "from")).ifEmpty { null }
                
                val finalCategory = if (category != null && isTimeKeyword(category.lowercase())) null else category
                return ChatIntent.GetSummary(finalCategory, type, range.first, range.second, label)
            }

            return ChatIntent.Unknown
        }

        private fun normalizeInput(input: String): String {
            return input.lowercase()
                .replace(Regex("[₹]|\\brs\\b|\\brupees\\b"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        private fun isGreeting(text: String): Boolean {
            val greetings = listOf("hi", "hello", "hey", "hey there")
            return greetings.any { text == it || text.startsWith("$it ") }
        }

        private fun extractAmount(text: String): Double? {
            val match = Regex("""(\d+(?:\.\d+)?)""").find(text)
            return match?.value?.toDoubleOrNull()
        }

        private fun extractCategory(text: String, amountStr: String, separators: List<String>): String {
            for (sep in separators) {
                if (text.contains(" $sep ")) {
                    val parts = text.split(" $sep ")
                    if (parts.size > 1) {
                        return cleanCategory(parts.last())
                    }
                }
            }
            if (amountStr.isNotEmpty()) {
                val parts = text.split(amountStr)
                if (parts.size > 1) {
                    return cleanCategory(parts.last())
                }
            }
            return ""
        }

        /**
         * Cleans category strings by removing noise words and formatting to Title Case.
         * Implementation for STEP 5.
         */
        fun cleanCategory(input: String): String {
            var cleaned = input.trim().lowercase()
            
            // 1. Remove noise/time words
            val noiseWords = listOf("today", "yesterday", "tomorrow", "earlier", "now", "tonight", "already", "morning", "evening")
            noiseWords.forEach { word ->
                cleaned = cleaned.replace(Regex("\\b$word\\b"), "")
            }

            // 2. Remove digits
            cleaned = cleaned.replace(Regex("\\d+"), "")

            // 3. Remove multiple spaces
            cleaned = cleaned.replace(Regex("\\s+"), " ").trim()

            if (cleaned.isEmpty()) return ""

            // 4. Convert to Clean Title Case (e.g., "ice cream" -> "Ice Cream")
            return cleaned.split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { word ->
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                }
        }

        private fun detectTimeRange(text: String): Pair<Pair<Long, Long>, String> {
            return when {
                text.contains("today") -> ChatDateUtils.getTodayRange() to "today"
                text.contains("yesterday") -> ChatDateUtils.getYesterdayRange() to "yesterday"
                text.contains("last month") -> ChatDateUtils.getLastMonthRange() to "last month"
                text.contains("this month") -> ChatDateUtils.getThisMonthRange() to "this month"
                text.contains("last year") -> ChatDateUtils.getLastYearRange() to "last year"
                text.contains("this year") -> ChatDateUtils.getThisYearRange() to "this year"
                else -> ChatDateUtils.getThisMonthRange() to "this month"
            }
        }

        private fun isTimeKeyword(word: String): Boolean {
            val keywords = listOf("today", "yesterday", "month", "year", "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
            return keywords.any { word.contains(it) }
        }

        private fun extractDate(text: String): Long {
            val calendar = Calendar.getInstance()
            if (text.contains("today")) return calendar.timeInMillis
            if (text.contains("yesterday")) {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                return calendar.timeInMillis
            }
            val cleanedText = text.replace(Regex("(\\d+)(st|nd|rd|th)"), "$1")
            val dateRegex = Pattern.compile("(\\d{1,2}\\s+[a-z]+\\s+\\d{4})|([a-z]+\\s+\\d{1,2}\\s+\\d{4})|(\\d{1,2}/\\d{1,2}/\\d{4})")
            val dateMatcher = dateRegex.matcher(cleanedText)
            if (dateMatcher.find()) {
                val dateStr = dateMatcher.group(0)
                val formats = listOf("dd MMMM yyyy", "MMMM dd yyyy", "dd/MM/yyyy")
                for (format in formats) {
                    try {
                        val sdf = SimpleDateFormat(format, Locale.ENGLISH)
                        return sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) { continue }
                }
            }
            return System.currentTimeMillis()
        }
    }
}
