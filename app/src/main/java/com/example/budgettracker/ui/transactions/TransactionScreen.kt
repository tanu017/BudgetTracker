package com.example.budgettracker.ui.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

    val transactionRepo = remember { TransactionRepository(database.transactionDao()) }
    val accountRepo = remember { AccountRepository(database.accountDao()) }
    val categoryRepo = remember { CategoryRepository(database.categoryDao()) }
    val reminderRepo = remember { ReminderRepository(database.reminderDao()) }
    val chatRepo = remember { ChatRepository(database.chatDao()) }

    val viewModel: TransactionViewModel = viewModel(
        factory = BudgetViewModelFactory(
            transactionRepo,
            accountRepo,
            categoryRepo,
            reminderRepo,
            chatRepo
        )
    )

    val transactions by viewModel.allTransactions.observeAsState(initial = emptyList())

    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var showEmailParserDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var collapsedSections by rememberSaveable { mutableStateOf(listOf<Long>()) }
    var isAddFormVisible by rememberSaveable { mutableStateOf(false) }

    val consolidatedTransactions = remember(transactions) {
        TransactionConsolidationEngine.consolidate(transactions)
    }

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
                        val matchesSearch = "${listItem.fromAccount} ${listItem.toAccount}".contains(searchQuery, ignoreCase = true)
                        matchesType && matchesCategory && matchesSearch
                    }
                }
            }
            .groupBy { TransactionDateUtils.startOfDay(it.timestamp) }
    }

    val categories = remember(transactions) {
        listOf("ALL") + transactions.map { it.category }.distinct().sorted()
    }

    val todayStart = remember { TransactionDateUtils.startOfDay(System.currentTimeMillis()) }
    val todayItems = consolidatedTransactions.filter { TransactionDateUtils.startOfDay(it.timestamp) == todayStart }
    val todaySpent = todayItems.filter { it is TransactionListItem.Regular && it.transaction.type == "EXPENSE" }.sumOf { it.amount }
    val todayEarned = todayItems.filter { it is TransactionListItem.Regular && it.transaction.type == "INCOME" }.sumOf { it.amount }

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

    // Removed windowInsetsPadding(WindowInsets.systemBars) as edge-to-edge is disabled
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isAddFormVisible = !isAddFormVisible }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Add New Record",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(
                                if (isAddFormVisible) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }
                        if (isAddFormVisible) {
                            Divider(modifier = Modifier.padding(horizontal = 16.dp))
                            Box(modifier = Modifier.padding(12.dp)) {
                                AddTransactionForm(onSave = { 
                                    viewModel.insertTransaction(it)
                                    isAddFormVisible = false
                                })
                            }
                        }
                    }
                }
            }

            item { TransactionActionRow(onPasteClick = { showEmailParserDialog = true }) }

            stickyHeader {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Spent Today", style = MaterialTheme.typography.labelSmall)
                                Text("₹%.0f".format(todaySpent), style = MaterialTheme.typography.titleMedium, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Earned Today", style = MaterialTheme.typography.labelSmall)
                                Text("₹%.0f".format(todayEarned), style = MaterialTheme.typography.titleMedium, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    TransactionFilterHeader(
                        searchQuery, 
                        { searchQuery = it }, 
                        selectedTypeFilter, 
                        { selectedTypeFilter = it }, 
                        categories, 
                        selectedCategoryFilter, 
                        { selectedCategoryFilter = it }
                    )
                }
            }

            if (filteredGroupedTransactions.isEmpty()) {
                item { EmptyTransactionsState() }
            } else {
                filteredGroupedTransactions.forEach { (date, itemsForDate) ->
                    val isCollapsed = collapsedSections.contains(date)
                    stickyHeader {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    collapsedSections = if (isCollapsed) collapsedSections.filter { it != date } else collapsedSections + date
                                },
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp
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
                                        onClick = { editingTransaction = listItem.transaction },
                                        showDelete = true
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
                                        isTransfer = true,
                                        showDelete = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
