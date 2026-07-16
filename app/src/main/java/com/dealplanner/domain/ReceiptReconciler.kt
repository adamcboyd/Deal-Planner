package com.dealplanner.domain

import com.dealplanner.data.model.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Receipt reconciliation engine.
 * Matches OCR receipt text to pantry items and deals using fuzzy matching (Levenshtein distance).
 */
class ReceiptReconciler {

    data class ReconciliationResult(
        val receiptItems: List<ReceiptItem>,
        val pantryUpdates: List<PantryItem>,
        val dealMatches: List<DealMatch>,
        val total: Double,
        val warnings: List<String>
    )

    data class DealMatch(
        val receiptItem: ReceiptItem,
        val dealItem: DealItem,
        val actualPPU: Double,
        val expectedPPU: Double,
        val variance: Double
    )

    /**
     * Parses receipt OCR text and matches to deals/pantry.
     */
    fun reconcileReceipt(
        ocrText: String,
        dealItems: List<DealItem>,
        pantryItems: List<PantryItem>,
        store: String
    ): ReconciliationResult {
        val receiptItems = mutableListOf<ReceiptItem>()
        val pantryUpdates = mutableListOf<PantryItem>()
        val dealMatches = mutableListOf<DealMatch>()
        val warnings = mutableListOf<String>()
        var total = 0.0

        val lines = ocrText.lines().filter { it.trim().isNotEmpty() }
        val receiptDate = extractReceiptDate(lines) ?: LocalDate.now()

        var lineIndex = 0
        while (lineIndex < lines.size) {
            val line = lines[lineIndex]
            val parsedLine = parseReceiptLine(line)

            if (parsedLine != null) {
                val (itemName, price, parsedQty) = parsedLine
                val qty = parsedQty ?: findSplitQuantity(lines, lineIndex + 1)

                // Try to match with deal items
                val dealMatch = findBestMatch(itemName, dealItems.map { it.name })
                val pantryMatch = findBestMatch(itemName, pantryItems.map { it.item })
                val matchedDeal = dealMatch?.let { match -> dealItems.find { it.name == match.first } }
                val matchedPantryItem = pantryMatch?.let { match -> pantryItems.find { it.item == match.first } }
                val useDealMatch = dealMatch != null && (
                    pantryMatch == null || dealMatch.second >= pantryMatch.second
                )
                val selectedConfidence = if (useDealMatch) {
                    dealMatch?.second
                } else {
                    pantryMatch?.second
                }

                val confidence = selectedConfidence ?: 0.5
                val needsReview = confidence < 0.7

                val receiptItem = ReceiptItem(
                    rawLine = line,
                    matchedItemId = if (useDealMatch) matchedDeal?.id else matchedPantryItem?.id,
                    matchedType = if (useDealMatch) "deal" else if (pantryMatch != null) "pantry" else null,
                    qty = qty,
                    totalCost = price,
                    date = receiptDate,
                    confidence = confidence,
                    store = store,
                    needsReview = needsReview
                )

                receiptItems.add(receiptItem)
                total += price

                // If matched to deal, check PPU variance
                if (useDealMatch) {
                    if (matchedDeal != null) {
                        val actualPPU = if (qty != null && qty > 0) price / qty else price
                        val variance = ((actualPPU - matchedDeal.pricePerUnit) / matchedDeal.pricePerUnit) * 100

                        if (kotlin.math.abs(variance) > 10) {
                            warnings.add("Price variance for ${matchedDeal.name}: ${"%.1f".format(variance)}%")
                        }

                        dealMatches.add(
                            DealMatch(
                                receiptItem = receiptItem,
                                dealItem = matchedDeal,
                                actualPPU = actualPPU,
                                expectedPPU = matchedDeal.pricePerUnit,
                                variance = variance
                            )
                        )
                    }
                }

                // If matched to pantry, create update to increment quantity
                if (!useDealMatch) {
                    if (matchedPantryItem != null && qty != null) {
                        pantryUpdates.add(
                            matchedPantryItem.copy(
                                qty = matchedPantryItem.qty + qty
                            )
                        )
                    }
                }
            }

            lineIndex++
        }

        return ReconciliationResult(
            receiptItems = receiptItems,
            pantryUpdates = pantryUpdates,
            dealMatches = dealMatches,
            total = roundCurrency(total),
            warnings = warnings
        )
    }

    /**
     * Parses a single receipt line.
     * Format examples:
     * - "CHICKEN BREAST    $5.99"
     * - "2 @ 2.49  BROCCOLI  $4.98"
     */
    private fun parseReceiptLine(line: String): Triple<String, Double, Double?>? {
        // Pattern: optional qty, item name, price
        val itemFirstWeightedPattern = Regex(
            """(.+?)\s+(\d+(?:[.,]\d+)?)\s*(?:lb|lbs|pound|pounds|oz|ounce|ounces)\s*@\s*\$?\d+[.,]\d{2}(?:\s*/\s*(?:lb|lbs|pound|pounds|oz|ounce|ounces))?\s+\$?(\d+[.,]\d{2})""",
            RegexOption.IGNORE_CASE
        )
        val pattern1 = Regex("""(\d+)\s*@\s*\$?(\d+[.,]\d{2})\s+(.+?)\s+\$?(\d+[.,]\d{2})""")
        val pattern2 = Regex("""(.+?)\s+\$?(\d+[.,]\d{2})""")

        itemFirstWeightedPattern.find(line)?.let { match ->
            val itemName = match.groupValues[1].trim()
            if (isSummaryOrTenderLine(itemName)) return null
            val qty = match.groupValues[2].toPriceDoubleOrNull()
            val totalPrice = match.groupValues[3].toPriceDoubleOrNull() ?: 0.0
            return Triple(itemName, totalPrice, qty)
        }

        pattern1.find(line)?.let { match ->
            val qty = match.groupValues[1].toDoubleOrNull() ?: 1.0
            val itemName = match.groupValues[3].trim()
            if (isSummaryOrTenderLine(itemName)) return null
            val totalPrice = match.groupValues[4].toPriceDoubleOrNull() ?: 0.0
            return Triple(itemName, totalPrice, qty)
        }

        if (isQuantityDetailLine(line)) return null

        pattern2.find(line)?.let { match ->
            val itemName = match.groupValues[1].trim()
            if (isSummaryOrTenderLine(itemName)) return null
            val price = match.groupValues[2].toPriceDoubleOrNull() ?: 0.0
            return Triple(itemName, price, null)
        }

        return null
    }

    private fun isSummaryOrTenderLine(itemName: String): Boolean {
        val normalized = itemName.lowercase()
            .replace(Regex("""[^a-z0-9]+"""), " ")
            .trim()
            .replace(Regex("""\s+"""), " ")

        val exactMatches = setOf(
            "sub total",
            "subtotal",
            "tax",
            "sales tax",
            "total",
            "grand total",
            "order total",
            "balance",
            "balance due",
            "amount due",
            "amount paid",
            "payment",
            "cash",
            "change",
            "change due",
            "card",
            "card tender",
            "card payment",
            "credit",
            "credit card",
            "credit tender",
            "credit purchase",
            "debit",
            "debit card",
            "debit tender",
            "debit purchase",
            "visa",
            "visa debit",
            "visa credit",
            "visa tender",
            "mastercard",
            "master card",
            "mastercard debit",
            "mastercard credit",
            "master card debit",
            "master card credit",
            "amex",
            "amex credit",
            "discover",
            "discover credit",
            "ebt",
            "ebt card",
            "ebt food",
            "ebt tender",
            "snap",
            "snap ebt",
            "wic",
            "wic benefit",
            "food stamp",
            "food stamps",
            "savings",
            "total savings",
            "coupon",
            "mfr coupon",
            "manufacturer coupon",
            "digital coupon",
            "ecoupon",
            "store coupon",
            "store discount",
            "discount",
            "refund",
            "return",
            "void",
            "promo",
            "promotion",
            "reward",
            "rewards",
            "reward savings",
            "rewards savings",
            "markdown",
            "adjustment"
        )

        val prefixes = listOf(
            "tax ",
            "sales tax ",
            "balance due ",
            "amount due ",
            "amount paid ",
            "payment ",
            "change due ",
            "card ",
            "card tender ",
            "card payment ",
            "credit card ",
            "credit tender ",
            "credit purchase ",
            "debit card ",
            "debit tender ",
            "debit purchase ",
            "visa debit ",
            "visa credit ",
            "visa tender ",
            "mastercard debit ",
            "mastercard credit ",
            "master card debit ",
            "master card credit ",
            "amex credit ",
            "discover credit ",
            "ebt ",
            "ebt card ",
            "ebt tender ",
            "snap ",
            "wic ",
            "food stamp ",
            "food stamps ",
            "total savings ",
            "coupon ",
            "mfr coupon ",
            "manufacturer coupon ",
            "digital coupon ",
            "ecoupon ",
            "store coupon ",
            "store discount ",
            "discount ",
            "refund ",
            "return ",
            "void ",
            "promo ",
            "promotion ",
            "reward ",
            "rewards ",
            "reward savings ",
            "rewards savings ",
            "markdown ",
            "adjustment "
        )
        val adjustmentTerms = listOf(
            " coupon",
            "discount",
            "savings",
            "refund",
            "return",
            "void",
            "promo",
            "promotion",
            "reward",
            "rewards",
            "markdown",
            "adjustment"
        )

        return normalized in exactMatches ||
            prefixes.any { normalized.startsWith(it) } ||
            adjustmentTerms.any { normalized.contains(it) }
    }

    private fun extractReceiptDate(lines: List<String>): LocalDate? {
        return lines.firstNotNullOfOrNull { line ->
            val trimmed = line.trim()
            val headerMatch = receiptDateHeaderPattern.find(trimmed)
            val standaloneMatch = receiptDateStandalonePattern.matchEntire(trimmed)
            val value = headerMatch?.groupValues?.getOrNull(1)
                ?: standaloneMatch?.groupValues?.getOrNull(1)

            value?.let { parseReceiptDateValue(it) }
        }
    }

    private fun parseReceiptDateValue(value: String): LocalDate? {
        val cleaned = value.trim()
        return receiptDateFormats.firstNotNullOfOrNull { formatter ->
            try {
                LocalDate.parse(cleaned, formatter)
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }

    private fun isQuantityDetailLine(line: String): Boolean {
        return splitQuantityPattern.matches(line.trim())
    }

    private fun findSplitQuantity(lines: List<String>, startIndex: Int): Double? {
        if (startIndex >= lines.size) return null

        return splitQuantityPattern.find(lines[startIndex].trim())
            ?.groupValues
            ?.get(1)
            ?.toPriceDoubleOrNull()
    }

    /**
     * Finds best match using Levenshtein distance.
     * Returns (matched string, confidence score 0-1).
     */
    private fun findBestMatch(query: String, candidates: List<String>): Pair<String, Double>? {
        if (candidates.isEmpty()) return null

        val queryLower = query.lowercase()
        var bestMatch: String? = null
        var bestScore = 0.0

        candidates.forEach { candidate ->
            val candidateLower = candidate.lowercase()
            val distance = levenshteinDistance(queryLower, candidateLower)
            val maxLen = maxOf(queryLower.length, candidateLower.length)
            val similarity = 1.0 - (distance.toDouble() / maxLen)

            if (similarity > bestScore) {
                bestScore = similarity
                bestMatch = candidate
            }
        }

        return if (bestMatch != null) Pair(bestMatch!!, bestScore) else null
    }

    /**
     * Calculates Levenshtein distance between two strings.
     */
    internal fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[m][n]
    }

    /**
     * Calculates Value Per Pound (VPP) for proteins.
     * VPP = package_price / servings_yield (default 0.5 lb servings).
     */
    fun calculateVPP(packagePrice: Double, packageWeight: Double, servingSize: Double = 0.5): Double {
        val servings = packageWeight / servingSize
        return packagePrice / servings
    }

    private fun roundCurrency(value: Double): Double {
        return kotlin.math.round(value * 100.0) / 100.0
    }

    private fun String.toPriceDoubleOrNull(): Double? {
        return replace(',', '.').toDoubleOrNull()
    }

    private companion object {
        private val receiptDateHeaderPattern = Regex(
            """(?i)\b(?:date|transaction date|trans date|purchase date)\s*[:#-]?\s*(\d{1,4}[/-]\d{1,2}[/-]\d{1,4})\b"""
        )
        private val receiptDateStandalonePattern = Regex(
            """(\d{1,4}[/-]\d{1,2}[/-]\d{1,4})"""
        )
        private val receiptDateFormats = listOf(
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yy"),
            DateTimeFormatter.ofPattern("M-d-yyyy"),
            DateTimeFormatter.ofPattern("M-d-yy"),
            DateTimeFormatter.ISO_LOCAL_DATE
        )
        private val splitQuantityPattern = Regex(
            """(?i)^(\d+(?:[.,]\d+)?)\s*(?:lb|lbs|pound|pounds|oz|ounce|ounces|ct|count|ea|each)?\s*@\s*\$?\d+[.,]\d{2}(?:\s*/\s*(?:lb|lbs|pound|pounds|oz|ounce|ounces|ct|count|ea|each))?$"""
        )
    }
}
