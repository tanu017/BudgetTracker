package com.example.budgettracker.ui.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SyncAlt
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
import com.example.budgettracker.data.local.entities.TransactionEntity
import com.example.budgettracker.repository.AccountRepository
import com.example.budgettracker.repository.CategoryRepository
import com.example.budgettracker.repository.ReminderRepository
import com.example.budgettracker.repository.TransactionRepository
import com.example.budgettracker.ui.theme.FinanceColors
import com.example.budgettracker.viewmodel.AccountsViewModel
import com.example.budgettracker.viewmodel.BudgetViewModelFactory
import com.example.budgettracker.viewmodel.TransactionViewModel
import com.example.budgettracker.ui.transactions.engine.BudgetHealthEngine

/**
 * Accounts Screen - Refactor complete: Now uses a pure Ledger-based SSOT system.
 * Account balances are derived in real-time from the transaction history.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsFragment() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }

    val accountRepo = remember { AccountRepository(database.accountDao()) }
    val transactionRepo = remember { TransactionRepository(database.transactionDao()) }
    val categoryRepo = remember { CategoryRepository(database.categoryDao()) }
    val reminderRepo = remember { ReminderRepository(database.reminderDao()) }

    val viewModel: AccountsViewModel = viewModel(
        factory = BudgetViewModelFactory(
            transactionRepo, accountRepo, categoryRepo, reminderRepo
        )
    )
    
    val txViewModel: TransactionViewModel = viewModel(
        factory = BudgetViewModelFactory(
            transactionRepo, accountRepo, categoryRepo, reminderRepo
        )
    )

    val accounts by viewModel.allAccounts.observeAsState(initial = emptyList())
    val transactions by txViewModel.allTransactions.observeAsState(initial = emptyList())

    // --- FORM STATE ---
    var accountName by remember { mutableStateOf("") }
    var initialBalance by remember { mutableStateOf("") }
    var selectedAccountType by remember { mutableStateOf("BANK") }
    var addAccountError by remember { mutableStateOf<String?>(null) }

    // --- TRANSFER STATE ---
    var transferAmount by remember { mutableStateOf("") }
    var fromAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var toAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }

    // --- DERIVED LEDGER LOGIC ---
    val netWorth: Double = accounts.sumOf { 
        BudgetHealthEngine.calculateAccountBalance(it.accountName, transactions) 
    }
    
    val amountToTransfer = transferAmount.toDoubleOrNull() ?: 0.0
    
    val fromAccountBalance = fromAccount?.let { 
        BudgetHealthEngine.calculateAccountBalance(it.accountName, transactions) 
    } ?: 0.0
    
    val hasInsufficientFunds = fromAccount != null && amountToTransfer > fromAccountBalance
    
    val transferErrorMessage = when {
        transferAmount.isNotEmpty() && amountToTransfer <= 0 -> "Enter valid amount"
        hasInsufficientFunds -> "Insufficient funds in ${fromAccount?.accountName}"
        else -> null
    }

    val canTransfer = fromAccount != null && 
                     toAccount != null && 
                     amountToTransfer > 0 && 
                     !hasInsufficientFunds

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp)
    ) {
        // 1. HERO NET WORTH CARD (Derived)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Net Worth",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "₹%.2f".format(netWorth),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // 2. MOVE MONEY SECTION (Transfer via Ledger)
        if (accounts.size >= 2) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Move Money",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(0.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ExposedDropdownMenuBox(
                                    expanded = fromExpanded,
                                    onExpandedChange = { fromExpanded = !fromExpanded },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = fromAccount?.accountName ?: "Source",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("From") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded) },
                                        modifier = Modifier.menuAnchor(),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    ExposedDropdownMenu(expanded = fromExpanded, onDismissRequest = { fromExpanded = false }) {
                                        accounts.filter { it != toAccount }.forEach { account ->
                                            DropdownMenuItem(
                                                text = { Text(account.accountName) },
                                                onClick = { fromAccount = account; fromExpanded = false }
                                            )
                                        }
                                    }
                                }

                                ExposedDropdownMenuBox(
                                    expanded = toExpanded,
                                    onExpandedChange = { toExpanded = !toExpanded },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = toAccount?.accountName ?: "Destination",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("To") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded) },
                                        modifier = Modifier.menuAnchor(),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    ExposedDropdownMenu(expanded = toExpanded, onDismissRequest = { toExpanded = false }) {
                                        accounts.filter { it != fromAccount }.forEach { account ->
                                            DropdownMenuItem(
                                                text = { Text(account.accountName) },
                                                onClick = { toAccount = account; toExpanded = false }
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = transferAmount,
                                onValueChange = { transferAmount = it },
                                label = { Text("Amount") },
                                isError = transferErrorMessage != null,
                                supportingText = { if (transferErrorMessage != null) Text(transferErrorMessage) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium
                            )

                            Button(
                                onClick = {
                                    if (canTransfer) {
                                        val now = System.currentTimeMillis()
                                        // Record double-entry transfer records
                                        txViewModel.insertTransaction(TransactionEntity(
                                            amount = amountToTransfer,
                                            type = "TRANSFER",
                                            category = "Transfer",
                                            accountName = fromAccount!!.accountName,
                                            source = "TRANSFER",
                                            timestamp = now,
                                            relatedAccountName = toAccount!!.accountName,
                                            transferDirection = "OUT"
                                        ))
                                        txViewModel.insertTransaction(TransactionEntity(
                                            amount = amountToTransfer,
                                            type = "TRANSFER",
                                            category = "Transfer",
                                            accountName = toAccount!!.accountName,
                                            source = "TRANSFER",
                                            timestamp = now + 1,
                                            relatedAccountName = fromAccount!!.accountName,
                                            transferDirection = "IN"
                                        ))
                                        transferAmount = ""; fromAccount = null; toAccount = null
                                    }
                                },
                                enabled = canTransfer,
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.Default.SyncAlt, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Confirm Transfer")
                            }
                        }
                    }
                }
            }
        }

        item { HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant) }

        // 3. ADD ACCOUNT (METADATA ONLY)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(0.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Add New Account", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = accountName, onValueChange = { accountName = it; addAccountError = null }, label = { Text("Account Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                    OutlinedTextField(value = initialBalance, onValueChange = { initialBalance = it; addAccountError = null }, label = { Text("Initial Balance") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("BANK", "WALLET", "CREDIT").forEach { type ->
                            FilterChip(selected = selectedAccountType == type, onClick = { selectedAccountType = type }, label = { Text(type, fontSize = 10.sp) }, modifier = Modifier.weight(1f))
                        }
                    }

                    Button(
                        onClick = {
                            val balance = initialBalance.toDoubleOrNull() ?: 0.0
                            if (accountName.isBlank()) { addAccountError = "Enter name" }
                            else {
                                viewModel.insertAccount(AccountEntity(accountName = accountName, accountType = selectedAccountType))
                                if (balance != 0.0) {
                                    txViewModel.insertTransaction(TransactionEntity(
                                        amount = kotlin.math.abs(balance),
                                        type = if (balance > 0) "INCOME" else "EXPENSE",
                                        category = "Initial Balance",
                                        accountName = accountName,
                                        source = "MANUAL",
                                        timestamp = System.currentTimeMillis()
                                    ))
                                }
                                accountName = ""; initialBalance = ""; addAccountError = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) { Text("Create Account") }
                }
            }
        }

        // 4. ACCOUNTS LIST (With Derived Balances)
        items(items = accounts, key = { it.id }) { account ->
            val derivedBalance = BudgetHealthEngine.calculateAccountBalance(account.accountName, transactions)
            AccountItem(account = account, derivedBalance = derivedBalance, onDelete = { viewModel.deleteAccount(account) })
        }
    }
}

@Composable
fun AccountItem(account: AccountEntity, derivedBalance: Double, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = account.accountName, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text(text = account.accountType, fontSize = 12.sp, color = Color.Gray)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "₹%.2f".format(derivedBalance),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (derivedBalance >= 0) FinanceColors.Income else FinanceColors.Expense,
                    modifier = Modifier.padding(end = 8.dp)
                )
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFB71C1C), modifier = Modifier.size(20.dp)) }
            }
        }
    }
}
