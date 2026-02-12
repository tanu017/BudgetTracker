package com.example.budgettracker.repository

import com.example.budgettracker.data.local.dao.AccountDao
import com.example.budgettracker.data.local.entities.AccountEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository class for managing financial accounts.
 * Provides a clean interface between the UI and the data layer.
 */
class AccountRepository(private val accountDao: AccountDao) {

    // Retrieves all accounts as a stream of data
    fun getAllAccounts(): Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    // Inserts a new account (e.g., Bank, Wallet)
    suspend fun insertAccount(account: AccountEntity) {
        accountDao.insertAccount(account)
    }

    // Updates existing account details or balance
    suspend fun updateAccount(account: AccountEntity) {
        accountDao.updateAccount(account)
    }
}
