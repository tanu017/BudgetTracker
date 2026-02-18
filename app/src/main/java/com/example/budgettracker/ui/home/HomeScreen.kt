package com.example.budgettracker.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.R
import com.example.budgettracker.ui.home.components.HeroBalanceCard
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
    val totalBalance = accounts.sumOf { it.balance }
    val savingsRate = (healthMetrics.savingsRatio * 100).toInt()

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

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
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp)
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.financial_overview),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        item {
            HeroBalanceCard(
                totalBalance = totalBalance,
                income = totalIncome,
                expense = totalExpense,
                savingsRate = savingsRate
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(0.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Monthly spending",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val spendingRatio = if (totalIncome > 0) (totalExpense / totalIncome).toFloat().coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { spendingRatio },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SmartInsightsCard(insights = insights)
                HealthInsightsChips(metrics = healthMetrics)
            }
        }

        item { AnalyticsCard(data = monthlyData) }

        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.recent_transactions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                if (transactions.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_records_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    transactions.take(3).forEach { tx ->
                        HomeTransactionPreviewItem(transaction = tx)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
