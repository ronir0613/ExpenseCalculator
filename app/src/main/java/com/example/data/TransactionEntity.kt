package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val encryptedTitle: String,
    val encryptedAmount: String,
    val encryptedCategory: String,
    val encryptedSmsText: String,
    val timestamp: Long
)
