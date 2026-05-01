package com.example.budgettracker.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.budgettracker.R
import com.example.budgettracker.ui.home.components.HeroBalanceCard
import com.example.budgettracker.ui.transactions.engine.*
import com.example.budgettracker.viewmodel.DashboardViewModel
import com.example.budgettracker.ui.transactions.model.MonthlyAnalytics
import com.example.budgettracker.ui.transactions.model.TransactionListItem
import com.example.budgettracker.ui.transactions.model.BudgetHealthMetrics
import com.example.budgettracker.ui.transactions.model.SmartInsight
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: DashboardViewModel, 
    navController: NavController,
    onLogout: () -> Unit = {},
    userName: String = "User",
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.allTransactions.observeAsState(initial = emptyList())
    val accounts by viewModel.allAccounts.observeAsState(initial = emptyList())
    
    // Existing Logout Dialog State
    var showLogoutDialog by remember { mutableStateOf(false) }

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
            verticalArrangement = Arrangement.spacedBy(20.dp),
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
                InsightPills(metrics = healthMetrics, insights = insights)
            }

            item { 
                HomeAnalyticsCard(data = monthlyData) 
            }

            item {
                ActivitySection(
                    transactions = consolidatedTransactions.take(4),
                    todayStats = "${todayTransactions.size} transactions today • ₹${"%,.0f".format(todaySpent)} spent",
                    onSeeAllClick = {
                        navController.navigate("transactions") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            
            // Bottom Logout Button (Existing)
            item {
                Button(
                    onClick = { showLogoutDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(Modifier.width(8.dp))
                    Text("Logout", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // REUSED AlertDialog Block
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Logout") },
                text = { Text("Are you sure you want to sign out of your account?") },
                confirmButton = {
                    TextButton(onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }) {
                        Text("Logout", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun InsightPills(metrics: BudgetHealthMetrics, insights: List<SmartInsight>) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        item {
            InsightPill(icon = Icons.Default.Favorite, text = "Health ${metrics.score}", iconColor = Color.Red)
        }
        item {
            InsightPill(icon = Icons.Default.TrendingUp, text = "Savings ${(metrics.savingsRatio * 100).toInt()}%", iconColor = Color(0xFF4CAF50))
        }
        
        // Find top category insight or use a default one
        val topCategory = insights.find { it.title == "Top Category" }
        val amountPart = topCategory?.value?.split("•")?.lastOrNull()?.trim() ?: "₹0"
        
        item {
            InsightPill(
                icon = Icons.Default.Whatshot, 
                text = "Top $amountPart", 
                iconColor = Color(0xFFFF9800)
            )
        }
    }
}

@Composable
fun InsightPill(icon: ImageVector, text: String, iconColor: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = iconColor)
            Text(text = text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun HomeAnalyticsCard(data: List<MonthlyAnalytics>) {
    if (data.isEmpty()) return

    val incomeColor = MaterialTheme.colorScheme.primary
    val expenseColor = MaterialTheme.colorScheme.error
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder(enabled = true)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Monthly Analytics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(20.dp))
            
            val maxVal = (data.maxOfOrNull { maxOf(it.income, it.expense) } ?: 100f).coerceAtLeast(100f)

            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val barWidthPx = 14.dp.toPx()
                    val groupWidthPx = barWidthPx * 2.8f
                    val spacing = (canvasWidth - (data.size * groupWidthPx)) / (data.size + 1)
                    val cornerRadius = CornerRadius(12.dp.toPx())

                    // Draw subtle grid lines
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val y = canvasHeight - (i * (canvasHeight / gridLines))
                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    
                    data.forEachIndexed { index, item ->
                        val xBase = spacing + index * (groupWidthPx + spacing)
                        
                        // Income Bar (Gradient)
                        val incomeHeight = (item.income / maxVal) * canvasHeight
                        if (incomeHeight > 0) {
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(incomeColor.copy(alpha = 0.85f), incomeColor.copy(alpha = 0.2f)),
                                    startY = canvasHeight - incomeHeight,
                                    endY = canvasHeight
                                ),
                                topLeft = Offset(xBase, canvasHeight - incomeHeight),
                                size = Size(barWidthPx, incomeHeight),
                                cornerRadius = cornerRadius
                            )
                        }
                        
                        // Expense Bar (Gradient)
                        val expenseHeight = (item.expense / maxVal) * canvasHeight
                        if (expenseHeight > 0) {
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(expenseColor.copy(alpha = 0.85f), expenseColor.copy(alpha = 0.2f)),
                                    startY = canvasHeight - expenseHeight,
                                    endY = canvasHeight
                                ),
                                topLeft = Offset(xBase + barWidthPx + 6.dp.toPx(), canvasHeight - expenseHeight),
                                size = Size(barWidthPx, expenseHeight),
                                cornerRadius = cornerRadius
                            )
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                data.forEach { item ->
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Legend
            Row(
                modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendIndicator(color = incomeColor, label = "Income")
                Spacer(modifier = Modifier.width(20.dp))
                LegendIndicator(color = expenseColor, label = "Expense")
            }
        }
    }
}

@Composable
fun LegendIndicator(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50))
                .background(color.copy(alpha = 0.85f))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ActivitySection(
    transactions: List<TransactionListItem>,
    todayStats: String,
    onSeeAllClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "See All",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onSeeAllClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        
        Text(
            text = todayStats,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        if (transactions.isEmpty()) {
            Text(
                text = stringResource(R.string.no_records_found),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            transactions.forEach { listItem ->
                ActivityItem(listItem)
            }
        }
    }
}

@Composable
fun ActivityItem(listItem: TransactionListItem) {
    val title: String
    val subtitle: String
    val amount: Double
    val isIncome: Boolean
    val type: String

    when (listItem) {
        is TransactionListItem.Regular -> {
            title = listItem.transaction.category
            val dateStr = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(listItem.transaction.timestamp))
            subtitle = "${listItem.transaction.type} • $dateStr"
            amount = listItem.transaction.amount
            isIncome = listItem.transaction.type == "INCOME"
            type = listItem.transaction.type
        }
        is TransactionListItem.Transfer -> {
            title = "${listItem.fromAccount} → ${listItem.toAccount}"
            val dateStr = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(listItem.sourceEntity.timestamp))
            subtitle = "Transfer • $dateStr"
            amount = listItem.sourceEntity.amount
            isIncome = false
            type = "TRANSFER"
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                val iconBgColor = if (isIncome) Color(0xFF4CAF50).copy(alpha = 0.1f) 
                                 else if (type == "TRANSFER") MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                 else MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                
                val iconColor = if (isIncome) Color(0xFF4CAF50) 
                               else if (type == "TRANSFER") MaterialTheme.colorScheme.secondary
                               else MaterialTheme.colorScheme.error

                val icon = if (isIncome) Icons.Default.ArrowUpward 
                          else if (type == "TRANSFER") Icons.Default.SyncAlt
                          else Icons.Default.ArrowDownward

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = iconColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "${if (isIncome) "+" else "-"}₹${"%,.0f".format(amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isIncome) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
            )
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
