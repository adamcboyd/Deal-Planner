package com.dealplanner.ai

import android.graphics.Bitmap
import android.util.Base64
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.dealplanner.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Thin Gemini Vision REST client for pantry photo extraction.
 *
 * API keys are supplied by BuildConfig from local.properties or GEMINI_API_KEY.
 */
class GeminiPantryVisionClient(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
    private val model: String = BuildConfig.GEMINI_MODEL
) {
    data class PantryVisionItem(
        val brand: String?,
        val product: String?,
        val quantity: Double?,
        val unit: String?,
        val size: String?,
        val location: String?,
        val expirationDate: String?,
        val openedDate: String?,
        val confidence: Double,
        val questions: List<String>
    )

    data class PantryVisionResult(
        val items: List<PantryVisionItem>,
        val warnings: List<String>,
        val rawResponse: String
    )

    data class ConnectionTestResult(
        val success: Boolean,
        val message: String
    )

    val modelName: String = model.trim()
        .removePrefix("models/")
        .ifBlank { DEFAULT_MODEL_NAME }

    fun isConfigured(): Boolean {
        val trimmedKey = apiKey.trim()
        return trimmedKey.isNotBlank() &&
            !trimmedKey.equals("YOUR_GEMINI_API_KEY", ignoreCase = true) &&
            !trimmedKey.startsWith("YOUR_", ignoreCase = true)
    }

    suspend fun analyzePantryPhoto(bitmap: Bitmap): PantryVisionResult = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            throw IOException("Gemini API key is not configured.")
        }

        val base64Image = bitmap.toJpegBase64()
        val requestBody = buildRequest(base64Image).toString()
        val responseText = postGenerateContent(requestBody)

        parseVisionResult(extractResponseText(responseText))
    }

    suspend fun testConnection(): ConnectionTestResult = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext ConnectionTestResult(
                success = false,
                message = "Gemini API key is not configured."
            )
        }

        try {
            val responseText = postGenerateContent(buildConnectionTestRequest().toString())
            val answer = extractResponseText(responseText).trim()
            if (answer.isBlank()) {
                ConnectionTestResult(
                    success = false,
                    message = "Gemini responded, but returned an empty test response."
                )
            } else {
                ConnectionTestResult(
                    success = true,
                    message = "Gemini connection OK using $modelName."
                )
            }
        } catch (e: Exception) {
            ConnectionTestResult(
                success = false,
                message = "Gemini connection failed: ${e.message ?: "unknown error"}"
            )
        }
    }

    private fun postGenerateContent(requestBody: String): String {
        val endpoint = URL("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent")
        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-goog-api-key", apiKey.trim())
        }

        return try {
            connection.outputStream.use { output ->
                output.write(requestBody.toByteArray(Charsets.UTF_8))
            }

            if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IOException("Gemini request failed: ${summarizeApiError(connection.responseCode, errorText)}")
            }
        } finally {
            connection.disconnect()
        }
    }

    internal fun summarizeApiError(responseCode: Int, errorText: String): String {
        val apiError = try {
            val root = JsonParser.parseString(errorText).asJsonObject
            val error = root.getAsJsonObject("error")
            val status = error?.getFlexibleStringOrNull("status")
            val message = error?.getFlexibleStringOrNull("message")
            listOfNotNull(status, message)
                .joinToString(": ")
                .ifBlank { null }
        } catch (_: Exception) {
            null
        }

        val detail = (apiError ?: errorText.compactForStatus()).ifBlank { "unknown error" }
        return "HTTP $responseCode ${detail.truncateStatusDetail()}"
    }

    private fun buildRequest(base64Image: String): JsonObject {
        val imagePart = JsonObject().apply {
            add(
                "inline_data",
                JsonObject().apply {
                    addProperty("mime_type", "image/jpeg")
                    addProperty("data", base64Image)
                }
            )
        }

        val promptPart = JsonObject().apply {
            addProperty("text", pantryPrompt)
        }

        val content = JsonObject().apply {
            add("parts", JsonArray().apply {
                add(imagePart)
                add(promptPart)
            })
        }

        return JsonObject().apply {
            add("contents", JsonArray().apply { add(content) })
            add(
                "generationConfig",
                JsonObject().apply {
                    addProperty("temperature", 0.1)
                    addProperty("responseMimeType", "application/json")
                }
            )
        }
    }

    private fun buildConnectionTestRequest(): JsonObject {
        val promptPart = JsonObject().apply {
            addProperty("text", "Reply with OK to confirm this Deal Planner Gemini setup works.")
        }

        val content = JsonObject().apply {
            add("parts", JsonArray().apply { add(promptPart) })
        }

        return JsonObject().apply {
            add("contents", JsonArray().apply { add(content) })
            add(
                "generationConfig",
                JsonObject().apply {
                    addProperty("temperature", 0.0)
                    addProperty("maxOutputTokens", 16)
                }
            )
        }
    }

    private fun Bitmap.toJpegBase64(): String {
        val output = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 86, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    private fun extractResponseText(responseBody: String): String {
        val root = JsonParser.parseString(responseBody).asJsonObject
        val candidates = root.getAsJsonArray("candidates") ?: return ""
        val first = candidates.firstOrNull()?.asJsonObject ?: return ""
        val content = first.getAsJsonObject("content") ?: return ""
        val parts = content.getAsJsonArray("parts") ?: return ""

        return parts
            .mapNotNull { part -> part.asJsonObject.get("text")?.asString }
            .joinToString("\n")
    }

    internal fun parseVisionResult(rawText: String): PantryVisionResult {
        val cleaned = rawText.extractJsonText()

        val root = JsonParser.parseString(cleaned)
        val warnings = root
            .takeIf { it.isJsonObject }
            ?.asJsonObject
            ?.get("warnings")
            ?.toStringList()
            .orEmpty()
        val items = root.toPantryVisionItemArray()
            ?.mapNotNull { element -> element.takeIf { it.isJsonObject }?.asJsonObject?.toPantryVisionItem() }
            .orEmpty()

        return PantryVisionResult(
            items = items,
            warnings = warnings,
            rawResponse = rawText
        )
    }

    private fun JsonElement.toPantryVisionItemArray(): JsonArray? {
        if (isJsonArray) return asJsonArray
        if (!isJsonObject) return null

        val root = asJsonObject
        listOf("items", "pantry_items", "pantryItems", "foods", "food_items")
            .firstNotNullOfOrNull { name ->
                root.get(name)?.let { element ->
                    when {
                        element.isJsonArray -> element.asJsonArray
                        element.isJsonObject -> JsonArray().apply { add(element) }
                        else -> null
                    }
                }
            }
            ?.let { return it }

        listOf("item", "pantry_item", "pantryItem", "food", "food_item")
            .firstNotNullOfOrNull { name ->
                root.get(name)
                    ?.takeIf { it.isJsonObject }
                    ?.let { JsonArray().apply { add(it) } }
            }
            ?.let { return it }

        return if (root.looksLikePantryVisionItem()) {
            JsonArray().apply { add(root) }
        } else {
            null
        }
    }

    private fun JsonObject.looksLikePantryVisionItem(): Boolean {
        return listOf("product", "item", "product_name", "name", "food", "food_name")
            .any { name -> getFlexibleStringOrNull(name) != null }
    }

    private fun JsonObject.toPantryVisionItem(): PantryVisionItem? {
        val product = firstStringOrNull("product", "item", "product_name", "name", "food", "food_name")
        if (product.isNullOrBlank()) return null
        val quantity = firstQuantityParts("quantity", "qty", "amount", "count")
        val explicitUnit = firstStringOrNull("unit", "units", "item_unit", "itemUnit", "package_unit", "packageUnit")

        return PantryVisionItem(
            brand = firstStringOrNull("brand", "brand_name", "brandName"),
            product = product,
            quantity = quantity.value,
            unit = (explicitUnit ?: quantity.unit).normalizePantryUnit(),
            size = firstStringOrNull("size", "package_size", "packageSize", "net_weight", "netWeight"),
            location = firstStringOrNull(
                "location",
                "storage_location",
                "storageLocation",
                "storage",
                "place"
            ).normalizeStorageLocation(),
            expirationDate = firstStringOrNull(
                "expirationDate",
                "expiration_date",
                "expiration",
                "expiryDate",
                "expiry_date",
                "expiry",
                "bestBy",
                "best_by",
                "bestByDate",
                "best_by_date",
                "bestBefore",
                "best_before",
                "bestBeforeDate",
                "best_before_date",
                "useBy",
                "use_by",
                "useByDate",
                "use_by_date"
            ),
            openedDate = firstStringOrNull("openedDate", "opened_date", "opened", "openedOn", "opened_on", "openDate", "open_date"),
            confidence = (get("confidence")?.asFlexibleDoubleOrNull() ?: 0.5).coerceIn(0.0, 1.0),
            questions = get("questions")?.toStringList().orEmpty()
        )
    }

    private fun String.extractJsonText(): String {
        val trimmed = trim()
        val unfenced = if (trimmed.startsWith("```")) {
            trimmed
                .lineSequence()
                .drop(1)
                .joinToString("\n")
                .removeSuffix("```")
                .trim()
        } else {
            trimmed
        }

        val objectStart = unfenced.indexOf('{')
        val objectEnd = unfenced.lastIndexOf('}')
        val arrayStart = unfenced.indexOf('[')
        val arrayEnd = unfenced.lastIndexOf(']')
        val useArray = arrayStart >= 0 && arrayEnd >= arrayStart && (
            objectStart == -1 || arrayStart < objectStart
        )

        if (useArray) {
            return unfenced.substring(arrayStart, arrayEnd + 1).trim()
        }

        val start = objectStart
        val end = objectEnd
        return if (start >= 0 && end >= start) {
            unfenced.substring(start, end + 1).trim()
        } else {
            unfenced
        }
    }

    private fun JsonObject.getFlexibleStringOrNull(name: String): String? {
        return get(name)?.asFlexibleStringOrNull()
    }

    private fun JsonElement.asFlexibleStringOrNull(): String? {
        if (isJsonNull) return null
        if (isJsonObject) {
            return asJsonObject.firstStringOrNull(
                "name",
                "value",
                "text",
                "label",
                "title",
                "location"
            )
        }
        if (isJsonArray) {
            return asJsonArray.firstNotNullOfOrNull { element ->
                element.asFlexibleStringOrNull()
            }
        }
        if (!isJsonPrimitive) return null
        return try {
            asString.trim().ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }

    private fun JsonObject.firstStringOrNull(vararg names: String): String? {
        return names.firstNotNullOfOrNull { name -> getFlexibleStringOrNull(name) }
    }

    private fun JsonObject.firstQuantityParts(vararg names: String): QuantityParts {
        return names.firstNotNullOfOrNull { name ->
            get(name)
                ?.toQuantityParts()
                ?.takeIf { it.value != null || it.unit != null }
        } ?: QuantityParts()
    }

    private fun JsonElement.asFlexibleDoubleOrNull(): Double? {
        return try {
            when {
                isJsonNull -> null
                isJsonPrimitive -> asString.toFlexibleDoubleOrNull()
                isJsonObject -> asJsonObject.firstStringOrNull(
                    "value",
                    "amount",
                    "confidence",
                    "score"
                )?.toFlexibleDoubleOrNull()
                isJsonArray -> asJsonArray.firstNotNullOfOrNull { element ->
                    element.asFlexibleDoubleOrNull()
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun JsonElement.toQuantityParts(): QuantityParts? {
        if (isJsonNull) return null
        if (isJsonObject) return asJsonObject.toQuantityParts()
        if (!isJsonPrimitive) return null

        asFlexibleDoubleOrNull()?.let { value -> return QuantityParts(value = value) }
        val text = try {
            asString.trim()
        } catch (_: Exception) {
            return null
        }
        if (text.isBlank()) return null

        val match = quantityPattern.find(text) ?: wordQuantityPattern.find(text) ?: return null
        val value = match.groupValues[1].toQuantityDoubleOrNull() ?: wordQuantities[match.groupValues[1].lowercase()]
            ?: return null
        val unit = match.groupValues.getOrNull(2)?.ifBlank { null }
        return QuantityParts(value = value, unit = unit)
    }

    private fun JsonObject.toQuantityParts(): QuantityParts? {
        val value = firstStringOrNull(
            "value",
            "amount",
            "quantity",
            "qty",
            "count",
            "number"
        )?.toQuantityDoubleOrNull()
        val unit = firstStringOrNull(
            "unit",
            "units",
            "item_unit",
            "itemUnit",
            "package_unit",
            "packageUnit"
        )

        return QuantityParts(value = value, unit = unit)
            .takeIf { it.value != null || it.unit != null }
    }

    private fun String.toQuantityDoubleOrNull(): Double? {
        val compact = replace(" ", "").replace(',', '.')
        val fractionParts = compact.split('/').takeIf { it.size == 2 }
        if (fractionParts != null) {
            val numerator = fractionParts[0].toDoubleOrNull()
            val denominator = fractionParts[1].toDoubleOrNull()
            if (numerator != null && denominator != null && denominator != 0.0) {
                return numerator / denominator
            }
        }
        return compact.toDoubleOrNull()
    }

    private fun String.toFlexibleDoubleOrNull(): Double? {
        return trim().replace(',', '.').toDoubleOrNull()
    }

    private fun String?.normalizePantryUnit(): String? {
        val normalized = this
            ?.trim()
            ?.lowercase()
            ?.trim('.', ',', ';', ':')
            ?.ifBlank { null }
            ?: return null

        return when (normalized) {
            "cans" -> "can"
            "jars" -> "jar"
            "boxes" -> "box"
            "bags" -> "bag"
            "bottles" -> "bottle"
            "containers" -> "container"
            "cups" -> "cup"
            "lbs", "pound", "pounds" -> "lb"
            "ounces", "ounce" -> "oz"
            "grams", "gram" -> "g"
            "kilograms", "kilogram" -> "kg"
            "ct", "each", "ea", "item", "items", "counts" -> "count"
            else -> normalized
        }
    }

    private fun String?.normalizeStorageLocation(): String? {
        val normalized = this
            ?.trim()
            ?.lowercase()
            ?.trim('.', ',', ';', ':')
            ?.ifBlank { null }
            ?: return null

        return when (normalized) {
            "refrigerator", "refrigerated" -> "fridge"
            "deep freezer" -> "freezer"
            else -> normalized
        }
    }

    private fun JsonElement.toStringList(): List<String> {
        return when {
            isJsonNull -> emptyList()
            isJsonArray -> asJsonArray.mapNotNull { element ->
                if (!element.isJsonPrimitive) {
                    null
                } else {
                    element.asString.trim().ifBlank { null }
                }
            }
            isJsonPrimitive -> listOfNotNull(asString.trim().ifBlank { null })
            else -> emptyList()
        }
    }

    private fun String.compactForStatus(): String {
        return lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .replace(Regex("""\s+"""), " ")
    }

    private fun String.truncateStatusDetail(): String {
        return if (length <= MAX_STATUS_DETAIL_LENGTH) {
            this
        } else {
            take(MAX_STATUS_DETAIL_LENGTH - 3).trimEnd() + "..."
        }
    }

    private companion object {
        private const val DEFAULT_MODEL_NAME = "gemini-3.5-flash"
        private const val MAX_STATUS_DETAIL_LENGTH = 180
        private val quantityPattern = Regex("""(\d+\s*/\s*\d+|\d+(?:[.,]\d+)?)\s*([A-Za-z]+)?""")
        private val wordQuantityPattern = Regex(
            """\b(one|two|three|four|five|six|seven|eight|nine|ten|half)\b\s*([A-Za-z]+)?""",
            RegexOption.IGNORE_CASE
        )
        private val wordQuantities = mapOf(
            "one" to 1.0,
            "two" to 2.0,
            "three" to 3.0,
            "four" to 4.0,
            "five" to 5.0,
            "six" to 6.0,
            "seven" to 7.0,
            "eight" to 8.0,
            "nine" to 9.0,
            "ten" to 10.0,
            "half" to 0.5
        )

        private val pantryPrompt = """
            You identify pantry, fridge, and freezer food items from a phone photo.
            Return only valid JSON with this exact shape:
            {
              "items": [
                {
                  "brand": "string or Generic or null",
                  "product": "specific food product name",
                  "quantity": 1.0,
                  "unit": "can|jar|box|bag|bottle|lb|oz|g|kg|count|cup|container|unknown",
                  "size": "package size if visible, such as 15 oz, or null",
                  "location": "pantry|fridge|freezer|unknown",
                  "expirationDate": "YYYY-MM-DD or null",
                  "openedDate": "YYYY-MM-DD or null",
                  "confidence": 0.0,
                  "questions": ["short clarification question when brand, amount, size, or expiration is missing"]
                }
              ],
              "warnings": ["short overall warnings"]
            }
            Do not invent brands, dates, nutrition, or quantities. If no brand is present, use "Generic".
            If a container is unlabeled or the remaining amount is not visible, include a question.
            Prefer one row per visible food item.
        """.trimIndent()
    }

    private data class QuantityParts(
        val value: Double? = null,
        val unit: String? = null
    )
}
