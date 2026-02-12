package com.example.budgettracker.viewmodel

import androidx.lifecycle.*
import com.example.budgettracker.data.local.entities.AccountEntity
import com.example.budgettracker.repository.AccountRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for Accounts.
 * Handles the logic for managing different money accounts (Bank, Cash, etc.).
 */
class AccountsViewModel(private val repository: AccountRepository) : ViewModel() {

    // Observable list of all accounts
    val allAccounts: LiveData<List<AccountEntity>> = repository.getAllAccounts().asLiveData()

    /**
     * Adds a new account source.
     */
    fun insertAccount(account: AccountEntity) = viewModelScope.launch {
        repository.insertAccount(account)
    }

    /**
     * Updates account info or current balance.
     */
    fun updateAccount(account: AccountEntity) = viewModelScope.launch {
        repository.updateAccount(account)
    }
}
