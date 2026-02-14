package com.example.budgettracker.repository

import com.example.budgettracker.data.local.dao.AccountDao
import com.example.budgettracker.data.local.entities.AccountEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository class for managing financial accounts.
 */
class AccountRepository(private val accountDao: AccountDao) {

    fun getAllAccounts(): Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    suspend fun insertAccount(account: AccountEntity) {
        accountDao.insertAccount(account)
    }

    suspend fun updateAccount(account: AccountEntity) {
        accountDao.updateAccount(account)
    }

    suspend fun deleteAccount(account: AccountEntity) {
        accountDao.deleteAccount(account)
    }
}
