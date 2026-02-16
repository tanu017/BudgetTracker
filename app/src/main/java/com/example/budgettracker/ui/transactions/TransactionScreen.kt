package com.example.budgettracker.ui.transactions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.data.local.entities.TransactionEntity
import com.example.budgettracker.parser.EmailParser
import com.example.budgettracker.parser.ParsedTransaction
import com.example.budgettracker.parser.toTransactionEntity
import com.example.budgettracker.repository.AccountRepository
import com.example.budgettracker.repository.CategoryRepository
import com.example.budgettracker.repository.ReminderRepository
import com.example.budgettracker.repository.TransactionRepository
import com.example.budgettracker.viewmodel.BudgetViewModelFactory
import com.example.budgettracker.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Lightweight data model for automatic spending insights.
 */
data class SmartInsight(
    val title: String,
    val value: String,
    val type: String // INFO, WARNING, POSITIVE
)

/**
 * Data model for financial health metrics.
 */
data class BudgetHealthMetrics(
    val score: Int,
    val savingsRatio: Float,
    val growthRate: Float,
    val predictedSpend: Double,
    val healthStatus: String
)

object BudgetHealthEngine {
    fun compute(transactions: List<TransactionEntity>): BudgetHealthMetrics {
        if (transactions.isEmpty()) return BudgetHealthMetrics(0, 0f, 0f, 0.0, "No Data")

        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        val thisMonth = transactions.filter {
            val tc = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            tc.get(Calendar.MONTH) == currentMonth && tc.get(Calendar.YEAR) == currentYear
        }

        val income = thisMonth.filter { it.type == "INCOME" }.sumOf { it.amount }
        val expense = thisMonth.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        val savingsRatio = if (income > 0) ((income - expense) / income).toFloat().coerceIn(0f, 1f) else 0f
        val dailyAvg = if (currentDay > 0) expense / currentDay else 0.0
        val predictedSpend = dailyAvg * daysInMonth

        val prevMonth = if (currentMonth == 0) 11 else currentMonth - 1
        val prevYear = if (currentMonth == 0) currentYear - 1 else currentYear
        val prevExpense = transactions.filter {
            val tc = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            it.type == "EXPENSE" && tc.get(Calendar.MONTH) == prevMonth && tc.get(Calendar.YEAR) == prevYear
        }.sumOf { it.amount }

        val growth = if (prevExpense > 0) ((expense - prevExpense) / prevExpense).toFloat() else 0f

        var score = 50
        score += (savingsRatio * 40).toInt()
        if (growth < 0) score += 10
        if (expense > income && income > 0) score -= 30
        
        val finalScore = score.coerceIn(0, 100)
        val status = when {
            finalScore > 80 -> "Excellent"
            finalScore > 60 -> "Good"
            finalScore > 40 -> "Risk"
            else -> "Critical"
        }

        return BudgetHealthMetrics(finalScore, savingsRatio, growth, predictedSpend, status)
    }
}

/**
 * Transaction Screen built with Jetpack Compose.
 * Features Search, Email Parsing, Smart Insights, Filters, Analytics, and a Grouped List with Sticky Headers.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

    // --- INSIGHTS LOGIC ---
    val insights = remember(transactions) {
        val list = mutableListOf<SmartInsight>()
        if (transactions.isNotEmpty()) {
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)
            
            val thisMonthTransactions = transactions.filter {
                val txCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                txCal.get(Calendar.MONTH) == currentMonth && txCal.get(Calendar.YEAR) == currentYear
            }

            // A) Top Category
            val topCategory = thisMonthTransactions.filter { it.type == "EXPENSE" }
                .groupBy { it.category }
                .mapValues { it.value.sumOf { tx -> tx.amount } }
                .maxByOrNull { it.value }
            
            if (topCategory != null) {
                list.add(SmartInsight("Top Category", "${topCategory.key} • ₹${"%.0f".format(topCategory.value)}", "INFO"))
            }

            // B) MoM Comparison
            val prevMonth = if (currentMonth == 0) 11 else currentMonth - 1
            val prevYear = if (currentMonth == 0) currentYear - 1 else currentYear
            
            val prevMonthExpense = transactions.filter {
                val txCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                it.type == "EXPENSE" && txCal.get(Calendar.MONTH) == prevMonth && txCal.get(Calendar.YEAR) == prevYear
            }.sumOf { it.amount }
            
            val currMonthExpense = thisMonthTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            
            if (prevMonthExpense > 0) {
                val diff = ((currMonthExpense - prevMonthExpense) / prevMonthExpense) * 100
                if (diff > 0) {
                    list.add(SmartInsight("Month Comparison", "⬆ Spending increased by ${diff.toInt()}%", "WARNING"))
                } else if (diff < 0) {
                    list.add(SmartInsight("Month Comparison", "⬇ Spending reduced by ${diff.toInt().let { if(it<0) -it else it }}%", "POSITIVE"))
                }
            }

            // C) Largest Single Transaction
            val largestTx = thisMonthTransactions.filter { it.type == "EXPENSE" }.maxByOrNull { it.amount }
            if (largestTx != null) {
                list.add(SmartInsight("Largest Expense", "${largestTx.category} • ₹${largestTx.amount}", "INFO"))
            }
        }
        
        if (list.isEmpty()) {
            list.add(SmartInsight("Welcome", "Add transactions to see smart insights", "INFO"))
        }
        list.take(3)
    }

    // New Intelligence Calculation
    val healthMetrics = remember(transactions) {
        BudgetHealthEngine.compute(transactions)
    }

    // --- DIALOG & PARSER STATE ---
    var showEmailParserDialog by remember { mutableStateOf(false) }

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

    // --- FILTERING & GROUPING LOGIC ---
    val groupedTransactions = remember(transactions, selectedTypeFilter, selectedCategoryFilter, searchQuery) {
        transactions
            .filter { tx ->
                val matchesType = if (selectedTypeFilter == "ALL") true else tx.type == selectedTypeFilter
                val matchesCategory = if (selectedCategoryFilter == "ALL") true else tx.category == selectedCategoryFilter
                val matchesSearch = tx.category.contains(searchQuery, ignoreCase = true)
                matchesType && matchesCategory && matchesSearch
            }
            .sortedByDescending { it.timestamp }
            .groupBy { startOfDay(it.timestamp) }
    }

    val categories = remember(transactions) {
        listOf("ALL") + transactions.map { it.category }.distinct().sorted()
    }

    // --- CALCULATE SUMMARY ---
    val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val balance = totalIncome - totalExpense

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
            val monthTransactions = transactions.filter { sdf.format(Date(it.timestamp)) == monthYearKey }
            data.add(MonthlyAnalytics(label = monthLabel, income = monthTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }.toFloat(), expense = monthTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }.toFloat()))
        }
        data
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
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { 
                TextButton(onClick = { 
                    selectedDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    showDatePicker = false 
                }) { Text("OK") } 
            }
        ) { DatePicker(state = datePickerState) }
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

    // --- MAIN UI ---
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
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
            Card(
                modifier = Modifier.fillMaxWidth(), 
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SummaryItem("Income", "₹%.2f".format(totalIncome), Color(0xFF2E7D32))
                    SummaryItem("Expense", "₹%.2f".format(totalExpense), Color(0xFFC62828))
                    SummaryItem("Balance", "₹%.2f".format(balance), MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }

        item { SmartInsightsCard(insights) }

        item { HealthInsightsChips(healthMetrics) }

        item {
            OutlinedButton(
                onClick = { showEmailParserDialog = true }, 
                modifier = Modifier.fillMaxWidth(), 
                contentPadding = PaddingValues(12.dp)
            ) {
                Icon(Icons.Default.Email, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Paste Email / SMS")
            }
        }

        item { AnalyticsCard(monthlyData) }

        item {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        label = { Text("Category") }, 
                        modifier = Modifier.fillMaxWidth(), 
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selectedDate)), 
                        onValueChange = { }, 
                        label = { Text("Date") }, 
                        readOnly = true, 
                        modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }, 
                        enabled = false, 
                        colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { selectedType = "INCOME" }, 
                            modifier = Modifier.weight(1f), 
                            colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == "INCOME") Color(0xFF4CAF50) else Color.Gray)
                        ) { Text("Income") }
                        Button(
                            onClick = { selectedType = "EXPENSE" }, 
                            modifier = Modifier.weight(1f), 
                            colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == "EXPENSE") Color(0xFFF44336) else Color.Gray)
                        ) { Text("Expense") }
                    }
                    errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull()
                            if (amount != null && amount > 0 && categoryText.isNotBlank()) {
                                viewModel.insertTransaction(TransactionEntity(amount = amount, type = selectedType, category = categoryText, accountName = "Cash", source = "MANUAL", timestamp = selectedDate))
                                amountText = ""; categoryText = ""; selectedDate = System.currentTimeMillis(); errorMessage = null
                            }
                        }, 
                        modifier = Modifier.fillMaxWidth(), 
                        shape = MaterialTheme.shapes.medium
                    ) { Text("Save Transaction") }
                }
            }
        }

        stickyHeader {
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(vertical = 8.dp)) {
                OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search transactions...") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, singleLine = true, shape = MaterialTheme.shapes.medium)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL", "INCOME", "EXPENSE").forEach { type -> 
                        FilterChip(
                            selected = selectedTypeFilter == type, 
                            onClick = { selectedTypeFilter = type }, 
                            label = { Text(type) }, 
                            leadingIcon = if (selectedTypeFilter == type) { { Icon(Icons.Default.Done, null, modifier = Modifier.size(FilterChipDefaults.IconSize)) } } else null
                        ) 
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { category -> 
                        FilterChip(
                            selected = selectedCategoryFilter == category, 
                            onClick = { selectedCategoryFilter = if (selectedCategoryFilter == category) "ALL" else category }, 
                            label = { Text(category) }, 
                            leadingIcon = if (selectedCategoryFilter == category) { { Icon(imageVector = Icons.Default.Done, null, modifier = Modifier.size(FilterChipDefaults.IconSize)) } } else null
                        ) 
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }
        }

        if (groupedTransactions.isEmpty()) {
            item { 
                Column(modifier = Modifier.fillMaxWidth().padding(top = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) { 
                    Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(80.dp), tint = Color.Gray)
                    Text("No transactions found", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    Text("Try changing filters or add a new transaction", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp)) 
                } 
            }
        } else {
            groupedTransactions.forEach { (startOfDayTimestamp, transactionsForDate) ->
                stickyHeader { 
                    Text(
                        text = formatHeaderDate(startOfDayTimestamp), 
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 16.dp, vertical = 6.dp), 
                        fontWeight = FontWeight.Medium, 
                        style = MaterialTheme.typography.labelLarge, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                }
                items(items = transactionsForDate, key = { it.id }) { tx -> 
                    TransactionItem(transaction = tx, onDelete = { viewModel.deleteTransaction(tx) }, onClick = { editingTransaction = tx }) 
                }
            }
        }
    }
}

@Composable
fun HealthInsightsChips(metrics: BudgetHealthMetrics) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistChip(
            onClick = { },
            label = { Text("Health: ${metrics.healthStatus} (${metrics.score})") },
            leadingIcon = {
                Icon(
                    Icons.Default.Favorite, 
                    contentDescription = null, 
                    modifier = Modifier.size(16.dp),
                    tint = if(metrics.score > 50) Color(0xFF4CAF50) else Color.Red
                )
            }
        )
        AssistChip(
            onClick = { },
            label = { Text("Savings: ${(metrics.savingsRatio * 100).toInt()}%") },
            leadingIcon = { Icon(Icons.Default.TrendingUp, null, Modifier.size(16.dp)) }
        )
        AssistChip(
            onClick = { },
            label = { Text("Est. ₹${"%.0f".format(metrics.predictedSpend)}") },
            leadingIcon = { Icon(Icons.Default.QueryStats, null, Modifier.size(16.dp)) }
        )
    }
}

@Composable
fun SmartInsightsCard(insights: List<SmartInsight>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), 
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), 
        elevation = CardDefaults.cardElevation(2.dp), 
        border = CardDefaults.outlinedCardBorder()
    ) { 
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { 
            Row(verticalAlignment = Alignment.CenterVertically) { 
                Icon(Icons.Default.Lightbulb, null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Smart Insights", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) 
            }
            insights.forEach { InsightRow(it) } 
        } 
    }
}

@Composable
fun InsightRow(insight: SmartInsight) {
    val (icon, color) = when (insight.type) { 
        "WARNING" -> Icons.Default.Warning to MaterialTheme.colorScheme.error
        "POSITIVE" -> Icons.Default.CheckCircle to Color(0xFF2E7D32)
        else -> Icons.Default.Info to MaterialTheme.colorScheme.onSurface 
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { 
        Icon(icon, null, tint = color.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Column { 
            Text(insight.title, fontSize = 12.sp, color = Color.Gray)
            Text(insight.value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = color) 
        } 
    }
}

@Composable
fun EmailParserDialog(onDismiss: () -> Unit, onTransactionParsed: (ParsedTransaction) -> Unit) {
    var rawText by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<ParsedTransaction?>(null) }
    var hasAttemptedParse by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss, 
        title = { Text("Paste Email / SMS Content") }, 
        text = { 
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { 
                OutlinedTextField(
                    value = rawText, 
                    onValueChange = { rawText = it; hasAttemptedParse = false }, 
                    modifier = Modifier.fillMaxWidth().height(120.dp), 
                    placeholder = { Text("e.g. Your A/c XX1234 debited for ₹500...") }
                )
                Button(
                    onClick = { result = EmailParser.parseEmail(rawText); hasAttemptedParse = true }, 
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Scan Content") }
                
                if (result != null) { 
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) { 
                        Column(Modifier.padding(12.dp).fillMaxWidth()) { 
                            Text("Transaction Found!", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Amount: ₹${result!!.amount}")
                            Text("Type: ${result!!.type}")
                            Text("Merchant: ${result!!.merchant ?: "Unknown"}") 
                        } 
                    } 
                } else if (hasAttemptedParse) { 
                    Text("No transaction detected.", color = Color.Red, fontSize = 12.sp) 
                } 
            } 
        }, 
        confirmButton = { 
            if (result != null) { 
                Button(onClick = { onTransactionParsed(result!!) }) { Text("Add to List") } 
            } 
        }, 
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AnalyticsCard(data: List<MonthlyAnalytics>) {
    if (data.isEmpty()) return
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), elevation = CardDefaults.cardElevation(2.dp)) { 
        Column(modifier = Modifier.padding(16.dp)) { 
            Text(text = "Monthly Analytics", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            val maxVal = (data.maxOfOrNull { maxOf(it.income, it.expense) } ?: 100f).coerceAtLeast(100f)
            Box(modifier = Modifier.fillMaxWidth().height(150.dp)) { 
                Canvas(modifier = Modifier.fillMaxSize()) { 
                    val barWidth = 16.dp.toPx()
                    val groupWidth = 2 * barWidth
                    val spacing = (size.width - (data.size * groupWidth)) / (data.size + 1)
                    data.forEachIndexed { i, item -> 
                        val xBase = spacing + i * (groupWidth + spacing)
                        drawRect(color = Color(0xFF4CAF50), topLeft = Offset(xBase, size.height - (item.income / maxVal) * size.height), size = Size(barWidth, (item.income / maxVal) * size.height))
                        drawRect(color = Color(0xFFF44336), topLeft = Offset(xBase + barWidth, size.height - (item.expense / maxVal) * size.height), size = Size(barWidth, (item.expense / maxVal) * size.height)) 
                    } 
                } 
            } 
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceAround) { 
                data.forEach { Text(it.label, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.weight(1f), textAlign = TextAlign.Center) } 
            } 
        } 
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(transaction: TransactionEntity, onDismiss: () -> Unit, onSave: (TransactionEntity) -> Unit) {
    var amountText by remember { mutableStateOf(transaction.amount.toString()) }
    var categoryText by remember { mutableStateOf(transaction.category) }
    var selectedType by remember { mutableStateOf(transaction.type) }
    var selectedDate by remember { mutableStateOf(transaction.timestamp) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    if (showDatePicker) { 
        val state = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { TextButton(onClick = { selectedDate = state.selectedDateMillis ?: transaction.timestamp; showDatePicker = false }) { Text("OK") } }) { DatePicker(state) } 
    }
    
    AlertDialog(
        onDismissRequest = onDismiss, 
        title = { Text("Edit Transaction") }, 
        text = { 
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { 
                OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = categoryText, onValueChange = { categoryText = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selectedDate)), onValueChange = { }, label = { Text("Date") }, readOnly = true, enabled = false, modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { 
                    Button(onClick = { selectedType = "INCOME" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == "INCOME") Color(0xFF4CAF50) else Color.Gray)) { Text("Income") }
                    Button(onClick = { selectedType = "EXPENSE" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == "EXPENSE") Color(0xFFF44336) else Color.Gray)) { Text("Expense") }
                } 
            } 
        }, 
        confirmButton = { 
            Button(onClick = { onSave(transaction.copy(amount = amountText.toDoubleOrNull() ?: 0.0, category = categoryText, type = selectedType, timestamp = selectedDate)) }) { Text("Save") } 
        }, 
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun TransactionItem(transaction: TransactionEntity, onDelete: () -> Unit, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { 
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { 
            Column(modifier = Modifier.weight(1f)) { 
                Text(transaction.category, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text("${transaction.type} • ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(transaction.timestamp))}", color = if (transaction.type == "INCOME") Color(0xFF4CAF50) else Color(0xFFF44336), fontSize = 12.sp) 
            }; 
            Row(verticalAlignment = Alignment.CenterVertically) { 
                Text("₹%.2f".format(transaction.amount), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFB71C1C)) } 
            } 
        } 
    }
}

@Composable
fun SummaryItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) { 
        Text(label, fontSize = 12.sp, color = Color.DarkGray)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color) 
    }
}

private fun startOfDay(timestamp: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = timestamp
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun formatHeaderDate(startOfDayTimestamp: Long): String {
    val today = startOfDay(System.currentTimeMillis())
    val yesterday = today - (24 * 60 * 60 * 1000)
    return when (startOfDayTimestamp) { 
        today -> "Today"
        yesterday -> "Yesterday"
        else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(startOfDayTimestamp)) 
    }
}

data class MonthlyAnalytics(val label: String, val income: Float, val expense: Float)
