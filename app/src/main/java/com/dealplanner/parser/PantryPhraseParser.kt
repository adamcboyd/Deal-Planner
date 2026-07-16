@file:OptIn(kotlin.ExperimentalStdlibApi::class)

package com.dealplanner.parser

import com.dealplanner.data.model.PantryItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Production-ready parser for converting natural language pantry entries to structured PantryItem objects.
 *
 * Examples:
 * - "2 cans of black beans 15oz"
 * - "Great Value peanut butter 16oz opened yesterday"
 * - "1.5 lb ground beef in freezer best by 12/25"
 * - "rice 5 lb bag pantry"
 */
class PantryPhraseParser {

    private val fractionMap = mapOf(
        "half" to 0.5, "quarter" to 0.25, "third" to 0.333,
        "1/2" to 0.5, "1/4" to 0.25, "1/3" to 0.333, "2/3" to 0.667, "3/4" to 0.75,
        "½" to 0.5, "¼" to 0.25, "¾" to 0.75, "⅓" to 0.333, "⅔" to 0.667
    )

    private val locationKeywords = listOf("pantry", "fridge", "freezer", "cabinet", "shelf", "counter")
    private val formKeywords = listOf("canned", "frozen", "fresh", "dried", "boxed", "bagged")
    private val unitKeywords = listOf(
        "lb", "lbs", "pound", "pounds", "oz", "ounce", "ounces", "g", "gram", "grams", "kg",
        "gal", "gallon", "gallons", "qt", "quart", "quarts", "pt", "pint", "pints",
        "ml", "l", "can", "cans", "jar", "jars", "box", "boxes", "bag", "bags",
        "bottle", "bottles", "carton", "cartons", "container", "containers", "cup", "cups",
        "pack", "packs", "package", "packages", "pkg", "pkgs", "ct", "count", "ea", "each"
    )

    private val qtyWords = mapOf(
        "a" to 1.0, "an" to 1.0, "one" to 1.0, "two" to 2.0, "three" to 3.0,
        "four" to 4.0, "five" to 5.0, "six" to 6.0, "seven" to 7.0, "eight" to 8.0,
        "nine" to 9.0, "ten" to 10.0, "dozen" to 12.0
    )

    private val commonBrands = listOf(
        "great value", "kroger", "walmart", "target", "kirkland", "365",
        "trader joe", "aldi", "simple truth", "o organics", "nature's promise"
    )

    data class ParseResult(
        val item: PantryItem,
        val confidence: Double,
        val warnings: List<String>
    )

    fun parse(input: String): ParseResult {
        val warnings = mutableListOf<String>()
        val tokens = tokenize(input.lowercase())

        var qty = 1.0
        var unit: String? = null
        var unitHintFromQuantity: String? = null
        var size: String? = null
        var brand: String? = null
        var location: String? = null
        var form: String? = null
        var opened: LocalDate? = null
        var bestBy: LocalDate? = null
        var needsVerify = false
        val notes = mutableListOf<String>()

        // Extract quantity (including fractions)
        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]

            // Check for numeric quantity
            val numericQty = token.toPantryNumberOrNull()
            if (numericQty != null) {
                qty = numericQty
                i++
                // Check for fraction following number (e.g., "2 1/2")
                if (i < tokens.size && tokens[i] in fractionMap) {
                    qty += fractionMap[tokens[i]]!!
                    i++
                }
                continue
            }

            // Check for word quantities
            if (token in qtyWords) {
                qty = qtyWords[token]!!
                if (token == "dozen") {
                    unitHintFromQuantity = "count"
                }
                i++
                continue
            }

            // Check for fraction words
            if (token in fractionMap) {
                qty = fractionMap[token]!!
                i++
                continue
            }

            break
        }

        // Extract form (canned, frozen, etc.)
        formKeywords.forEach { formKeyword ->
            if (tokens.contains(formKeyword)) {
                form = formKeyword
            }
        }

        // Extract package/container unit immediately after quantity.
        if (i < tokens.size && tokens[i] in unitKeywords) {
            unit = normalizeUnit(tokens[i])
        }
        if (unit == null) {
            unit = unitHintFromQuantity
        }

        // Extract unit and size
        val sizePattern = Regex("""($PANTRY_NUMBER_PATTERN)\s*((?:fl\.?\s*|fluid\s+)?oz|gallon|gallons|gal|quart|quarts|qt|pint|pints|pt|lb|lbs|kg|g|ml|l|ct|count)""")
        val sizeMatch = sizePattern.find(input.lowercase())
        if (sizeMatch != null) {
            val sizeUnitText = normalizeSizeUnitText(sizeMatch.groupValues[2])
            size = formatSizeText(sizeMatch.groupValues[1], sizeUnitText)
            val sizeUnit = normalizeUnit(sizeUnitText)
            if (unit == null || unit in listOf("lb", "oz", "g", "kg", "ml", "l")) {
                unit = sizeUnit
            }
        } else {
            // Look for standalone unit
            tokens.forEach { token ->
                if (token in unitKeywords) {
                    unit = normalizeUnit(token)
                }
            }
        }

        // Extract location
        locationKeywords.forEach { locKeyword ->
            if (tokens.contains(locKeyword)) {
                location = locKeyword
            }
        }

        // Extract brand (check for common brands)
        val inputLower = input.lowercase()
        commonBrands.forEach { brandName ->
            if (inputLower.contains(brandName)) {
                brand = brandName.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
        }

        // Extract dates (opened, best by)
        if (inputLower.contains("opened")) {
            opened = extractDate(input, "opened")
            if (opened == null) {
                warnings.add("Could not parse 'opened' date")
                needsVerify = true
            }
        }

        if (containsBestByCue(inputLower)) {
            bestBy = extractDate(input, *bestByDateCueKeywords.toTypedArray(), EXP_DATE_CUE)
            if (bestBy == null) {
                warnings.add("Could not parse 'best by' date")
                needsVerify = true
            }
        }

        // Extract item name (remove all parsed components)
        val itemName = extractItemName(tokens, unit, form, location, brand)

        // Confidence calculation
        var confidence = 1.0
        if (itemName.isBlank()) {
            confidence -= 0.5
            warnings.add("Could not identify item name")
            needsVerify = true
        } else if (looksUnclear(itemName)) {
            confidence -= 0.4
            warnings.add("Item name may need review")
            needsVerify = true
        }
        if (unit == null && qty > 1) {
            confidence -= 0.1
            warnings.add("Quantity specified but no unit found")
        }
        if (warnings.isNotEmpty()) {
            confidence -= 0.1 * warnings.size
        }

        val pantryItem = PantryItem(
            item = itemName.trim().ifBlank { "unknown" },
            form = form,
            qty = qty,
            unit = unit,
            size = size,
            brand = brand,
            location = location ?: "pantry",
            opened = opened,
            bestBy = bestBy,
            notes = if (notes.isNotEmpty()) notes.joinToString("; ") else null,
            needsVerify = needsVerify || confidence < 0.7
        )

        return ParseResult(
            item = pantryItem,
            confidence = confidence.coerceIn(0.0, 1.0),
            warnings = warnings
        )
    }

    private fun tokenize(input: String): List<String> {
        return input.split(Regex("""\s+""")).map { it.cleanToken() }
    }

    private fun String.cleanToken(): String {
        var token = trim('!', '?')
        while (
            token.length > 1 &&
            token.first() in listOf(',', '.') &&
            !token[1].isDigit()
        ) {
            token = token.drop(1)
        }
        while (
            token.length > 1 &&
            token.last() in listOf(',', '.')
        ) {
            token = token.dropLast(1)
        }
        return token
    }

    private fun normalizeUnit(unit: String): String {
        return when (unit.lowercase()) {
            "lbs", "pound", "pounds" -> "lb"
            "ounce", "ounces" -> "oz"
            "gram", "grams" -> "g"
            "fl oz", "fluid oz" -> "oz"
            "gallon", "gallons" -> "gal"
            "quart", "quarts" -> "qt"
            "pint", "pints" -> "pt"
            "can", "cans" -> "can"
            "jar", "jars" -> "jar"
            "box", "boxes" -> "box"
            "bag", "bags" -> "bag"
            "bottle", "bottles" -> "bottle"
            "carton", "cartons" -> "carton"
            "container", "containers" -> "container"
            "cup", "cups" -> "cup"
            "pack", "packs" -> "pack"
            "package", "packages", "pkg", "pkgs" -> "package"
            "ct", "ea", "each" -> "count"
            else -> unit.lowercase()
        }
    }

    private fun normalizeSizeUnitText(unit: String): String {
        val normalized = unit.lowercase()
            .replace(".", "")
            .replace(Regex("""\s+"""), " ")
            .trim()

        return when (normalized) {
            "fl oz", "fluid oz" -> "fl oz"
            "gallon", "gallons" -> "gal"
            "quart", "quarts" -> "qt"
            "pint", "pints" -> "pt"
            else -> normalized
        }
    }

    private fun formatSizeText(number: String, unit: String): String {
        val normalizedNumber = number.normalizePantryNumberText()
        return if (unit.contains(" ")) {
            "$normalizedNumber $unit"
        } else {
            "$normalizedNumber$unit"
        }
    }

    private fun extractItemName(
        tokens: List<String>,
        unit: String?,
        form: String?,
        location: String?,
        brand: String?
    ): String {
        val skipWords = mutableSetOf<String>()

        // Add parsed components to skip
        skipWords.addAll(qtyWords.keys)
        skipWords.addAll(fractionMap.keys)
        skipWords.addAll(unitKeywords)
        skipWords.addAll(locationKeywords)
        skipWords.addAll(formKeywords)
        skipWords.addAll(
            listOf(
                "of", "in", "on", "the", "a", "an", "opened", "best", "by", "bestby", "before",
                "best-by", "best-before", "if", "use", "use-by", "used", "expires", "expiration", "exp", "date",
                "today", "yesterday", "tomorrow", "days", "day", "ago", "fl", "fluid", "net", "wt", "weight"
            )
        )

        if (brand != null) {
            skipWords.addAll(brand.lowercase().split(" "))
        }
        if (unit != null) {
            skipWords.add(unit.lowercase())
        }
        if (form != null) {
            skipWords.add(form.lowercase())
        }
        if (location != null) {
            skipWords.add(location.lowercase())
        }

        // Remove numbers that are part of size (e.g., "15oz")
        val itemTokens = tokens.filter { token ->
            !skipWords.contains(token) &&
            token.toPantryNumberOrNull() == null &&
            !Regex("""$PANTRY_NUMBER_PATTERN(?:oz|lb|lbs|g|kg|ml|l|gal|gallon|gallons|qt|quart|quarts|pt|pint|pints|ct|count)""").matches(token) &&
            !DATE_TOKEN_PATTERN.matches(token)
        }

        return itemTokens.joinToString(" ")
    }

    private fun looksUnclear(itemName: String): Boolean {
        val normalized = itemName.lowercase().replace(Regex("""[^a-z]"""), "")
        val commonShortItems = setOf("oil", "tea", "egg", "ham", "yam")

        return normalized.length <= 3 &&
            normalized !in commonShortItems &&
            !normalized.any { it in "aeiou" }
    }

    private fun containsBestByCue(inputLower: String): Boolean {
        return bestByDateCueKeywords.any { inputLower.contains(it) } ||
            expDateCuePattern.containsMatchIn(inputLower)
    }

    private fun extractDate(input: String, vararg keywords: String): LocalDate? {
        val dateFormats = listOf(
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy-M-d"),
            DateTimeFormatter.ofPattern("M-d-yyyy"),
            DateTimeFormatter.ofPattern("M-d-yy")
        )

        val inputLower = input.lowercase()

        for (cue in findDateCueRanges(inputLower, keywords.asList())) {
            val afterKeywordStart = (cue.last + 1).coerceAtMost(input.length)
            val afterKeyword = input.substring(
                afterKeywordStart,
                nextDateCueIndex(inputLower, afterKeywordStart) ?: input.length
            )

            findFirstDateInSegment(afterKeyword, dateFormats)?.let { return it }
        }

        return null
    }

    private fun findFirstDateInSegment(
        segment: String,
        dateFormats: List<DateTimeFormatter>
    ): LocalDate? {
        val candidates = mutableListOf<Pair<Int, LocalDate>>()
        val segmentLower = segment.lowercase()

        relativeDatePattern.findAll(segmentLower).forEach { match ->
            val date = when (match.value) {
                "today" -> LocalDate.now()
                "yesterday" -> LocalDate.now().minusDays(1)
                "tomorrow" -> LocalDate.now().plusDays(1)
                else -> null
            }
            if (date != null) {
                candidates.add(match.range.first to date)
            }
        }

        daysAgoPattern.findAll(segmentLower).forEach { match ->
            val days = match.groupValues[1].toLongOrNull()
            if (days != null) {
                candidates.add(match.range.first to LocalDate.now().minusDays(days))
            }
        }

        DATE_TOKEN_PATTERN.findAll(segment).forEach { match ->
            parseAbsoluteDate(match.value, dateFormats)?.let { date ->
                candidates.add(match.range.first to date)
            }
        }

        return candidates.minByOrNull { it.first }?.second
    }

    private fun parseAbsoluteDate(
        value: String,
        dateFormats: List<DateTimeFormatter>
    ): LocalDate? {
        return dateFormats.firstNotNullOfOrNull { formatter ->
            try {
                LocalDate.parse(value, formatter)
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }

    private fun nextDateCueIndex(inputLower: String, afterKeywordStart: Int): Int? {
        return findDateCueRanges(inputLower, dateCueKeywords, afterKeywordStart)
            .map { it.first }
            .minOrNull()
    }

    private fun findDateCueRanges(
        inputLower: String,
        keywords: List<String>,
        startIndex: Int = 0
    ): List<IntRange> {
        return keywords.flatMap { keyword ->
            if (keyword == EXP_DATE_CUE) {
                expDateCuePattern.findAll(inputLower)
                    .map { it.range }
                    .filter { it.first >= startIndex }
                    .toList()
            } else {
                keywordRanges(inputLower, keyword, startIndex)
            }
        }.sortedBy { it.first }
    }

    private fun keywordRanges(
        inputLower: String,
        keyword: String,
        startIndex: Int
    ): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        var index = inputLower.indexOf(keyword, startIndex)
        while (index >= 0) {
            ranges.add(index until index + keyword.length)
            index = inputLower.indexOf(keyword, index + 1)
        }
        return ranges
    }

    /**
     * Checks if two pantry items are duplicates based on item name, size, and brand.
     * Used for duplicate detection and merging.
     */
    fun areDuplicates(item1: PantryItem, item2: PantryItem): Boolean {
        val item1Barcode = extractBarcode(item1.notes)
        val item2Barcode = extractBarcode(item2.notes)
        if (item1Barcode != null || item2Barcode != null) {
            return item1Barcode != null && item1Barcode == item2Barcode
        }

        return duplicateKey(item1) == duplicateKey(item2)
    }

    fun mergeDuplicateItems(existing: PantryItem, incoming: PantryItem): PantryItem {
        return existing.copy(
            item = chooseKnownValue(existing.item, incoming.item) ?: existing.item,
            form = existing.form ?: incoming.form,
            qty = existing.qty + incoming.qty,
            unit = existing.unit ?: incoming.unit,
            size = existing.size ?: incoming.size,
            brand = chooseKnownValue(existing.brand, incoming.brand),
            location = existing.location ?: incoming.location,
            opened = existing.opened ?: incoming.opened,
            bestBy = existing.bestBy ?: incoming.bestBy,
            notes = listOfNotNull(existing.notes, incoming.notes)
                .flatMap { it.split(";") }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString("; ")
                .ifBlank { null },
            needsVerify = existing.needsVerify || incoming.needsVerify
        )
    }

    /**
     * Merges duplicate pantry items by summing quantities.
     */
    fun mergeDuplicates(items: List<PantryItem>): List<PantryItem> {
        val grouped = items.groupBy { mergeKey(it) }

        return grouped.map { (_, group) ->
            group.drop(1).fold(group.first()) { merged, item ->
                mergeDuplicateItems(merged, item)
            }
        }
    }

    private fun mergeKey(item: PantryItem): PantryMergeKey {
        return extractBarcode(item.notes)?.let { barcode ->
            PantryMergeKey(barcode = barcode, duplicateKey = null)
        } ?: PantryMergeKey(barcode = null, duplicateKey = duplicateKey(item))
    }

    private fun duplicateKey(item: PantryItem): PantryDuplicateKey {
        return PantryDuplicateKey(
            item = normalizeKeyText(item.item),
            size = normalizeKeyText(item.size),
            brand = normalizeBrand(item.brand),
            location = normalizeKeyText(item.location ?: "pantry")
        )
    }

    private fun normalizeKeyText(value: String?): String? {
        return value
            ?.lowercase()
            ?.replace(Regex("""[^a-z0-9]+"""), "")
            ?.ifBlank { null }
    }

    private fun normalizeBrand(value: String?): String? {
        val normalized = normalizeKeyText(value)
        return if (normalized == "generic" || normalized == "unknown") null else normalized
    }

    private fun extractBarcode(notes: String?): String? {
        return notes
            ?.split(";")
            ?.map { it.trim() }
            ?.firstNotNullOfOrNull { note ->
                Regex("""(?i)^barcode:\s*([A-Za-z0-9-]+)""")
                    .find(note)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.uppercase()
            }
    }

    private fun chooseKnownValue(existing: String?, incoming: String?): String? {
        val cleanedExisting = existing?.trim()?.ifBlank { null }
        val cleanedIncoming = incoming?.trim()?.ifBlank { null }
        return when {
            cleanedExisting.isNullOrBlank() -> cleanedIncoming
            cleanedExisting.equals("unknown", ignoreCase = true) -> cleanedIncoming ?: cleanedExisting
            cleanedExisting.equals("generic", ignoreCase = true) && !cleanedIncoming.equals("generic", ignoreCase = true) -> cleanedIncoming ?: cleanedExisting
            else -> cleanedExisting
        }
    }

    private fun String.toPantryNumberOrNull(): Double? {
        return normalizePantryNumberText().toDoubleOrNull()
    }

    private fun String.normalizePantryNumberText(): String {
        val normalized = replace(',', '.')
        return if (normalized.startsWith(".")) {
            "0$normalized"
        } else {
            normalized
        }
    }

    private data class PantryDuplicateKey(
        val item: String?,
        val size: String?,
        val brand: String?,
        val location: String?
    )

    private data class PantryMergeKey(
        val barcode: String?,
        val duplicateKey: PantryDuplicateKey?
    )

    private companion object {
        private const val PANTRY_NUMBER_PATTERN = """(?:\d+)?[.,]?\d+"""
        private const val EXP_DATE_CUE = "exp"
        private val DATE_TOKEN_PATTERN = Regex("""(?:\d{4}[/-]\d{1,2}[/-]\d{1,2})|(?:\d{1,2}[/-]\d{1,2}[/-]\d{2,4})""")
        private val relativeDatePattern = Regex("""\b(today|yesterday|tomorrow)\b""")
        private val daysAgoPattern = Regex("""\b(\d+)\s*days?\s*ago\b""")
        private val expDateCuePattern = Regex("""\bexp\.?\b""")
        private val bestByDateCueKeywords = listOf(
            "best if used by",
            "best-before",
            "best before",
            "best-by",
            "best by",
            "bestby",
            "use-by",
            "use by",
            "expires",
            "expiration"
        )
        private val dateCueKeywords = listOf("opened") + bestByDateCueKeywords + EXP_DATE_CUE
    }
}
