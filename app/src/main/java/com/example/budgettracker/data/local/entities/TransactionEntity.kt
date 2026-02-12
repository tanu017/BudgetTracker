package com.example.budgettracker.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single financial transaction.
 * Annotating with @Entity tells Room to create a table named 'transactions'.
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // Unique ID for each transaction, auto-incremented by the database

    val amount: Double, // The monetary value of the transaction
    // Stored as String for simplicity, could be converted to Enum in future improvements
    val type: String, // Type: "INCOME", "EXPENSE", or "TRANSFER"

    val category: String, // Category name (e.g., Food, Salary, Travel)

    val accountName: String, // The account used (e.g., Bank, Cash)

    val source: String, // How it was recorded: "CASH", "EMAIL", or "MANUAL"

    val timestamp: Long // Date and time of transaction in milliseconds
)
