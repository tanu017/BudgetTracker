package com.example.budgettracker.data.local.dao

import androidx.room.*
import com.example.budgettracker.data.local.entities.AccountEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the accounts table.
 */
@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    /**
     * Deletes an account from the database.
     */
    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    /**
     * Returns a list of all bank/cash accounts.
     */
    @Query("SELECT * FROM accounts")
    fun getAllAccounts(): Flow<List<AccountEntity>>
}
