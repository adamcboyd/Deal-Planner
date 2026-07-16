package com.dealplanner.lookup

import com.dealplanner.lookup.OpenFoodFactsBarcodeClient.BarcodeLookupResult
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OpenFoodFactsBarcodeClientTest {

    private val client = OpenFoodFactsBarcodeClient()

    @Test
    fun `normalize barcode removes separators and preserves leading zero`() {
        val normalized = client.normalizeBarcode("  0 12345-67890 5 ")

        assertThat(normalized).isEqualTo("012345678905")
    }

    @Test
    fun `normalize barcode extracts UPC from pasted label text`() {
        val normalized = client.normalizeBarcode("UPC: 0 12345-67890 5")

        assertThat(normalized).isEqualTo("012345678905")
    }

    @Test
    fun `normalize barcode prefers labeled UPC over earlier item numbers`() {
        val normalized = client.normalizeBarcode("Item #12345678 UPC: 0 12345-67890 5 Best By 12/31/2026")

        assertThat(normalized).isEqualTo("012345678905")
    }

    @Test
    fun `normalize barcode accepts bare product code near label dates`() {
        assertThat(client.normalizeBarcode("012345678905 Best By 20261231")).isEqualTo("012345678905")
        assertThat(client.normalizeBarcode("Product 0 12345-67890 5 EXP 12-31-2026")).isEqualTo("012345678905")
    }

    @Test
    fun `normalize barcode skips item number and accepts later bare product code`() {
        val normalized = client.normalizeBarcode("Item #12345678 Product 0 12345-67890 5")

        assertThat(normalized).isEqualTo("012345678905")
    }

    @Test
    fun `normalize barcode rejects label dates without barcode label`() {
        assertThat(client.normalizeBarcode("Best By 20261231")).isEmpty()
        assertThat(client.normalizeBarcode("Use By 12-31-2026")).isEmpty()
        assertThat(client.normalizeBarcode("EXP 31-12-2026")).isEmpty()
    }

    @Test
    fun `normalize barcode rejects item and lot numbers without barcode label`() {
        assertThat(client.normalizeBarcode("Item #12345678")).isEmpty()
        assertThat(client.normalizeBarcode("LOT 12345678")).isEmpty()
        assertThat(client.normalizeBarcode("SKU 123456789012")).isEmpty()
    }

    @Test
    fun `normalize barcode returns blank for text without product code`() {
        val normalized = client.normalizeBarcode("not a product barcode")

        assertThat(normalized).isEmpty()
    }

    @Test
    fun `parse found product response`() {
        val response = """
            {
              "code": "3017624010701",
              "status": "success",
              "result": {
                "id": "product_found"
              },
              "product": {
                "code": "3017624010701",
                "product_name": "Nutella",
                "product_name_en": "Nutella Hazelnut Spread",
                "generic_name": "Chocolate spread",
                "brands": "Ferrero, Nutella",
                "quantity": "400.0 g",
                "categories_tags": ["en:breakfasts", "en:spreads"]
              }
            }
        """.trimIndent()

        val result = client.parseProductResponse(response, "fallback")

        assertThat(result).isInstanceOf(BarcodeLookupResult.Found::class.java)
        val product = (result as BarcodeLookupResult.Found).product
        assertThat(product.barcode).isEqualTo("3017624010701")
        assertThat(product.name).isEqualTo("Nutella Hazelnut Spread")
        assertThat(product.brand).isEqualTo("Ferrero")
        assertThat(product.quantity).isEqualTo("400 g")
        assertThat(product.categoryTags).containsExactly("en:breakfasts", "en:spreads").inOrder()
    }

    @Test
    fun `parse found product response falls back to product name`() {
        val response = """
            {
              "code": "012345678905",
              "status": "success",
              "result": {
                "id": "product_found"
              },
              "product": {
                "product_name": "Black Beans",
                "brands": "",
                "quantity": "15 oz"
              }
            }
        """.trimIndent()

        val result = client.parseProductResponse(response, "012345678905")

        assertThat(result).isInstanceOf(BarcodeLookupResult.Found::class.java)
        val product = (result as BarcodeLookupResult.Found).product
        assertThat(product.barcode).isEqualTo("012345678905")
        assertThat(product.name).isEqualTo("Black Beans")
        assertThat(product.brand).isNull()
        assertThat(product.quantity).isEqualTo("15 oz")
    }

    @Test
    fun `parse product not found response`() {
        val response = """
            {
              "code": "0000000000000",
              "status": "success",
              "result": {
                "id": "product_not_found"
              },
              "product": {}
            }
        """.trimIndent()

        val result = client.parseProductResponse(response, "0000000000000")

        assertThat(result).isEqualTo(BarcodeLookupResult.NotFound)
    }

    @Test
    fun `parse malformed product response returns error`() {
        val result = client.parseProductResponse("not json", "012345678905")

        assertThat(result).isInstanceOf(BarcodeLookupResult.Error::class.java)
    }
}
