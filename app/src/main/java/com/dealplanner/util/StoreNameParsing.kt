package com.dealplanner.util

fun String.toStoreNameOrUnknown(): String {
    return trim().ifBlank { "Unknown" }
}
