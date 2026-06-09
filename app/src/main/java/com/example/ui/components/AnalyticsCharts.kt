package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Transaction
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// Helper for category colors
fun getCategoryColor(category: String): Color {
    return when (category) {
        "Food" -> CategoryFood
        "Shopping" -> CategoryShopping
        "Health" -> CategoryHealth
        "Entertainment" -> CategoryEntertainment
        "Transport" -> CategoryTransport
        "Bills & Utilities" -> CategoryBills
        "Others" -> CategoryOthers
        else -> CategoryUnclassified
    }
}

@Composable
fun SpendingDonutChart(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    var viewSpends by remember { mutableStateOf(true) }

    val categoryTotals = remember(transactions, viewSpends) {
        transactions.filter { it.isDebit == viewSpends }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .filter { it.value > 0.0 }
    }

    val totalAmount = remember(categoryTotals) {
        categoryTotals.values.sum()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category Distribution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Spent / Received toggler
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp)
                ) {
                    val activeBgColor = if (viewSpends) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else Color(0xFF2E7D32).copy(alpha = 0.15f)
                    val activeColor = if (viewSpends) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (viewSpends) activeBgColor else Color.Transparent)
                            .clickable { viewSpends = true }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Spent",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (viewSpends) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!viewSpends) activeBgColor else Color.Transparent)
                            .clickable { viewSpends = false }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Received",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (!viewSpends) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (categoryTotals.isEmpty()) {
                // Empty state donut placeholder
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color.LightGray.copy(alpha = 0.2f),
                            style = Stroke(width = 24.dp.toPx())
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "₹0.00",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (viewSpends) "No Spends" else "No Income",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Donut Canvas
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .weight(1.2f),
                        contentAlignment = Alignment.Center
                    ) {
                        val animatedSweep = animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 1000),
                            label = "DonutAnimation"
                        )

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = -90f
                            categoryTotals.forEach { (cat, amt) ->
                                val sweepAngle = (amt / totalAmount * 360f).toFloat()
                                drawArc(
                                    color = getCategoryColor(cat),
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle * animatedSweep.value,
                                    useCenter = false,
                                    style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round),
                                    size = Size(size.width, size.height)
                                )
                                startAngle += sweepAngle
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format(Locale.ROOT, "₹%.2f", totalAmount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (viewSpends) "Spent" else "Received",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (viewSpends) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Legend Column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        categoryTotals.entries.sortedByDescending { it.value }.take(5).forEach { (cat, amt) ->
                            val percentage = (amt / totalAmount * 100).toInt()
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(getCategoryColor(cat), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = String.format(Locale.ROOT, "₹%.1f (%d%%)", amt, percentage),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        if (categoryTotals.size > 5) {
                            Text(
                                text = "+ ${categoryTotals.size - 5} more...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 20.dp, top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlySummaryBarChart(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    // Interactive filter for Debit vs Credit on charts
    var viewSpends by remember { mutableStateOf(true) }

    // Generate transactions grouped by Month for last 6 months
    val calendar = Calendar.getInstance()
    val monthFormatter = SimpleDateFormat("MMM", Locale.ROOT)

    // Build map of last 6 months sequentially
    val last6MonthsList = remember {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -5)
        for (i in 0..5) {
            list.add(monthFormatter.format(cal.time))
            cal.add(Calendar.MONTH, 1)
        }
        list
    }

    val monthlySums = remember(transactions, last6MonthsList, viewSpends) {
        val sums = mutableMapOf<String, Double>()
        last6MonthsList.forEach { sums[it] = 0.0 }

        val cal = Calendar.getInstance()
        transactions.filter { it.isDebit == viewSpends }.forEach { txn ->
            cal.timeInMillis = txn.timestamp
            val mName = monthFormatter.format(cal.time)
            if (sums.containsKey(mName)) {
                sums[mName] = (sums[mName] ?: 0.0) + txn.amount
            }
        }
        sums
    }

    val maxAmount = remember(monthlySums) {
        val maxVal = monthlySums.values.maxOrNull() ?: 0.0
        if (maxVal == 0.0) 100.0 else maxVal * 1.15
    }

    var selectedIndex by remember { mutableStateOf(5) } // Default to current month (index 5)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Monthly Ledger Chart",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (viewSpends) "Tracking expenditures" else "Tracking inflows",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Spent vs Received selector for Bar Chart
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp)
                ) {
                    val activeBgColor = if (viewSpends) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else Color(0xFF2E7D32).copy(alpha = 0.15f)
                    val activeColor = if (viewSpends) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (viewSpends) activeBgColor else Color.Transparent)
                            .clickable { viewSpends = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Spent",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (viewSpends) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!viewSpends) activeBgColor else Color.Transparent)
                            .clickable { viewSpends = false }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Received",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (!viewSpends) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val monthName = last6MonthsList[selectedIndex]
                val selectionValue = monthlySums[monthName] ?: 0.0
                
                Text(
                    text = if (viewSpends) "Expense Total" else "Inflow Total",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = String.format(Locale.ROOT, "%s: ₹%.2f", monthName, selectionValue),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = if (viewSpends) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Canvas Grid and Bars
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val accentColor = if (viewSpends) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                val mutedColor = accentColor.copy(alpha = 0.35f)
                val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    // Draw horizontal baseline
                    val baselineY = canvasHeight - 24.dp.toPx()
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, baselineY),
                        end = Offset(canvasWidth, baselineY),
                        strokeWidth = 2f
                    )

                    // Draw half grid line
                    val midY = baselineY / 2
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, midY),
                        end = Offset(canvasWidth, midY),
                        strokeWidth = 1f
                    )

                    // Draw bars
                    val barCount = last6MonthsList.size
                    val spacing = canvasWidth / barCount

                    for (i in 0 until barCount) {
                        val mName = last6MonthsList[i]
                        val sumVal = monthlySums[mName] ?: 0.0
                        val barHeight = ((sumVal / maxAmount) * (baselineY)).toFloat()

                        val barWidth = 24.dp.toPx()
                        val xOffset = (i * spacing) + (spacing / 2) - (barWidth / 2)
                        
                        // Draw vertical rounded bar
                        val colorToUse = if (i == selectedIndex) accentColor else mutedColor
                        val barTopY = baselineY - redactMinHeight(barHeight, 6f)

                        drawRect(
                            color = colorToUse,
                            topLeft = Offset(xOffset, barTopY),
                            size = Size(barWidth, baselineY - barTopY)
                        )
                    }
                }

                // Invisible touch elements covering bars for precise interaction
                Row(modifier = Modifier.fillMaxSize()) {
                    for (i in 0 until last6MonthsList.size) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    selectedIndex = i
                                }
                        )
                    }
                }
            }

            // Month Labels Row below Canvas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                last6MonthsList.forEachIndexed { idx, item ->
                    Box(
                        modifier = Modifier
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (idx == selectedIndex) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (idx == selectedIndex) (if (viewSpends) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// Ensure columns are slightly visible even for ₹0
private fun redactMinHeight(valIn: Float, min: Float): Float {
    return if (valIn == 0f) 0f else if (valIn < min) min else valIn
}
