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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.data.local.entities.TransactionEntity
import com.example.budgettracker.repository.*
import com.example.budgettracker.viewmodel.*
import com.example.budgettracker.ui.transactions.components.*
import com.example.budgettracker.ui.transactions.utils.TransactionDateUtils
import com.example.budgettracker.ui.transactions.model.TransactionListItem
import com.example.budgettracker.ui.transactions.engine.TransactionConsolidationEngine
import com.example.budgettracker.parser.toTransactionEntity

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
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var collapsedSections by rememberSaveable { mutableStateOf(listOf<Long>()) }

    // --- Logic: Use Centralized Consolidation Engine ---
    val consolidatedTransactions = remember(transactions) {
        TransactionConsolidationEngine.consolidate(transactions)
    }

    // --- Logic: Filter and Group the consolidated list ---
    val filteredGroupedTransactions = remember(consolidatedTransactions, selectedTypeFilter, selectedCategoryFilter, searchQuery) {
        consolidatedTransactions
            .filter { listItem ->
                when (listItem) {
                    is TransactionListItem.Regular -> {
                        val matchesType = if (selectedTypeFilter == "ALL") true else listItem.transaction.type == selectedTypeFilter
                        val matchesCategory = if (selectedCategoryFilter == "ALL") true else listItem.transaction.category == selectedCategoryFilter
                        val matchesSearch = listItem.transaction.category.contains(searchQuery, ignoreCase = true)
                        matchesType && matchesCategory && matchesSearch
                    }
                    is TransactionListItem.Transfer -> {
                        val matchesType = selectedTypeFilter == "ALL" || selectedTypeFilter == "TRANSFER"
                        val matchesCategory = selectedCategoryFilter == "ALL" || selectedCategoryFilter == "Transfer"
                        val matchesSearch = "Transfer".contains(searchQuery, ignoreCase = true) || 
                                          listItem.fromAccount.contains(searchQuery, ignoreCase = true) ||
                                          listItem.toAccount.contains(searchQuery, ignoreCase = true)
                        matchesType && matchesCategory && matchesSearch
                    }
                }
            }
            .groupBy { TransactionDateUtils.startOfDay(it.timestamp) }
    }

    val categories = remember(transactions) {
        listOf("ALL") + transactions.map { it.category }.distinct().sorted()
    }

    // Daily totals (Correctly ignores TRANSFERS via external flag)
    val todayStart = remember { TransactionDateUtils.startOfDay(System.currentTimeMillis()) }
    val todayItems = consolidatedTransactions.filter { TransactionDateUtils.startOfDay(it.timestamp) == todayStart }
    val todaySpent = todayItems.filter { it is TransactionListItem.Regular && it.transaction.type == "EXPENSE" }.sumOf { it.amount }
    val todayEarned = todayItems.filter { it is TransactionListItem.Regular && it.transaction.type == "INCOME" }.sumOf { it.amount }

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

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        item { AddTransactionForm(onSave = { viewModel.insertTransaction(it) }) }
        item { TransactionActionRow(onPasteClick = { showEmailParserDialog = true }) }

        stickyHeader {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Today spent: ₹%.0f".format(todaySpent), style = MaterialTheme.typography.labelMedium, color = Color(0xFFC62828))
                        Text(text = "Today earned: ₹%.0f".format(todayEarned), style = MaterialTheme.typography.labelMedium, color = Color(0xFF2E7D32))
                    }
                }
                TransactionFilterHeader(searchQuery, { searchQuery = it }, selectedTypeFilter, { selectedTypeFilter = it }, categories, selectedCategoryFilter, { selectedCategoryFilter = it })
            }
        }

        if (filteredGroupedTransactions.isEmpty()) {
            item { EmptyTransactionsState() }
        } else {
            filteredGroupedTransactions.forEach { (date, itemsForDate) ->
                val isCollapsed = collapsedSections.contains(date)
                stickyHeader {
                    Surface(
                        modifier = Modifier.clickable { 
                            collapsedSections = if (isCollapsed) collapsedSections.filter { it != date } else collapsedSections + date
                        },
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        DateHeader(date = "${TransactionDateUtils.formatHeaderDate(date)} (${itemsForDate.size})", isExpanded = !isCollapsed)
                    }
                }

                if (!isCollapsed) {
                    items(items = itemsForDate, key = { listItem -> 
                        when(listItem) {
                            is TransactionListItem.Regular -> "reg_${listItem.transaction.id}"
                            is TransactionListItem.Transfer -> "trf_${listItem.id}"
                        }
                    }) { listItem ->
                        when (listItem) {
                            is TransactionListItem.Regular -> {
                                TransactionItem(
                                    transaction = listItem.transaction,
                                    onDelete = { viewModel.deleteTransaction(listItem.transaction) },
                                    onClick = { editingTransaction = listItem.transaction }
                                )
                            }
                            is TransactionListItem.Transfer -> {
                                TransactionItem(
                                    transaction = listItem.sourceEntity,
                                    onDelete = {
                                        viewModel.deleteTransaction(listItem.sourceEntity)
                                        viewModel.deleteTransaction(listItem.destinationEntity)
                                    },
                                    onClick = {},
                                    overrideTitle = "${listItem.fromAccount} → ${listItem.toAccount}",
                                    isTransfer = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
