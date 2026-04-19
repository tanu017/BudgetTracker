package com.example.budgettracker.parser

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
        fun parse(input: String): ChatIntent {
            val text = input.lowercase().trim()

            // 1. Handle Greetings
            val greetings = listOf("hi", "hello", "hey", "hey there")
            if (greetings.any { text == it || text.startsWith("$it ") }) {
                return ChatIntent.Greeting
            }

            val timestamp = extractDate(text)

            // 2. Handle ADD_INCOME (earned, received, salary)
            val incomeRegex = Pattern.compile("(?:earned|received|got salary|income|salary)\\s+(\\d+(?:\\.\\d+)?)")
            val incomeMatcher = incomeRegex.matcher(text)
            if (incomeMatcher.find()) {
                val amount = incomeMatcher.group(1)?.toDoubleOrNull() ?: 0.0
                val catRegex = Pattern.compile("(?:from|as)\\s+([a-z]+)")
                val catMatcher = catRegex.matcher(text)
                val category = if (catMatcher.find()) catMatcher.group(1).replaceFirstChar { it.uppercase() } else "Salary"
                return ChatIntent.AddIncome(amount, category, timestamp)
            }

            // 3. Handle ADD_EXPENSE (spend, spent, add expense)
            val addRegex = Pattern.compile("(?:spend|add expense|spent)\\s+(\\d+(?:\\.\\d+)?)\\s+(?:on|for)\\s+([a-z\\s]+?)(?:\\s+on\\s+.*|$)")
            val addMatcher = addRegex.matcher(text)
            if (addMatcher.find()) {
                val amount = addMatcher.group(1)?.toDoubleOrNull() ?: 0.0
                val category = addMatcher.group(2)?.trim()?.replaceFirstChar { it.uppercase() } ?: "General"
                return ChatIntent.AddExpense(amount, category, timestamp)
            }

            // 4. Handle QUERIES (Income vs Expense + Time Range)
            if (text.contains("how much") || text.contains("total") || text.contains("summary")) {
                val type = if (text.contains("earn") || text.contains("income") || text.contains("received")) "INCOME" else "EXPENSE"
                
                // Time Range Detection
                val (range, label) = detectTimeRange(text)
                
                val categoryRegex = Pattern.compile("(?:on|for|from)\\s+([a-z]+)")
                val catMatcher = categoryRegex.matcher(text)
                val category = if (catMatcher.find()) {
                    val found = catMatcher.group(1)
                    // Ensure the found "category" isn't actually a month or time keyword
                    if (isTimeKeyword(found)) null else found.replaceFirstChar { it.uppercase() }
                } else null

                return ChatIntent.GetSummary(category, type, range.first, range.second, label)
            }

            return ChatIntent.Unknown
        }

        private fun detectTimeRange(text: String): Pair<Pair<Long, Long>, String> {
            return when {
                text.contains("today") -> ChatDateUtils.getTodayRange() to "today"
                text.contains("yesterday") -> ChatDateUtils.getYesterdayRange() to "yesterday"
                text.contains("last month") -> ChatDateUtils.getLastMonthRange() to "last month"
                text.contains("this month") -> ChatDateUtils.getThisMonthRange() to "this month"
                text.contains("last year") -> ChatDateUtils.getLastYearRange() to "last year"
                text.contains("this year") -> ChatDateUtils.getThisYearRange() to "this year"
                
                // Specific Months
                text.contains("january") || text.contains("jan") -> ChatDateUtils.getMonthRange("january") to "in January"
                text.contains("february") || text.contains("feb") -> ChatDateUtils.getMonthRange("february") to "in February"
                text.contains("march") || text.contains("mar") -> ChatDateUtils.getMonthRange("march") to "in March"
                text.contains("april") || text.contains("apr") -> ChatDateUtils.getMonthRange("april") to "in April"
                text.contains("may") -> ChatDateUtils.getMonthRange("may") to "in May"
                text.contains("june") || text.contains("jun") -> ChatDateUtils.getMonthRange("june") to "in June"
                text.contains("july") || text.contains("jul") -> ChatDateUtils.getMonthRange("july") to "in July"
                text.contains("august") || text.contains("aug") -> ChatDateUtils.getMonthRange("august") to "in August"
                text.contains("september") || text.contains("sep") -> ChatDateUtils.getMonthRange("september") to "in September"
                text.contains("october") || text.contains("oct") -> ChatDateUtils.getMonthRange("october") to "in October"
                text.contains("november") || text.contains("nov") -> ChatDateUtils.getMonthRange("november") to "in November"
                text.contains("december") || text.contains("dec") -> ChatDateUtils.getMonthRange("december") to "in December"

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
