package com.example.budgettracker.ui.dashboard

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
import com.example.budgettracker.data.local.entities.TransactionEntity
import com.example.budgettracker.repository.AccountRepository
import com.example.budgettracker.repository.CategoryRepository
import com.example.budgettracker.repository.ReminderRepository
import com.example.budgettracker.repository.TransactionRepository
import com.example.budgettracker.viewmodel.BudgetViewModelFactory
import com.example.budgettracker.viewmodel.DashboardViewModel

/**
 * Dashboard Screen built with Jetpack Compose.
 * Displays financial summaries, spending ratio, and recent transactions.
 */
@Composable
fun DashboardFragment() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    
    // Repositories needed for the factory
    val transactionRepo = remember { TransactionRepository(database.transactionDao()) }
    val accountRepo = remember { AccountRepository(database.accountDao()) }
    val categoryRepo = remember { CategoryRepository(database.categoryDao()) }
    val reminderRepo = remember { ReminderRepository(database.reminderDao()) }

    val viewModel: DashboardViewModel = viewModel(
        factory = BudgetViewModelFactory(
            transactionRepo, accountRepo, categoryRepo, reminderRepo
        )
    )

    val transactions by viewModel.allTransactions.observeAsState(initial = emptyList())
    val accounts by viewModel.allAccounts.observeAsState(initial = emptyList())

    // UI Logic / Calculations
    val totalBalance = accounts.sumOf { it.balance }
    val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val recentTransactions = transactions.take(5)

    Column(
        modifier = Modifier
            .fillMaxSize()
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

        // 1. Summary Card
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DashboardSummaryItem("Income", "₹%.2f".format(totalIncome), Color(0xFF2E7D32))
                    DashboardSummaryItem("Expense", "₹%.2f".format(totalExpense), Color(0xFFC62828))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Spending Analytics Section
        Text(
            text = "Spending Ratio",
            modifier = Modifier.align(Alignment.Start),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )

        val spendingRatio = if (totalIncome > 0) (totalExpense / totalIncome).toFloat().coerceIn(0f, 1f) else 0f
        
        LinearProgressIndicator(
            progress = { spendingRatio },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .padding(vertical = 8.dp),
            color = if (spendingRatio > 0.8f) Color.Red else MaterialTheme.colorScheme.primary,
            trackColor = Color.LightGray,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        Text(
            text = "${(spendingRatio * 100).toInt()}% of income spent",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.End)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Recent Transactions Section
        Text(
            text = "Recent Transactions",
            modifier = Modifier.align(Alignment.Start),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        
        if (transactions.isEmpty()) {
            Box(modifier = Modifier.fillWeight(1f), contentAlignment = Alignment.Center) {
                Text(text = "No records found", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(recentTransactions) { tx ->
                    RecentTransactionRow(tx)
                }
            }
        }
    }
}

@Composable
fun DashboardSummaryItem(label: String, value: String, color: Color) {
    Column {
        Text(text = label, fontSize = 12.sp)
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun RecentTransactionRow(tx: TransactionEntity) {
    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
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

fun Modifier.fillWeight(weight: Float): Modifier = this.then(Modifier.fillMaxHeight().fillMaxWidth())
