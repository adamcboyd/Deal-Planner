package com.dealplanner.ui.state

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BudgetInputValidatorTest {

    @Test
    fun `budget fields accept dot comma and leading decimal values`() {
        assertValidBudgetNumber("292.50", 292.50)
        assertValidBudgetNumber("292,50", 292.50)
        assertValidBudgetNumber(".55", 0.55)
        assertValidBudgetNumber(",55", 0.55)
    }

    @Test
    fun `budget fields accept zero`() {
        assertValidBudgetNumber("0", 0.0)
    }

    @Test
    fun `budget fields reject negative values`() {
        assertInvalidBudgetNumber("-1")
        assertInvalidBudgetNumber("-.55")
    }

    @Test
    fun `budget fields reject blank invalid and non finite values`() {
        assertInvalidBudgetNumber("")
        assertInvalidBudgetNumber("abc")
        assertInvalidBudgetNumber("NaN")
        assertInvalidBudgetNumber("Infinity")
        assertInvalidBudgetNumber("-Infinity")
    }

    private fun assertValidBudgetNumber(value: String, expected: Double) {
        val result = BudgetInputValidator.validateBudgetNumber(value)

        assertThat(result.isValid).isTrue()
        assertThat(result.parsedValue).isEqualTo(expected)
        assertThat(result.message).isEqualTo(BudgetInputValidator.BUDGET_NUMBER_ERROR)
    }

    private fun assertInvalidBudgetNumber(value: String) {
        val result = BudgetInputValidator.validateBudgetNumber(value)

        assertThat(result.isValid).isFalse()
        assertThat(result.message).isEqualTo(BudgetInputValidator.BUDGET_NUMBER_ERROR)
    }
}
