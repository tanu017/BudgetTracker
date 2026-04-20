package com.example.budgettracker.ui.accounts

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.data.local.entities.AccountEntity
import com.example.budgettracker.data.local.entities.TransactionEntity
import com.example.budgettracker.repository.*
import com.example.budgettracker.ui.accounts.components.AccountItem
import com.example.budgettracker.ui.accounts.components.AddAccountDialog
import com.example.budgettracker.ui.accounts.components.EditAccountDialog
import com.example.budgettracker.viewmodel.AccountsViewModel
import com.example.budgettracker.viewmodel.BudgetViewModelFactory
import com.example.budgettracker.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsFragment() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }

    val accountRepo = remember { AccountRepository(database.accountDao()) }
    val transactionRepo = remember { TransactionRepository(database.transactionDao()) }
    val categoryRepo = remember { CategoryRepository(database.categoryDao()) }
    val reminderRepo = remember { ReminderRepository(database.reminderDao()) }
    val chatRepo = remember { ChatRepository(database.chatDao()) }

    val factory = BudgetViewModelFactory(
        transactionRepo, accountRepo, categoryRepo, reminderRepo, chatRepo
    )

    val viewModel: AccountsViewModel = viewModel(factory = factory)
    val txViewModel: TransactionViewModel = viewModel(factory = factory)

    val accounts by viewModel.allAccounts.observeAsState(initial = emptyList())

    var transferAmount by remember { mutableStateOf("") }
    var fromAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var toAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedAccountForEdit by remember { mutableStateOf<AccountEntity?>(null) }

    val netWorth: Double = accounts.sumOf { it.balance }
    
    val amountToTransfer = transferAmount.toDoubleOrNull() ?: 0.0
    val fromAccountBalance = fromAccount?.balance ?: 0.0
    val hasInsufficientFunds = fromAccount != null && amountToTransfer > fromAccountBalance
    
    val transferErrorMessage = when {
        transferAmount.isNotEmpty() && amountToTransfer <= 0 -> "Enter valid amount"
        hasInsufficientFunds -> "Insufficient funds"
        else -> null
    }

    val canTransfer = fromAccount != null && toAccount != null && amountToTransfer > 0 && !hasInsufficientFunds

    // STEP 5 — Hook Add Account dialog
    if (showAddDialog) {
        AddAccountDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { account, openingBalance ->
                viewModel.insertAccount(account)
                if (openingBalance > 0) {
                    txViewModel.insertTransaction(TransactionEntity(
                        amount = openingBalance,
                        type = "INCOME",
                        category = "Opening Balance",
                        accountId = account.id,
                        accountName = account.accountName,
                        source = "MANUAL",
                        timestamp = System.currentTimeMillis()
                    ))
                }
                showAddDialog = false
            }
        )
    }

    // STEP 6 — Manage edit dialog state
    if (selectedAccountForEdit != null) {
        EditAccountDialog(
            account = selectedAccountForEdit!!,
            onDismiss = { selectedAccountForEdit = null },
            onSave = { updatedAccount ->
                viewModel.updateAccount(updatedAccount)
                selectedAccountForEdit = null
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Net Worth",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "₹%.2f".format(netWorth),
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            if (accounts.size >= 2) {
                item {
                    Text(
                        text = "Quick Transfer",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ExposedDropdownMenuBox(
                                    expanded = fromExpanded,
                                    onExpandedChange = { fromExpanded = !fromExpanded },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = fromAccount?.accountName ?: "From",
                                        onValueChange = {},
                                        readOnly = true,
                                        modifier = Modifier.menuAnchor(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    ExposedDropdownMenu(expanded = fromExpanded, onDismissRequest = { fromExpanded = false }) {
                                        accounts.forEach { DropdownMenuItem(text = { Text(it.accountName) }, onClick = { fromAccount = it; fromExpanded = false }) }
                                    }
                                }
                                ExposedDropdownMenuBox(
                                    expanded = toExpanded,
                                    onExpandedChange = { toExpanded = !toExpanded },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = toAccount?.accountName ?: "To",
                                        onValueChange = {},
                                        readOnly = true,
                                        modifier = Modifier.menuAnchor(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    ExposedDropdownMenu(expanded = toExpanded, onDismissRequest = { toExpanded = false }) {
                                        accounts.forEach { DropdownMenuItem(text = { Text(it.accountName) }, onClick = { toAccount = it; toExpanded = false }) }
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = transferAmount,
                                onValueChange = { transferAmount = it },
                                label = { Text("Amount") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Button(
                                onClick = {
                                    if (canTransfer) {
                                        txViewModel.insertTransaction(TransactionEntity(
                                            amount = amountToTransfer,
                                            type = "TRANSFER",
                                            category = "Transfer",
                                            accountId = fromAccount!!.id,
                                            accountName = fromAccount!!.accountName,
                                            source = "TRANSFER",
                                            timestamp = System.currentTimeMillis(),
                                            transferDirection = "OUT",
                                            relatedAccountName = toAccount!!.accountName
                                        ))
                                        txViewModel.insertTransaction(TransactionEntity(
                                            amount = amountToTransfer,
                                            type = "TRANSFER",
                                            category = "Transfer",
                                            accountId = toAccount!!.id,
                                            accountName = toAccount!!.accountName,
                                            source = "TRANSFER",
                                            timestamp = System.currentTimeMillis() + 1,
                                            transferDirection = "IN",
                                            relatedAccountName = fromAccount!!.accountName
                                        ))
                                        transferAmount = ""; fromAccount = null; toAccount = null
                                    }
                                },
                                enabled = canTransfer,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.SwapHoriz, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Transfer")
                            }
                        }
                    }
                }
            }

            // STEP 1 — Add "Add Account" header with icon
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Accounts",
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Account",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            items(accounts) { account ->
                AccountItem(
                    account = account,
                    balance = account.balance,
                    onDelete = { viewModel.deleteAccount(account) },
                    onClick = { selectedAccountForEdit = account }
                )
            }
        }
    }
}
