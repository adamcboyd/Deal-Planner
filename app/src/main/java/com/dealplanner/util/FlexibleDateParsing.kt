package com.dealplanner.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val flexibleDateFormats = listOf(
    DateTimeFormatter.ISO_LOCAL_DATE,
    DateTimeFormatter.ofPattern("M/d/yyyy"),
    DateTimeFormatter.ofPattern("M/d/yy"),
    DateTimeFormatter.ofPattern("MM/dd/yyyy"),
    DateTimeFormatter.ofPattern("MM/dd/yy"),
    DateTimeFormatter.ofPattern("M-d-yyyy"),
    DateTimeFormatter.ofPattern("M-d-yy"),
    DateTimeFormatter.ofPattern("MM-dd-yyyy"),
    DateTimeFormatter.ofPattern("MM-dd-yy"),
    DateTimeFormatter.ofPattern("yyyy/M/d"),
    DateTimeFormatter.ofPattern("yyyy-M-d")
)

fun String.toFlexibleLocalDateOrNull(): LocalDate? {
    val cleaned = trim().trim('.', ',', ';', ':')
    if (cleaned.isBlank()) return null

    return flexibleDateFormats.firstNotNullOfOrNull { formatter ->
        try {
            LocalDate.parse(cleaned, formatter)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
