package com.example.budgettracker.data.model

data class Account(
    val id: String,
    val name: String,
    val balance: Double,
    val type: String // CASH / BANK
)
