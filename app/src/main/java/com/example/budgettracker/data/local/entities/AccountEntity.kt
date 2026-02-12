package com.example.budgettracker.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a financial account (e.g., Bank Account, Cash, or Digital Wallet).
 */
// Represents user's money storage sources
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val accountName: String, // Name of the account (e.g., HDFC Bank, My Wallet)

    val balance: Double, // Current available balance in the account

    val accountType: String // Type: "BANK", "CASH", or "WALLET"
)
