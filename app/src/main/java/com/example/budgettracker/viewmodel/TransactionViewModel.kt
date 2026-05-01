package com.example.budgettracker.viewmodel

import androidx.lifecycle.*
import com.example.budgettracker.data.local.entities.AccountEntity
import com.example.budgettracker.data.local.entities.TransactionEntity
import com.example.budgettracker.repository.AccountRepository
import com.example.budgettracker.repository.TransactionRepository
import com.example.budgettracker.utils.Constants
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel for Transactions.
 * Handles the business logic for creating, updating, and deleting transactions
 * while maintaining account balance integrity.
 */
class TransactionViewModel(
    private val repository: TransactionRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    val allTransactions: LiveData<List<TransactionEntity>> = repository.getAllTransactions().asLiveData()
    
    val allAccounts: LiveData<List<AccountEntity>> = accountRepository.getAllAccounts().asLiveData()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent

    fun getTransactionsByType(type: String): LiveData<List<TransactionEntity>> {
        return repository.getTransactionsByType(type).asLiveData()
    }

    fun insertTransaction(transaction: TransactionEntity) = viewModelScope.launch {
        val accounts = accountRepository.getAllAccounts().first()
        val account = accounts.find { it.id == transaction.accountId } ?: return@launch

        if (transaction.type == "EXPENSE" && account.balance - transaction.amount < 0) {
            _uiEvent.emit("Insufficient balance in this account")
            return@launch
        }

        repository.insertTransaction(transaction)

        val newBalance = if (transaction.type == "INCOME") {
            account.balance + transaction.amount
        } else {
            account.balance - transaction.amount
        }

        accountRepository.updateAccount(account.copy(balance = newBalance))
    }

    fun updateTransaction(updatedTransaction: TransactionEntity) = viewModelScope.launch {
        val oldTransaction = repository.getAllTransactions().first().find { it.id == updatedTransaction.id } ?: return@launch
        val accounts = accountRepository.getAllAccounts().first()
        
        val oldAccount = accounts.find { it.id == oldTransaction.accountId } ?: return@launch
        val newAccount = if (updatedTransaction.accountId == oldTransaction.accountId) oldAccount else accounts.find { it.id == updatedTransaction.accountId } ?: return@launch

        var tempBalance = oldAccount.balance
        if (oldTransaction.type == "INCOME") {
            tempBalance -= oldTransaction.amount
        } else {
            tempBalance += oldTransaction.amount
        }

        if (oldTransaction.accountId == updatedTransaction.accountId) {
            if (updatedTransaction.type == "EXPENSE" && tempBalance - updatedTransaction.amount < 0) {
                _uiEvent.emit("Insufficient balance in this account")
                return@launch
            }
            
            val finalBalance = if (updatedTransaction.type == "INCOME") {
                tempBalance + updatedTransaction.amount
            } else {
                tempBalance - updatedTransaction.amount
            }
            
            repository.updateTransaction(updatedTransaction)
            accountRepository.updateAccount(oldAccount.copy(balance = finalBalance))
        } else {
            if (tempBalance < 0) {
                _uiEvent.emit("Cannot change account: current account would have negative balance")
                return@launch
            }
            
            if (updatedTransaction.type == "EXPENSE" && newAccount.balance - updatedTransaction.amount < 0) {
                _uiEvent.emit("Insufficient balance in the new account")
                return@launch
            }

            accountRepository.updateAccount(oldAccount.copy(balance = tempBalance))
            
            val finalNewBalance = if (updatedTransaction.type == "INCOME") {
                newAccount.balance + updatedTransaction.amount
            } else {
                newAccount.balance - updatedTransaction.amount
            }
            accountRepository.updateAccount(newAccount.copy(balance = finalNewBalance))
            
            repository.updateTransaction(updatedTransaction)
        }
    }

    /**
     * Deletes a transaction and reverses its impact on the account balance.
     */
    fun deleteTransaction(transaction: TransactionEntity) = viewModelScope.launch {
        val accounts = accountRepository.getAllAccounts().first()
        val account = accounts.find { it.id == transaction.accountId } ?: return@launch
        
        // Reverse balance effect: Subtract if it was income, Add if it was expense
        val restoredBalance = if (transaction.type == "INCOME") {
            account.balance - transaction.amount
        } else {
            account.balance + transaction.amount
        }
        
        // Validation: Ensure account balance doesn't become negative by deleting an income
        if (restoredBalance < 0) {
            _uiEvent.emit("Cannot delete: account balance would become negative")
            return@launch
        }

        repository.deleteTransaction(transaction)
        accountRepository.updateAccount(account.copy(balance = restoredBalance))
    }

    fun getDefaultAccountId(): String = Constants.DEFAULT_ACCOUNT_ID
}
