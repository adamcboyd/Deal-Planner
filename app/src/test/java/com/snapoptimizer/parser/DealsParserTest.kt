package com.snapoptimizer.parser

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
    fun `parse N for X deal`() {
        val text = "Kroger Pasta\n2 for $5"
        val result = parser.parse(text, "Kroger")

        assertThat(result.deals).hasSize(1)
        val deal = result.deals[0]
        assertThat(deal.price).isEqualTo(2.5)
        assertThat(deal.dealType).isEqualTo("n_for_x")
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
}
