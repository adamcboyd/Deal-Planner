package com.dealplanner.ui.state

import com.dealplanner.util.toFlexibleDoubleOrNull

object DealItemInputValidator {
    const val PRICE_ERROR = "Use a non-negative price like 2.99 or 2,99."
    const val LIMIT_ERROR = "Use a whole number limit or leave blank."
    const val PRICE_PER_UNIT_DISCOUNT_ERROR = "Use non-negative PPU and percent off from 0 to 100."
    const val SCORE_CONFIDENCE_ERROR = "Use values from 0 to 1 for score and confidence."

    fun validateNonNegativePrice(value: String): NumericInputValidation {
        return validateDouble(value, PRICE_ERROR) { parsed -> parsed >= 0.0 }
    }

    fun validatePricePerUnit(value: String): NumericInputValidation {
        return validateDouble(value, PRICE_PER_UNIT_DISCOUNT_ERROR) { parsed -> parsed >= 0.0 }
    }

    fun validateDiscountPercent(value: String): NumericInputValidation {
        return validateDouble(value, PRICE_PER_UNIT_DISCOUNT_ERROR) { parsed -> parsed in 0.0..100.0 }
    }

    fun validateScore(value: String): NumericInputValidation {
        return validateDouble(value, SCORE_CONFIDENCE_ERROR) { parsed -> parsed in 0.0..1.0 }
    }

    fun validateConfidence(value: String): NumericInputValidation {
        return validateDouble(value, SCORE_CONFIDENCE_ERROR) { parsed -> parsed in 0.0..1.0 }
    }

    fun validateLimit(value: String): IntegerInputValidation {
        val trimmed = value.trim()
        if (trimmed.isBlank()) {
            return IntegerInputValidation(parsedValue = null, isValid = true, message = LIMIT_ERROR)
        }

        val parsed = trimmed.toIntOrNull()
        return IntegerInputValidation(
            parsedValue = parsed,
            isValid = parsed != null && parsed >= 0,
            message = LIMIT_ERROR
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
}

data class IntegerInputValidation(
    val parsedValue: Int?,
    val isValid: Boolean,
    val message: String
)
