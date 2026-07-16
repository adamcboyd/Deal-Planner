package com.dealplanner.parser

import com.dealplanner.data.model.DealItem
import com.dealplanner.util.toStoreNameOrUnknown
import java.time.LocalDate
import kotlin.math.abs

/**
 * Parser for grocery flyer text from OCR.
 * Handles various deal formats:
 * - $X.XX/lb
 * - (N) for $X or N for $X
 * - Buy N Get M, Buy N Get M X% off, BOGO Free, BOGO X% off, B1G1
 * - X% off
 * - Member Price, Digital Coupon
 */
class DealsParser {

    data class ParseResult(
        val deals: List<DealItem>,
        val warnings: List<String>
    )

    private val pricePerPoundPattern = Regex("""\$?(\d+[.,]\d{2})\s*/\s*(?:lb|lbs|pound|pounds)""", RegexOption.IGNORE_CASE)
    private val pricePerUnitPattern = Regex("""\$?(\d+[.,]\d{2})\s*/\s*(ea|each|oz)""", RegexOption.IGNORE_CASE)
    private val centsPricePerPoundPattern = Regex("""(?<![\d.])(\d{1,3})\s*(?:¢|cents?|c)\s*/\s*(?:lb|lbs|pound|pounds)""", RegexOption.IGNORE_CASE)
    private val centsPricePerUnitPattern = Regex("""(?<![\d.])(\d{1,3})\s*(?:¢|cents?|c)\s*/\s*(ea|each|oz)""", RegexOption.IGNORE_CASE)
    private val nForXPattern = Regex("""(\d+)\s*for\s*\$?(\d+(?:[.,]\d{2})?)""", RegexOption.IGNORE_CASE)
    private val slashNForXPattern = Regex("""(?<![\d.,])(\d+)\s*/\s*\$?(\d+(?:[.,]\d{2})?)""", RegexOption.IGNORE_CASE)
    private val buyNGetMPattern = Regex(
        """buy\s*(\d+|one|two|three|four|five|six|seven|eight|nine|ten)\s*get\s*(\d+|one|two|three|four|five|six|seven|eight|nine|ten)(?:\s*free)?""",
        RegexOption.IGNORE_CASE
    )
    private val buyNGetMPercentPattern = Regex(
        """buy\s*(\d+|one|two|three|four|five|six|seven|eight|nine|ten)\s*get\s*(\d+|one|two|three|four|five|six|seven|eight|nine|ten)\s*(\d+)%\s*off""",
        RegexOption.IGNORE_CASE
    )
    private val bogoPercentPattern = Regex("""\bbogo\s*(\d+)%\s*off\b""", RegexOption.IGNORE_CASE)
    private val bogoPattern = Regex("""\b(?:bogo(?:\s*free)?|b1g1(?:\s*free)?)\b""", RegexOption.IGNORE_CASE)
    private val percentOffPattern = Regex("""(\d+)%\s*off""", RegexOption.IGNORE_CASE)
    private val limitPattern = Regex("""limit\s*(\d+)""", RegexOption.IGNORE_CASE)
    private val sizePattern = Regex("""(\d+(?:\.\d+)?)\s*(oz|lb|lbs|g|kg|ml|l)""", RegexOption.IGNORE_CASE)
    private val packagePricePattern = Regex("""(?<![\d.,])\$?(\d+[.,]\d{2})(?!\s*(?:oz|lb|lbs|pound|pounds|g|kg|ml|l)\b)""", RegexOption.IGNORE_CASE)
    private val centsPackagePricePattern = Regex("""(?<![\d.])(\d{1,3})\s*(?:¢|cents?|c)(?=\s|$)""", RegexOption.IGNORE_CASE)
    private val priceTextPattern = Regex("""\$?\d+[.,]\d{2}(?:\s*/\s*(?:lb|lbs|pound|pounds|ea|each|oz)|(?!\s*(?:oz|lb|lbs|pound|pounds|g|kg|ml|l)\b))""", RegexOption.IGNORE_CASE)

    private val couponKeywords = listOf("coupon", "digital coupon", "member price", "clip", "app only")
    private val packageWords = Regex("""\b(bag|can|box|bottle|jar|pack|family|fresh|wild|caught|boneless|skinless|extra|virgin|with)\b""", RegexOption.IGNORE_CASE)
    private val categoryWords = setOf(
        "kroger weekly ad",
        "fresh meat seafood",
        "fresh meat & seafood",
        "fresh produce",
        "grocery",
        "frozen"
    )

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
        val normalizedStore = store.toStoreNameOrUnknown()

        val lines = ocrText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        var i = 0
        while (i < lines.size) {
            val line = lines[i]

            if (isFlyerMetadataLine(line) || !containsDealSignal(line)) {
                i++
                continue
            }

            val (modifierLines, nextIndex) = collectTrailingModifiers(lines, i)

            val combinedLine = (listOf(line) + modifierLines).joinToString(" ")
            val fallbackName = findPreviousName(lines, i)

            // Try to parse deal from current line plus trailing flyer modifiers.
            val dealResult = parseDealLine(combinedLine, fallbackName, normalizedStore)

            if (dealResult != null) {
                deals.add(dealResult)
            }

            i = nextIndex
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
        var name: String
        var price: Double
        var unit: String?
        var dealType: String
        var confidence = 1.0

        // Check for coupon keywords
        val couponFlag = couponKeywords.any { line.lowercase().contains(it) }

        // Check for limit
        val limit = limitPattern.find(line)?.groupValues?.get(1)?.toIntOrNull()

        // Percent-off lines may be standalone deals or modifiers for a base price.
        var discountPercent = percentOffPattern.find(line)?.groupValues?.get(1)?.toDouble() ?: 0.0

        // Parse different deal types

        // 1. Price per pound: $3.99/lb
        pricePerPoundPattern.find(line)?.let { match ->
            price = match.groupValues[1].toPriceDouble()
            unit = "lb"
            dealType = "per_pound"
            name = chooseName(extractItemName(line, match.value), nextLine)
            if (name.isBlank() && nextLine.isNotBlank()) {
                name = nextLine.take(50)
                confidence = 0.8
            }
            return createDealItem(name, price, unit, dealType, limit, couponFlag, store, confidence, discountPercent, line)
        }

        centsPricePerPoundPattern.find(line)?.let { match ->
            price = centsToDollars(match.groupValues[1])
            unit = "lb"
            dealType = "per_pound"
            name = chooseName(extractItemName(line, match.value), nextLine)
            if (name.isBlank() && nextLine.isNotBlank()) {
                name = nextLine.take(50)
                confidence = 0.8
            }
            return createDealItem(name, price, unit, dealType, limit, couponFlag, store, confidence, discountPercent, line)
        }

        // 2. Price per unit: $2.99/ea
        pricePerUnitPattern.find(line)?.let { match ->
            price = match.groupValues[1].toPriceDouble()
            unit = match.groupValues[2].lowercase()
            dealType = "per_unit"
            name = chooseName(extractItemName(line, match.value), nextLine)
            if (name.isBlank() && nextLine.isNotBlank()) {
                name = nextLine.take(50)
                confidence = 0.8
            }
            return createDealItem(name, price, unit, dealType, limit, couponFlag, store, confidence, discountPercent, line)
        }

        // 3. N for $X: 2 for $5
        nForXPattern.find(line)?.let { match ->
            val n = match.groupValues[1].toInt()
            val totalPrice = match.groupValues[2].toPriceDouble()
            price = totalPrice / n
            unit = "ea"
            dealType = "n_for_x"
            name = chooseName(extractItemName(line, match.value), nextLine)
            if (name.isBlank() && nextLine.isNotBlank()) {
                name = nextLine.take(50)
                confidence = 0.8
            }
            return createDealItem(name, price, unit, dealType, limit, couponFlag, store, confidence, discountPercent, line)
        }

        centsPricePerUnitPattern.find(line)?.let { match ->
            price = centsToDollars(match.groupValues[1])
            unit = match.groupValues[2].lowercase()
            dealType = "per_unit"
            name = chooseName(extractItemName(line, match.value), nextLine)
            if (name.isBlank() && nextLine.isNotBlank()) {
                name = nextLine.take(50)
                confidence = 0.8
            }
            return createDealItem(name, price, unit, dealType, limit, couponFlag, store, confidence, discountPercent, line)
        }

        // 4. Slash N for X: 2/$5 or 10 / $10
        slashNForXPattern.find(line)?.let { match ->
            val n = match.groupValues[1].toInt()
            val totalPrice = match.groupValues[2].toPriceDouble()
            price = totalPrice / n
            unit = "ea"
            dealType = "n_for_x"
            name = chooseName(extractItemName(line, match.value), nextLine)
            if (name.isBlank() && nextLine.isNotBlank()) {
                name = nextLine.take(50)
                confidence = 0.8
            }
            return createDealItem(name, price, unit, dealType, limit, couponFlag, store, confidence, discountPercent, line)
        }

        buyNGetMPercentPattern.find(line)?.let { match ->
            val buyN = match.groupValues[1].toDealCount() ?: return null
            val getM = match.groupValues[2].toDealCount() ?: return null
            val secondItemDiscount = match.groupValues[3].toDouble()
            dealType = "buy_n_get_m"
            unit = "ea"

            val priceMatch = packagePricePattern.find(line)
            price = priceMatch?.groupValues?.get(1)?.toPriceDouble() ?: 0.0

            name = chooseName(extractItemName(line, match.value), nextLine)
            if (name.isBlank() && nextLine.isNotBlank()) {
                name = nextLine.take(50)
                confidence = 0.7
            }

            discountPercent = (secondItemDiscount * getM) / (buyN + getM)

            return createDealItem(name, price, unit, dealType, limit, couponFlag, store, confidence, discountPercent, line)
        }

        // 5. Buy N Get M: Buy 2 Get 1
        buyNGetMPattern.find(line)?.let { match ->
            val buyN = match.groupValues[1].toDealCount() ?: return null
            val getM = match.groupValues[2].toDealCount() ?: return null
            dealType = "buy_n_get_m"
            unit = "ea"

            // Extract price if available
            val priceMatch = packagePricePattern.find(line)
            price = priceMatch?.groupValues?.get(1)?.toPriceDouble() ?: 0.0

            name = chooseName(extractItemName(line, match.value), nextLine)
            if (name.isBlank() && nextLine.isNotBlank()) {
                name = nextLine.take(50)
                confidence = 0.7
            }

            // Effective discount: if buy 2 get 1, that's 33% off
            discountPercent = (getM.toDouble() / (buyN + getM)) * 100

            return createDealItem(name, price, unit, dealType, limit, couponFlag, store, confidence, discountPercent, line)
        }

        bogoPercentPattern.find(line)?.let { match ->
            dealType = "buy_n_get_m"
            unit = "ea"

            val priceMatch = packagePricePattern.find(line)
            price = priceMatch?.groupValues?.get(1)?.toPriceDouble() ?: 0.0

            name = chooseName(extractItemName(line, match.value), nextLine)
            if (name.isBlank() && nextLine.isNotBlank()) {
                name = nextLine.take(50)
                confidence = 0.7
            }

            val secondItemDiscount = match.groupValues[1].toDouble()
            discountPercent = secondItemDiscount / 2.0

            return createDealItem(name, price, unit, dealType, limit, couponFlag, store, confidence, discountPercent, line)
        }

        bogoPattern.find(line)?.let { match ->
            dealType = "buy_n_get_m"
            unit = "ea"

            // BOGO/B1G1 is buy one, get one free: 50% effective discount.
            val priceMatch = packagePricePattern.find(line)
            price = priceMatch?.groupValues?.get(1)?.toPriceDouble() ?: 0.0

            name = chooseName(extractItemName(line, match.value), nextLine)
            if (name.isBlank() && nextLine.isNotBlank()) {
                name = nextLine.take(50)
                confidence = 0.7
            }

            discountPercent = 50.0

            return createDealItem(name, price, unit, dealType, limit, couponFlag, store, confidence, discountPercent, line)
        }

        // 6. Percent off: 25% off
        percentOffPattern.find(line)?.let { match ->
            discountPercent = match.groupValues[1].toDouble()
            dealType = "percent_off"
            unit = "ea"

            // Extract price if available
            val priceMatch = packagePricePattern.find(line)
            price = priceMatch?.groupValues?.get(1)?.toPriceDouble() ?: 0.0

            name = chooseName(extractItemName(line, match.value), nextLine)
            if (name.isBlank() && nextLine.isNotBlank()) {
                name = nextLine.take(50)
                confidence = 0.7
            }

            return createDealItem(name, price, unit, dealType, limit, couponFlag, store, confidence, discountPercent, line)
        }

        // 7. Plain package price: Yellow Onions 3 lb bag $2.99 or Black Beans $0.89
        packagePricePattern.find(line)?.let { match ->
            price = match.groupValues[1].toPriceDouble()
            unit = "ea"
            dealType = "per_unit"
            name = chooseName(extractItemName(line, match.value), nextLine)
            if (name.isBlank() && nextLine.isNotBlank()) {
                name = nextLine.take(50)
                confidence = 0.8
            }
            return createDealItem(name, price, unit, dealType, limit, couponFlag, store, confidence, discountPercent, line)
        }

        centsPackagePricePattern.find(line)?.let { match ->
            price = centsToDollars(match.groupValues[1])
            unit = "ea"
            dealType = "per_unit"
            name = chooseName(extractItemName(line, match.value), nextLine)
            if (name.isBlank() && nextLine.isNotBlank()) {
                name = nextLine.take(50)
                confidence = 0.8
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
        couponKeywords.sortedByDescending { it.length }.forEach { keyword ->
            name = name.replace(keyword, "", ignoreCase = true)
        }
        name = name.replace(buyNGetMPattern, "")
        name = name.replace(buyNGetMPercentPattern, "")
        name = name.replace(bogoPercentPattern, "")
        name = name.replace(bogoPattern, "")
        name = name.replace(priceTextPattern, "")
        return name.trim()
    }

    private fun containsDealSignal(line: String): Boolean {
        return pricePerPoundPattern.containsMatchIn(line) ||
            pricePerUnitPattern.containsMatchIn(line) ||
            centsPricePerPoundPattern.containsMatchIn(line) ||
            centsPricePerUnitPattern.containsMatchIn(line) ||
            nForXPattern.containsMatchIn(line) ||
            slashNForXPattern.containsMatchIn(line) ||
            buyNGetMPercentPattern.containsMatchIn(line) ||
            buyNGetMPattern.containsMatchIn(line) ||
            bogoPercentPattern.containsMatchIn(line) ||
            bogoPattern.containsMatchIn(line) ||
            percentOffPattern.containsMatchIn(line) ||
            packagePricePattern.containsMatchIn(line) ||
            centsPackagePricePattern.containsMatchIn(line)
    }

    private fun isFlyerMetadataLine(line: String): Boolean {
        val normalized = line.lowercase().trim()
        return flyerDatePattern.containsMatchIn(normalized) ||
            flyerMetadataPrefixes.any { normalized.startsWith(it) }
    }

    private fun isModifierLine(line: String): Boolean {
        val lineLower = line.lowercase()
        return limitPattern.matches(line) ||
            percentOffPattern.matches(line) ||
            buyNGetMPercentPattern.matches(line) ||
            buyNGetMPattern.matches(line) ||
            bogoPercentPattern.matches(line) ||
            bogoPattern.matches(line) ||
            couponKeywords.any { lineLower.contains(it) }
    }

    private fun collectTrailingModifiers(lines: List<String>, currentIndex: Int): Pair<List<String>, Int> {
        if (isModifierLine(lines[currentIndex])) {
            return emptyList<String>() to currentIndex + 1
        }

        val modifierLines = mutableListOf<String>()
        var index = currentIndex + 1

        while (index < lines.size) {
            val line = lines[index]
            val nextLine = lines.getOrNull(index + 1)

            when {
                isModifierLine(line) -> {
                    modifierLines.add(line)
                    index++
                }
                !containsDealSignal(line) && nextLine != null && isModifierLine(nextLine) -> {
                    modifierLines.add(line)
                    modifierLines.add(nextLine)
                    index += 2
                }
                else -> break
            }
        }

        return modifierLines to index
    }

    private fun findPreviousName(lines: List<String>, currentIndex: Int): String {
        val nameParts = ArrayDeque<String>()

        for (index in currentIndex - 1 downTo 0) {
            val previous = lines[index]
            if (containsDealSignal(previous) || isModifierLine(previous) || isCategoryLine(previous)) {
                break
            }

            nameParts.addFirst(previous)
            if (nameParts.size >= 2) {
                break
            }
        }

        return nameParts.joinToString(" ").take(80)
    }

    private fun chooseName(extractedName: String, fallbackName: String): String {
        val cleaned = extractedName
            .replace(priceTextPattern, "")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .trim('-', '–', ':')
            .trim()

        return if (looksLikePackageOnly(cleaned)) fallbackName else cleaned
    }

    private fun looksLikePackageOnly(name: String): Boolean {
        if (name.isBlank()) return true
        val meaningful = name
            .replace(sizePattern, "")
            .replace(packageWords, "")
            .replace(Regex("""[^A-Za-z]+"""), "")

        return meaningful.length < 3
    }

    private fun isCategoryLine(line: String): Boolean {
        val normalized = line.lowercase()
            .replace("&", " ")
            .replace(Regex("""[^a-z0-9]+"""), " ")
            .trim()
            .replace(Regex("""\s+"""), " ")
        return normalized in categoryWords
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
        val sizeMatch = sizePattern.find(name) ?: rawText
            .takeUnless { bogoPattern.containsMatchIn(it) }
            ?.let { sizePattern.find(it) }
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
        if (deal.limit?.let { it > 2 } ?: true) stackability += 0.5
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

    private fun centsToDollars(value: String): Double {
        return value.toInt() / 100.0
    }

    private fun String.toDealCount(): Int? {
        return toIntOrNull() ?: dealCountWords[lowercase()]
    }

    private fun String.toPriceDouble(): Double {
        return replace(',', '.').toDouble()
    }

    private companion object {
        private val dealCountWords = mapOf(
            "one" to 1,
            "two" to 2,
            "three" to 3,
            "four" to 4,
            "five" to 5,
            "six" to 6,
            "seven" to 7,
            "eight" to 8,
            "nine" to 9,
            "ten" to 10
        )
        private val flyerDatePattern = Regex("""\b\d{1,2}[/-]\d{1,2}[/-]\d{2,4}\b""")
        private val flyerMetadataPrefixes = listOf(
            "valid ",
            "valid:",
            "valid thru",
            "valid through",
            "sale dates",
            "prices effective",
            "effective ",
            "through ",
            "thru "
        )
    }
}
