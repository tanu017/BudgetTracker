package com.example.budgettracker.ui.transactions.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.budgettracker.ui.transactions.model.BudgetHealthMetrics

@Composable
fun HealthInsightsChips(metrics: BudgetHealthMetrics) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistChip(
            onClick = { },
            label = { Text("Health: ${metrics.healthStatus} (${metrics.score})") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (metrics.score > 50) Color(0xFF4CAF50) else Color.Red
                )
            }
        )
        AssistChip(
            onClick = { },
            label = { Text("Savings: ${(metrics.savingsRatio * 100).toInt()}%") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        )
        AssistChip(
            onClick = { },
            label = { Text("Est. ₹${"%.0f".format(metrics.predictedSpend)}") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.QueryStats,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        )
    }
}
