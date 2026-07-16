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
                throw IOException("Gemini request failed (${connection.responseCode}): $errorText")
            }
        } finally {
            connection.disconnect()
        }
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
        val cleaned = rawText.extractJsonObjectText()

        val root = JsonParser.parseString(cleaned).asJsonObject
        val warnings = root.get("warnings")?.toStringList().orEmpty()
        val items = root.get("items")
            ?.takeIf { it.isJsonArray }
            ?.asJsonArray
            ?.mapNotNull { element -> element.takeIf { it.isJsonObject }?.asJsonObject?.toPantryVisionItem() }
            .orEmpty()

        return PantryVisionResult(
            items = items,
            warnings = warnings,
            rawResponse = rawText
        )
    }

    private fun JsonObject.toPantryVisionItem(): PantryVisionItem? {
        val product = getStringOrNull("product") ?: getStringOrNull("item")
        if (product.isNullOrBlank()) return null

        return PantryVisionItem(
            brand = getStringOrNull("brand"),
            product = product,
            quantity = get("quantity")?.asDoubleOrNull(),
            unit = getStringOrNull("unit"),
            size = getStringOrNull("size"),
            location = getStringOrNull("location"),
            expirationDate = getStringOrNull("expirationDate"),
            openedDate = getStringOrNull("openedDate"),
            confidence = (get("confidence")?.asDoubleOrNull() ?: 0.5).coerceIn(0.0, 1.0),
            questions = get("questions")?.toStringList().orEmpty()
        )
    }

    private fun String.extractJsonObjectText(): String {
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

        val start = unfenced.indexOf('{')
        val end = unfenced.lastIndexOf('}')
        return if (start >= 0 && end >= start) {
            unfenced.substring(start, end + 1).trim()
        } else {
            unfenced
        }
    }

    private fun JsonObject.getStringOrNull(name: String): String? {
        val element = get(name) ?: return null
        if (element.isJsonNull) return null
        return element.asString.trim().ifBlank { null }
    }

    private fun JsonElement.asDoubleOrNull(): Double? {
        return try {
            if (isJsonNull) null else asDouble
        } catch (_: Exception) {
            null
        }
    }

    private fun JsonElement.toStringList(): List<String> {
        return when {
            isJsonNull -> emptyList()
            isJsonArray -> asJsonArray.mapNotNull { element ->
                if (element.isJsonNull) null else element.asString.trim().ifBlank { null }
            }
            else -> listOfNotNull(asString.trim().ifBlank { null })
        }
    }

    private companion object {
        private const val DEFAULT_MODEL_NAME = "gemini-3.5-flash"

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
}
