package com.example.budgettracker.viewmodel

import androidx.lifecycle.*
import com.example.budgettracker.data.local.entities.AccountEntity
import com.example.budgettracker.repository.AccountRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for Accounts.
 */
class AccountsViewModel(private val repository: AccountRepository) : ViewModel() {

    val allAccounts: LiveData<List<AccountEntity>> = repository.getAllAccounts().asLiveData()

    fun insertAccount(account: AccountEntity) = viewModelScope.launch {
        repository.insertAccount(account)
    }

    fun updateAccount(account: AccountEntity) = viewModelScope.launch {
        repository.updateAccount(account)
    }

    fun deleteAccount(account: AccountEntity) = viewModelScope.launch {
        repository.deleteAccount(account)
    }
}
