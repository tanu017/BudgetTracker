package com.example.budgettracker.viewmodel

import androidx.lifecycle.*
import com.example.budgettracker.data.local.entities.AccountEntity
import com.example.budgettracker.data.local.entities.TransactionEntity
import com.example.budgettracker.repository.AccountRepository
import com.example.budgettracker.repository.TransactionRepository
import com.example.budgettracker.ui.transactions.engine.TransactionConsolidationEngine
import com.example.budgettracker.ui.transactions.model.TransactionListItem
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel for the Dashboard.
 * Consolidates data from both Accounts and Transactions for an overview.
 */
class DashboardViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    // Observe all transactions as LiveData
    val allTransactions: LiveData<List<TransactionEntity>> = 
        transactionRepository.getAllTransactions().asLiveData()
        
    // Observe all accounts as LiveData
    val allAccounts: LiveData<List<AccountEntity>> = 
        accountRepository.getAllAccounts().asLiveData()

    /**
     * STEP 2 & 3 — Fixed logic for Top Transactions (Highest absolute values)
     */
    val topTransactions: LiveData<List<TransactionListItem>> = transactionRepository.getAllTransactions()
        .map { transactions ->
            val consolidated = TransactionConsolidationEngine.consolidate(transactions)
            consolidated
                .sortedByDescending { kotlin.math.abs(it.amount) } // Rank by value, not time
                .take(3)
        }.asLiveData()

    /**
     * Data for Monthly Trend Chart (Last 30 days)
     */
    val monthlyTrend: LiveData<List<Pair<String, Float>>> = transactionRepository.getAllTransactions()
        .map { transactions ->
            val thirtyDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis
            val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
            
            transactions
                .filter { it.type == "EXPENSE" && it.timestamp >= thirtyDaysAgo }
                .groupBy { 
                    val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis 
                }
                .mapValues { it.value.sumOf { tx -> tx.amount }.toFloat() }
                .toList()
                .sortedBy { it.first }
                .map { sdf.format(Date(it.first)) to it.second }
        }.asLiveData()

    /**
     * Data for Category Distribution (Pie Chart)
     */
    val categoryDistribution: LiveData<List<Pair<String, Float>>> = transactionRepository.getAllTransactions()
        .map { transactions ->
            transactions
                .filter { it.type == "EXPENSE" }
                .groupBy { it.category }
                .map { it.key to it.value.sumOf { tx -> tx.amount }.toFloat() }
                .sortedByDescending { it.second }
        }.asLiveData()
}
