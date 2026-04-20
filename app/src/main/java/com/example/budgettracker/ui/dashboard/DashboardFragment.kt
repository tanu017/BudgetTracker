package com.example.budgettracker.ui.dashboard

import android.graphics.Color as AndroidColor
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.budgettracker.ui.transactions.utils.TransactionDateUtils
import com.example.budgettracker.ui.dashboard.components.*
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

@Composable
fun DashboardFragment(viewModel: DashboardViewModel) {
    val transactions by viewModel.allTransactions.observeAsState(initial = emptyList())
    val accounts by viewModel.allAccounts.observeAsState(initial = emptyList())
    val monthlyTrend by viewModel.monthlyTrend.observeAsState(initial = emptyList())
    val categoryDistribution by viewModel.categoryDistribution.observeAsState(initial = emptyList())
    val topTransactions by viewModel.topTransactions.observeAsState(initial = emptyList())
    
    val consolidatedTransactions = remember(transactions) {
        TransactionConsolidationEngine.consolidate(transactions)
    }

    val externalTransactions = consolidatedTransactions
        .filterIsInstance<TransactionListItem.Regular>()
        .map { it.transaction }
        .filter { it.type != "TRANSFER" }

    val totalIncome = externalTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = externalTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val netWorth = accounts.sumOf { it.balance }

    val highestExpense = externalTransactions.filter { it.type == "EXPENSE" }.maxByOrNull { it.amount }
    val mostUsedCategory = externalTransactions.groupBy { it.category }.maxByOrNull { it.value.size }?.key ?: "N/A"

    val expensesByCategory = remember(externalTransactions) {
        externalTransactions.filter { it.type == "EXPENSE" }
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
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // STEP 1 — Net Worth Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Total Net Worth",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
                Text(
                    text = "₹%.2f".format(netWorth),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // STEP 2 — Quick Insights Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InsightCard(
                modifier = Modifier.weight(1f),
                title = "Max Exp",
                value = highestExpense?.let { "₹%.0f".format(it.amount) } ?: "₹0",
                icon = Icons.Default.ArrowUpward,
                color = MaterialTheme.colorScheme.errorContainer
            )
            InsightCard(
                modifier = Modifier.weight(1f),
                title = "Popular",
                value = mostUsedCategory,
                icon = Icons.Default.Category,
                color = MaterialTheme.colorScheme.secondaryContainer
            )
            InsightCard(
                modifier = Modifier.weight(1f),
                title = "Total Tx",
                value = transactions.size.toString(),
                icon = Icons.Default.List,
                color = MaterialTheme.colorScheme.tertiaryContainer
            )
        }

        if (monthlyTrend.isNotEmpty()) {
            MonthlyTrendChart(data = monthlyTrend)
        }

        if (categoryDistribution.isNotEmpty()) {
            CategoryPieChart(data = categoryDistribution)
        }

        if (accounts.isNotEmpty()) {
            AccountBalanceChart(data = accounts)
        }

        // Using the new adaptive component for Top Spending Categories
        TopSpendingCategoriesChart(expensesByCategory = expensesByCategory)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Spending Ratio",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // REDESIGNED: Top Transactions (Limited to 3, sorted by amount)
        if (topTransactions.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Top Transactions",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 4.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        topTransactions.forEachIndexed { index, listItem ->
                            val rank = index + 1
                            when (listItem) {
                                is TransactionListItem.Regular -> {
                                    TopTransactionItem(
                                        rank = rank,
                                        title = listItem.transaction.category,
                                        amount = listItem.transaction.amount,
                                        date = TransactionDateUtils.formatDate(listItem.transaction.timestamp),
                                        account = listItem.transaction.accountName,
                                        isIncome = listItem.transaction.type == "INCOME"
                                    )
                                }
                                is TransactionListItem.Transfer -> {
                                    TopTransactionItem(
                                        rank = rank,
                                        title = "Transfer",
                                        amount = listItem.amount,
                                        date = TransactionDateUtils.formatDate(listItem.timestamp),
                                        account = "${listItem.fromAccount} → ${listItem.toAccount}",
                                        isIncome = false
                                    )
                                }
                            }
                            if (index < topTransactions.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopTransactionItem(
    rank: Int,
    title: String,
    amount: Double,
    date: String,
    account: String,
    isIncome: Boolean
) {
    val backgroundColor = if (rank == 1) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent
    val titleWeight = if (rank == 1) FontWeight.Bold else FontWeight.Medium
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#$rank",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.width(36.dp),
            fontWeight = FontWeight.Bold
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = titleWeight,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = "$account • $date",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = "${if (isIncome) "+" else "-"}₹%.0f".format(amount),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.ExtraBold,
            color = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
        )
    }
}

@Composable
fun InsightCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}
