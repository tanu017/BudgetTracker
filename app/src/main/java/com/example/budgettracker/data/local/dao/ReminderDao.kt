package com.example.budgettracker.data.local.dao

import androidx.room.*
import com.example.budgettracker.data.local.entities.ReminderEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the reminders table.
 */
@Dao
interface ReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    /**
     * Retrieves all reminders sorted by due date.
     */
    @Query("SELECT * FROM reminders ORDER BY dueDate ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>
}
