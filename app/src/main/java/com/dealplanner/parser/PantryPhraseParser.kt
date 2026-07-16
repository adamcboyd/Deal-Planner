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
    private val unitKeywords = listOf("lb", "lbs", "pound", "pounds", "oz", "ounce", "ounces", "g", "gram", "grams", "kg", "can", "cans", "jar", "jars", "box", "boxes", "bag", "bags", "count")

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
            val numericQty = token.toDoubleOrNull()
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

        // Extract unit and size
        val sizePattern = Regex("""(\d+(?:\.\d+)?)\s*(oz|lb|lbs|g|kg|ml|l)""")
        val sizeMatch = sizePattern.find(input.lowercase())
        if (sizeMatch != null) {
            size = sizeMatch.value
            val sizeUnit = normalizeUnit(sizeMatch.groupValues[2])
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

        if (inputLower.contains("best by") || inputLower.contains("bestby") || inputLower.contains("expires")) {
            bestBy = extractDate(input, "best by", "bestby", "expires")
            if (bestBy == null) {
                warnings.add("Could not parse 'best by' date")
                needsVerify = true
            }
        }

        // Extract item name (remove all parsed components)
        val itemName = extractItemName(tokens, qty, unit, form, location, brand)

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
        return input.split(Regex("""\s+""")).map { it.trim(',', '.', '!', '?') }
    }

    private fun normalizeUnit(unit: String): String {
        return when (unit.lowercase()) {
            "lbs", "pound", "pounds" -> "lb"
            "ounce", "ounces" -> "oz"
            "gram", "grams" -> "g"
            "can", "cans" -> "can"
            "jar", "jars" -> "jar"
            "box", "boxes" -> "box"
            "bag", "bags" -> "bag"
            else -> unit.lowercase()
        }
    }

    private fun extractItemName(
        tokens: List<String>,
        qty: Double,
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
                "of", "in", "the", "a", "an", "opened", "best", "by", "bestby", "expires",
                "today", "yesterday", "tomorrow", "days", "day", "ago"
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
            token.toDoubleOrNull() == null &&
            !Regex("""\d+(?:\.\d+)?(?:oz|lb|lbs|g|kg|ml|l)""").matches(token) &&
            !Regex("""\d{1,2}[/-]\d{1,2}[/-]\d{2,4}""").matches(token)
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

    private fun extractDate(input: String, vararg keywords: String): LocalDate? {
        val dateFormats = listOf(
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("M-d-yyyy")
        )

        // Look for relative dates
        val inputLower = input.lowercase()
        when {
            inputLower.contains("today") -> return LocalDate.now()
            inputLower.contains("yesterday") -> return LocalDate.now().minusDays(1)
            inputLower.contains("tomorrow") -> return LocalDate.now().plusDays(1)
        }

        // Look for "X days ago"
        val daysAgoPattern = Regex("""(\d+)\s*days?\s*ago""")
        daysAgoPattern.find(inputLower)?.let { match ->
            val days = match.groupValues[1].toLongOrNull()
            if (days != null) {
                return LocalDate.now().minusDays(days)
            }
        }

        // Look for explicit dates near keywords
        for (keyword in keywords) {
            val keywordIndex = inputLower.indexOf(keyword)
            if (keywordIndex != -1) {
                val afterKeyword = input.substring((keywordIndex + keyword.length).coerceAtMost(input.length))

                // Extract potential date strings
                val datePattern = Regex("""\d{1,2}[/-]\d{1,2}[/-]\d{2,4}""")
                val dateMatch = datePattern.find(afterKeyword)

                if (dateMatch != null) {
                    for (formatter in dateFormats) {
                        try {
                            return LocalDate.parse(dateMatch.value, formatter)
                        } catch (e: DateTimeParseException) {
                            // Try next format
                        }
                    }
                }
            }
        }

        return null
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
}
