package com.example

import com.example.ui.parser.TransactionParser
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testValidTransactionSmsMatches() {
        val sms = "Your HDFC card has been debited with Rs 1,499.00 at Netflix subscription renewal."
        val result = TransactionParser.parseSmsBody(sms)
        assertTrue(result.isSuccess)
        assertEquals(1499.00, result.amount, 0.001)
        assertEquals("Netflix", result.title)
        assertEquals("Entertainment", result.category)
    }

    @Test
    fun testValidUpiSmsMatches() {
        val sms = "Txn of Rs. 1,200.00 at Swiggy for lunch order on 31-May."
        val result = TransactionParser.parseSmsBody(sms)
        assertTrue(result.isSuccess)
        assertEquals(1200.00, result.amount, 0.001)
        assertEquals("Swiggy", result.title)
        assertEquals("Food", result.category)
    }

    @Test
    fun testSpamSmsIsFilteredOut() {
        // Test the user's specific spam message from the first screenshot
        val spamSms = "Account(756***9217) credited Rs.10782 Withdraw process @9pm today Click ls55.top/XHi-7569729217"
        val result = TransactionParser.parseSmsBody(spamSms)
        assertFalse("Spam message should be filtered out", result.isSuccess)
        assertEquals(0.0, result.amount, 0.001)
    }

    @Test
    fun testRenewalReminderSmsIsFilteredOut() {
        // Test the user's insurance policy renewal message (non-transaction)
        val renewalSms = "Dear Customer,\nYour SBIG PA Policy\n202501268888000 renews on\n21/05/2026. Premium: Rs 1000. Visit\nSBI branch or YONO SBI. Ignore if\npaid.\nSBIG\nT&C Apply. IRDAI RN 144"
        val result = TransactionParser.parseSmsBody(renewalSms)
        assertFalse("Insurance renewal reminders should be filtered out", result.isSuccess)
        assertEquals(0.0, result.amount, 0.001)
    }

    @Test
    fun testIppbDebitSms() {
        // Test parsing the IPPB debit SMS
        val rechargeSms = "A/C X1131 Debit Rs.323.00 for UPI to jio recharge on 17-04-26 Ref 59886581431. Avl Bal Rs.13671.94. If not you? SMS FREEZE \"full a/c\" to 7669034700-IPPB"
        val result = TransactionParser.parseSmsBody(rechargeSms)
        assertTrue("IPPB recharge transaction should succeed", result.isSuccess)
        assertEquals(323.0, result.amount, 0.001)
        assertEquals("Recharge", result.title)
        assertEquals("Bills & Utilities", result.category)

        // Test medical/personal payment
        val hostSms = "A/C X1131 Debit Rs.40.00 for UPI to srm hospitalit on 06-05-26 Ref 023752307882. Avl Bal Rs.12341.04."
        val resultHost = TransactionParser.parseSmsBody(hostSms)
        assertTrue("IPPB hospital transaction should succeed", resultHost.isSuccess)
        assertEquals(40.0, resultHost.amount, 0.001)
        assertEquals("Hospital", resultHost.title)
        assertEquals("Health", resultHost.category)
    }

    @Test
    fun testLinkBasedSpamIsFilteredOut() {
        val generalSpam = "Congratulations! You have been selected for Rs 5,000 promo cash reward. Click to claim now http://scam-rewards.xyz/promo"
        val result = TransactionParser.parseSmsBody(generalSpam)
        assertFalse("Promo link spam should be filtered out", result.isSuccess)
    }

    @Test
    fun testTrendsFootwearRewardPointsIsFiltered() {
        val sms = "Rs500 worth points credited in your wallet Redeem by 17May on Rs1500 purchase@TRENDS FOOTWEAR Use Code TRFWSWJDIP1506 Visit@ https://vil.ltd/TRNDFW/c/May26 T&C"
        val result = TransactionParser.parseSmsBody(sms)
        assertFalse("Promotional point reward coupons should be filtered", result.isSuccess)
    }

    @Test
    fun testJioRechargePlanAdIsFiltered() {
        val sms = "Recharge Jio no. 7569729217 with Rs.899 plan & get Exclusive Offer! Watch Live Cricket on JioHotstar + Free AI benefits from Google Gemini & 5000 GB storage + Unlimited 5G data + 2 GB/day & 20GB , Unlimited Voice, 90 Days. Use super.money app & get upto Rs.50 cashback. T&CA. https://link.super.money/6LLoZXljP0b"
        val result = TransactionParser.parseSmsBody(sms)
        assertFalse("Recharge advertisements and cashback offers should be filtered", result.isSuccess)
    }

    @Test
    fun testGoogleIndiaDebitIsParsedSuccess() {
        val sms = "A/C X1131 Debit Rs.350.90 for UPI to google india d on 30-05-26 Ref 123939756645. Avl Bal Rs.9543.89. If not you? SMS FREEZE \"full a/c\" to 7669034700-IPPB"
        val result = TransactionParser.parseSmsBody(sms)
        assertTrue("Google India Debit transaction should succeed", result.isSuccess)
        assertEquals(350.90, result.amount, 0.001)
        assertEquals("Google India D", result.title)
        assertEquals("Others", result.category)
    }

    @Test
    fun testNonTransactionMessageWithoutKeywordsIsFiltered() {
        val ordinaryChat = "Meet me tomorrow, I can bring Rs 500."
        val result = TransactionParser.parseSmsBody(ordinaryChat)
        assertFalse("Ordinary chat shouldn't be parsed as a direct transaction", result.isSuccess)
    }

    @Test
    fun testMessageWithBalanceFirst() {
        val sms = "Avl Bal Rs.9543.89. A/C X1131 debited Rs.350.90 for online payment."
        val result = TransactionParser.parseSmsBody(sms)
        assertTrue(result.isSuccess)
        assertEquals(350.90, result.amount, 0.001)
    }

    @Test
    fun testTransactionWithDisputeLinkNotSpammed() {
        val sms = "A/C X1131 Debited Rs.450.00 at Swiggy. If this is not you, please click https://bank-portal.com/freeze"
        val result = TransactionParser.parseSmsBody(sms)
        assertTrue("Genuine transaction with block safety link should not be categorized as spam", result.isSuccess)
        assertEquals(450.00, result.amount, 0.001)
        assertEquals("Swiggy", result.title)
    }

    @Test
    fun testTransactionCreditAndDebitDetection() {
        val debitSms = "A/C X1131 Debit Rs.350.90 for UPI to Netflix on 30-05-26."
        val creditSms = "Salary of Rs. 48,000.00 credited to account X1131 on 28-May."
        val refundSms = "Refund of Rs. 350.00 received from Swiggy on 15-May."
        
        val debResult = TransactionParser.parseSmsBody(debitSms)
        val credResult = TransactionParser.parseSmsBody(creditSms)
        val refResult = TransactionParser.parseSmsBody(refundSms)
        
        assertTrue(debResult.isSuccess)
        assertTrue(debResult.isDebit)
        
        assertTrue(credResult.isSuccess)
        assertFalse(credResult.isDebit)
        
        assertTrue(refResult.isSuccess)
        assertFalse(refResult.isDebit)
    }
}
