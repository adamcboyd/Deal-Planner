package com.dealplanner.ai

import com.dealplanner.data.model.PantryItem
import com.dealplanner.util.toFlexibleLocalDateOrNull

fun GeminiPantryVisionClient.PantryVisionItem.toPantryItem(warnings: List<String>): PantryItem? {
    val productName = product.normalizedText() ?: return null
    val normalizedBrand = brand.normalizeBrand()
    val normalizedUnit = unit.normalizePantryUnit()
    val normalizedSize = size.normalizedText()
    val normalizedLocation = location.normalizeStorageLocation()
    val parsedOpened = openedDate.toParsedDate()
    val parsedBestBy = expirationDate.toParsedDate()
    val unparsedOpenedNote = openedDate.toUnparsedDateNote("opened date", parsedOpened != null)
    val unparsedBestByNote = expirationDate.toUnparsedDateNote("best-by date", parsedBestBy != null)
    val questionNotes = questions.joinToString(" ")
    val warningNotes = warnings.joinToString(" ")
    val missingBrand = normalizedBrand == null
    val safeQuantity = quantity?.takeIf { it > 0.0 }
    val missingAmount = safeQuantity == null || normalizedUnit == null
    val missingLocation = normalizedLocation == null
    val missingDate = parsedBestBy == null
    val reviewNotes = buildReviewNotes(
        missingBrand = missingBrand,
        missingAmount = missingAmount,
        missingLocation = missingLocation,
        missingDate = missingDate
    )

    return PantryItem(
        item = productName,
        qty = safeQuantity ?: 1.0,
        unit = normalizedUnit,
        size = normalizedSize,
        brand = normalizedBrand ?: "Generic",
        location = normalizedLocation ?: "pantry",
        opened = parsedOpened,
        bestBy = parsedBestBy,
        notes = mergeNotes("AI photo import", reviewNotes, questionNotes, warningNotes, unparsedOpenedNote, unparsedBestByNote),
        needsVerify = confidence < 0.85 ||
            questions.isNotEmpty() ||
            missingBrand ||
            missingAmount ||
            missingLocation ||
            missingDate ||
            unparsedOpenedNote != null ||
            unparsedBestByNote != null
    )
}

private fun String?.toParsedDate() = this?.toFlexibleLocalDateOrNull()

private fun String?.normalizedText(): String? {
    return this
        ?.trim()
        ?.replace(Regex("""\s+"""), " ")
        ?.ifBlank { null }
}

private fun String?.normalizeBrand(): String? {
    val cleaned = normalizedText() ?: return null
    return when (cleaned.lowercase()) {
        "unknown", "generic" -> null
        else -> cleaned
    }
}

private fun String?.normalizePantryUnit(): String? {
    val normalized = this
        .normalizedText()
        ?.lowercase()
        ?.trim('.', ',', ';', ':')
        ?.ifBlank { null }
        ?: return null

    return when (normalized) {
        "unknown" -> null
        "cans" -> "can"
        "jars" -> "jar"
        "boxes" -> "box"
        "bags" -> "bag"
        "bottles" -> "bottle"
        "containers" -> "container"
        "cups" -> "cup"
        "lbs", "pound", "pounds" -> "lb"
        "ounces", "ounce", "fl oz", "fluid oz", "fluid ounce", "fluid ounces" -> "oz"
        "grams", "gram" -> "g"
        "kilograms", "kilogram" -> "kg"
        "ct", "each", "ea", "item", "items", "counts", "pack", "packs", "package", "packages", "pk" -> "count"
        "milliliters", "milliliter" -> "ml"
        "liters", "liter" -> "l"
        "gallons", "gallon" -> "gal"
        "quarts", "quart" -> "qt"
        "pints", "pint" -> "pt"
        "dozen", "dozens" -> "count"
        else -> normalized
    }
}

private fun String?.normalizeStorageLocation(): String? {
    val normalized = this
        .normalizedText()
        ?.lowercase()
        ?.trim('.', ',', ';', ':')
        ?.ifBlank { null }
        ?: return null

    return when (normalized) {
        "unknown" -> null
        "refrigerator", "refrigerated", "cold storage", "cold" -> "fridge"
        "deep freezer", "deep freeze", "frozen" -> "freezer"
        "cabinet", "cupboard", "shelf", "shelf stable", "shelf-stable", "room temp", "room temperature" -> "pantry"
        else -> normalized
    }
}

private fun buildReviewNotes(
    missingBrand: Boolean,
    missingAmount: Boolean,
    missingLocation: Boolean,
    missingDate: Boolean
): String? {
    return listOfNotNull(
        "Review brand.".takeIf { missingBrand },
        "Review amount/unit.".takeIf { missingAmount },
        "Review pantry/fridge/freezer location.".takeIf { missingLocation },
        "Review expiration or best-by date.".takeIf { missingDate }
    )
        .joinToString(" ")
        .ifBlank { null }
}

private fun String?.toUnparsedDateNote(label: String, parsed: Boolean): String? {
    val value = this?.trim()?.ifBlank { null } ?: return null
    return if (parsed) null else "Could not parse $label: $value"
}

private fun mergeNotes(vararg values: String?): String? {
    return values
        .mapNotNull { it?.trim()?.ifBlank { null } }
        .distinct()
        .joinToString("; ")
        .ifBlank { null }
}
