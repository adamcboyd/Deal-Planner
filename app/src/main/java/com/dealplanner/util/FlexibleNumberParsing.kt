package com.dealplanner.util

fun String.toFlexibleDoubleOrNull(): Double? {
    val normalized = trim()
        .replace(',', '.')

    val parsed = normalized
        .withLeadingZeroForDecimal()
        .toDoubleOrNull()

    return parsed?.takeIf { it.isFinite() }
}

private fun String.withLeadingZeroForDecimal(): String {
    return when {
        startsWith(".") -> "0$this"
        startsWith("-.") -> "-0.${drop(2)}"
        startsWith("+.") -> "+0.${drop(2)}"
        else -> this
    }
}
