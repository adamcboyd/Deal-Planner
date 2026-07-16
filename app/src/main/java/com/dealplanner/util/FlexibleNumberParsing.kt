package com.dealplanner.util

fun String.toFlexibleDoubleOrNull(): Double? {
    return trim()
        .replace(',', '.')
        .toDoubleOrNull()
}
