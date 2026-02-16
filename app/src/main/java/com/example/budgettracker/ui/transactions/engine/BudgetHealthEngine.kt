package com.example.budgettracker.ui.transactions.engine

import com.example.budgettracker.data.local.entities.TransactionEntity
import com.example.budgettracker.ui.transactions.model.BudgetHealthMetrics
import java.util.*

object BudgetHealthEngine {
    fun compute(transactions: List<TransactionEntity>): BudgetHealthMetrics {
        if (transactions.isEmpty()) return BudgetHealthMetrics(0, 0f, 0f, 0.0, "No Data")

        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        val thisMonth = transactions.filter {
            val tc = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            tc.get(Calendar.MONTH) == currentMonth && tc.get(Calendar.YEAR) == currentYear
        }

        val income = thisMonth.filter { it.type == "INCOME" }.sumOf { it.amount }
        val expense = thisMonth.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        val savingsRatio = if (income > 0) ((income - expense) / income).toFloat().coerceIn(0f, 1f) else 0f
        val dailyAvg = if (currentDay > 0) expense / currentDay else 0.0
        val predictedSpend = dailyAvg * daysInMonth

        val prevMonth = if (currentMonth == 0) 11 else currentMonth - 1
        val prevYear = if (currentMonth == 0) currentYear - 1 else currentYear
        val prevExpense = transactions.filter {
            val tc = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            it.type == "EXPENSE" && tc.get(Calendar.MONTH) == prevMonth && tc.get(Calendar.YEAR) == prevYear
        }.sumOf { it.amount }

        val growth = if (prevExpense > 0) ((expense - prevExpense) / prevExpense).toFloat() else 0f

        var score = 50
        score += (savingsRatio * 40).toInt()
        if (growth < 0) score += 10
        if (expense > income && income > 0) score -= 30
        
        return BudgetHealthMetrics(
            score = score.coerceIn(0, 100),
            savingsRatio = savingsRatio,
            growthRate = growth,
            predictedSpend = predictedSpend,
            healthStatus = when {
                score > 80 -> "Excellent"
                score > 60 -> "Good"
                score > 40 -> "Risk"
                else -> "Critical"
            }
        )
    }
}
