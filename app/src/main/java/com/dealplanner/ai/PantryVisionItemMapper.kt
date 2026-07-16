package com.dealplanner.ai

import com.dealplanner.data.model.PantryItem
import com.dealplanner.util.toFlexibleLocalDateOrNull

fun GeminiPantryVisionClient.PantryVisionItem.toPantryItem(warnings: List<String>): PantryItem? {
    val productName = product?.trim()?.ifBlank { null } ?: return null
    val parsedOpened = openedDate.toParsedDate()
    val parsedBestBy = expirationDate.toParsedDate()
    val unparsedOpenedNote = openedDate.toUnparsedDateNote("opened date", parsedOpened != null)
    val unparsedBestByNote = expirationDate.toUnparsedDateNote("best-by date", parsedBestBy != null)
    val questionNotes = questions.joinToString(" ")
    val warningNotes = warnings.joinToString(" ")
    val missingBrand = brand.isMissingBrand()
    val missingAmount = quantity == null || unit.isNullOrBlank() || unit.equals("unknown", ignoreCase = true)
    val missingLocation = location.isMissingLocation()
    val missingDate = parsedBestBy == null
    val reviewNotes = buildReviewNotes(
        missingBrand = missingBrand,
        missingAmount = missingAmount,
        missingLocation = missingLocation,
        missingDate = missingDate
    )

    return PantryItem(
        item = productName,
        qty = quantity ?: 1.0,
        unit = unit?.takeUnless { it.equals("unknown", ignoreCase = true) },
        size = size,
        brand = brand?.takeUnless { it.equals("unknown", ignoreCase = true) } ?: "Generic",
        location = location?.takeUnless { it.equals("unknown", ignoreCase = true) } ?: "pantry",
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

private fun String?.isMissingBrand(): Boolean {
    val normalized = this?.trim()?.lowercase()?.ifBlank { null } ?: return true
    return normalized == "unknown" || normalized == "generic"
}

private fun String?.isMissingLocation(): Boolean {
    val normalized = this?.trim()?.lowercase()?.ifBlank { null } ?: return true
    return normalized == "unknown"
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
