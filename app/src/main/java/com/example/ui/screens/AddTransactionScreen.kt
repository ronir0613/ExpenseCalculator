package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Calendar

val expenseCategories = listOf(
    "Shopping" to Icons.Default.ShoppingCart,
    "Food" to Icons.Default.Restaurant,
    "Phone" to Icons.Default.Phone,
    "Entertainment" to Icons.Default.Movie,
    "Education" to Icons.Default.School,
    "Beauty" to Icons.Default.Face,
    "Sports" to Icons.Default.SportsBasketball,
    "Social" to Icons.Default.People,
    "Transportation" to Icons.Default.DirectionsBus,
    "Clothing" to Icons.Default.Checkroom,
    "Car" to Icons.Default.DirectionsCar,
    "Alcohol" to Icons.Default.LocalBar,
    "Cigarettes" to Icons.Default.SmokingRooms,
    "Electronics" to Icons.Default.Computer,
    "Travel" to Icons.Default.Flight,
    "Health" to Icons.Default.Favorite,
    "Pets" to Icons.Default.Pets,
    "Repairs" to Icons.Default.Build,
    "Housing" to Icons.Default.Home,
    "Home" to Icons.Default.HomeRepairService,
    "Gifts" to Icons.Default.CardGiftcard,
    "Donations" to Icons.Default.VolunteerActivism,
    "Lottery" to Icons.Default.Casino,
    "Snacks" to Icons.Default.Fastfood,
    "Kids" to Icons.Default.ChildCare,
    "Vegetables" to Icons.Default.Eco,
    "Fruits" to Icons.Default.LocalDining,
    "Settings" to Icons.Default.Settings
)

val incomeCategories = listOf(
    "Salary" to Icons.Default.AttachMoney,
    "Investments" to Icons.Default.TrendingUp,
    "Part-Time" to Icons.Default.Work,
    "Bonus" to Icons.Default.EmojiEvents,
    "Others" to Icons.Default.Category,
    "Settings" to Icons.Default.Settings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, category: String, isDebit: Boolean, timestamp: Long) -> Unit
) {
    var currentTab by remember { mutableStateOf("Expense") }
    var isDebit by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf("Food") }
    var amountText by remember { mutableStateOf("") }
    var titleText by remember { mutableStateOf("") }

    val categories = when (currentTab) {
        "Expense" -> expenseCategories
        "Income" -> incomeCategories
        else -> listOf(
            "Contact" to Icons.Default.Person,
            "Bank" to Icons.Default.AccountBalance,
            "Wallet" to Icons.Default.AccountBalanceWallet,
            "App" to Icons.Default.PhoneAndroid,
            "Settings" to Icons.Default.Settings
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                onSave(
                                    titleText.ifBlank { selectedCategory },
                                    amt,
                                    selectedCategory,
                                    isDebit,
                                    System.currentTimeMillis()
                                )
                            }
                        }
                    ) {
                        Text("Save", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Amount & Title Input
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Text("₹", modifier = Modifier.padding(start = 16.dp)) },
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Note (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Tabs: Expense | Income | Transfer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                TabButton("Expense", currentTab == "Expense", onClick = { currentTab = "Expense"; isDebit = true; selectedCategory = "Food" }, modifier = Modifier.weight(1f))
                TabButton("Income", currentTab == "Income", onClick = { currentTab = "Income"; isDebit = false; selectedCategory = "Salary" }, modifier = Modifier.weight(1f))
                TabButton("Transfer", currentTab == "Transfer", onClick = { currentTab = "Transfer"; isDebit = true; selectedCategory = "Contact" }, modifier = Modifier.weight(1f))
            }
            
            if (currentTab == "Transfer") {
                // Sent vs Received Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    TabButton("Sent", isDebit, onClick = { isDebit = true }, modifier = Modifier.weight(1f))
                    TabButton("Received", !isDebit, onClick = { isDebit = false }, modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Categories Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(categories) { (name, icon) ->
                    CategoryIconItem(
                        name = name,
                        icon = icon,
                        isSelected = selectedCategory == name,
                        onClick = { selectedCategory = name }
                    )
                }
            }
        }
    }
}

@Composable
fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun CategoryIconItem(name: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}
