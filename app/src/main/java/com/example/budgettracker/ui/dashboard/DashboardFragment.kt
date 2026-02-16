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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.budgettracker.R
import com.example.budgettracker.data.local.entities.TransactionEntity
import com.example.budgettracker.viewmodel.DashboardViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

/**
 * Dashboard Screen - Refocused for professional data analytics without large branded headers.
 */
@Composable
fun DashboardFragment(viewModel: DashboardViewModel) {
    val transactions by viewModel.allTransactions.observeAsState(initial = emptyList())
    val accounts by viewModel.allAccounts.observeAsState(initial = emptyList())

    // Data aggregation for Summary
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // INCOME vs EXPENSE CHART
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Income vs Expense",
                fontWeight = FontWeight.Bold,
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
        }

        // TOP 5 CATEGORIES CHART
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Top Spending Categories",
                fontWeight = FontWeight.Bold,
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
                    text = stringResource(R.string.no_records_found),
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 16.dp),
                    fontSize = 12.sp
                )
            }
        }

        // SPENDING RATIO
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.spending_ratio),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            val spendingRatio = if (totalIncome > 0) (totalExpense / totalIncome).toFloat().coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(
                progress = { spendingRatio },
                modifier = Modifier.fillMaxWidth().height(16.dp),
                color = if (spendingRatio > 0.8f) Color.Red else MaterialTheme.colorScheme.primary,
                trackColor = Color.LightGray,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }

        // RECENT TRANSACTIONS
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Analytics Detail",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            transactions.take(5).forEach { tx ->
                DashboardRecentTxRow(tx)
            }
        }
    }
}

// Helper: Setup common chart styling
private fun setupChartStyle(chart: BarChart) {
    chart.description.isEnabled = false
    chart.legend.isEnabled = false
    chart.setDrawGridBackground(false)
    chart.setTouchEnabled(false)
    chart.setFitBars(true)
    chart.animateY(800)

    chart.xAxis.apply {
        position = XAxis.XAxisPosition.BOTTOM
        setDrawGridLines(false)
        setDrawAxisLine(false)
        granularity = 1f
    }

    chart.axisLeft.apply {
        setDrawGridLines(false)
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
    chart.invalidate()
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
