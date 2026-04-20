package com.example.budgettracker.ui.dashboard.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

@Composable
fun MonthlyTrendChart(data: List<Pair<String, Float>>) {
    val isDark = isSystemInDarkTheme()
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f).toArgb()
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Monthly Spending Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            AndroidView(
                factory = { context ->
                    LineChart(context).apply {
                        description.isEnabled = false
                        legend.isEnabled = false
                        setTouchEnabled(false)
                        xAxis.apply {
                            position = XAxis.XAxisPosition.BOTTOM
                            setDrawGridLines(false)
                            granularity = 1f
                            this.textColor = textColor
                            textSize = 10f
                        }
                        axisLeft.apply {
                            setDrawGridLines(true)
                            this.gridColor = gridColor
                            this.textColor = textColor
                        }
                        axisRight.isEnabled = false
                    }
                },
                update = { chart ->
                    val entries = data.mapIndexed { index, pair -> Entry(index.toFloat(), pair.second) }
                    val dataSet = LineDataSet(entries, "Spending").apply {
                        color = primaryColor
                        setCircleColor(primaryColor)
                        lineWidth = 3f
                        circleRadius = 4f
                        setDrawCircleHole(false)
                        mode = LineDataSet.Mode.CUBIC_BEZIER
                        setDrawFilled(true)
                        fillColor = primaryColor
                        fillAlpha = if (isDark) 80 else 50
                        valueTextColor = textColor
                        valueTextSize = 10f
                    }
                    chart.data = LineData(dataSet)
                    chart.xAxis.valueFormatter = IndexAxisValueFormatter(data.map { it.first })
                    chart.xAxis.textColor = textColor
                    chart.axisLeft.textColor = textColor
                    chart.axisLeft.gridColor = gridColor
                    chart.invalidate()
                },
                modifier = Modifier.fillMaxWidth().height(200.dp)
            )
        }
    }
}
