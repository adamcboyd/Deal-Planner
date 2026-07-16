package com.dealplanner.ai

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class PantryVisionItemMapperTest {

    @Test
    fun `map vision item with flexible dates into pantry item`() {
        val item = GeminiPantryVisionClient.PantryVisionItem(
            brand = "Kroger",
            product = "black beans",
            quantity = 2.0,
            unit = "cans",
            size = "15 oz",
            location = "Pantry",
            expirationDate = "12/31/2026",
            openedDate = "2026/7/1",
            confidence = 0.92,
            questions = emptyList()
        )

        val pantryItem = item.toPantryItem(warnings = emptyList())

        assertThat(pantryItem).isNotNull()
        assertThat(pantryItem!!.item).isEqualTo("black beans")
        assertThat(pantryItem.qty).isEqualTo(2.0)
        assertThat(pantryItem.unit).isEqualTo("cans")
        assertThat(pantryItem.bestBy).isEqualTo(LocalDate.of(2026, 12, 31))
        assertThat(pantryItem.opened).isEqualTo(LocalDate.of(2026, 7, 1))
        assertThat(pantryItem.needsVerify).isFalse()
    }

    @Test
    fun `unparseable vision dates stay visible and require review`() {
        val item = GeminiPantryVisionClient.PantryVisionItem(
            brand = "Generic",
            product = "rolled oats",
            quantity = 1.0,
            unit = "container",
            size = null,
            location = "pantry",
            expirationDate = "best before soon",
            openedDate = "last week",
            confidence = 0.95,
            questions = emptyList()
        )

        val pantryItem = item.toPantryItem(warnings = listOf("date was blurry"))

        assertThat(pantryItem).isNotNull()
        assertThat(pantryItem!!.bestBy).isNull()
        assertThat(pantryItem.opened).isNull()
        assertThat(pantryItem.needsVerify).isTrue()
        assertThat(pantryItem.notes).contains("Could not parse best-by date: best before soon")
        assertThat(pantryItem.notes).contains("Could not parse opened date: last week")
        assertThat(pantryItem.notes).contains("date was blurry")
    }

    @Test
    fun `missing brand amount or best by date requires review`() {
        val item = GeminiPantryVisionClient.PantryVisionItem(
            brand = null,
            product = "rice",
            quantity = null,
            unit = null,
            size = null,
            location = null,
            expirationDate = null,
            openedDate = null,
            confidence = 0.99,
            questions = emptyList()
        )

        val pantryItem = item.toPantryItem(warnings = emptyList())

        assertThat(pantryItem).isNotNull()
        assertThat(pantryItem!!.brand).isEqualTo("Generic")
        assertThat(pantryItem.qty).isEqualTo(1.0)
        assertThat(pantryItem.location).isEqualTo("pantry")
        assertThat(pantryItem.needsVerify).isTrue()
    }

    @Test
    fun `unknown amount unit requires review`() {
        val item = GeminiPantryVisionClient.PantryVisionItem(
            brand = "Kroger",
            product = "rolled oats",
            quantity = 1.0,
            unit = "unknown",
            size = null,
            location = "pantry",
            expirationDate = "2026-12-31",
            openedDate = null,
            confidence = 0.95,
            questions = emptyList()
        )

        val pantryItem = item.toPantryItem(warnings = emptyList())

        assertThat(pantryItem).isNotNull()
        assertThat(pantryItem!!.unit).isNull()
        assertThat(pantryItem.needsVerify).isTrue()
    }
}
