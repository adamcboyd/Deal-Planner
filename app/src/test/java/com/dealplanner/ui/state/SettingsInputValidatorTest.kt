package com.dealplanner.ui.state

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SettingsInputValidatorTest {

    @Test
    fun `protein per meal accepts dot comma and leading decimal values`() {
        assertValidProtein("0.5", 0.5)
        assertValidProtein("0,5", 0.5)
        assertValidProtein(".5", 0.5)
        assertValidProtein(",5", 0.5)
    }

    @Test
    fun `protein per meal accepts zero`() {
        assertValidProtein("0", 0.0)
    }

    @Test
    fun `protein per meal rejects negative values`() {
        assertInvalidProtein("-0.5")
        assertInvalidProtein("-.5")
    }

    @Test
    fun `protein per meal rejects blank invalid and non finite values`() {
        assertInvalidProtein("")
        assertInvalidProtein("abc")
        assertInvalidProtein("NaN")
        assertInvalidProtein("Infinity")
        assertInvalidProtein("-Infinity")
    }

    private fun assertValidProtein(value: String, expected: Double) {
        val result = SettingsInputValidator.validateProteinPerMeal(value)

        assertThat(result.isValid).isTrue()
        assertThat(result.parsedValue).isEqualTo(expected)
        assertThat(result.message).isEqualTo(SettingsInputValidator.PROTEIN_PER_MEAL_HELP)
    }

    private fun assertInvalidProtein(value: String) {
        val result = SettingsInputValidator.validateProteinPerMeal(value)

        assertThat(result.isValid).isFalse()
        assertThat(result.message).isEqualTo(SettingsInputValidator.PROTEIN_PER_MEAL_ERROR)
    }
}
