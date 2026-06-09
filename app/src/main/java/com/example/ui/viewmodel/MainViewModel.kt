package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.ui.parser.SmsReader
import com.example.ui.parser.TransactionParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar

class MainViewModel(
    application: Application,
    private val repository: TransactionRepository
) : AndroidViewModel(application) {

    private val scanMutex = Mutex()

    // Dark Mode preference stored in local Key-Value SharedPreferences
    private val sharedPrefs = application.getSharedPreferences("expense_calc_prefs", Context.MODE_PRIVATE)
    
    private val _isDarkMode = MutableStateFlow(sharedPrefs.getBoolean("dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _preferredChartType = MutableStateFlow(sharedPrefs.getString("chart_type", "Pie") ?: "Pie")
    val preferredChartType: StateFlow<String> = _preferredChartType.asStateFlow()

    // Decrypted transactions linked dynamically from Room
    val transactions: StateFlow<List<Transaction>> = repository.decryptedTransactions
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _parseStatusMessage = MutableStateFlow<String?>(null)
    val parseStatusMessage: StateFlow<String?> = _parseStatusMessage.asStateFlow()

    fun toggleDarkMode() {
        val newVal = !_isDarkMode.value
        _isDarkMode.value = newVal
        sharedPrefs.edit().putBoolean("dark_mode", newVal).apply()
    }

    fun setPreferredChartType(type: String) {
        _preferredChartType.value = type
        sharedPrefs.edit().putString("chart_type", type).apply()
    }

    /**
     * Parse a single custom raw text string entered or pasted by the user.
     */
    fun parseAndAddTransaction(rawText: String) {
        if (rawText.isBlank()) {
            _parseStatusMessage.value = "Please enter or paste a valid SMS message."
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            scanMutex.withLock {
                val currentDbList = repository.decryptedTransactions.first()
                val existingSmsSet = currentDbList.map { it.smsText.trim() }.toMutableSet()
                if (existingSmsSet.contains(rawText.trim())) {
                    _parseStatusMessage.value = "This transaction is already registered in your vault!"
                    return@withLock
                }

                val result = TransactionParser.parseSmsBody(rawText)
                if (result.isSuccess) {
                    repository.insert(
                        title = result.title,
                        amount = result.amount,
                        category = result.category,
                        smsText = result.smsText,
                        timestamp = System.currentTimeMillis()
                    )
                    _parseStatusMessage.value = "Successfully parsed: Added ₹${result.amount} at ${result.title} (${result.category})"
                } else {
                    _parseStatusMessage.value = "Failed to auto-parse details. Adding as custom expense with ₹10.00."
                    repository.insert(
                        title = if (result.title.isNotEmpty() && result.title != "Local Merchant") result.title else "Custom Transaction",
                        amount = if (result.amount > 0.0) result.amount else 10.0,
                        category = if (result.category != "Others") result.category else "Others",
                        smsText = rawText,
                        timestamp = System.currentTimeMillis()
                    )
                }
            }
        }
    }

    /**
     * Inserts standard simulated transactional messages to fast-track demonstration.
     */
    fun loadDemoTransactions() {
        viewModelScope.launch(Dispatchers.IO) {
            scanMutex.withLock {
                val currentDbList = repository.decryptedTransactions.first()
                val existingSmsSet = currentDbList.map { it.smsText.trim() }.toMutableSet()

                val demoList = TransactionParser.getDemoSmsList()
                var addedCount = 0
                
                // Distribute demo transactions over the last 6 months to populate the chart beautifully
                val calendar = Calendar.getInstance()
                
                demoList.forEachIndexed { index, sms ->
                    if (existingSmsSet.contains(sms.trim())) {
                        return@forEachIndexed
                    }
                    val result = TransactionParser.parseSmsBody(sms)
                    if (result.isSuccess) {
                        // Stagger dates: month offset
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.MONTH, -(index % 6))
                        cal.add(Calendar.DAY_OF_MONTH, -index)
                        
                        repository.insert(
                            title = result.title,
                            amount = result.amount,
                            category = result.category,
                            smsText = result.smsText,
                            timestamp = cal.timeInMillis
                        )
                        existingSmsSet.add(sms.trim())
                        addedCount++
                    }
                }
                if (addedCount > 0) {
                    _parseStatusMessage.value = "Imported $addedCount new demo transactions!"
                } else {
                    _parseStatusMessage.value = "Demo transactions are already loaded."
                }
            }
        }
    }

    /**
     * Reads actual on-device SMS messages (if granted) and runs categorization.
     */
    fun scanDeviceSmsInbox() {
        viewModelScope.launch(Dispatchers.IO) {
            scanMutex.withLock {
                val context = getApplication<Application>().applicationContext
                val rawMessages = SmsReader.readTransactionsFromInbox(context)
                
                val currentDbList = repository.decryptedTransactions.first()
                val existingSmsSet = currentDbList.map { it.smsText.trim() }.toMutableSet()
                
                var addedCount = 0

                rawMessages.forEach { smsMsg ->
                    val sms = smsMsg.body
                    val timestamp = smsMsg.timestampMillis
                    val trimmedSms = sms.trim()
                    if (existingSmsSet.contains(trimmedSms)) {
                        return@forEach
                    }

                    val parsed = TransactionParser.parseSmsBody(sms)
                    if (parsed.isSuccess) {
                        repository.insert(
                            title = parsed.title,
                            amount = parsed.amount,
                            category = parsed.category,
                            smsText = parsed.smsText,
                            timestamp = timestamp
                        )
                        existingSmsSet.add(trimmedSms)
                        addedCount++
                    }
                }

                if (addedCount > 0) {
                    _parseStatusMessage.value = "Successfully parsed and imported $addedCount financial messages!"
                } else {
                    _parseStatusMessage.value = "No new transaction-related messages discovered in the inbox."
                }
            }
        }
    }

    fun clearStatusMessage() {
        _parseStatusMessage.value = null
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(transaction)
            _parseStatusMessage.value = "Transaction secure-deleted."
        }
    }

    fun resetAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAll()
            _parseStatusMessage.value = "Secure storage sterilized."
        }
    }

    /**
     * Add a fully explicit transaction manually
     */
    fun addExplicitTransaction(title: String, amount: Double, category: String, isDebit: Boolean, timestamp: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val indicatorGroup = if (isDebit) "spent Rs. $amount" else "credited Rs. $amount"
            repository.insert(
                title = title,
                amount = amount,
                category = category,
                smsText = "Manually recorded offline transaction: $title ($indicatorGroup)",
                timestamp = timestamp
            )
            val actionText = if (isDebit) "spent" else "received"
            _parseStatusMessage.value = "Manually recorded: Custom $actionText ₹${amount} at $title."
        }
    }

    // ViewModel Factory provider
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                val database = AppDatabase.getDatabase(application)
                val cryptoManager = CryptoManager(application)
                val repository = TransactionRepository(database.transactionDao(), cryptoManager)
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
