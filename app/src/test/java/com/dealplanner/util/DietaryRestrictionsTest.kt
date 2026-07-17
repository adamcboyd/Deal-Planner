package com.dealplanner.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DietaryRestrictionsTest {

    @Test
    fun `parse accepts comma semicolon and newline separated restrictions`() {
        val restrictions = DietaryRestrictions.parse("pork, shellfish\npeanuts; mango")

        assertThat(restrictions)
            .containsExactly("pork", "shellfish", "peanuts", "mango")
            .inOrder()
    }

    @Test
    fun `parse normalizes avoid prefixes and duplicate terms`() {
        val restrictions = DietaryRestrictions.parse("No pork, avoid shellfish, exclude pork")

        assertThat(restrictions)
            .containsExactly("pork", "shellfish")
            .inOrder()
    }

    @Test
    fun `normalizeForStorage writes a stable comma separated value`() {
        val normalized = DietaryRestrictions.normalizeForStorage(" No Pork \n avoid shellfish ")

        assertThat(normalized).isEqualTo("pork, shellfish")
    }

    @Test
    fun `normalizeForStorage treats blank and none as no custom restrictions`() {
        assertThat(DietaryRestrictions.normalizeForStorage("")).isNull()
        assertThat(DietaryRestrictions.normalizeForStorage("none, n/a")).isNull()
    }

    @Test
    fun `matchesAny checks normalized deal text`() {
        val restrictions = DietaryRestrictions.parse("pork, shellfish")

        assertThat(DietaryRestrictions.matchesAny("Pork Shoulder Family Pack", restrictions)).isTrue()
        assertThat(DietaryRestrictions.matchesAny("Chicken Breast", restrictions)).isFalse()
    }
}
