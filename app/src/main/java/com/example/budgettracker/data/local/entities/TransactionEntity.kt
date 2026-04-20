package com.example.budgettracker.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a single financial transaction.
 * Annotating with @Entity tells Room to create a table named 'transactions'.
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(), // STEP 2 — Standardized to non-null unique String ID

    val amount: Double, // The monetary value of the transaction
    
    val type: String, // Type: "INCOME", "EXPENSE", or "TRANSFER"

    val category: String, // Category name (e.g., Food, Salary, Travel)

    val accountId: String, // Link to AccountEntity.id

    val accountName: String, // The account name (denormalized for convenience)

    val source: String, // How it was recorded: "CASH", "EMAIL", "MANUAL", or "TRANSFER"

    val timestamp: Long, // Date and time of transaction in milliseconds
    
    val relatedAccountName: String? = null, // For TRANSFER type: the name of the destination/source account
    
    val transferDirection: String? = null // For TRANSFER: "IN" or "OUT" to track accounting direction
)
