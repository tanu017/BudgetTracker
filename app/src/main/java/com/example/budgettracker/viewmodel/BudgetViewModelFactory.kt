package com.example.budgettracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.budgettracker.repository.AccountRepository
import com.example.budgettracker.repository.CategoryRepository
import com.example.budgettracker.repository.ReminderRepository
import com.example.budgettracker.repository.TransactionRepository

class BudgetViewModelFactory(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val reminderRepository: ReminderRepository
) : ViewModelProvider.Factory {

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
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
