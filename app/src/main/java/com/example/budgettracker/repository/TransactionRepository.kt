package com.example.budgettracker.repository

import com.example.budgettracker.data.local.dao.TransactionDao
import com.example.budgettracker.data.local.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository class that abstracts access to the transaction data source.
 * It provides a clean API for the ViewModel to access data.
 */
class TransactionRepository(private val transactionDao: TransactionDao) {

    // Exposes the flow of all transactions from the DAO
    fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    // Exposes the flow of transactions filtered by type
    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>> = 
        transactionDao.getTransactionsByType(type)

    // Suspended function to insert a transaction via the DAO
    suspend fun insertTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }

    // Suspended function to update a transaction via the DAO
    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    // Suspended function to delete a transaction via the DAO
    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }
}
