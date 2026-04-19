package com.example.budgettracker.data.local.dao

import androidx.room.*
import com.example.budgettracker.data.local.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY timestamp DESC")
    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type")
    suspend fun getTotalByType(type: String): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND category LIKE :category")
    suspend fun getTotalByTypeAndCategory(type: String, category: String): Double?

    /**
     * Returns total filtered by type and date range.
     */
    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND timestamp BETWEEN :start AND :end")
    suspend fun getTotalByTypeAndDateRange(type: String, start: Long, end: Long): Double?

    /**
     * Returns total filtered by type, category, and date range.
     */
    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND category LIKE :category AND timestamp BETWEEN :start AND :end")
    suspend fun getTotalByTypeCategoryAndDateRange(type: String, category: String, start: Long, end: Long): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE'")
    suspend fun getTotalExpenses(): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND category LIKE :category")
    suspend fun getTotalByCategory(category: String): Double?
}
