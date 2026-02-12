package com.example.budgettracker.viewmodel

import androidx.lifecycle.*
import com.example.budgettracker.data.local.entities.TransactionEntity
import com.example.budgettracker.repository.TransactionRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for Transactions.
 * Manages the UI data and handles user interactions for the transaction screen.
 */
class TransactionViewModel(private val repository: TransactionRepository) : ViewModel() {

    // Converts the Flow from repository into LiveData for the UI to observe
    val allTransactions: LiveData<List<TransactionEntity>> = repository.getAllTransactions().asLiveData()

    /**
     * Retrieves transactions filtered by type.
     * Useful for showing only 'Income' or 'Expense' lists.
     */
    fun getTransactionsByType(type: String): LiveData<List<TransactionEntity>> {
        return repository.getTransactionsByType(type).asLiveData()
    }

    /**
     * Inserts a new transaction.
     * viewModelScope.launch ensures the database operation runs on a background thread.
     */
    fun insertTransaction(transaction: TransactionEntity) = viewModelScope.launch {
        repository.insertTransaction(transaction)
    }

    /**
     * Updates an existing transaction.
     */
    fun updateTransaction(transaction: TransactionEntity) = viewModelScope.launch {
        repository.updateTransaction(transaction)
    }

    /**
     * Deletes a transaction record.
     */
    fun deleteTransaction(transaction: TransactionEntity) = viewModelScope.launch {
        repository.deleteTransaction(transaction)
    }
}
