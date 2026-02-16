package com.example.budgettracker.ui.transactions.utils

import java.text.SimpleDateFormat
import java.util.*

object TransactionDateUtils {
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatHeaderDate(startOfDayTimestamp: Long): String {
        val today = startOfDay(System.currentTimeMillis())
        val yesterday = today - (24 * 60 * 60 * 1000)
        return when (startOfDayTimestamp) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(startOfDayTimestamp))
        }
    }

    fun startOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
