package com.dealplanner.ui.state

import com.dealplanner.util.toFlexibleDoubleOrNull
import java.time.LocalDate
import java.time.format.DateTimeParseException

object ReceiptItemInputValidator {
    const val QUANTITY_TOTAL_ERROR = "Use non-negative quantity and total values, or leave quantity blank."
    const val MATCH_ID_ERROR = "Use a whole-number match ID or leave blank."
    const val CONFIDENCE_ERROR = "Use a confidence value from 0 to 1."
    const val DATE_ERROR = "Use YYYY-MM-DD."

    fun validateOptionalQuantity(value: String): NumericInputValidation {
        if (value.isBlank()) {
            return NumericInputValidation(parsedValue = null, isValid = true, message = QUANTITY_TOTAL_ERROR)
        }

        return validateDouble(value, QUANTITY_TOTAL_ERROR) { parsed -> parsed >= 0.0 }
    }

    fun validateTotalCost(value: String): NumericInputValidation {
        return validateDouble(value, QUANTITY_TOTAL_ERROR) { parsed -> parsed >= 0.0 }
    }

    fun validateMatchedItemId(value: String): LongInputValidation {
        val trimmed = value.trim()
        if (trimmed.isBlank()) {
            return LongInputValidation(parsedValue = null, isValid = true, message = MATCH_ID_ERROR)
        }

        val parsed = trimmed.toLongOrNull()
        return LongInputValidation(
            parsedValue = parsed,
            isValid = parsed != null && parsed >= 0L,
            message = MATCH_ID_ERROR
        )
    }

    fun validateConfidence(value: String): NumericInputValidation {
        return validateDouble(value, CONFIDENCE_ERROR) { parsed -> parsed in 0.0..1.0 }
    }

    fun validateDate(value: String): DateInputValidation {
        val parsed = value.toLocalDateOrNull()
        return DateInputValidation(
            parsedValue = parsed,
            isValid = parsed != null,
            message = DATE_ERROR
        )
    }

    private fun validateDouble(
        value: String,
        message: String,
        isValidValue: (Double) -> Boolean
    ): NumericInputValidation {
        val parsed = value.toFlexibleDoubleOrNull()
        return NumericInputValidation(
            parsedValue = parsed,
            isValid = parsed != null && isValidValue(parsed),
            message = message
        )
    }

    private fun String.toLocalDateOrNull(): LocalDate? {
        if (isBlank()) return null
        return try {
            LocalDate.parse(trim())
        } catch (_: DateTimeParseException) {
            null
        }
    }
}

data class LongInputValidation(
    val parsedValue: Long?,
    val isValid: Boolean,
    val message: String
)

data class DateInputValidation(
    val parsedValue: LocalDate?,
    val isValid: Boolean,
    val message: String
)
