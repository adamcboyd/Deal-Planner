package com.dealplanner.ui.state

import com.dealplanner.data.model.PantryItem
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PantryImportReviewQueueTest {

    @Test
    fun `stage appends imported photo items without saving over existing pending review`() {
        val existing = listOf(item("Black Beans"))
        val imported = listOf(item("Peanut Butter", needsVerify = true), item("Eggs", needsVerify = true))

        val result = PantryImportReviewQueue.stage(existing, imported)

        assertThat(result.items.map { it.item })
            .containsExactly("Black Beans", "Peanut Butter", "Eggs")
            .inOrder()
        assertThat(result.status)
            .isEqualTo("Review 2 photo items before saving; VERIFY checks included; 3 total pending.")
    }

    @Test
    fun `updateAt replaces only the selected pending pantry item`() {
        val items = listOf(item("Black Beans"), item("Eggs"))
        val updated = item("Large Eggs")

        val result = PantryImportReviewQueue.updateAt(items, 1, updated)

        assertThat(result.map { it.item }).containsExactly("Black Beans", "Large Eggs").inOrder()
    }

    @Test
    fun `removeAt deletes only the selected pending pantry item`() {
        val items = listOf(item("Black Beans"), item("Eggs"), item("Peanut Butter"))

        val result = PantryImportReviewQueue.removeAt(items, 1)

        assertThat(result.map { it.item }).containsExactly("Black Beans", "Peanut Butter").inOrder()
    }

    @Test
    fun `invalid update and remove indexes leave pending review unchanged`() {
        val items = listOf(item("Black Beans"), item("Eggs"))

        assertThat(PantryImportReviewQueue.updateAt(items, -1, item("Rice"))).isEqualTo(items)
        assertThat(PantryImportReviewQueue.removeAt(items, 2)).isEqualTo(items)
    }

    @Test
    fun `savedStatus reports reviewed count and duplicate merges`() {
        assertThat(PantryImportReviewQueue.savedStatus(savedCount = 1, updatedCount = 0))
            .isEqualTo("Saved 1 reviewed pantry item.")
        assertThat(PantryImportReviewQueue.savedStatus(savedCount = 3, updatedCount = 1))
            .isEqualTo("Saved 3 reviewed pantry items (1 merged).")
    }

    private fun item(name: String, needsVerify: Boolean = false): PantryItem {
        return PantryItem(
            item = name,
            qty = 1.0,
            needsVerify = needsVerify
        )
    }
}
