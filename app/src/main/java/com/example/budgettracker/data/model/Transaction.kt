package com.example.budgettracker.data.model

/**
 * Domain model representing a Transaction.
 */
data class Transaction(
    val id: String,
    val amount: Double,
    val type: String, // INCOME / EXPENSE
    val category: String,
    val accountId: String,
    val accountName: String,
    val timestamp: Long
)
