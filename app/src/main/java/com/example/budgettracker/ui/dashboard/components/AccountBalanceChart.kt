package com.example.budgettracker.ui.dashboard.components

import android.graphics.Color as AndroidColor
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.budgettracker.data.local.entities.AccountEntity
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

@Composable
fun AccountBalanceChart(data: List<AccountEntity>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Account Balance Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            AndroidView(
                factory = { context ->
                    HorizontalBarChart(context).apply {
                        description.isEnabled = false
                        legend.isEnabled = false
                        setTouchEnabled(false)
                        xAxis.apply {
                            position = XAxis.XAxisPosition.BOTTOM
                            setDrawGridLines(false)
                            setDrawAxisLine(false)
                            granularity = 1f
                            textColor = AndroidColor.GRAY
                            textSize = 10f
                        }
                        axisLeft.isEnabled = false
                        axisRight.apply {
                            setDrawGridLines(true)
                            gridColor = AndroidColor.LTGRAY
                            textColor = AndroidColor.GRAY
                        }
                    }
                },
                update = { chart ->
                    val entries = data.mapIndexed { index, account -> 
                        BarEntry(index.toFloat(), account.balance.toFloat()) 
                    }
                    val dataSet = BarDataSet(entries, "Balance").apply {
                        color = AndroidColor.parseColor("#0277BD") // Fintech Blue
                        valueTextColor = AndroidColor.BLACK
                        valueTextSize = 10f
                        setDrawValues(true)
                    }
                    chart.data = BarData(dataSet)
                    chart.xAxis.valueFormatter = IndexAxisValueFormatter(data.map { it.accountName })
                    chart.invalidate()
                },
                modifier = Modifier.fillMaxWidth().height((data.size * 50 + 100).coerceAtLeast(150).dp)
            )
        }
    }
}
