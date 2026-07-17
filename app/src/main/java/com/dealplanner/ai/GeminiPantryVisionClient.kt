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
fun interface GeminiContentTransport {
    fun postGenerateContent(modelName: String, apiKey: String, requestBody: String): String
}

class GeminiPantryVisionClient(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
    private val model: String = BuildConfig.GEMINI_MODEL,
    private val contentTransport: GeminiContentTransport? = null
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
        val requestBody = buildPantryPhotoRequest(base64Image).toString()
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
            val textResponse = postGenerateContent(buildConnectionTestRequest().toString())
            val textAnswer = extractResponseText(textResponse).trim()
            if (textAnswer.isBlank()) {
                ConnectionTestResult(
                    success = false,
                    message = "Gemini responded, but returned an empty test response."
                )
            } else {
                val imageResponse = postGenerateContent(buildImageConnectionTestRequest().toString())
                val imageAnswer = extractResponseText(imageResponse).trim()
                if (imageAnswer.isBlank()) {
                    ConnectionTestResult(
                        success = false,
                        message = "Gemini text test passed, but image test returned an empty response."
                    )
                } else {
                    ConnectionTestResult(
                        success = true,
                        message = "Gemini text and image connection OK using $modelName."
                    )
                }
            }
        } catch (e: Exception) {
            ConnectionTestResult(
                success = false,
                message = "Gemini connection failed: ${e.message ?: "unknown error"}"
            )
        }
    }

    private fun postGenerateContent(requestBody: String): String {
        contentTransport?.let { transport ->
            return transport.postGenerateContent(modelName, apiKey.trim(), requestBody)
        }

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

    internal fun buildPantryPhotoRequest(base64Image: String): JsonObject {
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
                    add(
                        "responseFormat",
                        JsonObject().apply {
                            add(
                                "text",
                                JsonObject().apply {
                                    addProperty("mimeType", "application/json")
                                }
                            )
                        }
                    )
                }
            )
        }
    }

    internal fun buildConnectionTestRequest(): JsonObject {
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
                    addProperty("maxOutputTokens", 16)
                }
            )
        }
    }

    internal fun buildImageConnectionTestRequest(base64Image: String = ONE_PIXEL_PNG_BASE64): JsonObject {
        val imagePart = JsonObject().apply {
            add(
                "inline_data",
                JsonObject().apply {
                    addProperty("mime_type", "image/png")
                    addProperty("data", base64Image)
                }
            )
        }

        val promptPart = JsonObject().apply {
            addProperty("text", "Reply with OK if you can process this Deal Planner pantry image test.")
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

        val root = try {
            JsonParser.parseString(cleaned)
        } catch (_: Exception) {
            return PantryVisionResult(
                items = emptyList(),
                warnings = listOf("Gemini returned non-JSON pantry output."),
                rawResponse = rawText
            )
        }
        val warnings = root
            .takeIf { it.isJsonObject }
            ?.asJsonObject
            ?.firstStringListOrEmpty(
                "warnings",
                "warning",
                "reviewWarnings",
                "review_warnings",
                "overallWarnings",
                "overall_warnings",
                "issues",
                "notes",
                "reviewNotes",
                "review_notes",
                "messages"
            )
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

        val root = this.asJsonObject
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
        val product = firstStringOrNull(
            "product",
            "productName",
            "product_name",
            "item",
            "itemName",
            "item_name",
            "name",
            "food",
            "foodName",
            "food_name",
            "description",
            "label"
        )
        if (product.isNullOrBlank()) return null
        val quantity = firstQuantityParts(
            "quantity",
            "qty",
            "amount",
            "count",
            "packageQuantity",
            "package_quantity",
            "numberOfItems",
            "number_of_items"
        )
        val explicitUnit = firstStringOrNull(
            "unit",
            "units",
            "quantityUnit",
            "quantity_unit",
            "amountUnit",
            "amount_unit",
            "item_unit",
            "itemUnit",
            "package_unit",
            "packageUnit"
        )
        val quantityWithExplicitUnit = quantity.withExplicitUnit(explicitUnit)

        return PantryVisionItem(
            brand = firstStringOrNull("brand", "brand_name", "brandName", "manufacturer", "maker", "labelBrand", "label_brand"),
            product = product,
            quantity = quantityWithExplicitUnit.value,
            unit = quantityWithExplicitUnit.unit.normalizePantryUnit(),
            size = firstStringOrNull(
                "size",
                "package",
                "packageSize",
                "package_size",
                "netWeight",
                "net_weight",
                "netQuantity",
                "net_quantity",
                "packageLabel",
                "package_label"
            ),
            location = firstStringOrNull(
                "location",
                "storage_location",
                "storageLocation",
                "storage",
                "storageArea",
                "storage_area",
                "storageType",
                "storage_type",
                "place",
                "section"
            ).normalizeStorageLocation(),
            expirationDate = firstStringOrNull(
                "expirationDate",
                "expiration_date",
                "expirationDateText",
                "expiration_date_text",
                "expirationDateValue",
                "expiration_date_value",
                "expiration",
                "expires",
                "expiresOn",
                "expires_on",
                "expiryDate",
                "expiry_date",
                "expiryDateText",
                "expiry_date_text",
                "expiry",
                "date",
                "dateText",
                "date_text",
                "dateLabel",
                "date_label",
                "labelDate",
                "label_date",
                "labelDateText",
                "label_date_text",
                "dateOnLabel",
                "date_on_label",
                "bestDate",
                "best_date",
                "bestBy",
                "best_by",
                "bestByText",
                "best_by_text",
                "bestByDate",
                "best_by_date",
                "bestByDateText",
                "best_by_date_text",
                "bestBefore",
                "best_before",
                "bestBeforeText",
                "best_before_text",
                "bestBeforeDate",
                "best_before_date",
                "bestBeforeDateText",
                "best_before_date_text",
                "sellBy",
                "sell_by",
                "sellByDate",
                "sell_by_date",
                "sellByText",
                "sell_by_text",
                "useBefore",
                "use_before",
                "useBy",
                "use_by",
                "useByText",
                "use_by_text",
                "useByDate",
                "use_by_date",
                "useByDateText",
                "use_by_date_text"
            ),
            openedDate = firstStringOrNull(
                "openedDate",
                "opened_date",
                "openedDateText",
                "opened_date_text",
                "opened",
                "openedAt",
                "opened_at",
                "openedText",
                "opened_text",
                "openedOn",
                "opened_on",
                "openedOnText",
                "opened_on_text",
                "dateOpened",
                "date_opened",
                "openDate",
                "open_date",
                "openDateText",
                "open_date_text",
                "purchaseDate",
                "purchase_date",
                "purchaseDateText",
                "purchase_date_text",
                "purchasedOn",
                "purchased_on",
                "purchasedAt",
                "purchased_at",
                "datePurchased",
                "date_purchased"
            ),
            confidence = (get("confidence")?.asFlexibleDoubleOrNull() ?: 0.5).coerceIn(0.0, 1.0),
            questions = firstStringListOrEmpty(
                "questions",
                "question",
                "clarifyingQuestions",
                "clarifying_questions",
                "clarificationQuestions",
                "clarification_questions",
                "followUpQuestions",
                "follow_up_questions",
                "reviewQuestions",
                "review_questions",
                "prompts"
            )
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
                "date",
                "dateValue",
                "date_value",
                "label",
                "title",
                "raw",
                "rawText",
                "raw_text",
                "display",
                "displayText",
                "display_text",
                "location",
                "question",
                "message",
                "warning",
                "note",
                "reason",
                "description",
                "prompt"
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

    private fun JsonObject.firstStringListOrEmpty(vararg names: String): List<String> {
        return names.firstNotNullOfOrNull { name ->
            get(name)
                ?.toStringList()
                ?.takeIf { it.isNotEmpty() }
        }.orEmpty()
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
        val quantityText = match.groupValues[1]
        val value = quantityText.toQuantityDoubleOrNull() ?: wordQuantities[quantityText.lowercase()]
            ?: return null
        val unit = match.groupValues.getOrNull(2)?.ifBlank { null }
        return QuantityParts(value = value, unit = unit).withQuantityText(quantityText)
    }

    private fun JsonObject.toQuantityParts(): QuantityParts? {
        val valueText = firstStringOrNull(
            "value",
            "amount",
            "quantity",
            "qty",
            "count",
            "number"
        )
        val value = valueText?.toQuantityDoubleOrNull()
        val unit = firstStringOrNull(
            "unit",
            "units",
            "item_unit",
            "itemUnit",
            "package_unit",
            "packageUnit"
        )

        return QuantityParts(value = value, unit = unit)
            .withQuantityText(valueText)
            .takeIf { it.value != null || it.unit != null }
    }

    private fun String.toQuantityDoubleOrNull(): Double? {
        val normalized = trim()
            .lowercase()
            .removePrefix("a ")
            .removePrefix("an ")
            .replace(Regex("""\s+"""), " ")
        wordQuantities[normalized]?.let { return it }
        if (normalized.endsWith(" dozen")) {
            val multiplierText = normalized.removeSuffix(" dozen").trim()
            val multiplier = wordQuantities[multiplierText] ?: multiplierText.toDoubleOrNull()
            if (multiplier != null) {
                return multiplier * DOZEN_COUNT
            }
        }

        val compact = replace(" ", "").replace(',', '.').withLeadingZeroForDecimal()
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

    private fun QuantityParts.withExplicitUnit(explicitUnit: String?): QuantityParts {
        if (explicitUnit == null) return this

        val normalizedExplicitUnit = explicitUnit.normalizePantryUnit()
        return if (unit == "count" && normalizedExplicitUnit !in knownQuantityUnits) {
            this
        } else {
            copy(unit = explicitUnit).expandDozenUnit()
        }
    }

    private fun QuantityParts.withQuantityText(quantityText: String?): QuantityParts {
        val normalizedQuantityText = quantityText?.normalizeQuantityText()
        val normalizedUnit = unit?.normalizePantryUnit()

        return when {
            unit.isDozenUnit() -> copy(
                value = value?.times(DOZEN_COUNT) ?: DOZEN_COUNT,
                unit = "count"
            )
            normalizedQuantityText.isDozenQuantityText() && unit.isNullOrBlank() -> copy(unit = "count")
            normalizedQuantityText.isDozenQuantityText() && normalizedUnit !in knownQuantityUnits -> copy(unit = "count")
            else -> this
        }
    }

    private fun QuantityParts.expandDozenUnit(): QuantityParts {
        return if (unit.isDozenUnit()) {
            copy(
                value = value?.times(DOZEN_COUNT) ?: DOZEN_COUNT,
                unit = "count"
            )
        } else {
            this
        }
    }

    private fun String?.normalizeQuantityText(): String? {
        return this
            ?.trim()
            ?.lowercase()
            ?.removePrefix("a ")
            ?.removePrefix("an ")
            ?.replace(Regex("""\s+"""), " ")
            ?.ifBlank { null }
    }

    private fun String?.isDozenQuantityText(): Boolean {
        val normalized = normalizeQuantityText() ?: return false
        return normalized == "dozen" || normalized.endsWith(" dozen")
    }

    private fun String?.isDozenUnit(): Boolean {
        val normalized = this
            ?.trim()
            ?.lowercase()
            ?.trim('.', ',', ';', ':')
            ?.ifBlank { null }
            ?: return false
        return normalized == "dozen" || normalized == "dozens"
    }

    private fun String.toFlexibleDoubleOrNull(): Double? {
        val parsed = trim().replace(',', '.').withLeadingZeroForDecimal().toDoubleOrNull()
        return parsed?.takeIf { it.isFinite() }
    }

    private fun String.withLeadingZeroForDecimal(): String {
        return if (startsWith(".")) {
            "0$this"
        } else {
            this
        }
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
            "ct", "each", "ea", "item", "items", "counts", "pack", "packs", "package", "packages", "pk" -> "count"
            "milliliters", "milliliter" -> "ml"
            "liters", "liter" -> "l"
            "gallons", "gallon" -> "gal"
            "quarts", "quart" -> "qt"
            "pints", "pint" -> "pt"
            "dozen", "dozens" -> "count"
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
            "refrigerator", "refrigerated", "cold storage", "cold" -> "fridge"
            "deep freezer", "deep freeze", "frozen" -> "freezer"
            "cabinet", "cupboard", "shelf", "shelf stable", "shelf-stable", "room temp", "room temperature" -> "pantry"
            else -> normalized
        }
    }

    private fun JsonElement.toStringList(): List<String> {
        return when {
            isJsonNull -> emptyList()
            isJsonArray -> asJsonArray.mapNotNull { element ->
                element.asFlexibleStringOrNull()
            }
            else -> listOfNotNull(asFlexibleStringOrNull())
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
        private const val DOZEN_COUNT = 12.0
        private const val MAX_STATUS_DETAIL_LENGTH = 180
        private const val ONE_PIXEL_PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII="
        private val quantityPattern = Regex("""(\d+\s*/\s*\d+|(?:\d+)?[.,]\d+|\d+)\s*([A-Za-z]+)?""")
        private val wordQuantityPattern = Regex(
            """\b(one|two|three|four|five|six|seven|eight|nine|ten|half|dozen)\b\s*([A-Za-z]+)?""",
            RegexOption.IGNORE_CASE
        )
        private val knownQuantityUnits = setOf(
            "can",
            "jar",
            "box",
            "bag",
            "bottle",
            "container",
            "cup",
            "lb",
            "oz",
            "g",
            "kg",
            "count",
            "ml",
            "l",
            "gal",
            "qt",
            "pt"
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
            "half" to 0.5,
            "dozen" to DOZEN_COUNT
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
