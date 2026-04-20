package com.example.budgettracker.viewmodel

import android.util.Log
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
 */
class TransactionViewModel(
    private val repository: TransactionRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    val allTransactions: LiveData<List<TransactionEntity>> = repository.getAllTransactions().asLiveData()
    
    // Expose accounts for the dropdowns
    val allAccounts: LiveData<List<AccountEntity>> = accountRepository.getAllAccounts().asLiveData()

    // SharedFlow to emit UI events like error messages
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent

    fun getTransactionsByType(type: String): LiveData<List<TransactionEntity>> {
        return repository.getTransactionsByType(type).asLiveData()
    }

    /**
     * STEP 1 — Add validation function
     */
    fun canApplyTransaction(accountBalance: Double, amount: Double, type: String): Boolean {
        return if (type == "EXPENSE") {
            accountBalance - amount >= 0
        } else {
            true // income is always allowed
        }
    }

    /**
     * STEP 2 — Apply validation before inserting transaction
     */
    fun insertTransaction(transaction: TransactionEntity) = viewModelScope.launch {
        val accounts = accountRepository.getAllAccounts().first()
        val account = accounts.find { it.id == transaction.accountId } ?: return@launch

        // Validate
        if (!canApplyTransaction(account.balance, transaction.amount, transaction.type)) {
            _uiEvent.emit("Insufficient balance in this account")
            return@launch
        }

        // Apply changes
        repository.insertTransaction(transaction)

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
     * STEP 3 — Handle edit transaction properly with balance validation
     */
    fun updateTransaction(updatedTransaction: TransactionEntity) = viewModelScope.launch {
        // 1. Get current data
        val oldTransaction = repository.getAllTransactions().first().find { it.id == updatedTransaction.id } ?: return@launch
        val accounts = accountRepository.getAllAccounts().first()
        
        val oldAccount = accounts.find { it.id == oldTransaction.accountId } ?: return@launch
        val newAccount = if (updatedTransaction.accountId == oldTransaction.accountId) oldAccount else accounts.find { it.id == updatedTransaction.accountId } ?: return@launch

        // 2. Calculate balance if we reverse the old transaction
        var tempBalance = oldAccount.balance
        if (oldTransaction.type == "INCOME") {
            tempBalance -= oldTransaction.amount
        } else {
            tempBalance += oldTransaction.amount
        }

        // If we are changing accounts, we need to handle both
        if (oldTransaction.accountId == updatedTransaction.accountId) {
            // Check if updated transaction is valid on tempBalance
            if (updatedTransaction.type == "EXPENSE" && tempBalance - updatedTransaction.amount < 0) {
                _uiEvent.emit("Insufficient balance in this account")
                return@launch
            }
            
            // Apply updates to the same account
            val finalBalance = if (updatedTransaction.type == "INCOME") {
                tempBalance + updatedTransaction.amount
            } else {
                tempBalance - updatedTransaction.amount
            }
            
            repository.updateTransaction(updatedTransaction)
            accountRepository.updateAccount(oldAccount.copy(balance = finalBalance))
            
        } else {
            // Changing account: 
            // 1. Check if old account would go negative if we reverse an income (Edge case)
            if (tempBalance < 0) {
                _uiEvent.emit("Cannot change account: current account would have negative balance")
                return@launch
            }
            
            // 2. Check if new account has enough balance for updated transaction (if expense)
            if (updatedTransaction.type == "EXPENSE" && newAccount.balance - updatedTransaction.amount < 0) {
                _uiEvent.emit("Insufficient balance in the new account")
                return@launch
            }

            // Apply:
            // Reverse old from oldAccount
            accountRepository.updateAccount(oldAccount.copy(balance = tempBalance))
            
            // Apply new to newAccount
            val finalNewBalance = if (updatedTransaction.type == "INCOME") {
                newAccount.balance + updatedTransaction.amount
            } else {
                newAccount.balance - updatedTransaction.amount
            }
            accountRepository.updateAccount(newAccount.copy(balance = finalNewBalance))
            
            repository.updateTransaction(updatedTransaction)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) = viewModelScope.launch {
        // STEP 7 — Debug check
        Log.d("DELETE_DEBUG", "Deleting transaction ID: ${transaction.id}")
        
        // Reverse balance effect when deleting
        val accounts = accountRepository.getAllAccounts().first()
        val account = accounts.find { it.id == transaction.accountId } ?: return@launch
        
        val restoredBalance = if (transaction.type == "INCOME") {
            account.balance - transaction.amount
        } else {
            account.balance + transaction.amount
        }
        
        // If deleting an income causes negative balance
        if (restoredBalance < 0) {
            _uiEvent.emit("Cannot delete: account balance would become negative")
            return@launch
        }

        repository.deleteTransaction(transaction)
        accountRepository.updateAccount(account.copy(balance = restoredBalance))
    }

    fun getDefaultAccountId(): String = Constants.DEFAULT_ACCOUNT_ID
}
