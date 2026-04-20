package com.example.budgettracker.viewmodel

import androidx.lifecycle.*
import com.example.budgettracker.data.local.entities.AccountEntity
import com.example.budgettracker.data.local.entities.TransactionEntity
import com.example.budgettracker.repository.AccountRepository
import com.example.budgettracker.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel for Transactions.
 * Manages the UI data and handles user interactions for the transaction screen.
 */
class TransactionViewModel(
    private val repository: TransactionRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    // Converts the Flow from repository into LiveData for the UI to observe
    val allTransactions: LiveData<List<TransactionEntity>> = repository.getAllTransactions().asLiveData()

    /**
     * Retrieves transactions filtered by type.
     */
    fun getTransactionsByType(type: String): LiveData<List<TransactionEntity>> {
        return repository.getTransactionsByType(type).asLiveData()
    }

    /**
     * Inserts a new transaction and updates account balance.
     */
    fun insertTransaction(transaction: TransactionEntity) = viewModelScope.launch {
        // 1. Save Transaction
        repository.insertTransaction(transaction)

        // 2. Update Account Balance
        val accounts = accountRepository.getAllAccounts().first()
        val account = accounts.find { it.id == transaction.accountId } ?: return@launch

        val newBalance = if (transaction.type == "INCOME") {
            account.balance + transaction.amount
        } else if (transaction.type == "EXPENSE") {
            account.balance - transaction.amount
        } else {
            account.balance
        }

        accountRepository.updateAccount(account.copy(balance = newBalance))
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
