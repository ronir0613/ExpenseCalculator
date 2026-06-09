package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SpendVault", appName)
  }

  @Test
  fun `test transaction sms parsing logic`() {
    // 1. Swiggy food order SMS
    val swiggySms = "Txn of Rs. 1,200.00 at Swiggy for lunch order on 31-May."
    val swiggyResult = com.example.ui.parser.TransactionParser.parseSmsBody(swiggySms)
    assertEquals(true, swiggyResult.isSuccess)
    assertEquals(1200.0, swiggyResult.amount, 0.01)
    assertEquals("Swiggy", swiggyResult.title)
    assertEquals("Food", swiggyResult.category)

    // 2. Walmart shopping order SMS with $ sign
    val walmartSms = "Thank you for payment of \$120.00 at Walmart store #2348."
    val walmartResult = com.example.ui.parser.TransactionParser.parseSmsBody(walmartSms)
    assertEquals(true, walmartResult.isSuccess)
    assertEquals(120.0, walmartResult.amount, 0.01)
    assertEquals("Walmart", walmartResult.title)
    assertEquals("Shopping", walmartResult.category)

    // 3. Fallback contextual parsing SMS
    val contextualSms = "Amt debited 45.00 spent at local gas stall towards Chevron station."
    val contextualResult = com.example.ui.parser.TransactionParser.parseSmsBody(contextualSms)
    assertEquals(true, contextualResult.isSuccess)
    assertEquals(45.00, contextualResult.amount, 0.01)
    assertEquals("Chevron", contextualResult.title)
    assertEquals("Transport", contextualResult.category)

    // 4. Invalid dummy messages (non-financial text)
    val greetingSms = "Hey, are we still meeting for coffee tomorrow?"
    val greetingResult = com.example.ui.parser.TransactionParser.parseSmsBody(greetingSms)
    assertEquals(false, greetingResult.isSuccess)
  }

  @Test
  fun `test cryptographic encryption and decryption cycle`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val cryptoManager = com.example.data.CryptoManager(context)

    val plainText = "Confidential Transaction Balance: $5,435.00"
    val encryptedText = cryptoManager.encryptString(plainText)
    
    // Ensure encrypted form is encoded successfully (and is not empty and not matching plain text)
    assertNotEquals("", encryptedText)
    assertNotEquals(plainText, encryptedText)

    // Ensure it correctly decrypts back to original string
    val decryptedText = cryptoManager.decryptString(encryptedText)
    assertEquals(plainText, decryptedText)
  }
}
