package com.example.ui.parser

import android.content.Context
import android.net.Uri
import android.util.Log

import android.provider.Telephony

data class SmsMessage(val body: String, val timestampMillis: Long)

object SmsReader {

    /**
     * Reads the SMS inbox from the device content provider.
     * Retrives the last 1000 messages to extract and filter transactions.
     */
    fun readTransactionsFromInbox(context: Context): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        val uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE)
        
        try {
            val cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT 1000"
            )
            
            cursor?.use {
                val bodyColumn = it.getColumnIndex(Telephony.Sms.BODY)
                val dateColumn = it.getColumnIndex(Telephony.Sms.DATE)
                if (bodyColumn != -1 && dateColumn != -1) {
                    while (it.moveToNext()) {
                        val body = it.getString(bodyColumn) ?: ""
                        val date = it.getLong(dateColumn)
                        if (body.trim().isNotEmpty()) {
                            messages.add(SmsMessage(body, date))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SmsReader", "Error reading SMS inbox ContentProvider: ", e)
        }
        
        return messages
    }
}
