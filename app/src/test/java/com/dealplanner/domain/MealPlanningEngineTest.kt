package com.dealplanner.domain

import com.google.common.truth.Truth.assertThat
import com.dealplanner.data.model.*
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
    fun `generate meal plan uses one row per requested date`() {
        val startDate = LocalDate.of(2026, 7, 16)
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
            )
        )

        val result = engine.generateMealPlan(
            MealPlanningEngine.MealPlanRequest(
                params = params,
                pantryItems = pantryItems,
                deals = deals,
                startDate = startDate,
                daysToGenerate = 7
            )
        )

        assertThat(result.mealPlans.map { it.date })
            .containsExactly(
                startDate,
                startDate.plusDays(1),
                startDate.plusDays(2),
                startDate.plusDays(3),
                startDate.plusDays(4),
                startDate.plusDays(5),
                startDate.plusDays(6)
            )
            .inOrder()
    }

    @Test
    fun `generate meal plan is deterministic for same inputs`() {
        val request = MealPlanningEngine.MealPlanRequest(
            params = Params(),
            pantryItems = listOf(
                PantryItem(item = "rice", qty = 5.0, unit = "lb"),
                PantryItem(item = "pasta", qty = 2.0, unit = "lb"),
                PantryItem(item = "oats", qty = 2.0, unit = "lb")
            ),
            deals = listOf(
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
                    name = "Pork Shoulder",
                    price = 3.99,
                    unit = "lb",
                    dealType = "per_pound",
                    store = "Kroger",
                    dealScore = 0.85,
                    pricePerUnit = 3.99
                ),
                DealItem(
                    name = "Broccoli",
                    price = 1.99,
                    unit = "lb",
                    dealType = "per_pound",
                    store = "Kroger",
                    dealScore = 0.7,
                    pricePerUnit = 1.99
                ),
                DealItem(
                    name = "Carrots",
                    price = 1.49,
                    unit = "lb",
                    dealType = "per_pound",
                    store = "Kroger",
                    dealScore = 0.65,
                    pricePerUnit = 1.49
                )
            ),
            startDate = LocalDate.of(2026, 7, 16),
            daysToGenerate = 7
        )

        val first = engine.generateMealPlan(request)
        val second = engine.generateMealPlan(request)

        assertThat(second.mealPlans.map { it.slots })
            .containsExactlyElementsIn(first.mealPlans.map { it.slots })
            .inOrder()
        assertThat(second.shoppingList.map { it.dealItem.name to it.quantity })
            .containsExactlyElementsIn(first.shoppingList.map { it.dealItem.name to it.quantity })
            .inOrder()
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

    @Test
    fun `shopping list keeps different unpersisted deals separate`() {
        val request = MealPlanningEngine.MealPlanRequest(
            params = Params(),
            pantryItems = listOf(PantryItem(item = "rice", qty = 5.0, unit = "lb")),
            deals = listOf(
                DealItem(
                    name = "Chicken Breast",
                    price = 2.99,
                    unit = "lb",
                    dealType = "per_pound",
                    store = "Kroger",
                    dealScore = 0.9,
                    pricePerUnit = 2.99
                ),
                DealItem(
                    name = "Pork Shoulder",
                    price = 3.99,
                    unit = "lb",
                    dealType = "per_pound",
                    store = "Kroger",
                    dealScore = 0.8,
                    pricePerUnit = 3.99
                )
            ),
            daysToGenerate = 2
        )

        val result = engine.generateMealPlan(request)

        assertThat(result.shoppingList.map { it.dealItem.name })
            .containsAtLeast("Chicken Breast", "Pork Shoulder")
    }

    @Test
    fun `shopping list estimated cost uses planned quantity and price per unit`() {
        val request = MealPlanningEngine.MealPlanRequest(
            params = Params(),
            pantryItems = listOf(PantryItem(item = "rice", qty = 5.0, unit = "lb")),
            deals = listOf(
                DealItem(
                    name = "Chicken Breast Family Pack 4 lb",
                    price = 10.0,
                    unit = "lb",
                    dealType = "per_pound",
                    store = "Kroger",
                    dealScore = 0.9,
                    pricePerUnit = 2.5
                )
            ),
            startDate = LocalDate.of(2026, 7, 16),
            daysToGenerate = 1
        )

        val result = engine.generateMealPlan(request)

        val chickenItem = result.shoppingList.single { it.dealItem.name == "Chicken Breast Family Pack 4 lb" }
        assertThat(chickenItem.quantity).isEqualTo(1.0)
        assertThat(chickenItem.estimatedCost).isWithin(0.001).of(2.5)
        assertThat(result.shoppingList.sumOf { it.estimatedCost }).isWithin(0.001).of(2.5)
    }
}
