package com.example.budgettracker.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a financial account.
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val accountName: String, // Name of the account (e.g., HDFC Bank, My Wallet)

    val accountType: String, // Type: "BANK", "CASH", or "WALLET"
    
    val balance: Double = 0.0 // Current balance of the account
)
