package com.dealplanner.domain

import com.dealplanner.data.model.ReceiptItem
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReceiptAdjustmentCalculatorTest {
    private val calculator = ReceiptAdjustmentCalculator()

    @Test
    fun `budget update delta is new total minus old total`() {
        val oldReceipt = receipt(totalCost = 3.25)
        val newReceipt = oldReceipt.copy(totalCost = 4.75)

        assertThat(calculator.budgetDeltaForUpdate(oldReceipt, newReceipt)).isEqualTo(1.5)
    }

    @Test
    fun `budget delete delta removes the receipt total`() {
        assertThat(calculator.budgetDeltaForDelete(receipt(totalCost = 3.25))).isEqualTo(-3.25)
    }

    @Test
    fun `same pantry match update returns net quantity delta`() {
        val oldReceipt = receipt(pantryItemId = 7, qty = 2.0)
        val newReceipt = oldReceipt.copy(qty = 3.5)

        assertThat(calculator.pantryDeltasForUpdate(oldReceipt, newReceipt))
            .containsExactly(PantryReceiptQuantityDelta(pantryItemId = 7, quantityDelta = 1.5))
    }

    @Test
    fun `changed pantry match reverses old pantry item and applies new pantry item`() {
        val oldReceipt = receipt(pantryItemId = 7, qty = 2.0)
        val newReceipt = receipt(pantryItemId = 9, qty = 1.0)

        assertThat(calculator.pantryDeltasForUpdate(oldReceipt, newReceipt))
            .containsExactly(
                PantryReceiptQuantityDelta(pantryItemId = 7, quantityDelta = -2.0),
                PantryReceiptQuantityDelta(pantryItemId = 9, quantityDelta = 1.0)
            )
    }

    @Test
    fun `pantry match removed by edit reverses the old pantry item`() {
        val oldReceipt = receipt(pantryItemId = 7, qty = 2.0)
        val newReceipt = oldReceipt.copy(matchedType = "deal")

        assertThat(calculator.pantryDeltasForUpdate(oldReceipt, newReceipt))
            .containsExactly(PantryReceiptQuantityDelta(pantryItemId = 7, quantityDelta = -2.0))
    }

    @Test
    fun `pantry match added by edit applies the new pantry item`() {
        val oldReceipt = receipt(matchedType = "deal", matchedItemId = 12, qty = 2.0)
        val newReceipt = receipt(pantryItemId = 7, qty = 2.0)

        assertThat(calculator.pantryDeltasForUpdate(oldReceipt, newReceipt))
            .containsExactly(PantryReceiptQuantityDelta(pantryItemId = 7, quantityDelta = 2.0))
    }

    @Test
    fun `delete reverses pantry matched quantity`() {
        assertThat(calculator.pantryDeltasForDelete(receipt(pantryItemId = 7, qty = 2.0)))
            .containsExactly(PantryReceiptQuantityDelta(pantryItemId = 7, quantityDelta = -2.0))
    }

    @Test
    fun `pantry deltas ignore non pantry matches missing ids and missing quantities`() {
        val oldReceipt = receipt(matchedType = "deal", matchedItemId = 7, qty = 2.0)
        val newReceipt = receipt(pantryItemId = null, qty = null)

        assertThat(calculator.pantryDeltasForUpdate(oldReceipt, newReceipt)).isEmpty()
        assertThat(calculator.pantryDeltasForDelete(oldReceipt)).isEmpty()
        assertThat(calculator.pantryDeltasForDelete(newReceipt)).isEmpty()
    }

    private fun receipt(
        totalCost: Double = 1.0,
        pantryItemId: Long? = null,
        qty: Double? = null,
        matchedType: String = if (pantryItemId != null) "pantry" else "unmatched",
        matchedItemId: Long? = pantryItemId
    ): ReceiptItem {
        return ReceiptItem(
            id = 1,
            rawLine = "BLACK BEANS $1.00",
            matchedItemId = matchedItemId,
            matchedType = matchedType,
            qty = qty,
            totalCost = totalCost
        )
    }
}
