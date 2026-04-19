package com.example.budgettracker.repository

import com.example.budgettracker.data.local.dao.TransactionDao
import com.example.budgettracker.data.local.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository class that abstracts access to the transaction data source.
 * It provides a clean API for the ViewModel to access data.
 */
class TransactionRepository(private val transactionDao: TransactionDao) {

    fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>> = 
        transactionDao.getTransactionsByType(type)

    suspend fun insertTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun getTotalByType(type: String): Double? = transactionDao.getTotalByType(type)

    suspend fun getTotalByTypeAndCategory(type: String, category: String): Double? = 
        transactionDao.getTotalByTypeAndCategory(type, category)

    suspend fun getTotalByTypeAndDateRange(type: String, start: Long, end: Long): Double? =
        transactionDao.getTotalByTypeAndDateRange(type, start, end)

    suspend fun getTotalByTypeCategoryAndDateRange(type: String, category: String, start: Long, end: Long): Double? =
        transactionDao.getTotalByTypeCategoryAndDateRange(type, category, start, end)

    suspend fun getTotalExpenses(): Double? = transactionDao.getTotalExpenses()
    suspend fun getTotalByCategory(category: String): Double? = transactionDao.getTotalByCategory(category)
}
