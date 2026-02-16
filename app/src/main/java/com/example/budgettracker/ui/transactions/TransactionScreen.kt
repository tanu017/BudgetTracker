package com.example.budgettracker.ui.transactions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.repository.*
import com.example.budgettracker.viewmodel.*
import com.example.budgettracker.ui.transactions.components.*
import com.example.budgettracker.ui.transactions.engine.*
import com.example.budgettracker.ui.transactions.utils.TransactionDateUtils
import com.example.budgettracker.ui.transactions.model.MonthlyAnalytics
import com.example.budgettracker.parser.toTransactionEntity
import java.text.SimpleDateFormat
import java.util.*

/**
 * Transaction Screen built with Jetpack Compose.
 * This file now serves as the orchestrator, with logic and components moved to dedicated files.
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

    // Business Logic Delegation to Engines
    val insights = remember(transactions) { InsightsEngine.calculate(transactions) }
    val healthMetrics = remember(transactions) { BudgetHealthEngine.compute(transactions) }

    // --- ANALYTICS LOGIC ---
    val monthlyData = remember(transactions) {
        val sdf = SimpleDateFormat("MM-yyyy", Locale.getDefault())
        val labelSdf = SimpleDateFormat("MMM", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val data = mutableListOf<MonthlyAnalytics>()
        
        for (i in 5 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.MONTH, -i)
            val monthYearKey = sdf.format(calendar.time)
            val monthLabel = labelSdf.format(calendar.time)
            
            val monthTransactions = transactions.filter { tx ->
                sdf.format(Date(tx.timestamp)) == monthYearKey
            }
            
            data.add(MonthlyAnalytics(
                label = monthLabel,
                income = monthTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }.toFloat(),
                expense = monthTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }.toFloat()
            ))
        }
        data
    }

    // UI State for Filters and Dialogs
    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var showEmailParserDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<com.example.budgettracker.data.local.entities.TransactionEntity?>(null) }

    // Aggregate Dynamic Data
    val categories = remember(transactions) {
        listOf("ALL") + transactions.map { it.category }.distinct().sorted()
    }

    val filteredTransactions = remember(transactions, selectedTypeFilter, selectedCategoryFilter, searchQuery) {
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

    val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val balance = totalIncome - totalExpense

    // Dialogs Management
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

    // --- MAIN UI LAYOUT ---
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
    ) {
        item {
            Text(
                text = "Budget Tracker",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        item {
            SummaryCard(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                balance = balance
            )
        }

        item { SmartInsightsCard(insights = insights) }

        item { HealthInsightsChips(metrics = healthMetrics) }

        item {
            TransactionActionRow(
                onPasteClick = { showEmailParserDialog = true }
            )
        }

        // Analytics Card
        item { AnalyticsCard(monthlyData) }

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

        if (filteredTransactions.isEmpty()) {
            item { EmptyTransactionsState() }
        } else {
            filteredTransactions.forEach { (date, itemsForDate) ->
                stickyHeader {
                    DateHeader(date = TransactionDateUtils.formatHeaderDate(date))
                }
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
