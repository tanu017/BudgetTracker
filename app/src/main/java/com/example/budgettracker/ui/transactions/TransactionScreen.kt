package com.example.budgettracker.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
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
import java.text.SimpleDateFormat
import java.util.*

/**
 * Transaction Screen built with Jetpack Compose.
 * Features Search, Filters, Summary Card, Add Form, and a list with Edit/Delete functionality.
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    // --- FILTER & SEARCH STATE ---
    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    // --- FORM STATE ---
    var amountText by remember { mutableStateOf("") }
    var categoryText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EXPENSE") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // --- EDIT STATE ---
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    // --- FILTERING & SEARCH LOGIC ---
    val categories = remember(transactions) {
        listOf("ALL") + transactions.map { it.category }.distinct().sorted()
    }

    val filteredTransactions = remember(transactions, selectedTypeFilter, selectedCategoryFilter, searchQuery) {
        transactions.filter { tx ->
            val matchesType = if (selectedTypeFilter == "ALL") true else tx.type == selectedTypeFilter
            val matchesCategory = if (selectedCategoryFilter == "ALL") true else tx.category == selectedCategoryFilter
            val matchesSearch = tx.category.contains(searchQuery, ignoreCase = true)
            matchesType && matchesCategory && matchesSearch
        }
    }

    // --- CALCULATE SUMMARY ---
    val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val balance = totalIncome - totalExpense

    // Material3 Date Picker Dialog Logic
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Show Edit Dialog if a transaction is selected
    editingTransaction?.let { transaction ->
        EditTransactionDialog(
            transaction = transaction,
            onDismiss = { editingTransaction = null },
            onSave = { updatedTransaction ->
                viewModel.updateTransaction(updatedTransaction)
                editingTransaction = null
            }
        )
    }

    // --- MAIN UI USING ONE LAZYCOLUMN ---
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
    ) {
        // 1. TITLE
        item {
            Text(
                text = "Budget Tracker",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // 2. SUMMARY SECTION
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryItem("Income", "₹%.2f".format(totalIncome), Color(0xFF2E7D32))
                    SummaryItem("Expense", "₹%.2f".format(totalExpense), Color(0xFFC62828))
                    SummaryItem("Balance", "₹%.2f".format(balance), MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }

        // 3. ADD TRANSACTION FORM
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "Add New Record", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it; errorMessage = null },
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = categoryText,
                        onValueChange = { categoryText = it; errorMessage = null },
                        label = { Text("Category (e.g. Food, Rent)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = formatDate(selectedDate),
                        onValueChange = { },
                        label = { Text("Date") },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showDatePicker = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { selectedType = "INCOME" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedType == "INCOME") Color(0xFF4CAF50) else Color.Gray
                            )
                        ) { Text("Income") }

                        Button(
                            onClick = { selectedType = "EXPENSE" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedType == "EXPENSE") Color(0xFFF44336) else Color.Gray
                            )
                        ) { Text("Expense") }
                    }

                    errorMessage?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull()
                            if (amount != null && amount > 0 && categoryText.isNotBlank()) {
                                viewModel.insertTransaction(TransactionEntity(amount = amount, type = selectedType, category = categoryText, accountName = "Cash", source = "MANUAL", timestamp = selectedDate))
                                amountText = ""; categoryText = ""; selectedDate = System.currentTimeMillis(); errorMessage = null
                            } else { errorMessage = "Invalid input" }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Save Transaction")
                    }
                }
            }
        }

        // 4. SEARCH BAR
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                placeholder = { Text("Search transactions...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
        }

        // 5. FILTERS SECTION
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(text = "Filters", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL", "INCOME", "EXPENSE").forEach { type ->
                        FilterChip(
                            selected = selectedTypeFilter == type,
                            onClick = { selectedTypeFilter = type },
                            label = { Text(type) },
                            leadingIcon = if (selectedTypeFilter == type) {
                                { Icon(imageVector = Icons.Default.Done, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                            } else null
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategoryFilter == category,
                            onClick = { selectedCategoryFilter = if (selectedCategoryFilter == category) "ALL" else category },
                            label = { Text(category) },
                            leadingIcon = if (selectedCategoryFilter == category) {
                                { Icon(imageVector = Icons.Default.Done, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                            } else null
                        )
                    }
                }
            }
        }

        // 6. TRANSACTION LIST ITEMS
        items(
            items = filteredTransactions,
            key = { it.id }
        ) { transaction ->
            TransactionItem(
                transaction = transaction,
                onDelete = { viewModel.deleteTransaction(transaction) },
                onClick = { editingTransaction = transaction }
            )
        }
    }
}

/**
 * Edit Dialog and Helper Components (Preserved)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(transaction: TransactionEntity, onDismiss: () -> Unit, onSave: (TransactionEntity) -> Unit) {
    var amountText by remember { mutableStateOf(transaction.amount.toString()) }
    var categoryText by remember { mutableStateOf(transaction.category) }
    var selectedType by remember { mutableStateOf(transaction.type) }
    var selectedDate by remember { mutableStateOf(transaction.timestamp) }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { selectedDate = datePickerState.selectedDateMillis ?: transaction.timestamp; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(datePickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = categoryText, onValueChange = { categoryText = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = formatDate(selectedDate), onValueChange = { }, label = { Text("Date") }, readOnly = true, enabled = false, modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { selectedType = "INCOME" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == "INCOME") Color(0xFF4CAF50) else Color.Gray)) { Text("Income") }
                    Button(onClick = { selectedType = "EXPENSE" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == "EXPENSE") Color(0xFFF44336) else Color.Gray)) { Text("Expense") }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(transaction.copy(amount = amountText.toDoubleOrNull() ?: 0.0, category = categoryText, type = selectedType, timestamp = selectedDate)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun SummaryItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = Color.DarkGray)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun TransactionItem(transaction: TransactionEntity, onDelete: () -> Unit, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.category, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text("${transaction.type} • ${formatDate(transaction.timestamp)}", color = if (transaction.type == "INCOME") Color(0xFF4CAF50) else Color(0xFFF44336), fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("₹%.2f".format(transaction.amount), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFB71C1C)) }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
}
