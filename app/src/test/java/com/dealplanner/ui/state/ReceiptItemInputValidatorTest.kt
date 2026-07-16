package com.dealplanner.ui.state

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class ReceiptItemInputValidatorTest {

    @Test
    fun `optional quantity accepts blank and flexible non negative values`() {
        val blank = ReceiptItemInputValidator.validateOptionalQuantity("")
        assertThat(blank.isValid).isTrue()
        assertThat(blank.parsedValue).isNull()

        assertValidNumber(ReceiptItemInputValidator.validateOptionalQuantity("1,5"), 1.5)
        assertValidNumber(ReceiptItemInputValidator.validateOptionalQuantity(".5"), 0.5)
        assertValidNumber(ReceiptItemInputValidator.validateOptionalQuantity(",5"), 0.5)
        assertValidNumber(ReceiptItemInputValidator.validateOptionalQuantity("0"), 0.0)
    }

    @Test
    fun `optional quantity rejects invalid negative and non finite values`() {
        assertInvalidNumber(ReceiptItemInputValidator.validateOptionalQuantity("abc"))
        assertInvalidNumber(ReceiptItemInputValidator.validateOptionalQuantity("-1"))
        assertInvalidNumber(ReceiptItemInputValidator.validateOptionalQuantity("NaN"))
        assertInvalidNumber(ReceiptItemInputValidator.validateOptionalQuantity("Infinity"))
    }

    @Test
    fun `total cost accepts flexible non negative values`() {
        assertValidNumber(ReceiptItemInputValidator.validateTotalCost("1,78"), 1.78)
        assertValidNumber(ReceiptItemInputValidator.validateTotalCost(".89"), 0.89)
        assertValidNumber(ReceiptItemInputValidator.validateTotalCost("0"), 0.0)
    }

    @Test
    fun `total cost rejects blank invalid negative and non finite values`() {
        assertInvalidNumber(ReceiptItemInputValidator.validateTotalCost(""))
        assertInvalidNumber(ReceiptItemInputValidator.validateTotalCost("abc"))
        assertInvalidNumber(ReceiptItemInputValidator.validateTotalCost("-1"))
        assertInvalidNumber(ReceiptItemInputValidator.validateTotalCost("Infinity"))
    }

    @Test
    fun `match id accepts blank and non negative whole numbers`() {
        val blank = ReceiptItemInputValidator.validateMatchedItemId("")
        assertThat(blank.isValid).isTrue()
        assertThat(blank.parsedValue).isNull()

        val matchId = ReceiptItemInputValidator.validateMatchedItemId("42")
        assertThat(matchId.isValid).isTrue()
        assertThat(matchId.parsedValue).isEqualTo(42L)
    }

    @Test
    fun `match id rejects negative decimal and invalid text`() {
        assertThat(ReceiptItemInputValidator.validateMatchedItemId("-1").isValid).isFalse()
        assertThat(ReceiptItemInputValidator.validateMatchedItemId("1.5").isValid).isFalse()
        assertThat(ReceiptItemInputValidator.validateMatchedItemId("abc").isValid).isFalse()
    }

    @Test
    fun `confidence accepts only zero through one`() {
        assertValidNumber(ReceiptItemInputValidator.validateConfidence("0"), 0.0)
        assertValidNumber(ReceiptItemInputValidator.validateConfidence(".75"), 0.75)
        assertValidNumber(ReceiptItemInputValidator.validateConfidence("0,82"), 0.82)
        assertValidNumber(ReceiptItemInputValidator.validateConfidence("1"), 1.0)

        assertInvalidNumber(ReceiptItemInputValidator.validateConfidence("abc"))
        assertInvalidNumber(ReceiptItemInputValidator.validateConfidence("-0.1"))
        assertInvalidNumber(ReceiptItemInputValidator.validateConfidence("7"))
        assertInvalidNumber(ReceiptItemInputValidator.validateConfidence("Infinity"))
    }

    @Test
    fun `date accepts only iso local dates`() {
        val valid = ReceiptItemInputValidator.validateDate("2025-10-27")
        assertThat(valid.isValid).isTrue()
        assertThat(valid.parsedValue).isEqualTo(LocalDate.of(2025, 10, 27))

        assertThat(ReceiptItemInputValidator.validateDate("").isValid).isFalse()
        assertThat(ReceiptItemInputValidator.validateDate("10/27/2025").isValid).isFalse()
        assertThat(ReceiptItemInputValidator.validateDate("abc").isValid).isFalse()
    }

    private fun assertValidNumber(result: NumericInputValidation, expected: Double) {
        assertThat(result.isValid).isTrue()
        assertThat(result.parsedValue).isEqualTo(expected)
    }

    private fun assertInvalidNumber(result: NumericInputValidation) {
        assertThat(result.isValid).isFalse()
    }
}
