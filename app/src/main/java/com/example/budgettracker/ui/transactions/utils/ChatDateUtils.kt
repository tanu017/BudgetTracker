package com.example.budgettracker.ui.transactions.utils

import java.util.*

/**
 * Utility for calculating date ranges for chatbot queries.
 */
object ChatDateUtils {

    fun getTodayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val end = cal.timeInMillis
        return Pair(start, end)
    }

    fun getYesterdayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val end = cal.timeInMillis
        return Pair(start, end)
    }

    fun getThisMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        val end = cal.timeInMillis
        return Pair(start, end)
    }

    fun getLastMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        val end = cal.timeInMillis
        return Pair(start, end)
    }

    fun getThisYearRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.MONTH, Calendar.DECEMBER)
        cal.set(Calendar.DAY_OF_MONTH, 31)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        val end = cal.timeInMillis
        return Pair(start, end)
    }

    fun getLastYearRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.YEAR, -1)
        cal.set(Calendar.MONTH, Calendar.JANUARY)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.MONTH, Calendar.DECEMBER)
        cal.set(Calendar.DAY_OF_MONTH, 31)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        val end = cal.timeInMillis
        return Pair(start, end)
    }

    fun getMonthRange(monthName: String): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val month = when (monthName.lowercase()) {
            "january", "jan" -> Calendar.JANUARY
            "february", "feb" -> Calendar.FEBRUARY
            "march", "mar" -> Calendar.MARCH
            "april", "apr" -> Calendar.APRIL
            "may" -> Calendar.MAY
            "june", "jun" -> Calendar.JUNE
            "july", "jul" -> Calendar.JULY
            "august", "aug" -> Calendar.AUGUST
            "september", "sep" -> Calendar.SEPTEMBER
            "october", "oct" -> Calendar.OCTOBER
            "november", "nov" -> Calendar.NOVEMBER
            "december", "dec" -> Calendar.DECEMBER
            else -> cal.get(Calendar.MONTH)
        }
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        val end = cal.timeInMillis
        return Pair(start, end)
    }
}
