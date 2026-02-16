package com.example.budgettracker.ui.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import com.example.budgettracker.data.local.entities.AccountEntity
import com.example.budgettracker.repository.AccountRepository
import com.example.budgettracker.repository.CategoryRepository
import com.example.budgettracker.repository.ReminderRepository
import com.example.budgettracker.repository.TransactionRepository
import com.example.budgettracker.viewmodel.AccountsViewModel
import com.example.budgettracker.viewmodel.BudgetViewModelFactory

/**
 * Accounts Screen - Refocused for pure account management without large titles.
 */
@Composable
fun AccountsFragment() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }

    // Repositories and ViewModel setup
    val accountRepo = remember { AccountRepository(database.accountDao()) }
    val transactionRepo = remember { TransactionRepository(database.transactionDao()) }
    val categoryRepo = remember { CategoryRepository(database.categoryDao()) }
    val reminderRepo = remember { ReminderRepository(database.reminderDao()) }

    val viewModel: AccountsViewModel = viewModel(
        factory = BudgetViewModelFactory(
            transactionRepo, accountRepo, categoryRepo, reminderRepo
        )
    )

    val accounts by viewModel.allAccounts.observeAsState(initial = emptyList())

    // --- FORM STATE ---
    var accountName by remember { mutableStateOf("") }
    var initialBalance by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
    ) {
        // --- ADD ACCOUNT FORM ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "Add New Account", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = accountName,
                        onValueChange = { accountName = it; errorMessage = null },
                        label = { Text("Account Name (e.g. HDFC Bank)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = initialBalance,
                        onValueChange = { initialBalance = it; errorMessage = null },
                        label = { Text("Initial Balance") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    errorMessage?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val balance = initialBalance.toDoubleOrNull()
                            if (accountName.isBlank()) {
                                errorMessage = "Please enter an account name"
                            } else if (balance == null) {
                                errorMessage = "Please enter a valid balance"
                            } else {
                                val newAccount = AccountEntity(
                                    accountName = accountName,
                                    balance = balance,
                                    accountType = "BANK"
                                )
                                viewModel.insertAccount(newAccount)
                                accountName = ""
                                initialBalance = ""
                                errorMessage = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create Account")
                    }
                }
            }
        }

        item { HorizontalDivider() }

        // --- ACCOUNTS LIST ---
        items(
            items = accounts,
            key = { it.id }
        ) { account ->
            AccountItem(
                account = account,
                onDelete = { viewModel.deleteAccount(account) }
            )
        }
    }
}

@Composable
fun AccountItem(account: AccountEntity, onDelete: () -> Unit) {
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
                    text = account.accountName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
                Text(
                    text = account.accountType,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "₹%.2f".format(account.balance),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFB71C1C)
                    )
                }
            }
        }
    }
}
