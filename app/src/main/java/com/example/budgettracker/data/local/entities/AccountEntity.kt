package com.example.budgettracker.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a financial account metadata.
 * Balance is now derived from the transaction ledger.
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val accountName: String, // Name of the account (e.g., HDFC Bank, My Wallet)

    val accountType: String // Type: "BANK", "CASH", or "WALLET"
)
