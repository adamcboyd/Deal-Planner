package com.dealplanner.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class FlexibleDateParsingTest {

    @Test
    fun `parse iso slash and dash label dates`() {
        assertThat("2026-12-31".toFlexibleLocalDateOrNull()).isEqualTo(LocalDate.of(2026, 12, 31))
        assertThat("12/31/2026".toFlexibleLocalDateOrNull()).isEqualTo(LocalDate.of(2026, 12, 31))
        assertThat("12-31-2026".toFlexibleLocalDateOrNull()).isEqualTo(LocalDate.of(2026, 12, 31))
        assertThat("2026/12/31".toFlexibleLocalDateOrNull()).isEqualTo(LocalDate.of(2026, 12, 31))
        assertThat("2026-7-1".toFlexibleLocalDateOrNull()).isEqualTo(LocalDate.of(2026, 7, 1))
    }

    @Test
    fun `parse two digit year label dates`() {
        assertThat("12/31/26".toFlexibleLocalDateOrNull()).isEqualTo(LocalDate.of(2026, 12, 31))
        assertThat("12-31-26".toFlexibleLocalDateOrNull()).isEqualTo(LocalDate.of(2026, 12, 31))
    }

    @Test
    fun `parse dates with surrounding whitespace and punctuation`() {
        assertThat(" 12/31/2026. ".toFlexibleLocalDateOrNull()).isEqualTo(LocalDate.of(2026, 12, 31))
    }

    @Test
    fun `reject blank and non date text`() {
        assertThat("".toFlexibleLocalDateOrNull()).isNull()
        assertThat("best by soon".toFlexibleLocalDateOrNull()).isNull()
    }
}
