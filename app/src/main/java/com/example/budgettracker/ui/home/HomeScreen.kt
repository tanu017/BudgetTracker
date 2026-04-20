package com.example.budgettracker.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.budgettracker.R
import com.example.budgettracker.ui.home.components.HeroBalanceCard
import com.example.budgettracker.ui.home.components.HomeTransactionPreviewItem
import com.example.budgettracker.ui.transactions.components.*
import com.example.budgettracker.ui.transactions.engine.*
import com.example.budgettracker.viewmodel.DashboardViewModel
import com.example.budgettracker.ui.transactions.model.MonthlyAnalytics
import com.example.budgettracker.ui.transactions.model.TransactionListItem
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: DashboardViewModel, 
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.allTransactions.observeAsState(initial = emptyList())
    val accounts by viewModel.allAccounts.observeAsState(initial = emptyList())

    val insights = remember(transactions) { InsightsEngine.calculate(transactions) }
    val healthMetrics = remember(transactions) { BudgetHealthEngine.compute(transactions) }
    
    val consolidatedTransactions = remember(transactions) {
        TransactionConsolidationEngine.consolidate(transactions)
    }

    val externalTransactions = transactions.filter { it.type != "TRANSFER" }
    val totalIncome = externalTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = externalTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    
    val totalBalance: Double = accounts.sumOf { account ->
        BudgetHealthEngine.calculateAccountBalance(account.accountName, transactions)
    }
    
    val savingsRate = (healthMetrics.savingsRatio * 100).toInt()

    // Calculate today's stats for micro-summary
    val today = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todayTransactions = transactions.filter { it.timestamp >= today && it.type != "TRANSFER" }
    val todaySpent = todayTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

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
            val monthTransactions = externalTransactions.filter { sdf.format(Date(it.timestamp)) == monthYearKey }
            data.add(MonthlyAnalytics(label = monthLabel, income = monthTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }.toFloat(), expense = monthTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }.toFloat()))
        }
        data
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 100.dp 
            )
        ) {
            item {
                HeroBalanceCard(
                    totalBalance = totalBalance,
                    income = totalIncome,
                    expense = totalExpense,
                    savingsRate = savingsRate
                )
            }

            item {
                SpendingLimitSection(
                    totalExpense = totalExpense,
                    totalIncome = totalIncome
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SmartInsightsCard(insights = insights)
                    HealthInsightsChips(metrics = healthMetrics)
                }
            }

            item { 
                AnalyticsCard(data = monthlyData) 
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.recent_transactions),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "See All",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    navController.navigate("transactions") {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    Text(
                        text = "${todayTransactions.size} transactions today • ₹${"%,.0f".format(todaySpent)} spent",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    if (consolidatedTransactions.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = stringResource(R.string.no_records_found),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        consolidatedTransactions.take(4).forEach { listItem ->
                            when (listItem) {
                                is TransactionListItem.Regular -> {
                                    HomeTransactionPreviewItem(transaction = listItem.transaction)
                                }
                                is TransactionListItem.Transfer -> {
                                    HomeTransactionPreviewItem(
                                        transaction = listItem.sourceEntity,
                                        overrideTitle = "${listItem.fromAccount} → ${listItem.toAccount}",
                                        isTransfer = true
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SpendingLimitSection(totalExpense: Double, totalIncome: Double) {
    val progressValue = if (totalIncome > 0) (totalExpense / totalIncome).toFloat().coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progressValue,
        label = "progressAnimation"
    )
    val usagePercentage = (progressValue * 100).toInt()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(16.dp)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Monthly Spending Limit",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "$usagePercentage% used",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (usagePercentage > 95) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = if (usagePercentage > 95) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "₹${"%,.0f".format(totalExpense)} spent", 
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Limit ₹${"%,.0f".format(totalIncome)}", 
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
