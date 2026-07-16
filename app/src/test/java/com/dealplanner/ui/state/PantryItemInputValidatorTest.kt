package com.dealplanner.ui.state

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class PantryItemInputValidatorTest {

    @Test
    fun `pantry quantity accepts dot comma and leading decimal values`() {
        assertValidQuantity("1.5", 1.5)
        assertValidQuantity("1,5", 1.5)
        assertValidQuantity(".5", 0.5)
        assertValidQuantity(",5", 0.5)
    }

    @Test
    fun `pantry quantity accepts zero`() {
        assertValidQuantity("0", 0.0)
    }

    @Test
    fun `pantry quantity rejects negative values`() {
        assertInvalidQuantity("-1")
        assertInvalidQuantity("-.5")
    }

    @Test
    fun `pantry quantity rejects blank invalid and non finite values`() {
        assertInvalidQuantity("")
        assertInvalidQuantity("abc")
        assertInvalidQuantity("NaN")
        assertInvalidQuantity("Infinity")
        assertInvalidQuantity("-Infinity")
    }

    @Test
    fun `pantry best by date accepts blank and flexible local date formats`() {
        val blank = PantryItemInputValidator.validateBestByDate("")
        val iso = PantryItemInputValidator.validateBestByDate("2026-12-31")
        val slash = PantryItemInputValidator.validateBestByDate("12/31/2026")
        val shortDash = PantryItemInputValidator.validateBestByDate("12-31-26")
        val yearFirstSlash = PantryItemInputValidator.validateBestByDate("2026/12/31")

        assertThat(blank.isValid).isTrue()
        assertThat(blank.parsedValue).isNull()
        assertValidBestByDate(iso)
        assertValidBestByDate(slash)
        assertValidBestByDate(shortDash)
        assertValidBestByDate(yearFirstSlash)
    }

    @Test
    fun `pantry best by date rejects invalid text`() {
        val result = PantryItemInputValidator.validateBestByDate("not a date")

        assertThat(result.isValid).isFalse()
        assertThat(result.message).isEqualTo(PantryItemInputValidator.BEST_BY_DATE_ERROR)
    }

    private fun assertValidQuantity(value: String, expected: Double) {
        val result = PantryItemInputValidator.validateQuantity(value)

        assertThat(result.isValid).isTrue()
        assertThat(result.parsedValue).isEqualTo(expected)
        assertThat(result.message).isEqualTo(PantryItemInputValidator.QUANTITY_ERROR)
    }

    private fun assertInvalidQuantity(value: String) {
        val result = PantryItemInputValidator.validateQuantity(value)

        assertThat(result.isValid).isFalse()
        assertThat(result.message).isEqualTo(PantryItemInputValidator.QUANTITY_ERROR)
    }

    private fun assertValidBestByDate(result: DateInputValidation) {
        assertThat(result.isValid).isTrue()
        assertThat(result.parsedValue).isEqualTo(LocalDate.of(2026, 12, 31))
        assertThat(result.message).isEqualTo(PantryItemInputValidator.BEST_BY_DATE_ERROR)
    }
}
