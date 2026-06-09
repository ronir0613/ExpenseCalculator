package com.example.ui.parser

import java.util.Locale

data class ParsedTransaction(
    val title: String,
    val amount: Double,
    val category: String,
    val smsText: String,
    val isSuccess: Boolean,
    val isDebit: Boolean = true
)

object TransactionParser {

    private val CATEGORIES = mapOf(
        "Food" to listOf(
            "swiggy", "zomato", "restaurant", "cafe", "food", "dining", "mcdonald", "starbucks", 
            "grocery", "supermarket", "mart", "kfc", "dominos", "pizza", "burger", "subway", 
            "eats", "bakery", "deli", "bistro"
        ),
        "Shopping" to listOf(
            "amazon", "flipkart", "myntra", "walmart", "shopping", "clothes", "fashion", "nike", 
            "adidas", "zara", "ebay", "target", "aliexpress", "costco", "boutique", "ikea", "nordstrom", "macy"
        ),
        "Health" to listOf(
            "pharmacy", "chemist", "hospital", "medical", "clinic", "doctor", "health", "insurance", 
            "fitness", "gym", "dentist", "optician", "cvs", "walgreens"
        ),
        "Entertainment" to listOf(
            "netflix", "spotify", "hotstar", "disney", "prime video", "movie", "cinema", "ticket", 
            "bookmyshow", "games", "steam", "playstation", "xbox", "hulu", "theatre", "arcade"
        ),
        "Transport" to listOf(
            "uber", "ola", "rapido", "metro", "fuel", "petrol", "diesel", "shell", "cab", "taxi", 
            "train", "flight", "aviation", "chevron", "bp", "exxon", "railway", "transit"
        ),
        "Bills & Utilities" to listOf(
            "electricity", "water", "gas", "recharge", "airtel", "jio", "broadband", "wifi", 
            "rent", "bill", "mobile", "utility", "power", "comcast", "verizon", "t-mobile"
        )
    )

    private val AMOUNT_REGEX_LIST = listOf(
        // Match Rs. 500, INR 500, ₹500, USD 45.50, $45.99
        Regex("(?i)(?:rs\\.?|inr|usd|\\$|₹)\\s*([\\d,]+(?:\\.\\d{1,2})?)"),
        // Match debited/paid/spent followed by a number (e.g. "debited 500", "spent 1500.00")
        Regex("(?i)(?:debited|spent|paid|paying|withdrawn|charged|sent|received)\\s+(?:rs\\.?|inr|usd|\\$|₹|by|of|for|to)?\\s*([\\d,]+(?:\\.\\d{1,2})?)"),
        // Match 250 spent or 250.00 debited
        Regex("(?i)([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:spent|debited|paid|withdrawn|charged|sent|received)")
    )

    private fun calculateTransactionConfidence(smsText: String, amount: Double): Boolean {
        if (amount <= 0.0) return false

        val lower = smsText.lowercase(Locale.ROOT)

        val hardBlockSignals = listOf(
            "withdraw process", ".top/", ".xyz/", ".cc/", ".vip/", "lottery", "jackpot",
            "casino", "earn cash", "job offer"
        )
        if (hardBlockSignals.any { lower.contains(it) }) {
            return false
        }

        // Strong Negative Signals
        val negativeSignals = listOf(
            "upto", "up to", "cashback", "offer", "sale", "discount", "reward", "rewards",
            "coupon", "voucher", "promotion", "promo", "shop now", "buy now",
            "limited period", "festival offer", "click here", "visit", "click",
            "http://", "https://", "www.", "t&c", "terms and conditions", "terms & conditions",
            "pre-approved", "loan offer", "credit card offer", "finance available", "eligible",
            "apply now", "win", "winner", "claim", "renews on", "due on", "otp"
        )

        // Strong Positive Signals
        val positiveSignals = listOf(
            "debited", "debit", "credited", "credit", "withdrawn", "paid", "paying",
            "a/c", "acct", "account", "card",
            "upi ref", "ref no", "ref.", "reference", "utr", "txn", "transaction",
            "avl bal", "available bal", "bal:", "balance", "remaining balance",
            "upi to", "upi from", "paid to", "sent to", "received from", "transfer", "refund", "payment",
            "shopping at", "spent", "purchased", "if not you", "freeze", "block", "report",
            "received", "sent", "successful", "success"
        )

        var negativeScore = 0
        for (signal in negativeSignals) {
            if (lower.contains(signal)) {
                negativeScore++
            }
        }

        var positiveScore = 1 // Start with 1 because amount > 0 is a positive signal

        for (signal in positiveSignals) {
            if (lower.contains(signal)) {
                positiveScore++
            }
        }
        
        // Check for masked account numbers like XX1234, X1234, ending in 1234
        if (Regex("(?i)([xX*]+|ending\\s*(in|with)?)\\s*-?\\d{3,4}").containsMatchIn(lower)) {
            positiveScore += 1
        }

        val hasStrongBankKeywords = (lower.contains("a/c") || lower.contains("acct") || lower.contains("account")) &&
                (lower.contains("debit") || lower.contains("credit") || lower.contains("withdrawn") || lower.contains("deposit"))

        if (hasStrongBankKeywords) {
            positiveScore += 2 // Boost confidence significantly for standard banking syntax
        }

        // If it looks highly promotional, we require overwhelming transactional evidence to approve it
        if (negativeScore > 0) {
            val pass = positiveScore >= 4 && positiveScore > (negativeScore * 2)
            println("Spam check: pos=$positiveScore neg=$negativeScore -> pass=$pass for $smsText")
            return pass
        }

        // Regular non-promotional message
        // Amount + 1 strong keyword (e.g. "spent", "debited", "txn") is enough
        val passClean = positiveScore >= 2
        println("Spam check: pos=$positiveScore neg=$negativeScore -> pass=$passClean for $smsText")
        return passClean
    }


    fun parseSmsBody(smsText: String, skipConfidenceCheck: Boolean = false): ParsedTransaction {
        val lowerText = smsText.lowercase(Locale.ROOT)
        
        // 1. Try to parse Amount
        var amount = 0.0
        for ((index, regex) in AMOUNT_REGEX_LIST.withIndex()) {
            val matches = regex.findAll(smsText)
            for (match in matches) {
                val matchIndex = match.range.first
                val precedingContext = smsText.substring((matchIndex - 20).coerceAtLeast(0), matchIndex).lowercase(Locale.ROOT)
                
                // Check if this matched number represents available balance or outstanding balance
                val isBalancePreceding = precedingContext.contains("bal") || 
                                         precedingContext.contains("balance") || 
                                         precedingContext.contains("avl") ||
                                         precedingContext.contains("available") ||
                                         precedingContext.contains("outstanding")
                
                if (isBalancePreceding) {
                    continue // Skip balance figures, look for the actual txn amount
                }

                // To avoid matching bank account numbers (e.g. "A/c 1234 debited")
                // we check if there are account descriptors right before the matched position
                val isAccountWordPreceding = precedingContext.contains("a/c") || 
                                             precedingContext.contains("acct") || 
                                             precedingContext.contains("account") || 
                                             precedingContext.contains("card") ||
                                             precedingContext.contains("no.")
                
                if (isAccountWordPreceding && index > 0) {
                    continue
                }

                val amountStr = match.groupValues[1].replace(",", "")
                val parsed = amountStr.toDoubleOrNull()
                if (parsed != null && parsed > 0.0) {
                    amount = parsed
                    break
                }
            }
            if (amount > 0.0) {
                break
            }
        }
        
        // Use Confidence Based Classification
        if (!skipConfidenceCheck) {
            val isConfirmedTransaction = calculateTransactionConfidence(smsText, amount)
            if (!isConfirmedTransaction) {
                return ParsedTransaction(
                    title = "Not a Transaction",
                    amount = 0.0,
                    category = "Others",
                    smsText = smsText,
                    isSuccess = false
                )
            }
        } else if (amount <= 0.0) {
            return ParsedTransaction(
                title = "Not a Transaction",
                amount = 0.0,
                category = "Others",
                smsText = smsText,
                isSuccess = false
            )
        }


        // 2. Identify Merchant/Title and Category via Keyword Scan
        var detectedTitle = ""
        var detectedCategory = "Others"

        // Search for known keyword matches (longest match preferred to match "prime video" before "video")
        var matchedKeyword: String? = null
        for (entry in CATEGORIES.entries) {
            for (keyword in entry.value) {
                if (lowerText.contains(keyword)) {
                    if (matchedKeyword == null || keyword.length > matchedKeyword.length) {
                        matchedKeyword = keyword
                        detectedCategory = entry.key
                        // Capitalize keyword properly for presentation
                        detectedTitle = keyword.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                    }
                }
            }
        }

        // 3. Fallback Contextual Parser for Merchant Name if Keyword Scan didn't find specific brands
        if (detectedTitle.isEmpty()) {
            val fallbackRegexes = listOf(
                Regex("(?i)vpa\\s+([A-Za-z0-9\\.\\-_]+@[A-Za-z0-9\\.\\-_]+)"), 
                Regex("(?i)upi\\/([^\\/]+)\\/"), 
                Regex("(?i)upi\\s+to\\s+([A-Za-z0-9\\s\\.\\-\\'&]+)(?:\\s+on|\\s+for|\\s+using|\\s+dated|\\.)"),
                Regex("(?i)paid\\s+to\\s+([A-Za-z0-9\\s\\.\\-\\'&]+)(?:\\s+on|\\s+for|\\s+using|\\s+dated|\\.)"),
                Regex("(?i)sent\\s+to\\s+([A-Za-z0-9\\s\\.\\-\\'&]+)(?:\\s+on|\\s+for|\\s+using|\\s+dated|\\.)"),
                Regex("(?i)at\\s+([A-Za-z0-9\\s\\.\\-\\'&]+)(?:\\s+on|\\s+for|\\s+using|\\s+dated|\\.)"),
                Regex("(?i)to\\s+([A-Za-z0-9\\s\\.\\-\\'&]+)(?:\\s+on|\\s+for|\\s+using|\\s+dated|\\.)"),
                Regex("(?i)towards\\s+([A-Za-z0-9\\s\\.\\-\\'&]+)(?:\\s+on|\\s+using|\\s+dated|\\.)")
            )
            for (regex in fallbackRegexes) {
                val match = regex.find(smsText)
                if (match != null) {
                    val rawMatch = match.groupValues[1].trim()
                    // Filter out non-merchant words we want to exclude
                    val filtered = rawMatch.split(Regex("\\s+")).take(3).joinToString(" ")
                    if (filtered.isNotEmpty() && !filtered.lowercase(Locale.ROOT).contains("your account")) {
                        detectedTitle = filtered.split(" ").joinToString(" ") { word ->
                            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                        }
                        break
                    }
                }
            }
        }

        // 4. Ultimate defaults
        if (detectedTitle.isEmpty()) {
            detectedTitle = "Local Merchant"
        }

        val isTxDebit = determineIsDebit(smsText)

        return ParsedTransaction(
            title = detectedTitle,
            amount = amount,
            category = detectedCategory,
            smsText = smsText,
            isSuccess = true, // We already filtered out non-transactions using calculateTransactionConfidence
            isDebit = isTxDebit
        )
    }

    /**
     * Determines whether an SMS text represents a debit (expenditure/money going out)
     * or a credit (inflow/money coming in).
     */
    fun determineIsDebit(smsText: String): Boolean {
        val lower = smsText.lowercase(Locale.ROOT)
        val creditWords = listOf("credited", "credit", "received", "refund", "refunded", "deposited", "deposit", "cashback")
        val debitWords = listOf("debit", "debited", "spent", "paid", "sent", "withdrawn", "charged", "transferred", "transfer", "paying")

        val hasCredit = creditWords.any { lower.contains(it) }
        val hasDebit = debitWords.any { lower.contains(it) }

        if (hasCredit && !hasDebit) {
            return false
        }
        if (hasDebit && !hasCredit) {
            return true
        }
        if (hasCredit) {
            return false
        }
        return true
    }

    /**
     * Provide list of standard simulated transactions for the 'Demo import' feature
     */
    fun getDemoSmsList(): List<String> {
        return listOf(
            "Txn of Rs. 1,200.00 at Swiggy for lunch order on 31-May.",
            "Your HDFC card has been debited with Rs 1,499.00 at Netflix subscription renewal.",
            "Salary Rs. 48,000.00 credited to your bank A/C on 28-May.",
            "Sent INR 450.00 to Ola Cabs towards ride charges.",
            "Paid Rs 2,500.00 for electricity bill online towards Torrent Power.",
            "Refund of Rs. 350.00 received from Swiggy on 15-May.",
            "Inr 180.00 spent at Starbucks coffee house on 30-May.",
            "Thank you for shopping of Rs 1,200.00 at Walmart store #2348.",
            "Medical bill of INR 850.00 debited at CVS Pharmacy near you.",
            "Cashback of Rs 75.00 credited on UPI payment Ref 3291 on 12-Apr.",
            "Spent Rs 1,850.00 for gaming purchase at Steam premium on digital store."
        )
    }
}
