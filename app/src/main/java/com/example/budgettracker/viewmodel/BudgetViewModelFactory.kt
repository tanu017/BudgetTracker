package com.example.budgettracker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.repository.*

class BudgetViewModelFactory(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val reminderRepository: ReminderRepository,
    private val chatRepository: ChatRepository
) : ViewModelProvider.Factory {

    companion object {
        fun getInstance(context: Context): BudgetViewModelFactory {
            val database = AppDatabase.getDatabase(context)
            return BudgetViewModelFactory(
                transactionRepository = TransactionRepository(database.transactionDao()),
                accountRepository = AccountRepository(database.accountDao()),
                categoryRepository = CategoryRepository(database.categoryDao()),
                reminderRepository = ReminderRepository(database.reminderDao()),
                chatRepository = ChatRepository(database.chatDao())
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(TransactionViewModel::class.java) -> {
                TransactionViewModel(transactionRepository) as T
            }
            modelClass.isAssignableFrom(AccountsViewModel::class.java) -> {
                AccountsViewModel(accountRepository) as T
            }
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                DashboardViewModel(transactionRepository, accountRepository) as T
            }
            modelClass.isAssignableFrom(CategoryViewModel::class.java) -> {
                CategoryViewModel(categoryRepository) as T
            }
            modelClass.isAssignableFrom(RemindersViewModel::class.java) -> {
                RemindersViewModel(reminderRepository) as T
            }
            modelClass.isAssignableFrom(ChatViewModel::class.java) -> {
                ChatViewModel(transactionRepository, chatRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
