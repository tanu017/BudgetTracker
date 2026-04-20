package com.example.budgettracker.ui.dashboard.components

import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

@Composable
fun CategoryPieChart(data: List<Pair<String, Float>>) {
    val isDark = isSystemInDarkTheme()
    val totalAmount = data.sumOf { it.second.toDouble() }.toFloat()
    
    // Theme-aware color palette
    val chartColors = if (isDark) {
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer
        )
    } else {
        listOf(
            Color(0xFF673AB7), // Deep Purple
            Color(0xFF03A9F4), // Light Blue
            Color(0xFF4CAF50), // Green
            Color(0xFFFFC107), // Amber
            Color(0xFFE91E63), // Pink
            Color(0xFF009688), // Teal
            Color(0xFFFF5722)  // Deep Orange
        )
    }

    var selectedEntry by remember { mutableStateOf<PieEntry?>(null) }
    val scale by animateFloatAsState(targetValue = if (selectedEntry != null) 1.03f else 1f, label = "ScaleAnimation")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Category Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { context ->
                        PieChart(context).apply {
                            description.isEnabled = false
                            legend.isEnabled = false // Custom legend implemented below
                            isDrawHoleEnabled = true
                            setHoleColor(AndroidColor.TRANSPARENT)
                            setTransparentCircleAlpha(0)
                            holeRadius = 65f
                            setDrawEntryLabels(false) // Remove labels inside slices
                            setTouchEnabled(true)
                            animateY(1000)
                            
                            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                                override fun onValueSelected(e: Entry?, h: Highlight?) {
                                    selectedEntry = e as? PieEntry
                                }

                                override fun onNothingSelected() {
                                    selectedEntry = null
                                }
                            })
                        }
                    },
                    update = { chart ->
                        val entries = data.map { PieEntry(it.second, it.first) }
                        val dataSet = PieDataSet(entries, "").apply {
                            colors = chartColors.map { it.toArgb() }
                            sliceSpace = 4f
                            setDrawValues(false) // Clean slices
                            selectionShift = 8f
                        }
                        chart.data = PieData(dataSet)
                        chart.invalidate()
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Center Content
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedVisibility(
                        visible = selectedEntry != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        selectedEntry?.let { entry ->
                            val percentage = (entry.value / totalAmount * 100).toInt()
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = entry.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "₹%.0f".format(entry.value),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$percentage%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    if (selectedEntry == null) {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₹%.0f".format(totalAmount),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Custom Legend
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                data.forEachIndexed { index, pair ->
                    LegendItem(
                        color = chartColors[index % chartColors.size],
                        label = pair.first,
                        amount = pair.second,
                        isSelected = selectedEntry?.label == pair.first
                    )
                }
            }
        }
    }
}

@Composable
fun LegendItem(
    color: Color,
    label: String,
    amount: Float,
    isSelected: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = "₹%.0f".format(amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
