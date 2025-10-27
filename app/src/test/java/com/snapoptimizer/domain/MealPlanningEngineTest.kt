package com.snapoptimizer.domain

import com.google.common.truth.Truth.assertThat
import com.snapoptimizer.data.model.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class MealPlanningEngineTest {

    private lateinit var engine: MealPlanningEngine

    @Before
    fun setup() {
        engine = MealPlanningEngine()
    }

    @Test
    fun `generate meal plan with defaults`() {
        val params = Params()
        val pantryItems = listOf(
            PantryItem(item = "rice", qty = 5.0, unit = "lb"),
            PantryItem(item = "oats", qty = 2.0, unit = "lb")
        )
        val deals = listOf(
            DealItem(
                name = "Chicken Breast",
                price = 2.99,
                unit = "lb",
                dealType = "per_pound",
                store = "Kroger",
                dealScore = 0.8,
                pricePerUnit = 2.99
            ),
            DealItem(
                name = "Broccoli",
                price = 1.99,
                unit = "lb",
                dealType = "per_pound",
                store = "Kroger",
                dealScore = 0.7,
                pricePerUnit = 1.99
            )
        )

        val request = MealPlanningEngine.MealPlanRequest(
            params = params,
            pantryItems = pantryItems,
            deals = deals,
            daysToGenerate = 7
        )

        val result = engine.generateMealPlan(request)

        assertThat(result.mealPlans).hasSize(7)
        assertThat(result.shoppingList).isNotEmpty()
    }

    @Test
    fun `filter GERD-friendly foods`() {
        val params = Params(gerdFriendly = true)
        val pantryItems = listOf(
            PantryItem(item = "rice", qty = 5.0, unit = "lb")
        )
        val deals = listOf(
            DealItem(
                name = "Tomatoes",
                price = 1.49,
                unit = "lb",
                dealType = "per_pound",
                store = "Kroger",
                dealScore = 0.8,
                pricePerUnit = 1.49
            ),
            DealItem(
                name = "Broccoli",
                price = 1.99,
                unit = "lb",
                dealType = "per_pound",
                store = "Kroger",
                dealScore = 0.7,
                pricePerUnit = 1.99
            )
        )

        val request = MealPlanningEngine.MealPlanRequest(
            params = params,
            pantryItems = pantryItems,
            deals = deals
        )

        val result = engine.generateMealPlan(request)

        // Tomatoes should be filtered out for GERD-friendly
        val usedVegetables = result.mealPlans.flatMap { it.slots }.mapNotNull { it.veg }
        assertThat(usedVegetables).doesNotContain("Tomatoes")
    }

    @Test
    fun `avoid peppers when specified`() {
        val params = Params(avoidPeppers = true)
        val pantryItems = listOf(
            PantryItem(item = "rice", qty = 5.0, unit = "lb")
        )
        val deals = listOf(
            DealItem(
                name = "Bell Peppers",
                price = 2.49,
                unit = "lb",
                dealType = "per_pound",
                store = "Kroger",
                dealScore = 0.8,
                pricePerUnit = 2.49
            ),
            DealItem(
                name = "Broccoli",
                price = 1.99,
                unit = "lb",
                dealType = "per_pound",
                store = "Kroger",
                dealScore = 0.7,
                pricePerUnit = 1.99
            )
        )

        val request = MealPlanningEngine.MealPlanRequest(
            params = params,
            pantryItems = pantryItems,
            deals = deals
        )

        val result = engine.generateMealPlan(request)

        val usedVegetables = result.mealPlans.flatMap { it.slots }.mapNotNull { it.veg }
        assertThat(usedVegetables).doesNotContain("Bell Peppers")
    }

    @Test
    fun `check fridge days warning`() {
        val oldItem = PantryItem(
            item = "chicken",
            qty = 2.0,
            unit = "lb",
            location = "fridge",
            opened = LocalDate.now().minusDays(4)
        )

        val warnings = engine.checkFridgeDays(listOf(oldItem))

        assertThat(warnings).hasSize(1)
        assertThat(warnings[0].item).isEqualTo("chicken")
    }

    @Test
    fun `consolidate shopping list`() {
        val params = Params()
        val pantryItems = listOf(
            PantryItem(item = "rice", qty = 5.0, unit = "lb")
        )
        val deal = DealItem(
            name = "Chicken Breast",
            price = 2.99,
            unit = "lb",
            dealType = "per_pound",
            store = "Kroger",
            dealScore = 0.8,
            pricePerUnit = 2.99
        )
        val deals = listOf(deal)

        val request = MealPlanningEngine.MealPlanRequest(
            params = params,
            pantryItems = pantryItems,
            deals = deals,
            daysToGenerate = 7
        )

        val result = engine.generateMealPlan(request)

        // Shopping list should consolidate quantities
        val chickenItems = result.shoppingList.filter { it.dealItem.name == "Chicken Breast" }
        assertThat(chickenItems).hasSize(1)
        assertThat(chickenItems[0].quantity).isGreaterThan(0.0)
    }
}
