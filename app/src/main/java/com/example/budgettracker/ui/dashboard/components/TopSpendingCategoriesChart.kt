package com.example.budgettracker.ui.dashboard.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

@Composable
fun TopSpendingCategoriesChart(expensesByCategory: List<Pair<String, Double>>) {
    val isDark = isSystemInDarkTheme()
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f).toArgb()
    val barColor = MaterialTheme.colorScheme.primary.toArgb()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Top Spending Categories",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (expensesByCategory.isNotEmpty()) {
                AndroidView(
                    factory = { ctx ->
                        BarChart(ctx).apply {
                            description.isEnabled = false
                            legend.isEnabled = false
                            setDrawGridBackground(false)
                            setTouchEnabled(false)
                            setFitBars(true)
                            
                            xAxis.apply {
                                position = XAxis.XAxisPosition.BOTTOM
                                setDrawGridLines(false)
                                setDrawAxisLine(false)
                                granularity = 1f
                                this.textColor = secondaryTextColor
                                textSize = 10f
                            }

                            axisLeft.apply {
                                setDrawGridLines(true)
                                this.gridColor = gridColor
                                axisLineColor = AndroidColor.TRANSPARENT
                                axisMinimum = 0f
                                this.textColor = secondaryTextColor
                            }

                            axisRight.isEnabled = false
                        }
                    },
                    update = { chart ->
                        val entries = expensesByCategory.mapIndexed { index, pair -> 
                            BarEntry(index.toFloat(), pair.second.toFloat()) 
                        }
                        val dataSet = BarDataSet(entries, "Categories").apply {
                            color = barColor
                            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                                override fun getFormattedValue(value: Float): String = formatAmount(value)
                            }
                            setDrawValues(true)
                            this.valueTextColor = textColor
                            valueTextSize = 10f
                        }
                        
                        chart.data = BarData(dataSet).apply { barWidth = 0.5f }
                        chart.xAxis.valueFormatter = IndexAxisValueFormatter(expensesByCategory.map { it.first })
                        chart.xAxis.textColor = secondaryTextColor
                        chart.axisLeft.textColor = secondaryTextColor
                        chart.axisLeft.gridColor = gridColor
                        chart.invalidate()
                    },
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No records found", 
                        color = MaterialTheme.colorScheme.onSurfaceVariant, 
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

private fun formatAmount(value: Float): String {
    return when {
        value >= 100000 -> "₹%.1fL".format(value / 100000f)
        value >= 1000 -> "₹%.1fk".format(value / 1000f)
        else -> "₹%.0f".format(value)
    }
}
