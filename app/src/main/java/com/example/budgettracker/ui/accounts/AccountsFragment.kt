package com.example.budgettracker.ui.accounts

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.budgettracker.repository.*
import com.example.budgettracker.ui.theme.FinanceColors
import com.example.budgettracker.viewmodel.AccountsViewModel
import com.example.budgettracker.viewmodel.BudgetViewModelFactory
import com.example.budgettracker.viewmodel.TransactionViewModel
import com.example.budgettracker.ui.transactions.engine.BudgetHealthEngine

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
    val transactions by txViewModel.allTransactions.observeAsState(initial = emptyList())

    var accountName by remember { mutableStateOf("") }
    var initialBalance by remember { mutableStateOf("") }
    var selectedAccountType by remember { mutableStateOf("BANK") }
    var addAccountError by remember { mutableStateOf<String?>(null) }

    var transferAmount by remember { mutableStateOf("") }
    var fromAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var toAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }

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
        hasInsufficientFunds -> "Insufficient funds"
        else -> null
    }

    val canTransfer = fromAccount != null && toAccount != null && amountToTransfer > 0 && !hasInsufficientFunds

    // Removed windowInsetsPadding(WindowInsets.systemBars)
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Net Worth",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "₹%.2f".format(netWorth),
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (accounts.size >= 2) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Money Transfer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ExposedDropdownMenuBox(
                                        expanded = fromExpanded,
                                        onExpandedChange = { fromExpanded = !fromExpanded },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        OutlinedTextField(
                                            value = fromAccount?.accountName ?: "From",
                                            onValueChange = {},
                                            readOnly = true,
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded) },
                                            modifier = Modifier.menuAnchor(),
                                            shape = RoundedCornerShape(12.dp),
                                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
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
                                            value = toAccount?.accountName ?: "To",
                                            onValueChange = {},
                                            readOnly = true,
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded) },
                                            modifier = Modifier.menuAnchor(),
                                            shape = RoundedCornerShape(12.dp),
                                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
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
                                    label = { Text("Transfer Amount") },
                                    isError = transferErrorMessage != null,
                                    supportingText = { if (transferErrorMessage != null) Text(transferErrorMessage) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Button(
                                    onClick = {
                                        if (canTransfer) {
                                            val now = System.currentTimeMillis()
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
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.SyncAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Transfer Funds")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Manage Accounts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value = accountName, onValueChange = { accountName = it; addAccountError = null }, label = { Text("New Account Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = initialBalance, onValueChange = { initialBalance = it; addAccountError = null }, label = { Text("Opening Balance (Optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("BANK", "WALLET", "CASH").forEach { type ->
                                    FilterChip(
                                        selected = selectedAccountType == type, 
                                        onClick = { selectedAccountType = type }, 
                                        label = { Text(type, fontSize = 11.sp) }, 
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    val balance = initialBalance.toDoubleOrNull() ?: 0.0
                                    if (accountName.isNotBlank()) {
                                        viewModel.insertAccount(AccountEntity(accountName = accountName, accountType = selectedAccountType))
                                        if (balance != 0.0) {
                                            txViewModel.insertTransaction(TransactionEntity(
                                                amount = Math.abs(balance),
                                                type = if (balance > 0) "INCOME" else "EXPENSE",
                                                category = "Initial Balance",
                                                accountName = accountName,
                                                source = "MANUAL",
                                                timestamp = System.currentTimeMillis()
                                            ))
                                        }
                                        accountName = ""; initialBalance = ""; addAccountError = null
                                    } else {
                                        addAccountError = "Name required"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Add Account")
                            }
                            
                            if (addAccountError != null) {
                                Text(text = addAccountError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            items(accounts) { account ->
                val balance = BudgetHealthEngine.calculateAccountBalance(account.accountName, transactions)
                Card(
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = account.accountName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text(text = account.accountType, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "₹%.2f".format(balance),
                                fontWeight = FontWeight.Black,
                                color = if (balance >= 0) FinanceColors.IncomeGreen else FinanceColors.ExpenseRed,
                                fontSize = 18.sp
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { viewModel.deleteAccount(account) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }
    }
}
