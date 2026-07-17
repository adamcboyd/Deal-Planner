package com.dealplanner.domain

import com.dealplanner.data.model.DealItem
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class ShoppingListExportFormatterTest {

    private val formatter = ShoppingListExportFormatter()

    @Test
    fun `build report formats shopping list rows and total`() {
        val report = formatter.buildReport(
            items = listOf(
                shoppingItem(
                    name = "Chicken Breast Family Pack",
                    store = "Kroger",
                    brand = "Simple Truth",
                    sizeText = "4 lb",
                    quantity = 3.5,
                    estimatedCost = 10.45,
                    pricePerUnit = 2.99,
                    couponFlag = true,
                    limit = 2,
                    purpose = "2026-07-16 lunch protein"
                ),
                shoppingItem(
                    name = "Broccoli Crowns",
                    store = "Aldi",
                    quantity = 1.0,
                    estimatedCost = 1.99,
                    pricePerUnit = 1.99,
                    purpose = "2026-07-16 lunch vegetable"
                )
            ),
            generatedOn = LocalDate.of(2026, 7, 16)
        )

        assertThat(report.title).isEqualTo("Deal Planner Shopping List")
        assertThat(report.generatedOnText).isEqualTo("2026-07-16")
        assertThat(report.totalEstimatedCostText).isEqualTo("$12.44")
        assertThat(report.rows.map { it.name })
            .containsExactly("Chicken Breast Family Pack", "Broccoli Crowns")
            .inOrder()
        assertThat(report.rows[0].quantityText).isEqualTo("3.5 lb")
        assertThat(report.rows[0].estimatedCostText).isEqualTo("$10.45")
        assertThat(report.rows[0].detailLines).contains("Coupon: clip before checkout")
        assertThat(report.rows[0].detailLines).contains("Limit: 2")
        assertThat(report.rows[0].detailLines)
            .contains("Brand: Simple Truth | Size: 4 lb")
    }

    @Test
    fun `build report uses shopping defaults for missing deal details`() {
        val report = formatter.buildReport(
            items = listOf(
                shoppingItem(
                    name = "",
                    store = "",
                    brand = null,
                    sizeText = null,
                    unit = null,
                    quantity = 1.0,
                    estimatedCost = 3.0,
                    price = 3.0,
                    pricePerUnit = 0.0,
                    purpose = ""
                )
            ),
            generatedOn = LocalDate.of(2026, 7, 16)
        )

        assertThat(report.rows.single().name).isEqualTo("Unnamed item")
        assertThat(report.rows.single().quantityText).isEqualTo("1 unit")
        assertThat(report.rows.single().detailLines)
            .containsExactly(
                "Store: Unknown",
                "Brand: Any | Size: N/A",
                "Price: $3.00/unit | Deal: per_pound",
                "For: meal plan"
            )
            .inOrder()
    }

    private fun shoppingItem(
        name: String,
        store: String,
        quantity: Double,
        estimatedCost: Double,
        purpose: String,
        brand: String? = null,
        sizeText: String? = null,
        unit: String? = "lb",
        price: Double = 0.0,
        pricePerUnit: Double = 0.0,
        couponFlag: Boolean = false,
        limit: Int? = null
    ): MealPlanningEngine.ShoppingListItem {
        return MealPlanningEngine.ShoppingListItem(
            dealItem = DealItem(
                name = name,
                brand = brand,
                sizeText = sizeText,
                price = price,
                unit = unit,
                dealType = "per_pound",
                limit = limit,
                couponFlag = couponFlag,
                store = store,
                pricePerUnit = pricePerUnit
            ),
            quantity = quantity,
            estimatedCost = estimatedCost,
            purpose = purpose
        )
    }
}
