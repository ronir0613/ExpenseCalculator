package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import android.app.Application
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.TextStyle
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Transaction
import com.example.ui.components.InteractiveAnalyticsCard
import com.example.ui.components.MonthlySummaryBarChart
import com.example.ui.components.SpendingDonutChart
import com.example.ui.components.getCategoryColor
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    onThemeToggle: () -> Unit,
    isDarkState: Boolean,
    viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory(LocalContext.current.applicationContext as Application))
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var isNotificationEnabled by remember { 
        mutableStateOf(NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)) 
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isNotificationEnabled = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val transactions by viewModel.transactions.collectAsState()
    val parseStatusText by viewModel.parseStatusMessage.collectAsState()

    var activeTab by remember { mutableStateOf("Home") } // "Home", "Charts", "Reports", "Profile"
    var searchText by remember { mutableStateOf("") }
    
    // Add custom expense dialog
    var showAddDialog by remember { mutableStateOf(false) }
    var addTitle by remember { mutableStateOf("") }
    var addAmount by remember { mutableStateOf("") }
    var addCategory by remember { mutableStateOf("Food") }
    var addIsDebit by remember { mutableStateOf(true) }
    var selectedMonthTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    
    val parentMonthList = remember {
        val list = mutableListOf<Pair<String, Long>>()
        val cal = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.ROOT)
        for (i in 0..5) {
            val customCal = Calendar.getInstance()
            customCal.time = cal.time
            customCal.set(Calendar.DAY_OF_MONTH, 15)
            list.add(Pair(monthFormat.format(customCal.time), customCal.timeInMillis))
            cal.add(Calendar.MONTH, -1)
        }
        list
    }

    val categories = listOf("Food", "Shopping", "Health", "Entertainment", "Transport", "Bills & Utilities", "Others")

    // SMS Permission launcher
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.scanDeviceSmsInbox()
        } else {
            Toast.makeText(context, "SMS read permission denied. Manual paste or simulation available.", Toast.LENGTH_LONG).show()
        }
    }

    // Monitor toast status messages from VM
    LaunchedEffect(parseStatusText) {
        parseStatusText?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                title = {
                    Text(
                        text = "Money Tracker",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                    )
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Calendar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButtonPosition = androidx.compose.material3.FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                val items = listOf("Home" to Icons.Default.Home, "Charts" to Icons.Default.PieChart, "Reports" to Icons.Default.Assessment, "Profile" to Icons.Default.Person)
                items.forEach { (item, icon) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = item) },
                        label = { Text(item) },
                        selected = activeTab == item,
                        onClick = { activeTab = item },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Visual Stats Total Card
            if (activeTab == "Home") {
                val totalSpent = remember(transactions) { transactions.filter { it.isDebit }.sumOf { it.amount } }
                val totalReceived = remember(transactions) { transactions.filter { !it.isDebit }.sumOf { it.amount } }
                val netBalance = remember(totalReceived, totalSpent) { totalReceived - totalSpent }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "2026", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Jun", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Select Month", modifier = Modifier.size(20.dp))
                        }
                    }
                    Column {
                        Text(text = "Expenses", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                        Text(text = String.format(Locale.ROOT, "%.0f", totalSpent), fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleMedium)
                    }
                    Column {
                        Text(text = "Income", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                        Text(text = String.format(Locale.ROOT, "%.0f", totalReceived), fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleMedium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Balance", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                        Text(text = String.format(Locale.ROOT, "%.0f", netBalance), fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleMedium)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            }

            // Body Area based on Selected Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    "Charts" -> {
                        // Analytics Tab containing canvas charts
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                val preferredChartType by viewModel.preferredChartType.collectAsState()
                                InteractiveAnalyticsCard(transactions = transactions, preferredChartType = preferredChartType)
                            }
                            item {
                                MonthlySummaryBarChart(transactions = transactions)
                            }
                            item {
                                // Bottom management card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Wipe Database",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Irreversibly sterilizes Room records",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Button(
                                            onClick = { viewModel.resetAllData() },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Wipe",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Wipe Log", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "Reports" -> {
                        // Message Parser & Imports Page
                        var manualTextSms by remember { mutableStateOf("") }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Text(
                                            text = "Paste Transaction SMS Text",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        OutlinedTextField(
                                            value = manualTextSms,
                                            onValueChange = { manualTextSms = it },
                                            placeholder = {
                                                Text(
                                                    text = "e.g. Txn of Rs. 450.00 at Zomato on card 3450.",
                                                    fontSize = 13.sp
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            textStyle = TextStyle(fontSize = 14.sp),
                                            minLines = 3,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Button(
                                            onClick = {
                                                keyboardController?.hide()
                                                viewModel.parseAndAddTransaction(manualTextSms)
                                                manualTextSms = ""
                                            },
                                            modifier = Modifier.fillMaxWidth().height(48.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Parse & Add Securely", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Text(
                                            text = "Automatic Device Harvesting",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Allow the secure sandbox to compile transactions from your device physical messages database safely. All lookups process locally.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))

                                         // Scan Device Inbox Button
                                         Button(
                                             onClick = {
                                                 val permissionCheck = ContextCompat.checkSelfPermission(
                                                     context,
                                                     Manifest.permission.READ_SMS
                                                 )
                                                 if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                                     viewModel.scanDeviceSmsInbox()
                                                 } else {
                                                     smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                                                 }
                                             },
                                             modifier = Modifier.fillMaxWidth().height(48.dp),
                                             colors = ButtonDefaults.buttonColors(
                                                 containerColor = MaterialTheme.colorScheme.primary
                                             ),
                                             shape = RoundedCornerShape(12.dp)
                                         ) {
                                             Row(
                                                 verticalAlignment = Alignment.CenterVertically,
                                                 horizontalArrangement = Arrangement.Center
                                             ) {
                                                 Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Scan icon", modifier = Modifier.size(18.dp))
                                                 Spacer(modifier = Modifier.width(8.dp))
                                                 Text("Scan Mobile SMS Inbox", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                             }
                                         }
                                    }
                                }
                            }
                        }
                    }

                    "Home" -> {
                        // Transactions list Vault
                        val filteredList = remember(transactions, searchText) {
                            if (searchText.isBlank()) {
                                transactions
                            } else {
                                transactions.filter {
                                    it.title.lowercase(Locale.ROOT).contains(searchText.lowercase(Locale.ROOT)) ||
                                    it.category.lowercase(Locale.ROOT).contains(searchText.lowercase(Locale.ROOT)) ||
                                    it.smsText.lowercase(Locale.ROOT).contains(searchText.lowercase(Locale.ROOT))
                                }
                            }
                        }

                        val groupedByMonth = remember(filteredList) {
                            filteredList.sortedByDescending { it.timestamp }.groupBy { txn ->
                                val cal = Calendar.getInstance()
                                cal.timeInMillis = txn.timestamp
                                val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.ROOT)
                                monthYearFormat.format(cal.time)
                            }
                        }

                        Column(modifier = Modifier.fillMaxSize()) {
                            if (filteredList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Empty log",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Empty Ledger Log",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Paste a transaction SMS or scan your inbox to begin.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    groupedByMonth.forEach { (monthName, monthTxns) ->
                                        item(key = monthName) {
                                            val totalSpentMonth = remember(monthTxns) { monthTxns.filter { it.isDebit }.sumOf { it.amount } }
                                            val totalReceivedMonth = remember(monthTxns) { monthTxns.filter { !it.isDebit }.sumOf { it.amount } }
                                            
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 16.dp, bottom = 4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = monthName,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        if (totalSpentMonth > 0.0) {
                                                            Text(
                                                                text = String.format(Locale.ROOT, "Spent: ₹%.2f", totalSpentMonth),
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.error
                                                            )
                                                        }
                                                        if (totalReceivedMonth > 0.0) {
                                                            Text(
                                                                text = String.format(Locale.ROOT, "Received: ₹%.2f", totalReceivedMonth),
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .background(Color(0xFF2E7D32).copy(alpha = 0.08f))
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF2E7D32)
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                            }
                                        }

                                        items(monthTxns, key = { it.id }) { txn ->
                                            TransactionRowItem(
                                                transaction = txn,
                                                onDeleteClick = { viewModel.deleteTransaction(txn) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    "Profile" -> {
                        SettingsScreenContent(
                            isDarkState = isDarkState,
                            onThemeToggle = onThemeToggle,
                            viewModel = viewModel,
                            isNotificationEnabled = isNotificationEnabled,
                            onToggleNotification = {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }

    // Manual ADD custom transaction dialog box
    if (showAddDialog) {
        AddTransactionScreen(
            onDismiss = { showAddDialog = false },
            onSave = { titleText, amt, selectedCategory, isDebit, timestamp ->
                viewModel.addExplicitTransaction(
                    title = titleText,
                    amount = amt,
                    category = selectedCategory,
                    isDebit = isDebit,
                    timestamp = timestamp
                )
                showAddDialog = false
            }
        )
    }
}

// Single list item displaying transactions elegantly
@Composable
fun TransactionRowItem(
    transaction: Transaction,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    val formatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.ROOT) }
    val dateString = remember(transaction.timestamp) { formatter.format(Date(transaction.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Icon + Title/Category block
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = getCategoryColor(transaction.category).copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    color = getCategoryColor(transaction.category),
                                    shape = CircleShape
                                )
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = transaction.title,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${transaction.category} • $dateString",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Right Amount + actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val amountPrefix = if (transaction.isDebit) "-₹" else "+₹"
                    val amountColor = if (transaction.isDebit) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                    Text(
                        text = String.format(Locale.ROOT, "%s%.2f", amountPrefix, transaction.amount),
                        fontWeight = FontWeight.Black,
                        color = amountColor,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete record",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ENCRYPTED ORIGIN SMS BODY:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        text = transaction.smsText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
