package com.dealplanner.parser

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class DealsParserTest {

    private lateinit var parser: DealsParser

    @Before
    fun setup() {
        parser = DealsParser()
    }

    @Test
    fun `parse price per pound deal`() {
        val text = "Chicken Breast\n$2.99/lb"
        val result = parser.parse(text, "Kroger")

        assertThat(result.deals).hasSize(1)
        val deal = result.deals[0]
        assertThat(deal.price).isEqualTo(2.99)
        assertThat(deal.unit).isEqualTo("lb")
        assertThat(deal.dealType).isEqualTo("per_pound")
    }

    @Test
    fun `parse per pound flyer prices when OCR drops slash`() {
        val text = """
            Chicken Breast
            ${'$'}2.99 lb

            Ground Beef
            2,99 per pound

            Roma Tomatoes
            99c lb

            Yellow Onions
            3 lb bag ${'$'}2.99
        """.trimIndent()

        val result = parser.parse(text, "Kroger")

        assertThat(result.deals).hasSize(4)

        val chicken = result.deals.first { it.name == "Chicken Breast" }
        assertThat(chicken.price).isEqualTo(2.99)
        assertThat(chicken.unit).isEqualTo("lb")
        assertThat(chicken.dealType).isEqualTo("per_pound")

        val beef = result.deals.first { it.name == "Ground Beef" }
        assertThat(beef.price).isEqualTo(2.99)
        assertThat(beef.unit).isEqualTo("lb")
        assertThat(beef.dealType).isEqualTo("per_pound")

        val tomatoes = result.deals.first { it.name == "Roma Tomatoes" }
        assertThat(tomatoes.price).isEqualTo(0.99)
        assertThat(tomatoes.unit).isEqualTo("lb")
        assertThat(tomatoes.dealType).isEqualTo("per_pound")

        val onions = result.deals.first { it.name == "Yellow Onions" }
        assertThat(onions.price).isEqualTo(2.99)
        assertThat(onions.unit).isEqualTo("ea")
        assertThat(onions.dealType).isEqualTo("per_unit")
        assertThat(onions.sizeText).isEqualTo("3 lb")
    }

    @Test
    fun `parse N for X deal`() {
        val text = "Kroger Pasta\n2 for $5"
        val result = parser.parse(text, "Kroger")

        assertThat(result.deals).hasSize(1)
        val deal = result.deals[0]
        assertThat(deal.price).isEqualTo(2.5)
        assertThat(deal.dealType).isEqualTo("n_for_x")
    }

    @Test
    fun `normalize store names for imported flyer deals`() {
        val text = "Black Beans\n$0.99"

        val result = parser.parse(text, " Kroger ")
        val unknownResult = parser.parse(text, "   ")

        assertThat(result.deals).hasSize(1)
        assertThat(result.deals.first().store).isEqualTo("Kroger")
        assertThat(unknownResult.deals).hasSize(1)
        assertThat(unknownResult.deals.first().store).isEqualTo("Unknown")
    }

    @Test
    fun `parse slash style N for X deal`() {
        val text = """
            Kroger Pasta 16 oz
            2/$5

            Black Beans
            10 / $10
        """.trimIndent()

        val result = parser.parse(text, "Kroger")

        assertThat(result.deals).hasSize(2)

        val pasta = result.deals.first { it.name == "Kroger Pasta 16 oz" }
        assertThat(pasta.price).isEqualTo(2.5)
        assertThat(pasta.dealType).isEqualTo("n_for_x")

        val beans = result.deals.first { it.name == "Black Beans" }
        assertThat(beans.price).isEqualTo(1.0)
        assertThat(beans.dealType).isEqualTo("n_for_x")
    }

    @Test
    fun `ignore flyer dates that look like slash multi-buy prices`() {
        val text = """
            Kroger Weekly Ad
            Valid 7/16/2026 - 7/22/2026

            Kroger Pasta
            2/$5
        """.trimIndent()

        val result = parser.parse(text, "Kroger")

        assertThat(result.deals).hasSize(1)
        assertThat(result.deals[0].name).isEqualTo("Kroger Pasta")
        assertThat(result.deals[0].price).isEqualTo(2.5)
        assertThat(result.deals[0].dealType).isEqualTo("n_for_x")
    }

    @Test
    fun `parse buy N get M deal`() {
        val text = "Olive Oil\nBuy 2 Get 1 Free"
        val result = parser.parse(text, "Kroger")

        assertThat(result.deals).hasSize(1)
        val deal = result.deals[0]
        assertThat(deal.dealType).isEqualTo("buy_n_get_m")
        assertThat(deal.discountPercent).isWithin(0.1).of(33.3)
    }

    @Test
    fun `parse buy get deals with word numbers`() {
        val text = """
            Bagels
            Buy One Get One Free

            Sparkling Water
            Buy Two Get One Free
        """.trimIndent()

        val result = parser.parse(text, "Kroger")

        assertThat(result.deals).hasSize(2)

        val bagels = result.deals.first { it.name == "Bagels" }
        assertThat(bagels.dealType).isEqualTo("buy_n_get_m")
        assertThat(bagels.discountPercent).isEqualTo(50.0)

        val sparklingWater = result.deals.first { it.name == "Sparkling Water" }
        assertThat(sparklingWater.dealType).isEqualTo("buy_n_get_m")
        assertThat(sparklingWater.discountPercent).isWithin(0.1).of(33.3)
    }

    @Test
    fun `parse buy get percent off flyer promos`() {
        val text = """
            Coffee Creamer
            Buy One Get One 50% off

            Seltzer
            Buy Two Get One 50% off
        """.trimIndent()

        val result = parser.parse(text, "Kroger")

        assertThat(result.deals).hasSize(2)

        val coffeeCreamer = result.deals.first { it.name == "Coffee Creamer" }
        assertThat(coffeeCreamer.dealType).isEqualTo("buy_n_get_m")
        assertThat(coffeeCreamer.discountPercent).isEqualTo(25.0)

        val seltzer = result.deals.first { it.name == "Seltzer" }
        assertThat(seltzer.dealType).isEqualTo("buy_n_get_m")
        assertThat(seltzer.discountPercent).isWithin(0.1).of(16.7)
    }

    @Test
    fun `parse BOGO flyer shorthand deals`() {
        val text = """
            Greek Yogurt
            BOGO Free

            Granola Bars
            B1G1
        """.trimIndent()

        val result = parser.parse(text, "Kroger")

        assertThat(result.deals).hasSize(2)

        val yogurt = result.deals.first { it.name == "Greek Yogurt" }
        assertThat(yogurt.dealType).isEqualTo("buy_n_get_m")
        assertThat(yogurt.discountPercent).isEqualTo(50.0)
        assertThat(yogurt.sizeText).isNull()

        val granolaBars = result.deals.first { it.name == "Granola Bars" }
        assertThat(granolaBars.dealType).isEqualTo("buy_n_get_m")
        assertThat(granolaBars.discountPercent).isEqualTo(50.0)
        assertThat(granolaBars.sizeText).isNull()
    }

    @Test
    fun `parse BOGO percent off second item flyer shorthand`() {
        val text = """
            Coffee Creamer
            BOGO 50% off
        """.trimIndent()

        val result = parser.parse(text, "Kroger")

        assertThat(result.deals).hasSize(1)
        val deal = result.deals.first()
        assertThat(deal.name).isEqualTo("Coffee Creamer")
        assertThat(deal.dealType).isEqualTo("buy_n_get_m")
        assertThat(deal.discountPercent).isEqualTo(25.0)
        assertThat(deal.sizeText).isNull()
    }

    @Test
    fun `parse percent off deal`() {
        val text = "Mandarin Oranges\n25% off"
        val result = parser.parse(text, "Kroger")

        assertThat(result.deals).hasSize(1)
        val deal = result.deals[0]
        assertThat(deal.dealType).isEqualTo("percent_off")
        assertThat(deal.discountPercent).isEqualTo(25.0)
    }

    @Test
    fun `detect coupon flag`() {
        val text = "Chicken Breast\n$2.99/lb\nMember Price"
        val result = parser.parse(text, "Kroger")

        assertThat(result.deals).hasSize(1)
        assertThat(result.deals[0].couponFlag).isTrue()
    }

    @Test
    fun `extract limit`() {
        val text = "Pork Shoulder\n$3.99/lb\nLimit 2"
        val result = parser.parse(text, "Kroger")

        assertThat(result.deals).hasSize(1)
        assertThat(result.deals[0].limit).isEqualTo(2)
    }

    @Test
    fun `calculate deal score`() {
        val text = "Pork Shoulder\n$3.99/lb\n30% off"
        val result = parser.parse(text, "Kroger")

        assertThat(result.deals).hasSize(1)
        val deal = result.deals[0]
        assertThat(deal.dealScore).isGreaterThan(0.0)
        assertThat(deal.dealScore).isLessThan(1.0)
    }

    @Test
    fun `parse multiple deals from flyer`() {
        val text = """
            Chicken Breast
            $2.99/lb

            Broccoli
            $1.99/lb

            Pasta
            10 for $10
        """.trimIndent()

        val result = parser.parse(text, "Kroger")

        assertThat(result.deals).hasSize(3)
    }

    @Test
    fun `parse package price deal`() {
        val text = "Yellow Onions\n3 lb bag $2.99"
        val result = parser.parse(text, "Kroger")

        assertThat(result.deals).hasSize(1)
        val deal = result.deals[0]
        assertThat(deal.name).isEqualTo("Yellow Onions")
        assertThat(deal.sizeText).isEqualTo("3 lb")
        assertThat(deal.price).isEqualTo(2.99)
        assertThat(deal.dealType).isEqualTo("per_unit")
        assertThat(deal.store).isEqualTo("Kroger")
    }

    @Test
    fun `parse flyer prices when OCR drops dollar signs`() {
        val text = """
            Chicken Breast
            2.99/lb

            Kroger Pasta
            10 for 10

            Yellow Onions
            3 lb bag 2.99

            Store Pasta
            16.00 oz
            1.49
        """.trimIndent()

        val result = parser.parse(text, "Kroger")

        assertThat(result.deals).hasSize(4)

        val chicken = result.deals.first { it.name == "Chicken Breast" }
        assertThat(chicken.price).isEqualTo(2.99)
        assertThat(chicken.unit).isEqualTo("lb")
        assertThat(chicken.dealType).isEqualTo("per_pound")

        val pasta = result.deals.first { it.name == "Kroger Pasta" }
        assertThat(pasta.price).isEqualTo(1.0)
        assertThat(pasta.dealType).isEqualTo("n_for_x")

        val onions = result.deals.first { it.name == "Yellow Onions" }
        assertThat(onions.price).isEqualTo(2.99)
        assertThat(onions.sizeText).isEqualTo("3 lb")

        val pastaSize = result.deals.first { it.name == "Store Pasta 16.00 oz" }
        assertThat(pastaSize.price).isEqualTo(1.49)
        assertThat(pastaSize.sizeText).isEqualTo("16.00 oz")
    }

    @Test
    fun `parse flyer prices when OCR uses comma decimals`() {
        val text = """
            Chicken Breast
            2,99/lb

            Kroger Pasta
            2 for 5,00

            Yellow Onions
            3 lb bag 2,99
        """.trimIndent()

        val result = parser.parse(text, "Kroger")

        assertThat(result.deals).hasSize(3)

        val chicken = result.deals.first { it.name == "Chicken Breast" }
        assertThat(chicken.price).isEqualTo(2.99)
        assertThat(chicken.unit).isEqualTo("lb")

        val pasta = result.deals.first { it.name == "Kroger Pasta" }
        assertThat(pasta.price).isEqualTo(2.5)
        assertThat(pasta.dealType).isEqualTo("n_for_x")

        val onions = result.deals.first { it.name == "Yellow Onions" }
        assertThat(onions.price).isEqualTo(2.99)
        assertThat(onions.sizeText).isEqualTo("3 lb")
    }

    @Test
    fun `parse flyer prices when OCR omits leading zero`() {
        val text = """
            Chicken Thighs
            .99/lb

            Roma Tomatoes
            .99 lb

            Black Beans
            .89

            Kroger Pasta
            2 for .99

            Seltzer
            2/.99
        """.trimIndent()

        val result = parser.parse(text, "Kroger")

        assertThat(result.deals).hasSize(5)

        val chicken = result.deals.first { it.name == "Chicken Thighs" }
        assertThat(chicken.price).isEqualTo(0.99)
        assertThat(chicken.unit).isEqualTo("lb")
        assertThat(chicken.dealType).isEqualTo("per_pound")

        val tomatoes = result.deals.first { it.name == "Roma Tomatoes" }
        assertThat(tomatoes.price).isEqualTo(0.99)
        assertThat(tomatoes.unit).isEqualTo("lb")
        assertThat(tomatoes.dealType).isEqualTo("per_pound")

        val beans = result.deals.first { it.name == "Black Beans" }
        assertThat(beans.price).isEqualTo(0.89)
        assertThat(beans.unit).isEqualTo("ea")

        val pasta = result.deals.first { it.name == "Kroger Pasta" }
        assertThat(pasta.price).isEqualTo(0.495)
        assertThat(pasta.dealType).isEqualTo("n_for_x")

        val seltzer = result.deals.first { it.name == "Seltzer" }
        assertThat(seltzer.price).isEqualTo(0.495)
        assertThat(seltzer.dealType).isEqualTo("n_for_x")
    }

    @Test
    fun `parse cent style flyer prices`() {
        val text = """
            Roma Tomatoes
            99¢/lb

            Black Beans
            88c
        """.trimIndent()

        val result = parser.parse(text, "Kroger")

        assertThat(result.deals).hasSize(2)

        val tomatoes = result.deals.first { it.name == "Roma Tomatoes" }
        assertThat(tomatoes.price).isEqualTo(0.99)
        assertThat(tomatoes.unit).isEqualTo("lb")
        assertThat(tomatoes.dealType).isEqualTo("per_pound")

        val beans = result.deals.first { it.name == "Black Beans" }
        assertThat(beans.price).isEqualTo(0.88)
        assertThat(beans.unit).isEqualTo("ea")
        assertThat(beans.dealType).isEqualTo("per_unit")
    }

    @Test
    fun `parse phone checklist cent price text`() {
        val text = """
            Roma Tomatoes
            99c/lb
        """.trimIndent()

        val result = parser.parse(text, "Kroger")

        assertThat(result.deals).hasSize(1)
        val tomatoes = result.deals.first()
        assertThat(tomatoes.name).isEqualTo("Roma Tomatoes")
        assertThat(tomatoes.price).isEqualTo(0.99)
        assertThat(tomatoes.unit).isEqualTo("lb")
        assertThat(tomatoes.dealType).isEqualTo("per_pound")
        assertThat(tomatoes.store).isEqualTo("Kroger")
    }

    @Test
    fun `parse demo flyer style multiline modifiers`() {
        val text = """
            Pork Shoulder Roast
            $3.99/lb
            Family Pack
            Limit 2

            Chicken Breast
            Boneless Skinless
            $2.99/lb
            Member Price

            Olive Oil
            Extra Virgin
            24 oz $6.99
            Buy 2 Get 1 Free
        """.trimIndent()

        val result = parser.parse(text, "Kroger")

        assertThat(result.deals).hasSize(3)

        val pork = result.deals.first { it.name.contains("Pork Shoulder") }
        assertThat(pork.limit).isEqualTo(2)
        assertThat(pork.price).isEqualTo(3.99)

        val chicken = result.deals.first { it.name.contains("Chicken Breast") }
        assertThat(chicken.name).contains("Boneless Skinless")
        assertThat(chicken.couponFlag).isTrue()

        val oliveOil = result.deals.first { it.name.contains("Olive Oil") }
        assertThat(oliveOil.price).isEqualTo(6.99)
        assertThat(oliveOil.dealType).isEqualTo("buy_n_get_m")
        assertThat(oliveOil.discountPercent).isWithin(0.1).of(33.3)
    }

    @Test
    fun `parse bundled demo flyer deal names`() {
        val text = java.io.File("src/main/assets/demo_flyer.txt").readText()

        val result = parser.parse(text, "Kroger")

        assertThat(result.deals.map { it.name }).containsAtLeast(
            "Pork Shoulder Roast",
            "Chicken Breast Boneless Skinless",
            "85% Lean Ground Beef",
            "Mandarin Oranges",
            "Kroger Pasta 16 oz",
            "Black Beans 15 oz can",
            "Olive Oil Extra Virgin"
        )
        assertThat(result.deals.all { it.store == "Kroger" }).isTrue()
    }
}
