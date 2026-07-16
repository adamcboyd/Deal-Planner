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
    fun `parse dozen pantry quantities as count unless explicit unit follows`() {
        val eggs = parser.parse("a dozen eggs fridge")
        val cans = parser.parse("dozen cans black beans pantry")

        assertThat(eggs.item.item).isEqualTo("eggs")
        assertThat(eggs.item.qty).isEqualTo(12.0)
        assertThat(eggs.item.unit).isEqualTo("count")
        assertThat(eggs.item.location).isEqualTo("fridge")

        assertThat(cans.item.item).isEqualTo("black beans")
        assertThat(cans.item.qty).isEqualTo(12.0)
        assertThat(cans.item.unit).isEqualTo("can")
        assertThat(cans.item.location).isEqualTo("pantry")
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
    fun `parse pantry quantities and sizes when OCR uses comma decimals`() {
        val weightedItem = parser.parse("1,5 lb ground beef in freezer")
        val labelItem = parser.parse("Kroger yogurt 5,3oz fridge")

        assertThat(weightedItem.item.item).isEqualTo("ground beef")
        assertThat(weightedItem.item.qty).isEqualTo(1.5)
        assertThat(weightedItem.item.unit).isEqualTo("lb")
        assertThat(weightedItem.item.location).isEqualTo("freezer")

        assertThat(labelItem.item.item).isEqualTo("yogurt")
        assertThat(labelItem.item.brand).isEqualTo("Kroger")
        assertThat(labelItem.item.size).isEqualTo("5.3oz")
        assertThat(labelItem.item.unit).isEqualTo("oz")
        assertThat(labelItem.item.location).isEqualTo("fridge")
    }

    @Test
    fun `parse pantry quantities and sizes when OCR omits leading zero`() {
        val weightedItem = parser.parse(".5 lb ground beef in freezer")
        val labelItem = parser.parse("Kroger yogurt .75oz fridge")
        val measuredItem = parser.parse(".25 cups olive oil pantry")

        assertThat(weightedItem.item.item).isEqualTo("ground beef")
        assertThat(weightedItem.item.qty).isEqualTo(0.5)
        assertThat(weightedItem.item.unit).isEqualTo("lb")
        assertThat(weightedItem.item.location).isEqualTo("freezer")

        assertThat(labelItem.item.item).isEqualTo("yogurt")
        assertThat(labelItem.item.brand).isEqualTo("Kroger")
        assertThat(labelItem.item.size).isEqualTo("0.75oz")
        assertThat(labelItem.item.unit).isEqualTo("oz")
        assertThat(labelItem.item.location).isEqualTo("fridge")

        assertThat(measuredItem.item.item).isEqualTo("olive oil")
        assertThat(measuredItem.item.qty).isEqualTo(0.25)
        assertThat(measuredItem.item.unit).isEqualTo("cup")
        assertThat(measuredItem.item.location).isEqualTo("pantry")
    }

    @Test
    fun `parse common pantry container and count units`() {
        val oil = parser.parse("2 bottles olive oil 32 fl oz pantry")
        val milk = parser.parse("1 carton milk 64 fluid oz fridge")
        val eggs = parser.parse("Kroger eggs 12 ct fridge")

        assertThat(oil.item.item).isEqualTo("olive oil")
        assertThat(oil.item.qty).isEqualTo(2.0)
        assertThat(oil.item.unit).isEqualTo("bottle")
        assertThat(oil.item.size).isEqualTo("32 fl oz")
        assertThat(oil.item.location).isEqualTo("pantry")

        assertThat(milk.item.item).isEqualTo("milk")
        assertThat(milk.item.qty).isEqualTo(1.0)
        assertThat(milk.item.unit).isEqualTo("carton")
        assertThat(milk.item.size).isEqualTo("64 fl oz")
        assertThat(milk.item.location).isEqualTo("fridge")

        assertThat(eggs.item.item).isEqualTo("eggs")
        assertThat(eggs.item.brand).isEqualTo("Kroger")
        assertThat(eggs.item.qty).isEqualTo(1.0)
        assertThat(eggs.item.unit).isEqualTo("count")
        assertThat(eggs.item.size).isEqualTo("12ct")
        assertThat(eggs.item.location).isEqualTo("fridge")
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
    fun `parse opened and best by dates independently`() {
        val result = parser.parse("Great Value peanut butter opened yesterday best by 2026-12-31")

        assertThat(result.item.item).isEqualTo("peanut butter")
        assertThat(result.item.brand).isEqualTo("Great Value")
        assertThat(result.item.opened).isEqualTo(LocalDate.now().minusDays(1))
        assertThat(result.item.bestBy).isEqualTo(LocalDate.parse("2026-12-31"))
    }

    @Test
    fun `parse checklist pantry entry with iso best by date`() {
        val result = parser.parse("2 cans black beans 15oz pantry best by 2026-12-31")

        assertThat(result.item.item).isEqualTo("black beans")
        assertThat(result.item.qty).isEqualTo(2.0)
        assertThat(result.item.unit).isEqualTo("can")
        assertThat(result.item.size).isEqualTo("15oz")
        assertThat(result.item.location).isEqualTo("pantry")
        assertThat(result.item.bestBy).isEqualTo(LocalDate.parse("2026-12-31"))
        assertThat(result.item.needsVerify).isFalse()
    }

    @Test
    fun `parse label style expiration date cues`() {
        val bestBefore = parser.parse("Kroger yogurt best before 2026-12-31")
        val useBy = parser.parse("milk use by 12/31/2026")
        val exp = parser.parse("pasta exp 12/31/2026")

        assertThat(bestBefore.item.item).isEqualTo("yogurt")
        assertThat(bestBefore.item.brand).isEqualTo("Kroger")
        assertThat(bestBefore.item.bestBy).isEqualTo(LocalDate.parse("2026-12-31"))

        assertThat(useBy.item.item).isEqualTo("milk")
        assertThat(useBy.item.bestBy).isEqualTo(LocalDate.of(2026, 12, 31))

        assertThat(exp.item.item).isEqualTo("pasta")
        assertThat(exp.item.bestBy).isEqualTo(LocalDate.of(2026, 12, 31))
    }

    @Test
    fun `parse date label wording without leaking date into item name`() {
        val expirationDate = parser.parse("milk expiration date 12/31/2026")
        val bestByDate = parser.parse("yogurt best by date 2026-12-31")

        assertThat(expirationDate.item.item).isEqualTo("milk")
        assertThat(expirationDate.item.bestBy).isEqualTo(LocalDate.of(2026, 12, 31))

        assertThat(bestByDate.item.item).isEqualTo("yogurt")
        assertThat(bestByDate.item.bestBy).isEqualTo(LocalDate.parse("2026-12-31"))
    }

    @Test
    fun `parse opened on date wording without leaking on into item name`() {
        val result = parser.parse("Great Value peanut butter opened on 2026-07-01 best by date 2026-12-31")

        assertThat(result.item.item).isEqualTo("peanut butter")
        assertThat(result.item.brand).isEqualTo("Great Value")
        assertThat(result.item.opened).isEqualTo(LocalDate.parse("2026-07-01"))
        assertThat(result.item.bestBy).isEqualTo(LocalDate.parse("2026-12-31"))
    }

    @Test
    fun `parse hyphenated date label cues without leaking cue into item name`() {
        val useBy = parser.parse("milk use-by 12/31/2026")
        val bestBy = parser.parse("yogurt best-by 2026-12-31")

        assertThat(useBy.item.item).isEqualTo("milk")
        assertThat(useBy.item.bestBy).isEqualTo(LocalDate.of(2026, 12, 31))

        assertThat(bestBy.item.item).isEqualTo("yogurt")
        assertThat(bestBy.item.bestBy).isEqualTo(LocalDate.parse("2026-12-31"))
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
    fun `duplicate detection normalizes package size and generic brand`() {
        val typedItem = parser.parse("black beans 15oz").item
        val photoItem = parser.parse("black beans 15 oz").item.copy(brand = "Generic")

        assertThat(parser.areDuplicates(typedItem, photoItem)).isTrue()
    }

    @Test
    fun `duplicate detection keeps different locations separate`() {
        val pantryItem = parser.parse("1 lb ground beef in fridge").item
        val freezerItem = parser.parse("1 lb ground beef in freezer").item

        assertThat(parser.areDuplicates(pantryItem, freezerItem)).isFalse()
    }

    @Test
    fun `duplicate detection matches same barcode only`() {
        val firstScan = parser.parse("scanned barcode item").item.copy(
            notes = "Barcode: 012345678905; Product lookup: Open Food Facts"
        )
        val sameScan = parser.parse("scanned barcode item").item.copy(
            notes = "Barcode: 012345678905"
        )
        val differentScan = parser.parse("scanned barcode item").item.copy(
            notes = "Barcode: 999999999999"
        )

        assertThat(parser.areDuplicates(firstScan, sameScan)).isTrue()
        assertThat(parser.areDuplicates(firstScan, differentScan)).isFalse()
    }

    @Test
    fun `merge duplicates keeps different barcodes separate`() {
        val firstScan = parser.parse("scanned barcode item").item.copy(
            notes = "Barcode: 012345678905"
        )
        val differentScan = parser.parse("scanned barcode item").item.copy(
            notes = "Barcode: 999999999999"
        )

        val merged = parser.mergeDuplicates(listOf(firstScan, differentScan))

        assertThat(merged).hasSize(2)
    }

    @Test
    fun `merge duplicate items sums quantity and preserves review notes`() {
        val existing = parser.parse("2 cans black beans 15oz").item.copy(
            id = 42,
            notes = "Photo OCR import",
            needsVerify = true
        )
        val incoming = parser.parse("3 cans black beans 15 oz").item.copy(
            brand = "Generic",
            notes = "What is the expiration or best-by date?"
        )

        val merged = parser.mergeDuplicateItems(existing, incoming)

        assertThat(merged.id).isEqualTo(42)
        assertThat(merged.qty).isEqualTo(5.0)
        assertThat(merged.notes).contains("Photo OCR import")
        assertThat(merged.notes).contains("What is the expiration or best-by date?")
        assertThat(merged.needsVerify).isTrue()
    }

    @Test
    fun `low confidence for unclear input`() {
        val result = parser.parse("xyz")

        assertThat(result.item.needsVerify).isTrue()
        assertThat(result.confidence).isLessThan(0.7)
    }
}
