package com.dealplanner.ui.state

import com.dealplanner.util.toFlexibleDoubleOrNull

object PantryItemInputValidator {
    const val QUANTITY_ERROR = "Use a non-negative number like 1.5 or 1,5."

    fun validateQuantity(value: String): NumericInputValidation {
        val parsed = value.toFlexibleDoubleOrNull()
        return NumericInputValidation(
            parsedValue = parsed,
            isValid = parsed != null && parsed >= 0.0,
            message = QUANTITY_ERROR
        )
    }
}
