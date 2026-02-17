package com.example.budgettracker.ui.transactions

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.repository.*
import com.example.budgettracker.viewmodel.*
import com.example.budgettracker.ui.transactions.components.*
import com.example.budgettracker.ui.transactions.utils.TransactionDateUtils
import com.example.budgettracker.parser.toTransactionEntity

/**
 * Transaction Screen - Focused hub for all transaction management.
 * Optimized with stable collapsible groups and edge-to-edge support.
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

    // Using a List for saveable state to prevent runtime crash during state restoration.
    var collapsedSections by rememberSaveable { mutableStateOf(listOf<Long>()) }

    // --- Logic Delegation ---
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

    // --- Dialogs ---
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

    // --- UI Layout ---
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp) // Bottom padding for FAB/Nav
    ) {
        // Form Section (End-to-End management)
        item {
            AddTransactionForm(onSave = { viewModel.insertTransaction(it) })
        }

        // Email Parser Entry
        item {
            TransactionActionRow(onPasteClick = { showEmailParserDialog = true })
        }

        // Filters (Sticky)
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

        // Transactions List with Optimized Collapsible Date Groups
        if (filteredGroupedTransactions.isEmpty()) {
            item { EmptyTransactionsState() }
        } else {
            filteredGroupedTransactions.forEach { (date, itemsForDate) ->
                val isCollapsed = collapsedSections.contains(date)
                
                stickyHeader {
                    Surface(
                        modifier = Modifier.clickable { 
                            collapsedSections = if (isCollapsed) {
                                collapsedSections.filter { it != date }
                            } else {
                                collapsedSections + date
                            }
                        },
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        DateHeader(
                            date = TransactionDateUtils.formatHeaderDate(date),
                            isExpanded = !isCollapsed
                        )
                    }
                }

                // FIXED: Only emit items when expanded to remove phantom spacing
                if (!isCollapsed) {
                    items(items = itemsForDate, key = { it.id }) { tx ->
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
