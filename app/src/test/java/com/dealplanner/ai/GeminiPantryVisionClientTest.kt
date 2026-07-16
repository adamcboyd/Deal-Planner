package com.dealplanner.ai

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GeminiPantryVisionClientTest {

    @Test
    fun `blank key is not configured`() {
        val client = GeminiPantryVisionClient(apiKey = "", model = "gemini-3.5-flash")

        assertThat(client.isConfigured()).isFalse()
    }

    @Test
    fun `placeholder key is not configured`() {
        val client = GeminiPantryVisionClient(apiKey = "YOUR_GEMINI_API_KEY", model = "gemini-3.5-flash")

        assertThat(client.isConfigured()).isFalse()
    }

    @Test
    fun `real-looking key is configured`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "gemini-3.5-flash")

        assertThat(client.isConfigured()).isTrue()
    }

    @Test
    fun `blank model falls back to default model name`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "")

        assertThat(client.modelName).isEqualTo("gemini-3.5-flash")
    }

    @Test
    fun `model name is trimmed and accepts models prefix`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = " models/gemini-3.5-flash ")

        assertThat(client.modelName).isEqualTo("gemini-3.5-flash")
    }

    @Test
    fun `whitespace around real-looking key is configured`() {
        val client = GeminiPantryVisionClient(apiKey = " test-real-key-for-unit-tests ", model = "gemini-3.5-flash")

        assertThat(client.isConfigured()).isTrue()
    }

    @Test
    fun `connection test reports missing key without network`() = runTest {
        val client = GeminiPantryVisionClient(apiKey = "", model = "gemini-3.5-flash")

        val result = client.testConnection()

        assertThat(result.success).isFalse()
        assertThat(result.message).contains("not configured")
    }

    @Test
    fun `parse pantry vision response from fenced json`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "gemini-3.5-flash")
        val response = """
            ```JSON
            {
              "items": [
                {
                  "brand": "Great Value",
                  "product": "black beans",
                  "quantity": 2,
                  "unit": "can",
                  "size": "15 oz",
                  "location": "pantry",
                  "expirationDate": "2026-12-31",
                  "openedDate": null,
                  "confidence": 0.91,
                  "questions": []
                }
              ],
              "warnings": ["label partially visible"]
            }
            ```
        """.trimIndent()

        val result = client.parseVisionResult(response)

        assertThat(result.items).hasSize(1)
        val item = result.items.first()
        assertThat(item.brand).isEqualTo("Great Value")
        assertThat(item.product).isEqualTo("black beans")
        assertThat(item.quantity).isEqualTo(2.0)
        assertThat(item.unit).isEqualTo("can")
        assertThat(item.size).isEqualTo("15 oz")
        assertThat(item.location).isEqualTo("pantry")
        assertThat(item.expirationDate).isEqualTo("2026-12-31")
        assertThat(item.confidence).isEqualTo(0.91)
        assertThat(result.warnings).containsExactly("label partially visible")
    }

    @Test
    fun `parse pantry vision response with commentary and scalar fields`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "gemini-3.5-flash")
        val response = """
            Here is the JSON:
            {
              "items": [
                {
                  "item": "rolled oats",
                  "quantity": "1.5",
                  "unit": "container",
                  "confidence": 1.4,
                  "questions": "What is the expiration date?"
                },
                "not an object"
              ],
              "warnings": "amount estimated"
            }
        """.trimIndent()

        val result = client.parseVisionResult(response)

        assertThat(result.items).hasSize(1)
        val item = result.items.first()
        assertThat(item.product).isEqualTo("rolled oats")
        assertThat(item.quantity).isEqualTo(1.5)
        assertThat(item.confidence).isEqualTo(1.0)
        assertThat(item.questions).containsExactly("What is the expiration date?")
        assertThat(result.warnings).containsExactly("amount estimated")
    }

    @Test
    fun `parse pantry vision response with snake case date fields`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "gemini-3.5-flash")
        val response = """
            {
              "items": [
                {
                  "product_name": "peanut butter",
                  "brand": "Great Value",
                  "quantity": 1,
                  "unit": "jar",
                  "expiration_date": "2026-12-31",
                  "opened_date": "2026-07-16",
                  "confidence": 0.88
                },
                {
                  "name": "rolled oats",
                  "best_by_date": "2027-01-15",
                  "openedDate": null,
                  "confidence": 0.77
                }
              ],
              "warnings": []
            }
        """.trimIndent()

        val result = client.parseVisionResult(response)

        assertThat(result.items).hasSize(2)

        val peanutButter = result.items.first { it.product == "peanut butter" }
        assertThat(peanutButter.brand).isEqualTo("Great Value")
        assertThat(peanutButter.expirationDate).isEqualTo("2026-12-31")
        assertThat(peanutButter.openedDate).isEqualTo("2026-07-16")

        val oats = result.items.first { it.product == "rolled oats" }
        assertThat(oats.expirationDate).isEqualTo("2027-01-15")
    }

    @Test
    fun `parse pantry vision response with alternate item wrappers`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "gemini-3.5-flash")
        val wrappedResponse = """
            {
              "pantry_items": [
                {
                  "product": "brown rice",
                  "quantity": 1,
                  "unit": "bag"
                }
              ],
              "warnings": "used pantry_items key"
            }
        """.trimIndent()
        val arrayResponse = """
            [
              {
                "product": "olive oil",
                "quantity": 1,
                "unit": "bottle"
              }
            ]
        """.trimIndent()

        val wrappedResult = client.parseVisionResult(wrappedResponse)
        val arrayResult = client.parseVisionResult(arrayResponse)

        assertThat(wrappedResult.items).hasSize(1)
        assertThat(wrappedResult.items.first().product).isEqualTo("brown rice")
        assertThat(wrappedResult.warnings).containsExactly("used pantry_items key")

        assertThat(arrayResult.items).hasSize(1)
        assertThat(arrayResult.items.first().product).isEqualTo("olive oil")
        assertThat(arrayResult.warnings).isEmpty()
    }

    @Test
    fun `parse pantry vision response with single item object shapes`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "gemini-3.5-flash")
        val rootItemResponse = """
            {
              "product": "canned corn",
              "quantity": 2,
              "unit": "cans",
              "warnings": ["single object response"]
            }
        """.trimIndent()
        val singularWrappedResponse = """
            {
              "pantry_item": {
                "name": "frozen peas",
                "amount": "1 bag",
                "storage": "freezer"
              },
              "warnings": "singular wrapper response"
            }
        """.trimIndent()

        val rootItemResult = client.parseVisionResult(rootItemResponse)
        val singularWrappedResult = client.parseVisionResult(singularWrappedResponse)

        assertThat(rootItemResult.items).hasSize(1)
        assertThat(rootItemResult.items.first().product).isEqualTo("canned corn")
        assertThat(rootItemResult.items.first().unit).isEqualTo("can")
        assertThat(rootItemResult.warnings).containsExactly("single object response")

        assertThat(singularWrappedResult.items).hasSize(1)
        assertThat(singularWrappedResult.items.first().product).isEqualTo("frozen peas")
        assertThat(singularWrappedResult.items.first().quantity).isEqualTo(1.0)
        assertThat(singularWrappedResult.items.first().unit).isEqualTo("bag")
        assertThat(singularWrappedResult.items.first().location).isEqualTo("freezer")
        assertThat(singularWrappedResult.warnings).containsExactly("singular wrapper response")
    }

    @Test
    fun `parse pantry vision response with plural item wrapper holding one object`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "gemini-3.5-flash")
        val response = """
            {
              "items": {
                "product": "brown rice",
                "quantity": 1,
                "unit": "bags"
              },
              "warnings": "items object response"
            }
        """.trimIndent()

        val result = client.parseVisionResult(response)

        assertThat(result.items).hasSize(1)
        assertThat(result.items.first().product).isEqualTo("brown rice")
        assertThat(result.items.first().quantity).isEqualTo(1.0)
        assertThat(result.items.first().unit).isEqualTo("bag")
        assertThat(result.warnings).containsExactly("items object response")
    }

    @Test
    fun `parse pantry vision response with quantity and storage aliases`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "gemini-3.5-flash")
        val response = """
            {
              "items": [
                {
                  "brand_name": "Kroger",
                  "food_name": "black beans",
                  "amount": "2 cans",
                  "package_size": "15 oz",
                  "storage_location": "pantry",
                  "best_before_date": "2027-02-03",
                  "opened_on": "2026-07-16",
                  "confidence": "0.82"
                }
              ],
              "warnings": []
            }
        """.trimIndent()

        val result = client.parseVisionResult(response)

        assertThat(result.items).hasSize(1)
        val item = result.items.first()
        assertThat(item.brand).isEqualTo("Kroger")
        assertThat(item.product).isEqualTo("black beans")
        assertThat(item.quantity).isEqualTo(2.0)
        assertThat(item.unit).isEqualTo("can")
        assertThat(item.size).isEqualTo("15 oz")
        assertThat(item.location).isEqualTo("pantry")
        assertThat(item.expirationDate).isEqualTo("2027-02-03")
        assertThat(item.openedDate).isEqualTo("2026-07-16")
        assertThat(item.confidence).isEqualTo(0.82)
    }

    @Test
    fun `parse pantry vision response with object quantity fields`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "gemini-3.5-flash")
        val response = """
            {
              "items": [
                {
                  "product": "black beans",
                  "quantity": {
                    "value": "2",
                    "unit": "cans"
                  },
                  "confidence": 0.9
                },
                {
                  "product": "ground turkey",
                  "amount": {
                    "amount": "1,5",
                    "units": "pounds"
                  },
                  "storage": "refrigerator"
                }
              ],
              "warnings": []
            }
        """.trimIndent()

        val result = client.parseVisionResult(response)

        assertThat(result.items).hasSize(2)

        val beans = result.items.first { it.product == "black beans" }
        assertThat(beans.quantity).isEqualTo(2.0)
        assertThat(beans.unit).isEqualTo("can")

        val turkey = result.items.first { it.product == "ground turkey" }
        assertThat(turkey.quantity).isEqualTo(1.5)
        assertThat(turkey.unit).isEqualTo("lb")
        assertThat(turkey.location).isEqualTo("fridge")
    }

    @Test
    fun `parse pantry vision response with word quantity amount`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "gemini-3.5-flash")
        val response = """
            {
              "items": [
                {
                  "product": "tuna",
                  "amount": "two cans",
                  "location": "pantry"
                }
              ],
              "warnings": []
            }
        """.trimIndent()

        val result = client.parseVisionResult(response)

        assertThat(result.items).hasSize(1)
        val item = result.items.first()
        assertThat(item.product).isEqualTo("tuna")
        assertThat(item.quantity).isEqualTo(2.0)
        assertThat(item.unit).isEqualTo("can")
    }

    @Test
    fun `parse pantry vision response with comma decimal quantity and confidence`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "gemini-3.5-flash")
        val response = """
            {
              "items": [
                {
                  "product": "ground beef",
                  "amount": "1,5 lb",
                  "confidence": "0,82"
                }
              ],
              "warnings": []
            }
        """.trimIndent()

        val result = client.parseVisionResult(response)

        assertThat(result.items).hasSize(1)
        val item = result.items.first()
        assertThat(item.product).isEqualTo("ground beef")
        assertThat(item.quantity).isEqualTo(1.5)
        assertThat(item.unit).isEqualTo("lb")
        assertThat(item.confidence).isEqualTo(0.82)
    }

    @Test
    fun `parse pantry vision response skips malformed string fields`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "gemini-3.5-flash")
        val response = """
            {
              "items": [
                {
                  "brand": {"name": "bad shape"},
                  "product": {"name": "not a string"},
                  "quantity": 1,
                  "unit": "box",
                  "confidence": 0.8,
                  "questions": ["Keep this question", {"bad": true}, null, 7]
                },
                {
                  "brand": ["bad shape"],
                  "product": "corn flakes",
                  "quantity": 1,
                  "unit": "box",
                  "confidence": 0.8,
                  "questions": ["What is the expiration date?", {"bad": true}]
                }
              ],
              "warnings": ["label glare", {"bad": true}, null, 3]
            }
        """.trimIndent()

        val result = client.parseVisionResult(response)

        assertThat(result.items).hasSize(1)
        val item = result.items.first()
        assertThat(item.product).isEqualTo("corn flakes")
        assertThat(item.brand).isNull()
        assertThat(item.questions).containsExactly("What is the expiration date?")
        assertThat(result.warnings).containsExactly("label glare", "3")
    }
}
