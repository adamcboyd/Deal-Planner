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
        assertThat(pantryItem.unit).isEqualTo("can")
        assertThat(pantryItem.bestBy).isEqualTo(LocalDate.of(2026, 12, 31))
        assertThat(pantryItem.opened).isEqualTo(LocalDate.of(2026, 7, 1))
        assertThat(pantryItem.location).isEqualTo("pantry")
        assertThat(pantryItem.needsVerify).isFalse()
    }

    @Test
    fun `map vision item normalizes raw unit brand size and storage aliases`() {
        val item = GeminiPantryVisionClient.PantryVisionItem(
            brand = "  Great Value  ",
            product = "  milk  ",
            quantity = 1.0,
            unit = "fluid ounces",
            size = "  64 fl oz  ",
            location = "Cold Storage",
            expirationDate = "12/31/2026",
            openedDate = null,
            confidence = 0.91,
            questions = emptyList()
        )

        val pantryItem = item.toPantryItem(warnings = emptyList())

        assertThat(pantryItem).isNotNull()
        assertThat(pantryItem!!.brand).isEqualTo("Great Value")
        assertThat(pantryItem.item).isEqualTo("milk")
        assertThat(pantryItem.unit).isEqualTo("oz")
        assertThat(pantryItem.size).isEqualTo("64 fl oz")
        assertThat(pantryItem.location).isEqualTo("fridge")
        assertThat(pantryItem.needsVerify).isFalse()
    }

    @Test
    fun `map vision item with unpadded year first dash dates into pantry item`() {
        val item = completeVisionItem().copy(
            expirationDate = "2026-7-1",
            openedDate = "2026-6-30"
        )

        val pantryItem = item.toPantryItem(warnings = emptyList())

        assertThat(pantryItem).isNotNull()
        assertThat(pantryItem!!.bestBy).isEqualTo(LocalDate.of(2026, 7, 1))
        assertThat(pantryItem.opened).isEqualTo(LocalDate.of(2026, 6, 30))
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
        assertThat(pantryItem.notes).contains("Review brand.")
        assertThat(pantryItem.notes).contains("Review amount/unit.")
        assertThat(pantryItem.notes).contains("Review pantry/fridge/freezer location.")
        assertThat(pantryItem.notes).contains("Review expiration or best-by date.")
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
        assertThat(pantryItem.notes).contains("Review amount/unit.")
    }

    @Test
    fun `negative vision quantity defaults to reviewable amount`() {
        val item = GeminiPantryVisionClient.PantryVisionItem(
            brand = "Kroger",
            product = "ground beef",
            quantity = -1.0,
            unit = "lb",
            size = null,
            location = "fridge",
            expirationDate = "2026-12-31",
            openedDate = null,
            confidence = 0.95,
            questions = emptyList()
        )

        val pantryItem = item.toPantryItem(warnings = emptyList())

        assertThat(pantryItem).isNotNull()
        assertThat(pantryItem!!.qty).isEqualTo(1.0)
        assertThat(pantryItem.unit).isEqualTo("lb")
        assertThat(pantryItem.needsVerify).isTrue()
        assertThat(pantryItem.notes).contains("Review amount/unit.")
    }

    @Test
    fun `generic or unknown vision brand requires review`() {
        val genericItem = completeVisionItem().copy(brand = "Generic")
        val unknownItem = completeVisionItem().copy(brand = "unknown")

        val genericPantryItem = genericItem.toPantryItem(warnings = emptyList())
        val unknownPantryItem = unknownItem.toPantryItem(warnings = emptyList())

        assertThat(genericPantryItem).isNotNull()
        assertThat(genericPantryItem!!.brand).isEqualTo("Generic")
        assertThat(genericPantryItem.needsVerify).isTrue()
        assertThat(genericPantryItem.notes).contains("Review brand.")

        assertThat(unknownPantryItem).isNotNull()
        assertThat(unknownPantryItem!!.brand).isEqualTo("Generic")
        assertThat(unknownPantryItem.needsVerify).isTrue()
        assertThat(unknownPantryItem.notes).contains("Review brand.")
    }

    @Test
    fun `missing or unknown vision location requires review`() {
        val missingLocation = completeVisionItem().copy(location = null)
        val unknownLocation = completeVisionItem().copy(location = "unknown")

        val missingLocationPantryItem = missingLocation.toPantryItem(warnings = emptyList())
        val unknownLocationPantryItem = unknownLocation.toPantryItem(warnings = emptyList())

        assertThat(missingLocationPantryItem).isNotNull()
        assertThat(missingLocationPantryItem!!.location).isEqualTo("pantry")
        assertThat(missingLocationPantryItem.needsVerify).isTrue()
        assertThat(missingLocationPantryItem.notes).contains("Review pantry/fridge/freezer location.")

        assertThat(unknownLocationPantryItem).isNotNull()
        assertThat(unknownLocationPantryItem!!.location).isEqualTo("pantry")
        assertThat(unknownLocationPantryItem.needsVerify).isTrue()
        assertThat(unknownLocationPantryItem.notes).contains("Review pantry/fridge/freezer location.")
    }

    private fun completeVisionItem() = GeminiPantryVisionClient.PantryVisionItem(
        brand = "Kroger",
        product = "rolled oats",
        quantity = 1.0,
        unit = "container",
        size = "18 oz",
        location = "pantry",
        expirationDate = "2026-12-31",
        openedDate = null,
        confidence = 0.95,
        questions = emptyList()
    )
}
