package com.dealplanner.ui.state

import com.dealplanner.util.DietaryRestrictions
import com.dealplanner.util.toFlexibleDoubleOrNull

object SettingsInputValidator {
    const val PROTEIN_PER_MEAL_HELP = "Default: 0.5 lb per meal"
    const val PROTEIN_PER_MEAL_ERROR = "Use a non-negative number like 0.5 or 0,5."
    const val DIETARY_RESTRICTIONS_HELP = "Separate custom avoid terms with commas or lines."

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

    fun normalizeDietaryRestrictions(value: String): String? {
        return DietaryRestrictions.normalizeForStorage(value)
    }

    fun dietaryRestrictionsHelpText(value: String): String {
        val terms = DietaryRestrictions.parse(value)
        return if (terms.isEmpty()) {
            DIETARY_RESTRICTIONS_HELP
        } else {
            "Avoiding: ${terms.joinToString(", ")}"
        }
    }
}

data class NumericInputValidation(
    val parsedValue: Double?,
    val isValid: Boolean,
    val message: String
)
