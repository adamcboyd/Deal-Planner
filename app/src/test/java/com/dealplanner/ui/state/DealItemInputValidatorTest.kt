package com.dealplanner.ui.state

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class DealItemInputValidatorTest {

    @Test
    fun `deal price accepts dot comma and leading decimal values`() {
        assertValidNumber(DealItemInputValidator.validateNonNegativePrice("2.99"), 2.99)
        assertValidNumber(DealItemInputValidator.validateNonNegativePrice("2,99"), 2.99)
        assertValidNumber(DealItemInputValidator.validateNonNegativePrice(".99"), 0.99)
        assertValidNumber(DealItemInputValidator.validateNonNegativePrice(",99"), 0.99)
    }

    @Test
    fun `deal price rejects invalid negative and non finite values`() {
        assertInvalidNumber(DealItemInputValidator.validateNonNegativePrice("abc"))
        assertInvalidNumber(DealItemInputValidator.validateNonNegativePrice("-1"))
        assertInvalidNumber(DealItemInputValidator.validateNonNegativePrice("NaN"))
        assertInvalidNumber(DealItemInputValidator.validateNonNegativePrice("Infinity"))
    }

    @Test
    fun `deal limit accepts blank and non negative whole numbers`() {
        val blank = DealItemInputValidator.validateLimit("")
        assertThat(blank.isValid).isTrue()
        assertThat(blank.parsedValue).isNull()

        val limit = DealItemInputValidator.validateLimit("2")
        assertThat(limit.isValid).isTrue()
        assertThat(limit.parsedValue).isEqualTo(2)
    }

    @Test
    fun `deal limit rejects negative decimal and invalid text`() {
        assertThat(DealItemInputValidator.validateLimit("-1").isValid).isFalse()
        assertThat(DealItemInputValidator.validateLimit("1.5").isValid).isFalse()
        assertThat(DealItemInputValidator.validateLimit("abc").isValid).isFalse()
    }

    @Test
    fun `price per unit accepts flexible non negative numbers`() {
        assertValidNumber(DealItemInputValidator.validatePricePerUnit("2,49"), 2.49)
        assertValidNumber(DealItemInputValidator.validatePricePerUnit(".99"), 0.99)
    }

    @Test
    fun `discount percent accepts only zero through one hundred`() {
        assertValidNumber(DealItemInputValidator.validateDiscountPercent("0"), 0.0)
        assertValidNumber(DealItemInputValidator.validateDiscountPercent("50,5"), 50.5)
        assertValidNumber(DealItemInputValidator.validateDiscountPercent("100"), 100.0)

        assertInvalidNumber(DealItemInputValidator.validateDiscountPercent("-1"))
        assertInvalidNumber(DealItemInputValidator.validateDiscountPercent("101"))
        assertInvalidNumber(DealItemInputValidator.validateDiscountPercent("abc"))
    }

    @Test
    fun `score and confidence accept only zero through one`() {
        assertValidNumber(DealItemInputValidator.validateScore("0"), 0.0)
        assertValidNumber(DealItemInputValidator.validateScore(".75"), 0.75)
        assertValidNumber(DealItemInputValidator.validateScore("1"), 1.0)
        assertValidNumber(DealItemInputValidator.validateConfidence("0,82"), 0.82)

        assertInvalidNumber(DealItemInputValidator.validateScore("-0.1"))
        assertInvalidNumber(DealItemInputValidator.validateScore("7"))
        assertInvalidNumber(DealItemInputValidator.validateConfidence("abc"))
        assertInvalidNumber(DealItemInputValidator.validateConfidence("Infinity"))
    }

    @Test
    fun `valid until date accepts blank and flexible local date formats`() {
        val blank = DealItemInputValidator.validateValidUntilDate("")
        val iso = DealItemInputValidator.validateValidUntilDate("2026-12-31")
        val slash = DealItemInputValidator.validateValidUntilDate("12/31/2026")
        val shortDash = DealItemInputValidator.validateValidUntilDate("12-31-26")
        val yearFirstSlash = DealItemInputValidator.validateValidUntilDate("2026/12/31")

        assertThat(blank.isValid).isTrue()
        assertThat(blank.parsedValue).isNull()
        assertValidDate(iso)
        assertValidDate(slash)
        assertValidDate(shortDash)
        assertValidDate(yearFirstSlash)
    }

    @Test
    fun `valid until date rejects invalid text`() {
        val result = DealItemInputValidator.validateValidUntilDate("not a date")

        assertThat(result.isValid).isFalse()
        assertThat(result.message).isEqualTo(DealItemInputValidator.VALID_UNTIL_DATE_ERROR)
    }

    private fun assertValidNumber(result: NumericInputValidation, expected: Double) {
        assertThat(result.isValid).isTrue()
        assertThat(result.parsedValue).isEqualTo(expected)
    }

    private fun assertInvalidNumber(result: NumericInputValidation) {
        assertThat(result.isValid).isFalse()
    }

    private fun assertValidDate(result: DateInputValidation) {
        assertThat(result.isValid).isTrue()
        assertThat(result.parsedValue).isEqualTo(LocalDate.of(2026, 12, 31))
        assertThat(result.message).isEqualTo(DealItemInputValidator.VALID_UNTIL_DATE_ERROR)
    }
}
