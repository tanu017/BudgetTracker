package com.example.budgettracker.ui.transactions.engine

import com.example.budgettracker.data.local.entities.TransactionEntity
import com.example.budgettracker.ui.transactions.model.SmartInsight
import java.util.*

object InsightsEngine {
    fun calculate(transactions: List<TransactionEntity>): List<SmartInsight> {
        val list = mutableListOf<SmartInsight>()
        if (transactions.isEmpty()) {
            return listOf(SmartInsight("Welcome", "Add transactions to see smart insights", "INFO"))
        }

        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        val thisMonthTransactions = transactions.filter {
            val txCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            txCal.get(Calendar.MONTH) == currentMonth && txCal.get(Calendar.YEAR) == currentYear
        }

        // Top Category logic
        thisMonthTransactions.filter { it.type == "EXPENSE" }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .maxByOrNull { it.value }?.let {
                list.add(SmartInsight("Top Category", "${it.key} • ₹${"%.0f".format(it.value)}", "INFO"))
            }

        // MoM Comparison logic
        val prevMonth = if (currentMonth == 0) 11 else currentMonth - 1
        val prevYear = if (currentMonth == 0) currentYear - 1 else currentYear
        val prevExp = transactions.filter {
            val txCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            it.type == "EXPENSE" && txCal.get(Calendar.MONTH) == prevMonth && txCal.get(Calendar.YEAR) == prevYear
        }.sumOf { it.amount }
        val currExp = thisMonthTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        if (prevExp > 0) {
            val diff = ((currExp - prevExp) / prevExp) * 100
            if (diff > 0) list.add(SmartInsight("Month Comparison", "⬆ Increased by ${diff.toInt()}%", "WARNING"))
            else if (diff < 0) list.add(SmartInsight("Month Comparison", "⬇ Reduced by ${(-diff).toInt()}%", "POSITIVE"))
        }

        return list.take(3)
    }
}
