package com.example.budgettracker.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a classification for transactions.
 */

// Type determines whether this category is for income or expense

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String, // Name of the category (e.g., Grocery, Rent, Bonus)

    val type: String // Associated type: "INCOME" or "EXPENSE"
)
