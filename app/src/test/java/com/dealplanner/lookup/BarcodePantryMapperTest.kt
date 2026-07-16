package com.dealplanner.lookup

import com.dealplanner.lookup.OpenFoodFactsBarcodeClient.BarcodeLookupResult
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BarcodePantryMapperTest {

    @Test
    fun `found product maps to reviewable pantry item with lookup notes`() {
        val result = BarcodeLookupResult.Found(
            OpenFoodFactsBarcodeClient.BarcodeProduct(
                barcode = "012345678905",
                name = "Great Value Black Beans",
                brand = "Great Value",
                quantity = "15 oz",
                categoryTags = listOf("en:canned-foods")
            )
        )

        val item = BarcodePantryMapper.toPantryItem(result, "fallback")

        assertThat(item.item).isEqualTo("Great Value Black Beans")
        assertThat(item.brand).isEqualTo("Great Value")
        assertThat(item.size).isEqualTo("15 oz")
        assertThat(item.unit).isEqualTo("count")
        assertThat(item.location).isEqualTo("pantry")
        assertThat(item.needsVerify).isTrue()
        assertThat(item.notes).contains("Barcode: 012345678905")
        assertThat(item.notes).contains("Product lookup: Open Food Facts")
        assertThat(item.notes).contains("Review quantity, location, and expiration.")
    }

    @Test
    fun `not found product maps to barcode fallback item`() {
        val item = BarcodePantryMapper.toPantryItem(
            BarcodeLookupResult.NotFound,
            "012345678905"
        )

        assertThat(item.item).isEqualTo("Scanned barcode item")
        assertThat(item.unit).isEqualTo("count")
        assertThat(item.location).isEqualTo("pantry")
        assertThat(item.needsVerify).isTrue()
        assertThat(item.notes).contains("Barcode: 012345678905")
        assertThat(item.notes).contains("Product lookup did not find this code.")
        assertThat(item.notes).contains("Review item name, brand, size, and expiration.")
    }

    @Test
    fun `lookup error maps to barcode fallback item without leaking raw result object`() {
        val item = BarcodePantryMapper.toPantryItem(
            BarcodeLookupResult.Error("timeout"),
            "012345678905"
        )

        assertThat(item.item).isEqualTo("Scanned barcode item")
        assertThat(item.needsVerify).isTrue()
        assertThat(item.notes).contains("Barcode: 012345678905")
        assertThat(item.notes).contains("Product lookup unavailable. timeout")
        assertThat(item.notes).doesNotContain("BarcodeLookupResult")
    }

    @Test
    fun `status messages preserve phone visible add and update wording`() {
        val found = BarcodeLookupResult.Found(
            OpenFoodFactsBarcodeClient.BarcodeProduct(
                barcode = "012345678905",
                name = "Great Value Black Beans",
                brand = null,
                quantity = null,
                categoryTags = emptyList()
            )
        )

        assertThat(BarcodePantryMapper.statusMessage(found, mergedExisting = false))
            .isEqualTo("Added Great Value Black Beans from barcode lookup with VERIFY checks")
        assertThat(BarcodePantryMapper.statusMessage(found, mergedExisting = true))
            .isEqualTo("Updated Great Value Black Beans from barcode lookup with VERIFY checks")
        assertThat(BarcodePantryMapper.statusMessage(BarcodeLookupResult.NotFound, mergedExisting = false))
            .isEqualTo("Added barcode item with VERIFY checks; no product lookup match found.")
        assertThat(BarcodePantryMapper.statusMessage(BarcodeLookupResult.Error("offline"), mergedExisting = true))
            .isEqualTo("Updated barcode item with VERIFY checks; product lookup unavailable.")
    }
}
