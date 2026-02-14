package com.example.budgettracker.ui.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
 * Features a real input form and a reactive list of transactions.
 */
@Composable
fun TransactionScreen() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }

    // Repositories and ViewModel setup
    val transactionRepo = remember { TransactionRepository(database.transactionDao()) }
    val accountRepo = remember { AccountRepository(database.accountDao()) }
    val categoryRepo = remember { CategoryRepository(database.categoryDao()) }
    val reminderRepo = remember { ReminderRepository(database.reminderDao()) }

    val viewModel: TransactionViewModel = viewModel(
        factory = BudgetViewModelFactory(
            transactionRepo, accountRepo, categoryRepo, reminderRepo
        )
    )

    val transactions by viewModel.allTransactions.observeAsState(initial = emptyList())

    // --- FORM STATE ---
    var amountText by remember { mutableStateOf("") }
    var categoryText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EXPENSE") } // Default type

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Budget Tracker",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // --- ADD TRANSACTION FORM ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Add Transaction", fontWeight = FontWeight.Bold)
                
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = categoryText,
                    onValueChange = { categoryText = it },
                    label = { Text("Category (e.g. Food, Salary)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Simple Toggle Buttons for Income/Expense
                    Button(
                        onClick = { selectedType = "INCOME" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "INCOME") Color(0xFF4CAF50) else Color.Gray
                        )
                    ) { Text("Income") }

                    Button(
                        onClick = { selectedType = "EXPENSE" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "EXPENSE") Color(0xFFF44336) else Color.Gray
                        )
                    ) { Text("Expense") }
                }

                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull()
                        if (amount != null && amount > 0 && categoryText.isNotBlank()) {
                            // Create Entity
                            val newTransaction = TransactionEntity(
                                amount = amount,
                                type = selectedType,
                                category = categoryText,
                                accountName = "Cash", // Default for now
                                source = "MANUAL",
                                timestamp = System.currentTimeMillis()
                            )
                            // Save via ViewModel
                            viewModel.insertTransaction(newTransaction)
                            
                            // Reset Fields
                            amountText = ""
                            categoryText = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Transaction")
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // --- TRANSACTION LIST ---
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
        elevation = CardDefaults.cardElevation(2.dp),
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
