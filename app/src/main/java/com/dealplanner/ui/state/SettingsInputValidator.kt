package com.dealplanner.ui.state

import com.dealplanner.util.toFlexibleDoubleOrNull

object SettingsInputValidator {
    const val PROTEIN_PER_MEAL_HELP = "Default: 0.5 lb per meal"
    const val PROTEIN_PER_MEAL_ERROR = "Use a non-negative number like 0.5 or 0,5."

    fun validateProteinPerMeal(value: String): NumericInputValidation {
        val parsed = value.toFlexibleDoubleOrNull()
        return NumericInputValidation(
            parsedValue = parsed,
            isValid = parsed != null && parsed >= 0.0,
            message = if (parsed != null && parsed >= 0.0) {
                PROTEIN_PER_MEAL_HELP
            } else {
                PROTEIN_PER_MEAL_ERROR
            }
        )
    }
}

data class NumericInputValidation(
    val parsedValue: Double?,
    val isValid: Boolean,
    val message: String
)
