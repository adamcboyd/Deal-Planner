package com.dealplanner.parser

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class PantryPhraseParserTest {

    private lateinit var parser: PantryPhraseParser

    @Before
    fun setup() {
        parser = PantryPhraseParser()
    }

    @Test
    fun `parse simple item`() {
        val result = parser.parse("rice")

        assertThat(result.item.item).isEqualTo("rice")
        assertThat(result.item.qty).isEqualTo(1.0)
        assertThat(result.confidence).isGreaterThan(0.7)
    }

    @Test
    fun `parse item with quantity`() {
        val result = parser.parse("2 cans black beans")

        assertThat(result.item.item).isEqualTo("black beans")
        assertThat(result.item.qty).isEqualTo(2.0)
        assertThat(result.item.unit).isEqualTo("can")
    }

    @Test
    fun `parse item with size`() {
        val result = parser.parse("peanut butter 16oz")

        assertThat(result.item.item).contains("peanut butter")
        assertThat(result.item.size).isEqualTo("16oz")
        assertThat(result.item.unit).isEqualTo("oz")
    }

    @Test
    fun `parse item with brand`() {
        val result = parser.parse("Great Value peanut butter")

        assertThat(result.item.item).contains("peanut butter")
        assertThat(result.item.brand).isEqualTo("Great Value")
    }

    @Test
    fun `parse item with location`() {
        val result = parser.parse("milk in fridge")

        assertThat(result.item.item).isEqualTo("milk")
        assertThat(result.item.location).isEqualTo("fridge")
    }

    @Test
    fun `parse item with fractional quantity`() {
        val result = parser.parse("1.5 lb ground beef")

        assertThat(result.item.item).isEqualTo("ground beef")
        assertThat(result.item.qty).isEqualTo(1.5)
        assertThat(result.item.unit).isEqualTo("lb")
    }

    @Test
    fun `parse item with fraction word`() {
        val result = parser.parse("half lb butter")

        assertThat(result.item.item).isEqualTo("butter")
        assertThat(result.item.qty).isEqualTo(0.5)
        assertThat(result.item.unit).isEqualTo("lb")
    }

    @Test
    fun `parse item with form`() {
        val result = parser.parse("frozen peas")

        assertThat(result.item.item).isEqualTo("peas")
        assertThat(result.item.form).isEqualTo("frozen")
    }

    @Test
    fun `parse item with opened date`() {
        val result = parser.parse("milk opened yesterday")

        assertThat(result.item.item).isEqualTo("milk")
        assertThat(result.item.opened).isEqualTo(LocalDate.now().minusDays(1))
    }

    @Test
    fun `parse item with best by date`() {
        val today = LocalDate.now()
        val dateStr = "${today.monthValue}/${today.dayOfMonth}/${today.year}"
        val result = parser.parse("yogurt best by $dateStr")

        assertThat(result.item.item).isEqualTo("yogurt")
        assertThat(result.item.bestBy).isEqualTo(today)
    }

    @Test
    fun `parse complex item`() {
        val result = parser.parse("2 cans Great Value black beans 15oz in pantry")

        assertThat(result.item.item).contains("black beans")
        assertThat(result.item.qty).isEqualTo(2.0)
        assertThat(result.item.unit).isEqualTo("can")
        assertThat(result.item.size).isEqualTo("15oz")
        assertThat(result.item.brand).isEqualTo("Great Value")
        assertThat(result.item.location).isEqualTo("pantry")
    }

    @Test
    fun `detect duplicates`() {
        val item1 = parser.parse("rice 5lb").item
        val item2 = parser.parse("rice 5lb").item

        assertThat(parser.areDuplicates(item1, item2)).isTrue()
    }

    @Test
    fun `merge duplicates`() {
        val item1 = parser.parse("rice 5lb").item.copy(qty = 2.0)
        val item2 = parser.parse("rice 5lb").item.copy(qty = 3.0)

        val merged = parser.mergeDuplicates(listOf(item1, item2))

        assertThat(merged).hasSize(1)
        assertThat(merged[0].qty).isEqualTo(5.0)
    }

    @Test
    fun `low confidence for unclear input`() {
        val result = parser.parse("xyz")

        assertThat(result.item.needsVerify).isTrue()
        assertThat(result.confidence).isLessThan(0.7)
    }
}
