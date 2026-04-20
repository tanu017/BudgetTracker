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
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

@Composable
fun CategoryPieChart(data: List<Pair<String, Float>>) {
    val pastelColors = listOf(
        "#FFB7B2", "#FFDAC1", "#E2F0CB", "#B5EAD7", "#C7CEEA",
        "#F3D1F4", "#F4CFDF", "#B9FBC0", "#98F5E1", "#8ECAE6"
    ).map { AndroidColor.parseColor(it) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Category Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            AndroidView(
                factory = { context ->
                    PieChart(context).apply {
                        description.isEnabled = false
                        isDrawHoleEnabled = true
                        setHoleColor(AndroidColor.TRANSPARENT)
                        setTransparentCircleAlpha(0)
                        holeRadius = 60f
                        legend.isEnabled = true
                        legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                        legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                        legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
                        legend.setDrawInside(false)
                        legend.textColor = AndroidColor.GRAY
                        animateY(1000)
                    }
                },
                update = { chart ->
                    val entries = data.map { PieEntry(it.second, it.first) }
                    val dataSet = PieDataSet(entries, "").apply {
                        colors = pastelColors
                        sliceSpace = 3f
                        setDrawValues(false)
                    }
                    chart.data = PieData(dataSet)
                    chart.invalidate()
                },
                modifier = Modifier.fillMaxWidth().height(250.dp)
            )
        }
    }
}
