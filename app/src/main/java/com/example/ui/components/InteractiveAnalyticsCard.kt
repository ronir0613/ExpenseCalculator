package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Transaction
import java.util.*

@Composable
fun InteractiveAnalyticsCard(
    transactions: List<Transaction>,
    preferredChartType: String,
    modifier: Modifier = Modifier
) {
    var showingChart by remember { mutableStateOf(false) }
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

    val topTransactions = remember(transactions, viewSpends) {
        transactions.filter { it.isDebit == viewSpends }
            .sortedByDescending { it.amount }
            .take(3)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showingChart = !showingChart },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    text = if (showingChart) "$preferredChartType Chart" else "Top 3 ${if(viewSpends) "Spends" else "Income"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
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

            if (!showingChart) {
                Top3ListView(topTransactions = topTransactions)
            } else {
                if (preferredChartType == "Pie" || preferredChartType == "Donut") {
                    DonutChartView(categoryTotals, totalAmount, viewSpends)
                } else if (preferredChartType == "Bar") {
                    // Quick bar view for categories
                    CategoryBarChartView(categoryTotals, totalAmount)
                } else {
                    DonutChartView(categoryTotals, totalAmount, viewSpends)
                }
            }
        }
    }
}

@Composable
fun Top3ListView(topTransactions: List<Transaction>) {
    if (topTransactions.isEmpty()) {
        Text("No records found", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            topTransactions.forEach { txn ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.List, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = txn.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(text = txn.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = String.format(Locale.ROOT, "₹%.2f", txn.amount),
                        fontWeight = FontWeight.Black,
                        color = if (txn.isDebit) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}

@Composable
fun DonutChartView(categoryTotals: Map<String, Double>, totalAmount: Double, viewSpends: Boolean) {
    if (categoryTotals.isEmpty()) {
        Text("No records found", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .weight(1.2f),
            contentAlignment = Alignment.Center
        ) {
            val animatedSweep = animateFloatAsState(targetValue = 1f, animationSpec = tween(1000), label = "Donut")
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
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            categoryTotals.entries.sortedByDescending { it.value }.take(5).forEach { (cat, amt) ->
                val percentage = (amt / totalAmount * 100).toInt()
                Row(
                    modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(12.dp).background(getCategoryColor(cat), CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = cat, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(text = String.format(Locale.ROOT, "₹%.1f (%d%%)", amt, percentage), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBarChartView(categoryTotals: Map<String, Double>, totalAmount: Double) {
    if (categoryTotals.isEmpty() || totalAmount == 0.0) {
        Text("No records found", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val maxAmt = categoryTotals.values.maxOrNull()?.toFloat() ?: 1f
    
    Column(modifier = Modifier.fillMaxWidth().height(200.dp)) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            categoryTotals.entries.sortedByDescending { it.value }.take(5).forEach { (cat, amt) ->
                val fraction = (amt.toFloat() / maxAmt)
                val animatedFraction = animateFloatAsState(targetValue = fraction, animationSpec = tween(800), label = "Bar").value
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "₹${amt.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .fillMaxHeight(animatedFraction.coerceAtLeast(0.05f))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(getCategoryColor(cat))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = cat.take(3),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
