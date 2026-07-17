package com.dealplanner.ui.state

import com.dealplanner.data.model.PantryItem

object PantryImportReviewQueue {
    data class StageResult(
        val items: List<PantryItem>,
        val status: String
    )

    fun stage(
        existingItems: List<PantryItem>,
        importedItems: List<PantryItem>
    ): StageResult {
        val combined = existingItems + importedItems
        return StageResult(
            items = combined,
            status = stagedStatus(
                importedCount = importedItems.size,
                totalPending = combined.size,
                hasVerifyItems = importedItems.any { it.needsVerify }
            )
        )
    }

    fun updateAt(
        items: List<PantryItem>,
        index: Int,
        updatedItem: PantryItem
    ): List<PantryItem> {
        if (index !in items.indices) return items
        return items.mapIndexed { itemIndex, item ->
            if (itemIndex == index) updatedItem else item
        }
    }

    fun removeAt(items: List<PantryItem>, index: Int): List<PantryItem> {
        if (index !in items.indices) return items
        return items.filterIndexed { itemIndex, _ -> itemIndex != index }
    }

    fun savedStatus(savedCount: Int, updatedCount: Int): String {
        return buildString {
            append("Saved $savedCount reviewed pantry item")
            if (savedCount != 1) append("s")
            if (updatedCount > 0) {
                append(" ($updatedCount merged)")
            }
            append(".")
        }
    }

    private fun stagedStatus(
        importedCount: Int,
        totalPending: Int,
        hasVerifyItems: Boolean
    ): String {
        if (importedCount <= 0) {
            return "No pantry photo items found to review."
        }

        return buildString {
            append("Review $importedCount photo item")
            if (importedCount != 1) append("s")
            append(" before saving")
            if (hasVerifyItems) append("; VERIFY checks included")
            if (totalPending > importedCount) append("; $totalPending total pending")
            append(".")
        }
    }
}
