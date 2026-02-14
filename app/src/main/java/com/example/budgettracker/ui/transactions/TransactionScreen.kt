package com.example.budgettracker.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.data.local.entities.TransactionEntity
import com.example.budgettracker.repository.AccountRepository
import com.example.budgettracker.repository.CategoryRepository
import com.example.budgettracker.repository.ReminderRepository
import com.example.budgettracker.repository.TransactionRepository
import com.example.budgettracker.viewmodel.BudgetViewModelFactory
import com.example.budgettracker.viewmodel.TransactionViewModel

/**
 * Transaction Screen built with Jetpack Compose.
 * Displays transaction count and a button to add dummy data for testing.
 */
@Composable
fun TransactionScreen() {
    // 1. Get Context and Database Instance
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }

    // 2. Initialize Repositories (wrapped in remember for efficiency)
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
    val transactions by viewModel.allTransactions.observeAsState(initial = emptyList())

    // 5. Layout with Text and Add Button
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Total Transactions Recorded: ${transactions.size}")
        
        Button(
            onClick = {
                // Create a dummy transaction object
                val dummyTransaction = TransactionEntity(
                    amount = 100.0,
                    type = "EXPENSE",
                    category = "Food",
                    accountName = "Cash",
                    source = "MANUAL",
                    timestamp = System.currentTimeMillis()
                )
                // Insert it into the database via ViewModel
                viewModel.insertTransaction(dummyTransaction)
            }
        ) {
            Text(text = "Add Dummy Transaction")
        }
    }
}
