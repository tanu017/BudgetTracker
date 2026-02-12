package com.example.budgettracker.repository

import com.example.budgettracker.data.local.dao.ReminderDao
import com.example.budgettracker.data.local.entities.ReminderEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing reminders and payment alerts.
 */
class ReminderRepository(private val reminderDao: ReminderDao) {

    // Returns all reminders as an observable flow
    fun getAllReminders(): Flow<List<ReminderEntity>> = reminderDao.getAllReminders()

    // Schedules or saves a new reminder
    suspend fun insertReminder(reminder: ReminderEntity) {
        reminderDao.insertReminder(reminder)
    }

    // Updates a reminder (e.g., marks it as completed)
    suspend fun updateReminder(reminder: ReminderEntity) {
        reminderDao.updateReminder(reminder)
    }
}
