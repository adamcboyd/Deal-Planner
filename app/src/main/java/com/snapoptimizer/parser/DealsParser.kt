package com.snapoptimizer.parser

import com.snapoptimizer.data.model.DealItem
import java.time.LocalDate
import kotlin.math.abs

/**
 * Parser for grocery flyer text from OCR.
 * Handles various deal formats:
 * - $X.XX/lb
 * - (N) for $X or N for $X
 * - Buy N Get M
 * - X% off
 * - Member Price, Digital Coupon
 */
class DealsParser {

    data class ParseResult(
        val deals: List<DealItem>,
        val warnings: List<String>
    )

    private val pricePerPoundPattern = Regex("""\$(\d+\.\d{2})\s*/\s*lb""", RegexOption.IGNORE_CASE)
    private val pricePerUnitPattern = Regex("""\$(\d+\.\d{2})\s*/\s*(ea|each|oz)""", RegexOption.IGNORE_CASE)
    private val nForXPattern = Regex("""(\d+)\s*for\s*\$(\d+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
    private val buyNGetMPattern = Regex("""buy\s*(\d+)\s*get\s*(\d+)(?:\s*free)?""", RegexOption.IGNORE_CASE)
    private val percentOffPattern = Regex("""(\d+)%\s*off""", RegexOption.IGNORE_CASE)
    private val limitPattern = Regex("""limit\s*(\d+)""", RegexOption.IGNORE_CASE)
    private val sizePattern = Regex("""(\d+(?:\.\d+)?)\s*(oz|lb|lbs|g|kg|ml|l)""", RegexOption.IGNORE_CASE)

    private val couponKeywords = listOf("coupon", "digital coupon", "member price", "clip", "app only")

    // Baseline prices for common items (for deal scoring)
    private val baselinePrices = mapOf(
        "chicken" to 3.50,
        "beef" to 6.00,
        "pork" to 4.50,
        "salmon" to 9.00,
        "broccoli" to 2.50,
        "carrots" to 1.50,
        "potatoes" to 1.00,
        "rice" to 2.00,
        "pasta" to 1.50,
        "beans" to 1.00
    )

    fun parse(ocrText: String, store: String = "Unknown"): ParseResult {
        val deals = mutableListOf<DealItem>()
        val warnings = mutableListOf<String>()

        val lines = ocrText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        var i = 0
        while (i < lines.size) {
            val line = lines[i]

            if (!containsDealSignal(line)) {
                i++
                continue
            }

            val modifierLines = mutableListOf<String>()
            var j = i + 1
            while (j < lines.size && isModifierLine(lines[j])) {
                modifierLines.add(lines[j])
                j++
            }

            val combinedLine = (listOf(line) + modifierLines).joinToString(" ")
            val fallbackName = findPreviousName(lines, i)

            // Try to parse deal from current line plus trailing flyer modifiers.
            val dealResult = parseDealLine(combinedLine, fallbackName, store)

            if (dealResult != null) {
                deals.add(dealResult)
            }

            i = if (modifierLines.isNotEmpty()) j else i + 1
        }

        // Calculate deal scores
        val scoredDeals = deals.map { deal ->
            deal.copy(dealScore = calculateDealScore(deal))
        }

        return ParseResult(
            deals = scoredDeals,
            warnings = warnings
        )
    }

    private fun parseDealLine(line: String, nextLine: String, store: String): DealItem? {
        var name = ""
        var price = 0.0
        var unit: String? = null
        var dealType = ""
        var limit: Int? = null
        var couponFlag = false
        var discountPercent = 0.0
        var confidence = 1.0

        // Check for coupon keywords
        couponFlag = couponKeywords.any { line.lowercase().contains(it) }

        // Check for limit
        limitPattern.find(line)?.let {
            limit = it.groupValues[1].toIntOrNull()
        }

        // Percent-off lines may be standalone deals or modifiers for a base price.
        percentOffPattern.find(line)?.let { match ->
            discountPercent = match.groupValues[1].toDouble()
        }

        // Parse different deal types

        // 1. Price per pound: $3.99/lb
        pricePerPoundPattern.find(line)?.let { match ->
            price = match.groupValues[1].toDouble()
            unit = "lb"
            dealType = "per_pound"
            name = extractItemName(line, match.value)
            if (name.isBlank() && nextLine.isNotBlank()) {
                name = nextLine.take(50)
                confidence = 0.8
            }
            return createDealItem(name, price, unit, dealType, limit, couponFlag, store, confidence, discountPercent, line)
        }

        // 2. Price per unit: $2.99/ea
        pricePerUnitPattern.find(line)?.let { match ->
            price = match.groupValues[1].toDouble()
            unit = match.groupValues[2].lowercase()
            dealType = "per_unit"
            name = extractItemName(line, match.value)
            if (name.isBlank() && nextLine.isNotBlank()) {
                name = nextLine.take(50)
                confidence = 0.8
            }
            return createDealItem(name, price, unit, dealType, limit, couponFlag, store, confidence, discountPercent, line)
        }

        // 3. N for $X: 2 for $5
        nForXPattern.find(line)?.let { match ->
            val n = match.groupValues[1].toInt()
            val totalPrice = match.groupValues[2].toDouble()
            price = totalPrice / n
            unit = "ea"
            dealType = "n_for_x"
            name = extractItemName(line, match.value)
            if (name.isBlank() && nextLine.isNotBlank()) {
                name = nextLine.take(50)
                confidence = 0.8
            }
            return createDealItem(name, price, unit, dealType, limit, couponFlag, store, confidence, discountPercent, line)
        }

        // 4. Buy N Get M: Buy 2 Get 1
        buyNGetMPattern.find(line)?.let { match ->
            val buyN = match.groupValues[1].toInt()
            val getM = match.groupValues[2].toInt()
            dealType = "buy_n_get_m"
            unit = "ea"

            // Extract price if available
            val priceMatch = Regex("""\$(\d+\.\d{2})""").find(line)
            price = priceMatch?.groupValues?.get(1)?.toDouble() ?: 0.0

            name = extractItemName(line, match.value)
            if (name.isBlank() && nextLine.isNotBlank()) {
                name = nextLine.take(50)
                confidence = 0.7
            }

            // Effective discount: if buy 2 get 1, that's 33% off
            discountPercent = (getM.toDouble() / (buyN + getM)) * 100

            return createDealItem(name, price, unit, dealType, limit, couponFlag, store, confidence, discountPercent, line)
        }

        // 5. Percent off: 25% off
        percentOffPattern.find(line)?.let { match ->
            discountPercent = match.groupValues[1].toDouble()
            dealType = "percent_off"
            unit = "ea"

            // Extract price if available
            val priceMatch = Regex("""\$(\d+\.\d{2})""").find(line)
            price = priceMatch?.groupValues?.get(1)?.toDouble() ?: 0.0

            name = extractItemName(line, match.value)
            if (name.isBlank() && nextLine.isNotBlank()) {
                name = nextLine.take(50)
                confidence = 0.7
            }

            return createDealItem(name, price, unit, dealType, limit, couponFlag, store, confidence, discountPercent, line)
        }

        return null
    }

    private fun extractItemName(line: String, dealText: String): String {
        // Remove the deal text and clean up
        var name = line.replace(dealText, "")
        name = name.replace(Regex("""limit\s*\d+""", RegexOption.IGNORE_CASE), "")
        name = name.replace(percentOffPattern, "")
        couponKeywords.forEach { keyword ->
            name = name.replace(keyword, "", ignoreCase = true)
        }
        return name.trim()
    }

    private fun containsDealSignal(line: String): Boolean {
        return pricePerPoundPattern.containsMatchIn(line) ||
            pricePerUnitPattern.containsMatchIn(line) ||
            nForXPattern.containsMatchIn(line) ||
            buyNGetMPattern.containsMatchIn(line) ||
            percentOffPattern.containsMatchIn(line)
    }

    private fun isModifierLine(line: String): Boolean {
        val lineLower = line.lowercase()
        return limitPattern.matches(line) ||
            percentOffPattern.matches(line) ||
            couponKeywords.any { lineLower.contains(it) }
    }

    private fun findPreviousName(lines: List<String>, currentIndex: Int): String {
        for (index in currentIndex - 1 downTo 0) {
            val previous = lines[index]
            if (!containsDealSignal(previous) && !isModifierLine(previous)) {
                return previous
            }
        }
        return ""
    }

    private fun createDealItem(
        name: String,
        price: Double,
        unit: String?,
        dealType: String,
        limit: Int?,
        couponFlag: Boolean,
        store: String,
        confidence: Double,
        discountPercent: Double,
        rawText: String
    ): DealItem {
        // Extract size if present in name
        val sizeMatch = sizePattern.find(name)
        val sizeText = sizeMatch?.value

        // Calculate price per unit (normalized to per pound)
        val pricePerUnit = when (unit) {
            "lb" -> price
            "oz" -> price * 16 // Convert oz to lb
            "ea" -> price // Keep as-is for "each"
            else -> price
        }

        return DealItem(
            name = name.trim().ifBlank { "Unknown Item" },
            price = price,
            unit = unit,
            dealType = dealType,
            limit = limit,
            couponFlag = couponFlag,
            store = store,
            confidence = confidence,
            sizeText = sizeText,
            pricePerUnit = pricePerUnit,
            discountPercent = discountPercent,
            rawText = rawText
        )
    }

    /**
     * Calculate deal score based on multiple factors:
     * - 40% discount percentage
     * - 25% price per unit vs baseline
     * - 20% stackability (coupons, limits)
     * - 15% utility fit
     */
    private fun calculateDealScore(deal: DealItem): Double {
        var score = 0.0

        // 1. Discount percentage (40% weight)
        if (deal.discountPercent > 0) {
            score += (deal.discountPercent / 100.0) * 0.4
        }

        // 2. Price per unit vs baseline (25% weight)
        val itemKeyword = findItemKeyword(deal.name.lowercase())
        if (itemKeyword != null && deal.pricePerUnit > 0) {
            val baseline = baselinePrices[itemKeyword] ?: deal.pricePerUnit
            val priceDiff = (baseline - deal.pricePerUnit) / baseline
            score += (priceDiff.coerceIn(-0.5, 1.0) * 0.25)
        }

        // 3. Stackability (20% weight)
        var stackability = 0.0
        if (deal.couponFlag) stackability += 0.5
        if (deal.limit == null || deal.limit!! > 2) stackability += 0.5
        score += stackability * 0.2

        // 4. Utility fit (15% weight) - basic heuristic
        val utilityScore = when {
            deal.dealType == "per_pound" -> 0.8 // Protein deals are valuable
            deal.dealType == "buy_n_get_m" -> 0.7
            deal.dealType == "n_for_x" -> 0.6
            else -> 0.5
        }
        score += utilityScore * 0.15

        return score.coerceIn(0.0, 1.0)
    }

    private fun findItemKeyword(name: String): String? {
        return baselinePrices.keys.firstOrNull { name.contains(it) }
    }
}
