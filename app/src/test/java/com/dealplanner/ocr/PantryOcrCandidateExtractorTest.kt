package com.dealplanner.ocr

import com.dealplanner.parser.PantryPhraseParser
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import java.time.LocalDate

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
    fun `bundled demo pantry labels parse into expected phone checklist rows`() {
        val text = File("src/main/assets/demo_pantry_labels.txt").readText()
        val parser = PantryPhraseParser()

        val candidates = PantryOcrCandidateExtractor.extractCandidates(text)
        val items = candidates.map { parser.parse(it).item }

        assertThat(candidates).containsExactly(
            "Great Value Black Beans net wt: 15 oz pantry best by: 12/31/2026",
            "Kroger Pasta 16 oz pantry exp: 12-31-26",
            "Great Value Peanut Butter 16-ounce pantry best-by 2027-03-04",
            "Kroger Eggs 12-count fridge use by 12-31-26",
            "Private Selection Salsa 16 oz fridge opened: 2026-07-01 use by: 12/31/2026"
        ).inOrder()

        val blackBeans = items[0]
        assertThat(blackBeans.item).isEqualTo("black beans")
        assertThat(blackBeans.brand).isEqualTo("Great Value")
        assertThat(blackBeans.size).isEqualTo("15oz")
        assertThat(blackBeans.location).isEqualTo("pantry")
        assertThat(blackBeans.bestBy).isEqualTo(LocalDate.of(2026, 12, 31))

        val pasta = items[1]
        assertThat(pasta.item).isEqualTo("pasta")
        assertThat(pasta.brand).isEqualTo("Kroger")
        assertThat(pasta.size).isEqualTo("16oz")
        assertThat(pasta.bestBy).isEqualTo(LocalDate.of(2026, 12, 31))

        val peanutButter = items[2]
        assertThat(peanutButter.item).isEqualTo("peanut butter")
        assertThat(peanutButter.brand).isEqualTo("Great Value")
        assertThat(peanutButter.size).isEqualTo("16oz")
        assertThat(peanutButter.bestBy).isEqualTo(LocalDate.of(2027, 3, 4))

        val eggs = items[3]
        assertThat(eggs.item).isEqualTo("eggs")
        assertThat(eggs.brand).isEqualTo("Kroger")
        assertThat(eggs.size).isEqualTo("12ct")
        assertThat(eggs.location).isEqualTo("fridge")
        assertThat(eggs.bestBy).isEqualTo(LocalDate.of(2026, 12, 31))

        val salsa = items[4]
        assertThat(salsa.item).isEqualTo("private selection salsa")
        assertThat(salsa.size).isEqualTo("16oz")
        assertThat(salsa.location).isEqualTo("fridge")
        assertThat(salsa.opened).isEqualTo(LocalDate.of(2026, 7, 1))
        assertThat(salsa.bestBy).isEqualTo(LocalDate.of(2026, 12, 31))
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
