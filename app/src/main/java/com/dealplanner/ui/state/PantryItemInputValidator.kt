package com.dealplanner.ui.state

import com.dealplanner.util.toFlexibleDoubleOrNull
import com.dealplanner.util.toFlexibleLocalDateOrNull

object PantryItemInputValidator {
    const val QUANTITY_ERROR = "Use a non-negative number like 1.5 or 1,5."
    const val BEST_BY_DATE_ERROR = "Use YYYY-MM-DD, M/D/YYYY, or M-D-YY; or leave blank."

    fun validateQuantity(value: String): NumericInputValidation {
        val parsed = value.toFlexibleDoubleOrNull()
        return NumericInputValidation(
            parsedValue = parsed,
            isValid = parsed != null && parsed >= 0.0,
            message = QUANTITY_ERROR
        )
    }

    fun validateBestByDate(value: String): DateInputValidation {
        val parsed = value.toFlexibleLocalDateOrNull()
        return DateInputValidation(
            parsedValue = parsed,
            isValid = value.isBlank() || parsed != null,
            message = BEST_BY_DATE_ERROR
        )
    }
}
