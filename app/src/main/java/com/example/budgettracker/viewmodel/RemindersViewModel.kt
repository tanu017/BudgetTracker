package com.example.budgettracker.viewmodel

import androidx.lifecycle.*
import com.example.budgettracker.data.local.entities.ReminderEntity
import com.example.budgettracker.repository.ReminderRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for Reminders.
 * Handles the display and scheduling of payment alerts.
 */
class RemindersViewModel(private val repository: ReminderRepository) : ViewModel() {

    // List of all reminders observed as LiveData
    val allReminders: LiveData<List<ReminderEntity>> = repository.getAllReminders().asLiveData()

    /**
     * Adds a new bill or task reminder.
     */
    fun insertReminder(reminder: ReminderEntity) = viewModelScope.launch {
        repository.insertReminder(reminder)
    }

    /**
     * Updates an existing reminder status or details.
     */
    fun updateReminder(reminder: ReminderEntity) = viewModelScope.launch {
        repository.updateReminder(reminder)
    }
}
