package com.dealplanner.util

object DietaryRestrictions {
    fun parse(value: String?): List<String> {
        return value.orEmpty()
            .split(Regex("""[,;\r\n]+"""))
            .map { normalizeTerm(it) }
            .filter { it.length >= MIN_TERM_LENGTH && it !in emptyTerms }
            .distinct()
    }

    fun normalizeForStorage(value: String?): String? {
        return parse(value)
            .joinToString(", ")
            .ifBlank { null }
    }

    fun matchesAny(text: String, restrictions: List<String>): Boolean {
        val normalizedText = text.lowercase()
        return restrictions.any { restriction -> normalizedText.contains(restriction) }
    }

    private fun normalizeTerm(value: String): String {
        return value
            .trim()
            .lowercase()
            .removePrefix("no ")
            .removePrefix("avoid ")
            .removePrefix("exclude ")
            .trim()
    }

    private val emptyTerms = setOf("none", "na", "n/a")
    private const val MIN_TERM_LENGTH = 2
}
