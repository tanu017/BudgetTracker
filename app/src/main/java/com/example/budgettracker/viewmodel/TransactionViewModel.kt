package com.example.budgettracker.viewmodel

import androidx.lifecycle.*
import com.example.budgettracker.data.local.entities.AccountEntity
import com.example.budgettracker.data.local.entities.TransactionEntity
import com.example.budgettracker.repository.AccountRepository
import com.example.budgettracker.repository.TransactionRepository
import com.example.budgettracker.utils.Constants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel for Transactions.
 */
class TransactionViewModel(
    private val repository: TransactionRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    val allTransactions: LiveData<List<TransactionEntity>> = repository.getAllTransactions().asLiveData()
    
    // Expose accounts for the dropdowns
    val allAccounts: LiveData<List<AccountEntity>> = accountRepository.getAllAccounts().asLiveData()

    fun getTransactionsByType(type: String): LiveData<List<TransactionEntity>> {
        return repository.getTransactionsByType(type).asLiveData()
    }

    fun insertTransaction(transaction: TransactionEntity) = viewModelScope.launch {
        repository.insertTransaction(transaction)

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

    fun updateTransaction(transaction: TransactionEntity) = viewModelScope.launch {
        repository.updateTransaction(transaction)
    }

    fun deleteTransaction(transaction: TransactionEntity) = viewModelScope.launch {
        repository.deleteTransaction(transaction)
    }

    fun getDefaultAccountId(): String = Constants.DEFAULT_ACCOUNT_ID
}
