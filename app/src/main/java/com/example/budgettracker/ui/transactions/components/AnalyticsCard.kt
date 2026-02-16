package com.example.budgettracker.ui.transactions.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.ui.transactions.model.MonthlyAnalytics

@Composable
fun AnalyticsCard(data: List<MonthlyAnalytics>) {
    if (data.isEmpty()) return

    val barWidthDp = 16.dp
    val groupWidthDp = barWidthDp * 2

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Monthly Analytics",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            val maxVal = (data.maxOfOrNull { maxOf(it.income, it.expense) } ?: 100f).coerceAtLeast(100f)

            // Chart Drawing Section
            Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val barWidthPx = barWidthDp.toPx()
                    val groupWidthPx = groupWidthDp.toPx()
                    val spacing = (canvasWidth - (data.size * groupWidthPx)) / (data.size + 1)
                    
                    data.forEachIndexed { index, item ->
                        val xBase = spacing + index * (groupWidthPx + spacing)
                        
                        // Income Bar (Green)
                        val incomeHeight = (item.income / maxVal) * canvasHeight
                        drawRect(
                            color = Color(0xFF4CAF50),
                            topLeft = Offset(xBase, canvasHeight - incomeHeight),
                            size = Size(barWidthPx, incomeHeight)
                        )
                        
                        // Expense Bar (Red)
                        val expenseHeight = (item.expense / maxVal) * canvasHeight
                        drawRect(
                            color = Color(0xFFF44336),
                            topLeft = Offset(xBase + barWidthPx, canvasHeight - expenseHeight),
                            size = Size(barWidthPx, expenseHeight)
                        )
                    }
                }
            }
            
            // Refactored Label Section: Using Row with SpaceEvenly for perfect alignment
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                data.forEach { item ->
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.width(groupWidthDp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Legend Section
            Row(
                modifier = Modifier.padding(top = 8.dp), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(10.dp).background(Color(0xFF4CAF50)))
                Text(text = " Income", fontSize = 10.sp, modifier = Modifier.padding(end = 16.dp))
                Box(modifier = Modifier.size(10.dp).background(Color(0xFFF44336)))
                Text(text = " Expense", fontSize = 10.sp)
            }
        }
    }
}
