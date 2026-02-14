package com.example.budgettracker.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.example.budgettracker.data.local.entities.AccountEntity
import com.example.budgettracker.data.local.entities.TransactionEntity
import com.example.budgettracker.repository.AccountRepository
import com.example.budgettracker.repository.TransactionRepository

/**
 * ViewModel for the Dashboard.
 * Consolidates data from both Accounts and Transactions for an overview.
 */
class DashboardViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    // Observe all transactions as LiveData
    val allTransactions: LiveData<List<TransactionEntity>> = 
        transactionRepository.getAllTransactions().asLiveData()
        
    // Observe all accounts as LiveData
    val allAccounts: LiveData<List<AccountEntity>> = 
        accountRepository.getAllAccounts().asLiveData()
}
