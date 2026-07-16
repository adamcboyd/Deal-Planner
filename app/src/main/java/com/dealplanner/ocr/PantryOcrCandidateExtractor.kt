package com.dealplanner.ocr

object PantryOcrCandidateExtractor {
    fun extractCandidates(ocrText: String): List<String> {
        val lines = ocrText
            .lines()
            .map { it.trim() }
            .filter { it.length >= 2 }
            .filterNot { it.isIgnoredLine() }
            .distinct()

        if (lines.isEmpty()) return emptyList()

        val standaloneItems = lines
            .filter { it.looksLikeStandaloneItemLine() }
            .take(MAX_CANDIDATES)

        if (standaloneItems.size >= 2) {
            return standaloneItems
        }

        return listOf(
            lines
                .take(MAX_SINGLE_LABEL_LINES)
                .joinToString(" ")
                .trim()
        ).filter { it.isNotBlank() }
    }

    private fun String.isIgnoredLine(): Boolean {
        val normalized = lowercase()
        return normalized.contains("nutrition") ||
            normalized.contains("calories") ||
            normalized.contains("serving") ||
            normalized.contains("ingredients") ||
            normalized.contains("distributed by") ||
            normalized.contains("allergen") ||
            normalized.matches(Regex("""\d+%"""))
    }

    private fun String.looksLikeStandaloneItemLine(): Boolean {
        val normalized = lowercase()
        return normalized.hasPantrySignal() && normalized.hasProductWord()
    }

    private fun String.hasPantrySignal(): Boolean {
        return amountOrSizePattern.containsMatchIn(this) ||
            dateCuePattern.containsMatchIn(this) ||
            locationPattern.containsMatchIn(this)
    }

    private fun String.hasProductWord(): Boolean {
        val words = Regex("""[a-z][a-z']+""")
            .findAll(this)
            .map { it.value.trim('\'') }
            .filter { it.length >= 2 }
            .filterNot { it in nonProductWords }
            .toList()

        return words.isNotEmpty()
    }

    private const val MAX_CANDIDATES = 6
    private const val MAX_SINGLE_LABEL_LINES = 8

    private val amountOrSizePattern = Regex(
        """\b(?:\d+\s*/\s*\d+|(?:\d+)?[.,]\d+|\d+)\s*(?:fl\.?\s*)?(?:oz|ounce|ounces|lb|lbs|pound|pounds|g|gram|grams|kg|ml|l|gal|gallon|gallons|qt|quart|quarts|pt|pint|pints|ct|count|can|cans|jar|jars|box|boxes|bag|bags|bottle|bottles|carton|cartons|container|containers|cup|cups|pack|packs|pkg|package|packages|ea|each)\b""",
        RegexOption.IGNORE_CASE
    )
    private val dateCuePattern = Regex(
        """\b(?:best\s*by|best\s*before|use\s*by|use-by|sell\s*by|sell-by|exp\.?|expiration)\b""",
        RegexOption.IGNORE_CASE
    )
    private val locationPattern = Regex(
        """\b(?:pantry|fridge|freezer|refrigerator|refrigerated|cabinet|cupboard|shelf)\b""",
        RegexOption.IGNORE_CASE
    )
    private val nonProductWords = setOf(
        "best",
        "before",
        "by",
        "use",
        "sell",
        "exp",
        "expiration",
        "date",
        "opened",
        "pantry",
        "fridge",
        "freezer",
        "refrigerator",
        "refrigerated",
        "cabinet",
        "cupboard",
        "shelf",
        "oz",
        "ounce",
        "ounces",
        "lb",
        "lbs",
        "pound",
        "pounds",
        "gram",
        "grams",
        "kg",
        "ml",
        "gal",
        "gallon",
        "gallons",
        "qt",
        "quart",
        "quarts",
        "pt",
        "pint",
        "pints",
        "count",
        "can",
        "cans",
        "jar",
        "jars",
        "box",
        "boxes",
        "bag",
        "bags",
        "bottle",
        "bottles",
        "carton",
        "cartons",
        "container",
        "containers",
        "cup",
        "cups",
        "pack",
        "packs",
        "pkg",
        "package",
        "packages",
        "ea",
        "each"
    )
}
