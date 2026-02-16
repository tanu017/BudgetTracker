package com.example.budgettracker.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.R
import com.example.budgettracker.ui.home.components.HomeTransactionPreviewItem
import com.example.budgettracker.ui.transactions.components.*
import com.example.budgettracker.ui.transactions.engine.*
import com.example.budgettracker.viewmodel.DashboardViewModel
import com.example.budgettracker.ui.transactions.model.MonthlyAnalytics
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(viewModel: DashboardViewModel) {
    val transactions by viewModel.allTransactions.observeAsState(initial = emptyList())
    val accounts by viewModel.allAccounts.observeAsState(initial = emptyList())

    val insights = remember(transactions) { InsightsEngine.calculate(transactions) }
    val healthMetrics = remember(transactions) { BudgetHealthEngine.compute(transactions) }

    val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val balance = totalIncome - totalExpense

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

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.financial_overview),
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
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

        item { AnalyticsCard(data = monthlyData) }

        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.recent_transactions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                // Fintech-style read-only preview for the Home screen
                transactions.take(3).forEach { tx ->
                    HomeTransactionPreviewItem(transaction = tx)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
