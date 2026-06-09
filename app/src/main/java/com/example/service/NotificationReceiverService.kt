package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.CryptoManager
import com.example.data.Transaction
import com.example.data.TransactionRepository
import com.example.ui.parser.ParsedTransaction
import com.example.ui.parser.TransactionParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class NotificationReceiverService : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return

        // We target typical UPI / banking apps for Android
        val targetPackages = listOf(
            "com.phonepe.app",
            "com.google.android.apps.nbu.paisa.user", // Google Pay
            "net.one97.paytm",
            "com.dreamplug.androidapp" // CRED
        )

        if (targetPackages.contains(packageName)) {
            val notification = sbn.notification
            val extras = notification.extras
            if (extras != null) {
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
                val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
                
                val combinedText = "$title. $text $bigText".trim()
                
                // If it looks like a payment, try parsing it
                val lower = combinedText.lowercase(Locale.ROOT)
                if (lower.contains("paid") ||
                    lower.contains("sent") ||
                    lower.contains("received") ||
                    lower.contains("cashback") ||
                    lower.contains("success") ||
                    lower.contains("successful") ||
                    lower.contains("payment")) {
                    
                    Log.d("NotificationReceiver", "Intercepted payment notification: $combinedText")

                    val result = TransactionParser.parseSmsBody(combinedText, skipConfidenceCheck = true)
                    if (result.isSuccess) {
                        Log.d("NotificationReceiver", "Parsed transaction successfully: $result")
                        
                        // Check if we didn't receive this exact transaction recently to avoid duplicates
                        scope.launch {
                            try {
                                val dao = AppDatabase.getDatabase(applicationContext).transactionDao()
                                val cryptoManager = CryptoManager(applicationContext)
                                val repository = TransactionRepository(dao, cryptoManager)

                                // Quick duplicate check against recently flowing transactions
                                val recentTransactions = repository.decryptedTransactions.first()
                                val recentMatches = recentTransactions.filter { txn ->
                                    txn.category == result.category && 
                                    txn.amount == result.amount && 
                                    (System.currentTimeMillis() - txn.timestamp) < 60000 
                                }
                                
                                if (recentMatches.isEmpty()) {
                                    repository.insert(
                                        title = result.title,
                                        amount = result.amount,
                                        category = result.category,
                                        smsText = combinedText,
                                        timestamp = sbn.postTime
                                    )
                                    Log.d("NotificationReceiver", "Saved notification transaction via Repo")
                                }
                            } catch (e: Exception) {
                                Log.e("NotificationReceiver", "Error saving transaction", e)
                            }
                        }
                    }
                }
            }
        }
    }
}
