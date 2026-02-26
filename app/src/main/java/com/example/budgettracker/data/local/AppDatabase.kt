package com.example.budgettracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
 */
@Database(
    entities = [
        TransactionEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        ReminderEntity::class
    ],
    version = 4, // Bumped from 3 to 4
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE transactions ADD COLUMN relatedAccountName TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE transactions ADD COLUMN transferDirection TEXT")
            }
        }

        /**
         * Migration from version 3 to 4:
         * Removes the 'balance' column from 'accounts' table.
         * SQLite doesn't support DROP COLUMN, so we recreate the table.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Create the new table schema
                database.execSQL("""
                    CREATE TABLE accounts_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        accountName TEXT NOT NULL,
                        accountType TEXT NOT NULL
                    )
                """.trimIndent())

                // 2. Copy existing data (excluding balance)
                database.execSQL("""
                    INSERT INTO accounts_new (id, accountName, accountType)
                    SELECT id, accountName, accountType
                    FROM accounts
                """.trimIndent())

                // 3. Remove old table
                database.execSQL("DROP TABLE accounts")

                // 4. Rename new table to original name
                database.execSQL("ALTER TABLE accounts_new RENAME TO accounts")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "budget_tracker_db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}
