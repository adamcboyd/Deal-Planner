package com.dealplanner.lookup

import com.dealplanner.data.model.PantryItem
import com.dealplanner.lookup.OpenFoodFactsBarcodeClient.BarcodeLookupResult

object BarcodePantryMapper {
    fun toPantryItem(lookupResult: BarcodeLookupResult, barcode: String): PantryItem {
        return when (lookupResult) {
            is BarcodeLookupResult.Found -> PantryItem(
                item = lookupResult.product.name,
                qty = 1.0,
                unit = "count",
                size = lookupResult.product.quantity,
                brand = lookupResult.product.brand,
                location = "pantry",
                notes = mergeNotes(
                    "Barcode: ${lookupResult.product.barcode}",
                    "Product lookup: Open Food Facts",
                    "Review quantity, location, and expiration."
                ),
                needsVerify = true
            )
            BarcodeLookupResult.NotFound -> fallbackBarcodePantryItem(
                barcode = barcode,
                lookupNote = "Product lookup did not find this code."
            )
            is BarcodeLookupResult.Error -> fallbackBarcodePantryItem(
                barcode = barcode,
                lookupNote = "Product lookup unavailable. ${lookupResult.message.trim()}".trim()
            )
        }
    }

    fun statusMessage(
        lookupResult: BarcodeLookupResult,
        mergedExisting: Boolean
    ): String {
        return when (lookupResult) {
            is BarcodeLookupResult.Found -> {
                val action = if (mergedExisting) "Updated" else "Added"
                "$action ${lookupResult.product.name} from barcode lookup with VERIFY checks"
            }
            BarcodeLookupResult.NotFound -> {
                val action = if (mergedExisting) "Updated" else "Added"
                "$action barcode item with VERIFY checks; no product lookup match found."
            }
            is BarcodeLookupResult.Error -> {
                val action = if (mergedExisting) "Updated" else "Added"
                "$action barcode item with VERIFY checks; product lookup unavailable."
            }
        }
    }

    private fun fallbackBarcodePantryItem(barcode: String, lookupNote: String): PantryItem {
        return PantryItem(
            item = "Scanned barcode item",
            qty = 1.0,
            unit = "count",
            location = "pantry",
            notes = mergeNotes(
                "Barcode: $barcode",
                lookupNote,
                "Review item name, brand, size, and expiration."
            ),
            needsVerify = true
        )
    }

    private fun mergeNotes(vararg values: String?): String? {
        return values
            .mapNotNull { it?.trim()?.ifBlank { null } }
            .distinct()
            .joinToString("; ")
            .ifBlank { null }
    }
}
