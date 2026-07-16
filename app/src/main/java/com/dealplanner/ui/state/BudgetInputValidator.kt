package com.dealplanner.ui.state

import com.dealplanner.util.toFlexibleDoubleOrNull

object BudgetInputValidator {
    const val BUDGET_NUMBER_ERROR = "Use a non-negative number like 292 or 292,50."

    fun validateBudgetNumber(value: String): NumericInputValidation {
        val parsed = value.toFlexibleDoubleOrNull()
        return NumericInputValidation(
            parsedValue = parsed,
            isValid = parsed != null && parsed >= 0.0,
            message = BUDGET_NUMBER_ERROR
        )
    }
}
