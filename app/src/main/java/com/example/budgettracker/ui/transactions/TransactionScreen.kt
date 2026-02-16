package com.example.budgettracker.ui.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgettracker.R
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.repository.*
import com.example.budgettracker.viewmodel.*
import com.example.budgettracker.ui.transactions.components.*
import com.example.budgettracker.ui.transactions.utils.TransactionDateUtils
import com.example.budgettracker.parser.toTransactionEntity

/**
 * Transaction Screen built with Jetpack Compose.
 * Acts as an orchestrator for transaction management.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionScreen() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }

    val viewModel: TransactionViewModel = viewModel(
        factory = BudgetViewModelFactory(
            TransactionRepository(database.transactionDao()),
            AccountRepository(database.accountDao()),
            CategoryRepository(database.categoryDao()),
            ReminderRepository(database.reminderDao())
        )
    )

    val transactions by viewModel.allTransactions.observeAsState(initial = emptyList())

    // --- UI State ---
    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var showEmailParserDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<com.example.budgettracker.data.local.entities.TransactionEntity?>(null) }

    // Replaced mutableStateMapOf with a Set of timestamps for collapsed sections to prevent runtime crash.
    var collapsedSections by rememberSaveable { mutableStateOf(setOf<Long>()) }

    // Aggregate Dynamic Data for Filters
    val categories = remember(transactions) {
        listOf("ALL") + transactions.map { it.category }.distinct().sorted()
    }

    val filteredGroupedTransactions = remember(transactions, selectedTypeFilter, selectedCategoryFilter, searchQuery) {
        transactions
            .filter { tx ->
                val matchesType = if (selectedTypeFilter == "ALL") true else tx.type == selectedTypeFilter
                val matchesCategory = if (selectedCategoryFilter == "ALL") true else tx.category == selectedCategoryFilter
                val matchesSearch = tx.category.contains(searchQuery, ignoreCase = true)
                matchesType && matchesCategory && matchesSearch
            }
            .sortedByDescending { it.timestamp }
            .groupBy { TransactionDateUtils.startOfDay(it.timestamp) }
    }

    // Dialogs
    if (showEmailParserDialog) {
        EmailParserDialog(
            onDismiss = { showEmailParserDialog = false },
            onTransactionParsed = { parsedTx ->
                viewModel.insertTransaction(parsedTx.toTransactionEntity())
                showEmailParserDialog = false
            }
        )
    }

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
    ) {
        // Form Section
        item {
            AddTransactionForm(
                onSave = { viewModel.insertTransaction(it) }
            )
        }

        // Email Parser Entry Button
        item {
            TransactionActionRow(
                onPasteClick = { showEmailParserDialog = true }
            )
        }

        // Sticky Search and Filters
        stickyHeader {
            TransactionFilterHeader(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                selectedType = selectedTypeFilter,
                onTypeChange = { selectedTypeFilter = it },
                categories = categories,
                selectedCategory = selectedCategoryFilter,
                onCategoryChange = { selectedCategoryFilter = it }
            )
        }

        if (filteredGroupedTransactions.isEmpty()) {
            item { EmptyTransactionsState() }
        } else {
            filteredGroupedTransactions.forEach { (date, itemsForDate) ->
                val isCollapsed = collapsedSections.contains(date)
                
                stickyHeader {
                    Surface(
                        modifier = Modifier.clickable { 
                            collapsedSections = if (isCollapsed) {
                                collapsedSections - date
                            } else {
                                collapsedSections + date
                            }
                        },
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        DateHeader(date = TransactionDateUtils.formatHeaderDate(date))
                    }
                }

                items(items = itemsForDate, key = { it.id }) { tx ->
                    AnimatedVisibility(visible = !isCollapsed) {
                        TransactionItem(
                            transaction = tx,
                            onDelete = { viewModel.deleteTransaction(tx) },
                            onClick = { editingTransaction = tx }
                        )
                    }
                }
            }
        }
    }
}
