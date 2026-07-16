package com.dealplanner.ui.state

import com.google.common.truth.Truth.assertThat
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
}
