package com.dealplanner.domain

import com.dealplanner.data.model.ReceiptItem

class ReceiptAdjustmentCalculator {
    fun budgetDeltaForUpdate(oldReceipt: ReceiptItem, newReceipt: ReceiptItem): Double {
        return newReceipt.totalCost - oldReceipt.totalCost
    }

    fun budgetDeltaForDelete(receipt: ReceiptItem): Double {
        return -receipt.totalCost
    }

    fun pantryDeltasForUpdate(
        oldReceipt: ReceiptItem,
        newReceipt: ReceiptItem
    ): List<PantryReceiptQuantityDelta> {
        return combine(
            listOfNotNull(
                oldReceipt.toPantryDelta(direction = -1.0),
                newReceipt.toPantryDelta(direction = 1.0)
            )
        )
    }

    fun pantryDeltasForDelete(receipt: ReceiptItem): List<PantryReceiptQuantityDelta> {
        return listOfNotNull(receipt.toPantryDelta(direction = -1.0))
    }

    private fun ReceiptItem.toPantryDelta(direction: Double): PantryReceiptQuantityDelta? {
        if (matchedType != "pantry") return null
        val pantryItemId = matchedItemId ?: return null
        val receiptQty = qty ?: return null
        return PantryReceiptQuantityDelta(
            pantryItemId = pantryItemId,
            quantityDelta = receiptQty * direction
        )
    }

    private fun combine(deltas: List<PantryReceiptQuantityDelta>): List<PantryReceiptQuantityDelta> {
        return deltas
            .groupBy { it.pantryItemId }
            .mapNotNull { (pantryItemId, groupedDeltas) ->
                val totalDelta = groupedDeltas.sumOf { it.quantityDelta }
                if (totalDelta == 0.0) {
                    null
                } else {
                    PantryReceiptQuantityDelta(
                        pantryItemId = pantryItemId,
                        quantityDelta = totalDelta
                    )
                }
            }
    }
}

data class PantryReceiptQuantityDelta(
    val pantryItemId: Long,
    val quantityDelta: Double
)
