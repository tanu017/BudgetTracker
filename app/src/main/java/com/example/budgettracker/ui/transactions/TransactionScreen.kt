package com.example.budgettracker.ui.transactions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.repository.AccountRepository
import com.example.budgettracker.repository.CategoryRepository
import com.example.budgettracker.repository.ReminderRepository
import com.example.budgettracker.repository.TransactionRepository
import com.example.budgettracker.viewmodel.BudgetViewModelFactory
import com.example.budgettracker.viewmodel.TransactionViewModel
import androidx.compose.runtime.remember


/**
 * Transaction Screen built with Jetpack Compose.
 * Uses MVVM pattern to display data from TransactionViewModel.
 */
@Composable
fun TransactionScreen() {
    // 1. Get Context and Database Instance
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }

    val transactionRepo = remember { TransactionRepository(database.transactionDao()) }
    val accountRepo = remember { AccountRepository(database.accountDao()) }
    val categoryRepo = remember { CategoryRepository(database.categoryDao()) }
    val reminderRepo = remember { ReminderRepository(database.reminderDao()) }


    // 3. Obtain ViewModel using the Custom Factory
    val viewModel: TransactionViewModel = viewModel(
        factory = BudgetViewModelFactory(
            transactionRepo,
            accountRepo,
            categoryRepo,
            reminderRepo
        )
    )

    // 4. Observe the LiveData list as a Compose State
    // Whenever the list in the DB changes, the UI will automatically recompose.
    val transactions by viewModel.allTransactions.observeAsState(initial = emptyList())

    // 5. Simple UI to display the count
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Total Transactions Recorded: ${transactions.size}")
    }
}
