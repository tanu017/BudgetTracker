package com.example.budgettracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.budgettracker.data.local.dao.AccountDao
import com.example.budgettracker.data.local.dao.CategoryDao
import com.example.budgettracker.data.local.dao.ReminderDao
import com.example.budgettracker.data.local.dao.TransactionDao
import com.example.budgettracker.data.local.entities.AccountEntity
import com.example.budgettracker.data.local.entities.CategoryEntity
import com.example.budgettracker.data.local.entities.ReminderEntity
import com.example.budgettracker.data.local.entities.TransactionEntity

/**
 * Main Room Database class for the application.
 * This class serves as the main entry point to the persisted data.
 */
@Database(
    entities = [
        TransactionEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        ReminderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Abstract methods to get the DAOs
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        /**
         * The Singleton pattern is used to ensure only one instance of the database
         * exists across the entire application to save resources.
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // If the INSTANCE is not null, return it; otherwise, create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "budget_tracker_db"
                )
                /**
                 * fallbackToDestructiveMigration: If the database schema changes and 
                 * no migration path is found, it will wipe and recreate the database.
                 * Good for development/learning phases.
                 */
                .fallbackToDestructiveMigration()
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}
