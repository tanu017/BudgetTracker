package com.example.budgettracker.ui.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * Displays a list of real transactions from the database and an option to add test data.
 */
@Composable
fun TransactionScreen() {
    // 1. Get Context and Database Instance
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }

    // 2. Initialize Repositories
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

    // 5. Main UI Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "My Transactions",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Total Records: ${transactions.size}",
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Button to add dummy data for testing
        Button(
            onClick = {
                val dummyTransaction = TransactionEntity(
                    amount = 100.0,
                    type = "EXPENSE",
                    category = "Food",
                    accountName = "Cash",
                    source = "MANUAL",
                    timestamp = System.currentTimeMillis()
                )
                viewModel.insertTransaction(dummyTransaction)
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text(text = "Add Dummy Transaction")
        }

        // 6. List of Transactions using LazyColumn
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = transactions,
                key = { it.id }
            ) { transaction ->
            TransactionItem(transaction)
            }
        }
    }
}

/**
 * Individual Transaction Item UI component.
 */
@Composable
fun TransactionItem(transaction: TransactionEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = transaction.category,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
                Text(
                    text = transaction.type,
                    color = if (transaction.type == "INCOME") Color(0xFF4CAF50) else Color(0xFFF44336),
                    fontSize = 14.sp
                )
            }
            
            Text(
                text = "₹%.2f".format(transaction.amount),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
