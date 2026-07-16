package com.dealplanner.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FlexibleNumberParsingTest {

    @Test
    fun `parse dot comma and integer numeric text`() {
        assertThat("1.5".toFlexibleDoubleOrNull()).isEqualTo(1.5)
        assertThat("1,5".toFlexibleDoubleOrNull()).isEqualTo(1.5)
        assertThat("2".toFlexibleDoubleOrNull()).isEqualTo(2.0)
    }

    @Test
    fun `parse numeric text with surrounding whitespace`() {
        assertThat("  2,49  ".toFlexibleDoubleOrNull()).isEqualTo(2.49)
    }

    @Test
    fun `reject blank non numeric and ambiguous thousands text`() {
        assertThat("".toFlexibleDoubleOrNull()).isNull()
        assertThat("not a number".toFlexibleDoubleOrNull()).isNull()
        assertThat("1,234.56".toFlexibleDoubleOrNull()).isNull()
    }
}
