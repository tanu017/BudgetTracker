package com.example.budgettracker.ui.transactions.model

data class BudgetHealthMetrics(
    val score: Int,
    val savingsRatio: Float,
    val growthRate: Float,
    val predictedSpend: Double,
    val healthStatus: String
)
