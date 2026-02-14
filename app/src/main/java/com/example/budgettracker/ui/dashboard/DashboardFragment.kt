package com.example.budgettracker.ui.dashboard

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.data.local.entities.TransactionEntity
import com.example.budgettracker.repository.AccountRepository
import com.example.budgettracker.repository.CategoryRepository
import com.example.budgettracker.repository.ReminderRepository
import com.example.budgettracker.repository.TransactionRepository
import com.example.budgettracker.viewmodel.BudgetViewModelFactory
import com.example.budgettracker.viewmodel.DashboardViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

/**
 * Dashboard Screen with MPAndroidChart for professional analytics.
 */
@Composable
fun DashboardFragment() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    
    val viewModel: DashboardViewModel = viewModel(
        factory = BudgetViewModelFactory(
            TransactionRepository(database.transactionDao()),
            AccountRepository(database.accountDao()),
            CategoryRepository(database.categoryDao()),
            ReminderRepository(database.reminderDao())
        )
    )

    val transactions by viewModel.allTransactions.observeAsState(initial = emptyList())
    val accounts by viewModel.allAccounts.observeAsState(initial = emptyList())

    // Data aggregation for Summary
    val totalBalance = accounts.sumOf { it.balance }
    val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

    // Aggregate data for Category Chart
    val expensesByCategory = remember(transactions) {
        transactions.filter { it.type == "EXPENSE" }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Financial Overview",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // SUMMARY CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Total Balance", fontSize = 14.sp)
                Text(
                    text = "₹%.2f".format(totalBalance),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DashboardSummaryMiniItem("Income", "₹%.2f".format(totalIncome), Color(0xFF2E7D32))
                    DashboardSummaryMiniItem("Expense", "₹%.2f".format(totalExpense), Color(0xFFC62828))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // INCOME vs EXPENSE CHART
        Text(
            text = "Income vs Expense",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start),
            style = MaterialTheme.typography.titleMedium
        )
        AndroidView(
            factory = { ctx ->
                BarChart(ctx).apply {
                    setupChartStyle(this)
                }
            },
            update = { chart ->
                updateIncomeExpenseChart(chart, totalIncome.toFloat(), totalExpense.toFloat())
            },
            modifier = Modifier.fillMaxWidth().height(200.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // TOP 5 CATEGORIES CHART
        Text(
            text = "Top Spending Categories",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start),
            style = MaterialTheme.typography.titleMedium
        )
        if (expensesByCategory.isNotEmpty()) {
            AndroidView(
                factory = { ctx ->
                    BarChart(ctx).apply {
                        setupChartStyle(this)
                    }
                },
                update = { chart ->
                    updateCategoryChart(chart, expensesByCategory)
                },
                modifier = Modifier.fillMaxWidth().height(200.dp)
            )
        } else {
            Text(
                text = "No expenses recorded yet",
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 16.dp),
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SPENDING RATIO
        Text(
            text = "Spending Ratio",
            modifier = Modifier.align(Alignment.Start),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        val spendingRatio = if (totalIncome > 0) (totalExpense / totalIncome).toFloat().coerceIn(0f, 1f) else 0f
        LinearProgressIndicator(
            progress = { spendingRatio },
            modifier = Modifier.fillMaxWidth().height(16.dp).padding(vertical = 8.dp),
            color = if (spendingRatio > 0.8f) Color.Red else MaterialTheme.colorScheme.primary,
            trackColor = Color.LightGray,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        Spacer(modifier = Modifier.height(24.dp))

        // RECENT TRANSACTIONS
        Text(
            text = "Recent Transactions",
            modifier = Modifier.align(Alignment.Start),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        transactions.take(5).forEach { tx ->
            DashboardRecentTxRow(tx)
        }
    }
}

// Helper: Setup common chart styling
private fun setupChartStyle(chart: BarChart) {
    chart.description.isEnabled = false
    chart.legend.isEnabled = false
    chart.setDrawGridBackground(false)
    chart.setTouchEnabled(false)

    // ⭐ NEW POLISH
    chart.setFitBars(true)
    chart.animateY(800)

    chart.xAxis.apply {
        position = XAxis.XAxisPosition.BOTTOM
        setDrawGridLines(false)
        setDrawAxisLine(false)   // NEW
        granularity = 1f
    }

    chart.axisLeft.apply {
        setDrawGridLines(false)  // NEW
        axisMinimum = 0f
    }

    chart.axisRight.isEnabled = false
}


// Helper: Update Income vs Expense Data
private fun updateIncomeExpenseChart(chart: BarChart, income: Float, expense: Float) {
    val entries = listOf(BarEntry(0f, income), BarEntry(1f, expense))
    val dataSet = BarDataSet(entries, "Overview").apply {
        colors = listOf(AndroidColor.parseColor("#2E7D32"), AndroidColor.parseColor("#C62828"))
        valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return formatAmount(value)
            }
        }
        setDrawValues(true)
    }
    
    chart.data = BarData(dataSet)
    chart.xAxis.valueFormatter = IndexAxisValueFormatter(listOf("Income", "Expense"))
    chart.animateY(1000)
    chart.invalidate()
}

// Helper: Update Category Data
private fun updateCategoryChart(chart: BarChart, categories: List<Pair<String, Double>>) {
    val entries = categories.mapIndexed { index, pair -> BarEntry(index.toFloat(), pair.second.toFloat()) }
    val dataSet = BarDataSet(entries, "Categories").apply {
        color = AndroidColor.parseColor("#6200EE")
        valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return formatAmount(value)
            }
        }
    }
    
    chart.data = BarData(dataSet)
    chart.xAxis.valueFormatter = IndexAxisValueFormatter(categories.map { it.first })
    chart.animateY(1000)
    chart.invalidate()
}

@Composable
fun DashboardSummaryMiniItem(label: String, value: String, color: Color) {
    Column {
        Text(text = label, fontSize = 12.sp)
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun DashboardRecentTxRow(tx: TransactionEntity) {
    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = tx.category, fontWeight = FontWeight.Medium)
            Text(
                text = tx.type,
                fontSize = 12.sp,
                color = if (tx.type == "INCOME") Color(0xFF2E7D32) else Color(0xFFC62828)
            )
        }
        Text(
            text = "₹%.2f".format(tx.amount),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}
private fun formatAmount(value: Float): String {
    return when {
        value >= 1000000 -> "₹%.1fM".format(value / 1000000f)
        value >= 1000 -> "₹%.1fk".format(value / 1000f)
        else -> "₹%.0f".format(value)
    }
}
