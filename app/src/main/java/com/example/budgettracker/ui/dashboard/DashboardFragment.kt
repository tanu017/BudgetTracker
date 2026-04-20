package com.example.budgettracker.ui.dashboard

import android.graphics.Color as AndroidColor
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.budgettracker.ui.transactions.engine.TransactionConsolidationEngine
import com.example.budgettracker.ui.transactions.model.TransactionListItem
import com.example.budgettracker.ui.transactions.components.TransactionItem
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

@Composable
fun DashboardFragment(viewModel: DashboardViewModel) {
    val transactions by viewModel.allTransactions.observeAsState(initial = emptyList())
    
    val consolidatedTransactions = remember(transactions) {
        TransactionConsolidationEngine.consolidate(transactions)
    }

    val externalTransactions = consolidatedTransactions
        .filterIsInstance<TransactionListItem.Regular>()
        .map { it.transaction }
        .filter { it.type != "TRANSFER" }

    val totalIncome = externalTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = externalTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

    val expensesByCategory = remember(externalTransactions) {
        externalTransactions.filter { it.type == "EXPENSE" }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
    }

    // Removed windowInsetsPadding(WindowInsets.systemBars)
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            modifier = Modifier.fillMaxWidth().height(220.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_records_found),
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.spending_ratio),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    val spendingRatio = if (totalIncome > 0) (totalExpense / totalIncome).toFloat().coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { spendingRatio },
                        modifier = Modifier.fillMaxWidth().height(12.dp),
                        color = if (spendingRatio > 0.8f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Text(
                        text = "%.0f%% of income spent".format(spendingRatio * 100),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Analytics Detail",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 4.dp)
                )
                consolidatedTransactions.take(5).forEach { listItem ->
                    when (listItem) {
                        is TransactionListItem.Regular -> {
                            TransactionItem(
                                transaction = listItem.transaction,
                                onDelete = {},
                                onClick = {},
                                showDelete = false
                            )
                        }
                        is TransactionListItem.Transfer -> {
                            TransactionItem(
                                transaction = listItem.sourceEntity,
                                onDelete = {},
                                onClick = {},
                                overrideTitle = "${listItem.fromAccount} → ${listItem.toAccount}",
                                isTransfer = true,
                                showDelete = false
                            )
                        }
                    }
                }
            }
        }
    }
}

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
        textColor = AndroidColor.GRAY
    }

    chart.axisLeft.apply {
        setDrawGridLines(true)
        gridColor = AndroidColor.LTGRAY
        axisLineColor = AndroidColor.TRANSPARENT
        axisMinimum = 0f
        textColor = AndroidColor.GRAY
    }

    chart.axisRight.isEnabled = false
}

private fun updateIncomeExpenseChart(chart: BarChart, income: Float, expense: Float) {
    val entries = listOf(BarEntry(0f, income), BarEntry(1f, expense))
    val dataSet = BarDataSet(entries, "Overview").apply {
        colors = listOf(AndroidColor.parseColor("#4CAF50"), AndroidColor.parseColor("#F44336"))
        valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String = formatAmount(value)
        }
        setDrawValues(true)
        valueTextSize = 10f
    }
    
    chart.data = BarData(dataSet).apply { barWidth = 0.6f }
    chart.xAxis.valueFormatter = IndexAxisValueFormatter(listOf("Income", "Expense"))
    chart.invalidate()
}

private fun updateCategoryChart(chart: BarChart, categories: List<Pair<String, Double>>) {
    val entries = categories.mapIndexed { index, pair -> BarEntry(index.toFloat(), pair.second.toFloat()) }
    val dataSet = BarDataSet(entries, "Categories").apply {
        color = AndroidColor.parseColor("#673AB7")
        valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String = formatAmount(value)
        }
        setDrawValues(true)
        valueTextSize = 10f
    }
    
    chart.data = BarData(dataSet).apply { barWidth = 0.6f }
    chart.xAxis.valueFormatter = IndexAxisValueFormatter(categories.map { it.first })
    chart.invalidate()
}

private fun formatAmount(value: Float): String {
    return when {
        value >= 100000 -> "₹%.1fL".format(value / 100000f)
        value >= 1000 -> "₹%.1fk".format(value / 1000f)
        else -> "₹%.0f".format(value)
    }
}
