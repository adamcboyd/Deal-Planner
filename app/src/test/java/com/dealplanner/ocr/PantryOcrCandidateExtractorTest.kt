package com.dealplanner.ocr

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PantryOcrCandidateExtractorTest {

    @Test
    fun `single label OCR falls back to one combined phrase`() {
        val text = """
            Great Value
            Black Beans
            15 oz
            Best By 2026-12-31
            Nutrition Facts
        """.trimIndent()

        val candidates = PantryOcrCandidateExtractor.extractCandidates(text)

        assertThat(candidates).containsExactly(
            "Great Value Black Beans 15 oz Best By 2026-12-31"
        )
    }

    @Test
    fun `multi item OCR keeps complete pantry lines separate`() {
        val text = """
            Great Value Black Beans 15 oz pantry
            NET WT 15 OZ
            Kroger Pasta 16 oz pantry
            Best By 2026-12-31
            Calories 120
        """.trimIndent()

        val candidates = PantryOcrCandidateExtractor.extractCandidates(text)

        assertThat(candidates).containsExactly(
            "Great Value Black Beans 15 oz pantry",
            "Kroger Pasta 16 oz pantry Best By 2026-12-31"
        ).inOrder()
    }

    @Test
    fun `multi item OCR attaches wrapped date continuation lines`() {
        val text = """
            Great Value Peanut Butter 16-ounce pantry best by
            2027-03-04
            Kroger Eggs 12-count fridge
            Best By
            2026-07-31
            Private Selection Salsa 16 oz fridge opened
            2026-07-01 best by 2026-08-15
        """.trimIndent()

        val candidates = PantryOcrCandidateExtractor.extractCandidates(text)

        assertThat(candidates).containsExactly(
            "Great Value Peanut Butter 16-ounce pantry best by 2027-03-04",
            "Kroger Eggs 12-count fridge Best By 2026-07-31",
            "Private Selection Salsa 16 oz fridge opened 2026-07-01 best by 2026-08-15"
        ).inOrder()
    }

    @Test
    fun `multi item OCR recognizes liquid package units`() {
        val text = """
            Kroger Milk 1 gal fridge
            Chicken Broth 1 qt pantry
            Cream 1 pint fridge
        """.trimIndent()

        val candidates = PantryOcrCandidateExtractor.extractCandidates(text)

        assertThat(candidates).containsExactly(
            "Kroger Milk 1 gal fridge",
            "Chicken Broth 1 qt pantry",
            "Cream 1 pint fridge"
        ).inOrder()
    }

    @Test
    fun `multi item OCR recognizes hyphenated package sizes`() {
        val text = """
            Great Value Peanut Butter 16-ounce
            Kroger Eggs 12-count
            Nutrition Facts
            Serving Size 2 tbsp
        """.trimIndent()

        val candidates = PantryOcrCandidateExtractor.extractCandidates(text)

        assertThat(candidates).containsExactly(
            "Great Value Peanut Butter 16-ounce",
            "Kroger Eggs 12-count"
        ).inOrder()
    }

    @Test
    fun `empty or ignored OCR lines return no candidates`() {
        val text = """
            Nutrition Facts
            Calories 120
            Serving Size 1 cup
            10%
        """.trimIndent()

        val candidates = PantryOcrCandidateExtractor.extractCandidates(text)

        assertThat(candidates).isEmpty()
    }
}
