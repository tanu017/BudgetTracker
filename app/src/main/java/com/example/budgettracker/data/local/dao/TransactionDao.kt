package com.example.budgettracker.data.local.dao

import androidx.room.*
import com.example.budgettracker.data.local.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the transactions table.
 * Defines the methods to interact with transaction data in the database.
 */
@Dao
interface TransactionDao {

    /**
     * Inserts a new transaction into the database.
     * 'suspend' ensures this runs on a background thread.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    /**
     * Updates an existing transaction details.
     */
    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    /**
     * Deletes a transaction from the database.
     */
    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    /**
     * Returns all transactions sorted by timestamp (latest first).
     * Using 'Flow' allows the UI to automatically update when data changes.
     */
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    /**
     * Returns transactions filtered by their type (INCOME, EXPENSE, etc.).
     */
    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY timestamp DESC")
    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>>
}
