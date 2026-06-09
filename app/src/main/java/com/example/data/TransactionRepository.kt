package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.example.ui.parser.TransactionParser

data class Transaction(
    val id: Long,
    val title: String,
    val amount: Double,
    val category: String,
    val smsText: String,
    val timestamp: Long,
    val isDebit: Boolean
)

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val cryptoManager: CryptoManager
) {
    // Expose flow of decrypted transactions
    val decryptedTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactionsFlow()
        .map { entities ->
            entities.map { entity ->
                val title = cryptoManager.decryptString(entity.encryptedTitle)
                val amountStr = cryptoManager.decryptString(entity.encryptedAmount)
                val category = cryptoManager.decryptString(entity.encryptedCategory)
                val smsText = cryptoManager.decryptString(entity.encryptedSmsText)
                val amount = amountStr.toDoubleOrNull() ?: 0.0

                Transaction(
                    id = entity.id,
                    title = title.ifEmpty { "Unknown Merchant" },
                    amount = amount,
                    category = category.ifEmpty { "Others" },
                    smsText = smsText,
                    timestamp = entity.timestamp,
                    isDebit = TransactionParser.determineIsDebit(smsText)
                )
            }
        }

    suspend fun insert(title: String, amount: Double, category: String, smsText: String, timestamp: Long): Long {
        val encryptedTitle = cryptoManager.encryptString(title)
        val encryptedAmount = cryptoManager.encryptString(amount.toString())
        val encryptedCategory = cryptoManager.encryptString(category)
        val encryptedSmsText = cryptoManager.encryptString(smsText)

        val entity = TransactionEntity(
            encryptedTitle = encryptedTitle,
            encryptedAmount = encryptedAmount,
            encryptedCategory = encryptedCategory,
            encryptedSmsText = encryptedSmsText,
            timestamp = timestamp
        )
        return transactionDao.insertTransaction(entity)
    }

    suspend fun delete(transaction: Transaction) {
        val encryptedTitle = cryptoManager.encryptString(transaction.title)
        val encryptedAmount = cryptoManager.encryptString(transaction.amount.toString())
        val encryptedCategory = cryptoManager.encryptString(transaction.category)
        val encryptedSmsText = cryptoManager.encryptString(transaction.smsText)

        val entity = TransactionEntity(
            id = transaction.id,
            encryptedTitle = encryptedTitle,
            encryptedAmount = encryptedAmount,
            encryptedCategory = encryptedCategory,
            encryptedSmsText = encryptedSmsText,
            timestamp = transaction.timestamp
        )
        transactionDao.deleteTransaction(entity)
    }

    suspend fun clearAll() {
        transactionDao.deleteAllTransactions()
    }
}
