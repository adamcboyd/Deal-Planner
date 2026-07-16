package com.dealplanner.lookup

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

class OpenFoodFactsBarcodeClient(
    private val baseUrl: String = "https://world.openfoodfacts.org/api/v3/product",
    private val userAgent: String = "DealPlanner/1.0 (https://github.com/adamcboyd/Deal-Planner)"
) {
    data class BarcodeProduct(
        val barcode: String,
        val name: String,
        val brand: String?,
        val quantity: String?,
        val categoryTags: List<String>
    )

    sealed class BarcodeLookupResult {
        data class Found(val product: BarcodeProduct) : BarcodeLookupResult()
        object NotFound : BarcodeLookupResult()
        data class Error(val message: String) : BarcodeLookupResult()
    }

    suspend fun lookupBarcode(rawBarcode: String): BarcodeLookupResult = withContext(Dispatchers.IO) {
        val barcode = normalizeBarcode(rawBarcode)
        if (barcode.isBlank()) {
            return@withContext BarcodeLookupResult.Error("No barcode found.")
        }

        try {
            val connection = (buildProductUrl(barcode).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", userAgent)
            }

            try {
                val responseText = if (connection.responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    return@withContext BarcodeLookupResult.Error(
                        "Product lookup failed (${connection.responseCode})."
                    )
                }

                parseProductResponse(responseText, barcode)
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            BarcodeLookupResult.Error(e.message ?: "Product lookup unavailable.")
        }
    }

    internal fun parseProductResponse(responseText: String, fallbackBarcode: String): BarcodeLookupResult {
        val root = try {
            JsonParser.parseString(responseText).asJsonObject
        } catch (_: Exception) {
            return BarcodeLookupResult.Error("Product lookup returned unreadable data.")
        }

        val resultId = root.getAsJsonObjectOrNull("result")?.getStringOrNull("id")
        val status = root.getStringOrNull("status")
        if (status != "success" || resultId != "product_found") {
            return BarcodeLookupResult.NotFound
        }

        val product = root.getAsJsonObjectOrNull("product") ?: return BarcodeLookupResult.NotFound
        val name = firstNonBlank(
            product.getStringOrNull("product_name_en"),
            product.getStringOrNull("product_name"),
            product.getStringOrNull("generic_name")
        ) ?: return BarcodeLookupResult.NotFound

        return BarcodeLookupResult.Found(
            BarcodeProduct(
                barcode = product.getStringOrNull("code") ?: root.getStringOrNull("code") ?: fallbackBarcode,
                name = name,
                brand = firstBrand(product.getStringOrNull("brands")),
                quantity = normalizeQuantity(product.getStringOrNull("quantity")),
                categoryTags = product.get("categories_tags")?.toStringList().orEmpty()
            )
        )
    }

    internal fun normalizeBarcode(rawBarcode: String): String {
        val trimmed = rawBarcode.trim()
        val compact = trimmed.replace(Regex("""[\s-]+"""), "")
        return compact.ifBlank { trimmed }.uppercase()
    }

    private fun buildProductUrl(barcode: String): URL {
        val encodedBarcode = URLEncoder.encode(barcode, Charsets.UTF_8.name())
        val fields = listOf(
            "code",
            "status",
            "result",
            "product_name",
            "product_name_en",
            "generic_name",
            "brands",
            "quantity",
            "categories_tags"
        ).joinToString(",")
        return URL("$baseUrl/$encodedBarcode.json?fields=$fields")
    }

    private fun firstBrand(value: String?): String? {
        return value
            ?.split(",")
            ?.map { it.trim() }
            ?.firstOrNull { it.isNotBlank() }
    }

    private fun normalizeQuantity(value: String?): String? {
        return value
            ?.trim()
            ?.replace(Regex("""\s+"""), " ")
            ?.replace(Regex("""\b(\d+)\.0(?=\s)"""), "$1")
            ?.ifBlank { null }
    }

    private fun firstNonBlank(vararg values: String?): String? {
        return values.firstOrNull { !it.isNullOrBlank() }?.trim()
    }

    private fun JsonObject.getAsJsonObjectOrNull(name: String): JsonObject? {
        val element = get(name) ?: return null
        return if (element.isJsonObject) element.asJsonObject else null
    }

    private fun JsonObject.getStringOrNull(name: String): String? {
        val element = get(name) ?: return null
        if (!element.isJsonPrimitive) return null
        return try {
            element.asString.trim().ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }

    private fun JsonElement.toStringList(): List<String> {
        return when {
            isJsonArray -> asJsonArray.mapNotNull { element ->
                if (element.isJsonPrimitive) {
                    element.asString.trim().ifBlank { null }
                } else {
                    null
                }
            }
            isJsonPrimitive -> listOfNotNull(asString.trim().ifBlank { null })
            else -> emptyList()
        }
    }
}
