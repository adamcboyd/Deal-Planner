package com.dealplanner.ai

import com.google.common.truth.Truth.assertThat
import java.io.IOException
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
    fun `connection test reports success from api response without network`() = runTest {
        var capturedModel: String? = null
        var capturedKey: String? = null
        var capturedRequestBody: String? = null
        val client = GeminiPantryVisionClient(
            apiKey = " test-real-key-for-unit-tests ",
            model = " models/gemini-3.5-flash ",
            contentTransport = GeminiContentTransport { modelName, apiKey, requestBody ->
                capturedModel = modelName
                capturedKey = apiKey
                capturedRequestBody = requestBody
                """
                    {
                      "candidates": [
                        {
                          "content": {
                            "parts": [
                              {"text": "OK"}
                            ]
                          }
                        }
                      ]
                    }
                """.trimIndent()
            }
        )

        val result = client.testConnection()

        assertThat(result.success).isTrue()
        assertThat(result.message).isEqualTo("Gemini connection OK using gemini-3.5-flash.")
        assertThat(capturedModel).isEqualTo("gemini-3.5-flash")
        assertThat(capturedKey).isEqualTo("test-real-key-for-unit-tests")
        assertThat(capturedRequestBody).contains("Reply with OK")
        assertThat(capturedRequestBody).doesNotContain("temperature")
    }

    @Test
    fun `pantry photo request asks for json without overriding model sampling defaults`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "gemini-3.5-flash")

        val request = client.buildPantryPhotoRequest("base64-image").toString()

        assertThat(request).contains("inline_data")
        assertThat(request).contains("application/json")
        assertThat(request).doesNotContain("temperature")
        assertThat(request).doesNotContain("topP")
        assertThat(request).doesNotContain("topK")
    }

    @Test
    fun `connection test request caps output without overriding model sampling defaults`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "gemini-3.5-flash")

        val request = client.buildConnectionTestRequest().toString()

        assertThat(request).contains("maxOutputTokens")
        assertThat(request).doesNotContain("temperature")
        assertThat(request).doesNotContain("topP")
        assertThat(request).doesNotContain("topK")
    }

    @Test
    fun `connection test reports empty api response without network`() = runTest {
        val client = GeminiPantryVisionClient(
            apiKey = "test-real-key-for-unit-tests",
            model = "gemini-3.5-flash",
            contentTransport = GeminiContentTransport { _, _, _ ->
                """{"candidates":[{"content":{"parts":[]}}]}"""
            }
        )

        val result = client.testConnection()

        assertThat(result.success).isFalse()
        assertThat(result.message).isEqualTo("Gemini responded, but returned an empty test response.")
    }

    @Test
    fun `connection test reports transport failure without raw json`() = runTest {
        val client = GeminiPantryVisionClient(
            apiKey = "test-real-key-for-unit-tests",
            model = "gemini-3.5-flash",
            contentTransport = GeminiContentTransport { _, _, _ ->
                throw IOException("Gemini request failed: HTTP 400 INVALID_ARGUMENT: API key not valid.")
            }
        )

        val result = client.testConnection()

        assertThat(result.success).isFalse()
        assertThat(result.message).isEqualTo(
            "Gemini connection failed: Gemini request failed: HTTP 400 INVALID_ARGUMENT: API key not valid."
        )
    }

    @Test
    fun `summarize api error extracts google status and message`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "gemini-3.5-flash")
        val error = """
            {
              "error": {
                "code": 400,
                "message": "API key not valid. Please pass a valid API key.",
                "status": "INVALID_ARGUMENT"
              }
            }
        """.trimIndent()

        val summary = client.summarizeApiError(400, error)

        assertThat(summary).isEqualTo(
            "HTTP 400 INVALID_ARGUMENT: API key not valid. Please pass a valid API key."
        )
    }

    @Test
    fun `summarize api error handles empty body`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "gemini-3.5-flash")

        val summary = client.summarizeApiError(503, "")

        assertThat(summary).isEqualTo("HTTP 503 unknown error")
    }

    @Test
    fun `summarize api error compacts and truncates raw text`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "gemini-3.5-flash")
        val longError = "Gateway timeout\n" + "x".repeat(240)

        val summary = client.summarizeApiError(504, longError)

        assertThat(summary).startsWith("HTTP 504 Gateway timeout")
        assertThat(summary).endsWith("...")
        assertThat(summary.length).isAtMost(190)
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
    fun `parse pantry vision response with object wrapped questions and warnings`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "gemini-3.5-flash")
        val response = """
            {
              "items": [
                {
                  "product": "rice",
                  "quantity": 1,
                  "unit": "bag",
                  "questions": [
                    {"question": "What is the expiration date?"},
                    {"message": "Confirm the package size."},
                    "Confirm pantry location.",
                    {"bad": true}
                  ]
                }
              ],
              "warnings": [
                {"message": "Label was partially cropped."},
                {"warning": "Brand was not visible."},
                "Amount estimated",
                {"bad": true}
              ]
            }
        """.trimIndent()

        val result = client.parseVisionResult(response)

        assertThat(result.items).hasSize(1)
        assertThat(result.items.first().questions).containsExactly(
            "What is the expiration date?",
            "Confirm the package size.",
            "Confirm pantry location."
        )
        assertThat(result.warnings).containsExactly(
            "Label was partially cropped.",
            "Brand was not visible.",
            "Amount estimated"
        )
    }

    @Test
    fun `parse pantry vision response with alternate review question and warning aliases`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "gemini-3.5-flash")
        val response = """
            {
              "items": [
                {
                  "product": "milk",
                  "quantity": 1,
                  "unit": "gallon",
                  "clarifying_questions": [
                    {"prompt": "Confirm the printed use-by date."},
                    "Confirm the brand."
                  ]
                },
                {
                  "product": "rice",
                  "quantity": 1,
                  "unit": "bag",
                  "followUpQuestions": {"message": "Confirm package size."}
                }
              ],
              "review_notes": [
                {"note": "Some label text was obscured."},
                "Brand may be missing."
              ]
            }
        """.trimIndent()

        val result = client.parseVisionResult(response)

        assertThat(result.items).hasSize(2)
        assertThat(result.items.first { it.product == "milk" }.questions).containsExactly(
            "Confirm the printed use-by date.",
            "Confirm the brand."
        )
        assertThat(result.items.first { it.product == "rice" }.questions).containsExactly(
            "Confirm package size."
        )
        assertThat(result.warnings).containsExactly(
            "Some label text was obscured.",
            "Brand may be missing."
        )
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
    fun `parse pantry vision response with common label aliases`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "gemini-3.5-flash")
        val response = """
            {
              "items": [
                {
                  "manufacturer": "Great Value",
                  "productName": "peanut butter",
                  "packageQuantity": "1",
                  "quantityUnit": "jar",
                  "netQuantity": "16 oz",
                  "storageArea": "cabinet",
                  "sell_by_date": "2027-03-04",
                  "dateOpened": "2026-07-15",
                  "confidence": "0.86"
                },
                {
                  "itemName": "milk",
                  "number_of_items": 2,
                  "amount_unit": "packages",
                  "package_label": "1 gal",
                  "storageType": "cold storage",
                  "expirationDateText": {"text": "12/31/2026"},
                  "confidence": 0.93
                }
              ],
              "warnings": []
            }
        """.trimIndent()

        val result = client.parseVisionResult(response)

        assertThat(result.items).hasSize(2)

        val peanutButter = result.items.first { it.product == "peanut butter" }
        assertThat(peanutButter.brand).isEqualTo("Great Value")
        assertThat(peanutButter.quantity).isEqualTo(1.0)
        assertThat(peanutButter.unit).isEqualTo("jar")
        assertThat(peanutButter.size).isEqualTo("16 oz")
        assertThat(peanutButter.location).isEqualTo("pantry")
        assertThat(peanutButter.expirationDate).isEqualTo("2027-03-04")
        assertThat(peanutButter.openedDate).isEqualTo("2026-07-15")

        val milk = result.items.first { it.product == "milk" }
        assertThat(milk.quantity).isEqualTo(2.0)
        assertThat(milk.unit).isEqualTo("count")
        assertThat(milk.size).isEqualTo("1 gal")
        assertThat(milk.location).isEqualTo("fridge")
        assertThat(milk.expirationDate).isEqualTo("12/31/2026")
    }

    @Test
    fun `parse pantry vision response with liquid unit aliases`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "gemini-3.5-flash")
        val response = """
            {
              "items": [
                {
                  "product": "milk",
                  "quantity": 1,
                  "unit": "gallon"
                },
                {
                  "product": "broth",
                  "quantity": 1,
                  "unit": "quarts"
                },
                {
                  "product": "cream",
                  "quantity": 1,
                  "unit": "pint"
                }
              ],
              "warnings": []
            }
        """.trimIndent()

        val result = client.parseVisionResult(response)

        assertThat(result.items).hasSize(3)
        assertThat(result.items.first { it.product == "milk" }.unit).isEqualTo("gal")
        assertThat(result.items.first { it.product == "broth" }.unit).isEqualTo("qt")
        assertThat(result.items.first { it.product == "cream" }.unit).isEqualTo("pt")
    }

    @Test
    fun `parse pantry vision response with object and array wrapped string fields`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "gemini-3.5-flash")
        val response = """
            {
              "items": [
                {
                  "brand": {"name": "Great Value"},
                  "product": {"name": "black beans"},
                  "quantity": {"value": "2", "unit": "cans"},
                  "unit": ["cans"],
                  "size": {"value": "15 oz"},
                  "storage": {"location": "Refrigerator"},
                  "bestBy": {"text": "2026-12-31"},
                  "confidence": {"value": "0.89"}
                }
              ],
              "warnings": []
            }
        """.trimIndent()

        val result = client.parseVisionResult(response)

        assertThat(result.items).hasSize(1)
        val item = result.items.first()
        assertThat(item.brand).isEqualTo("Great Value")
        assertThat(item.product).isEqualTo("black beans")
        assertThat(item.quantity).isEqualTo(2.0)
        assertThat(item.unit).isEqualTo("can")
        assertThat(item.size).isEqualTo("15 oz")
        assertThat(item.location).isEqualTo("fridge")
        assertThat(item.expirationDate).isEqualTo("2026-12-31")
        assertThat(item.confidence).isEqualTo(0.89)
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
    fun `parse pantry vision response with dozen quantity amount`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "gemini-3.5-flash")
        val response = """
            {
              "items": [
                {
                  "product": "eggs",
                  "amount": "a dozen eggs",
                  "location": "fridge"
                },
                {
                  "product": "tuna",
                  "amount": "dozen cans",
                  "location": "pantry"
                },
                {
                  "product": "tortillas",
                  "quantity": {
                    "value": "half dozen"
                  }
                },
                {
                  "product": "sparkling water",
                  "quantity": 2,
                  "unit": "dozen"
                },
                {
                  "product": "eggs",
                  "quantity": "dozen",
                  "unit": "eggs"
                }
              ],
              "warnings": []
            }
        """.trimIndent()

        val result = client.parseVisionResult(response)

        assertThat(result.items).hasSize(5)

        val eggs = result.items.filter { it.product == "eggs" }.first()
        assertThat(eggs.quantity).isEqualTo(12.0)
        assertThat(eggs.unit).isEqualTo("count")
        assertThat(eggs.location).isEqualTo("fridge")

        val tuna = result.items.first { it.product == "tuna" }
        assertThat(tuna.quantity).isEqualTo(12.0)
        assertThat(tuna.unit).isEqualTo("can")

        val tortillas = result.items.first { it.product == "tortillas" }
        assertThat(tortillas.quantity).isEqualTo(6.0)
        assertThat(tortillas.unit).isEqualTo("count")

        val sparklingWater = result.items.first { it.product == "sparkling water" }
        assertThat(sparklingWater.quantity).isEqualTo(24.0)
        assertThat(sparklingWater.unit).isEqualTo("count")

        val splitEggs = result.items.filter { it.product == "eggs" }.last()
        assertThat(splitEggs.quantity).isEqualTo(12.0)
        assertThat(splitEggs.unit).isEqualTo("count")
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
    fun `parse pantry vision response with leading decimal quantity and confidence`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "gemini-3.5-flash")
        val response = """
            {
              "items": [
                {
                  "product": "ground beef",
                  "amount": ".5 lb",
                  "confidence": ".82"
                },
                {
                  "product": "olive oil",
                  "quantity": {
                    "value": ".75",
                    "unit": "cups"
                  },
                  "confidence": {
                    "value": ".91"
                  }
                }
              ],
              "warnings": []
            }
        """.trimIndent()

        val result = client.parseVisionResult(response)

        assertThat(result.items).hasSize(2)

        val beef = result.items.first { it.product == "ground beef" }
        assertThat(beef.quantity).isEqualTo(0.5)
        assertThat(beef.unit).isEqualTo("lb")
        assertThat(beef.confidence).isEqualTo(0.82)

        val oliveOil = result.items.first { it.product == "olive oil" }
        assertThat(oliveOil.quantity).isEqualTo(0.75)
        assertThat(oliveOil.unit).isEqualTo("cup")
        assertThat(oliveOil.confidence).isEqualTo(0.91)
    }

    @Test
    fun `parse pantry vision response rejects non finite numeric text`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "gemini-3.5-flash")
        val response = """
            {
              "items": [
                {
                  "product": "ground beef",
                  "amount": "NaN lb",
                  "confidence": "Infinity"
                }
              ],
              "warnings": []
            }
        """.trimIndent()

        val result = client.parseVisionResult(response)

        assertThat(result.items).hasSize(1)
        val item = result.items.first()
        assertThat(item.product).isEqualTo("ground beef")
        assertThat(item.quantity).isNull()
        assertThat(item.unit).isNull()
        assertThat(item.confidence).isEqualTo(0.5)
    }

    @Test
    fun `parse pantry vision response skips malformed string fields`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "gemini-3.5-flash")
        val response = """
            {
              "items": [
                {
                  "brand": {"name": "bad shape"},
                  "product": {"name": {"bad": true}},
                  "quantity": 1,
                  "unit": "box",
                  "confidence": 0.8,
                  "questions": ["Keep this question", {"bad": true}, null, 7]
                },
                {
                  "brand": [{"bad": true}],
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

    @Test
    fun `parse pantry vision response returns empty result for non json text`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "gemini-3.5-flash")
        val response = "I cannot confidently identify pantry items in this image."

        val result = client.parseVisionResult(response)

        assertThat(result.items).isEmpty()
        assertThat(result.warnings).containsExactly("Gemini returned non-JSON pantry output.")
        assertThat(result.rawResponse).isEqualTo(response)
    }
}
