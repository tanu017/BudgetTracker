package com.example.budgettracker.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a payment reminder or task notification.
 */
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String, // Short name for the reminder (e.g., Electricity Bill)

    val description: String, // Detailed information about the reminder

    val dueDate: Long, // Date and time when the reminder is due (in milliseconds)

    val isCompleted: Boolean = false // Status to track if the task/payment is done
)
