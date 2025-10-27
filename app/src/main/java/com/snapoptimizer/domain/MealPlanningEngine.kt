package com.snapoptimizer.domain

import com.snapoptimizer.data.model.*
import java.time.LocalDate
import kotlin.math.ceil

/**
 * Rules-based meal planning engine.
 * Core principle: Params → Deals + Pantry → Meals. "Deals drive the meals."
 *
 * Builds 7-day meal plans using:
 * - Parameters (GERD-friendly, avoid peppers, protein per meal)
 * - Pantry anchors (rice, pasta, oats)
 * - Top-scored deals for vegetables and proteins
 *
 * No LLM required - pure rule-based logic.
 */
class MealPlanningEngine {

    data class MealPlanRequest(
        val params: Params,
        val pantryItems: List<PantryItem>,
        val deals: List<DealItem>,
        val startDate: LocalDate = LocalDate.now(),
        val daysToGenerate: Int = 7
    )

    data class MealPlanResult(
        val mealPlans: List<MealPlan>,
        val shoppingList: List<ShoppingListItem>,
        val warnings: List<String>
    )

    data class ShoppingListItem(
        val dealItem: DealItem,
        val quantity: Double,
        val purpose: String // e.g., "Monday dinner protein"
    )

    private val gerdFriendlyVegetables = setOf(
        "broccoli", "cauliflower", "green beans", "carrots", "spinach",
        "kale", "sweet potato", "squash", "zucchini", "cucumber", "lettuce"
    )

    private val acidicVegetables = setOf(
        "tomato", "pepper", "onion", "garlic"
    )

    private val breakfastAnchors = setOf(
        "oats", "oatmeal", "cereal", "eggs", "bread", "bagel"
    )

    private val starchAnchors = setOf(
        "rice", "pasta", "bread", "potato", "quinoa", "couscous"
    )

    private val proteinKeywords = setOf(
        "chicken", "beef", "pork", "salmon", "tilapia", "turkey", "ground"
    )

    fun generateMealPlan(request: MealPlanRequest): MealPlanResult {
        val warnings = mutableListOf<String>()
        val mealPlans = mutableListOf<MealPlan>()
        val shoppingList = mutableListOf<ShoppingListItem>()

        // 1. Find pantry anchors
        val pantryStarches = findPantryAnchors(request.pantryItems, starchAnchors)
        val pantryBreakfastItems = findPantryAnchors(request.pantryItems, breakfastAnchors)

        if (pantryStarches.isEmpty()) {
            warnings.add("No starch anchors found in pantry (rice, pasta, etc.). Consider adding.")
        }

        // 2. Filter deals based on params
        val suitableDeals = filterDealsByParams(request.deals, request.params)

        // 3. Select top protein deals
        val proteinDeals = suitableDeals
            .filter { deal -> proteinKeywords.any { deal.name.lowercase().contains(it) } }
            .sortedByDescending { it.dealScore }
            .take(3)

        if (proteinDeals.isEmpty()) {
            warnings.add("No protein deals found. Meal planning may be limited.")
        }

        // 4. Select top vegetable deals
        val vegDeals = suitableDeals
            .filter { deal ->
                !proteinKeywords.any { deal.name.lowercase().contains(it) } &&
                !starchAnchors.any { deal.name.lowercase().contains(it) }
            }
            .sortedByDescending { it.dealScore }
            .take(4)

        // 5. Generate daily meal plans
        for (dayOffset in 0 until request.daysToGenerate) {
            val date = request.startDate.plusDays(dayOffset.toLong())
            val slots = mutableListOf<MealSlot>()

            // Breakfast
            if (request.params.breakfastAnchor && pantryBreakfastItems.isNotEmpty()) {
                val breakfastItem = pantryBreakfastItems.random()
                slots.add(
                    MealSlot(
                        mealType = "breakfast",
                        starch = breakfastItem.item,
                        starchQty = 1.0,
                        starchUnit = "serving",
                        estimatedCost = 0.55
                    )
                )
            }

            // Lunch & Dinner
            listOf("lunch", "dinner").forEach { mealType ->
                val proteinDeal = proteinDeals.randomOrNull()
                val vegDeal = vegDeals.randomOrNull()
                val starch = pantryStarches.randomOrNull()

                if (proteinDeal != null) {
                    val proteinQty = request.params.proteinPerMealLb
                    val vegQty = 0.5 // Default 0.5 lb vegetables per meal
                    val starchQty = 0.5 // Default serving

                    // Calculate if we need freezer directives
                    val freezerDirective = calculateFreezerDirective(
                        proteinDeal,
                        proteinQty,
                        request.daysToGenerate
                    )

                    slots.add(
                        MealSlot(
                            mealType = mealType,
                            protein = proteinDeal.name,
                            proteinQty = proteinQty,
                            proteinUnit = "lb",
                            veg = vegDeal?.name ?: "mixed vegetables",
                            vegQty = vegQty,
                            vegUnit = "lb",
                            starch = starch?.item ?: "rice",
                            starchQty = starchQty,
                            starchUnit = "cup",
                            freezerDirective = freezerDirective,
                            estimatedCost = calculateMealCost(proteinDeal, vegDeal, proteinQty, vegQty)
                        )
                    )

                    // Add to shopping list
                    addToShoppingList(shoppingList, proteinDeal, proteinQty, "$date $mealType protein")
                    if (vegDeal != null) {
                        addToShoppingList(shoppingList, vegDeal, vegQty, "$date $mealType vegetable")
                    }
                }
            }

            mealPlans.add(
                MealPlan(
                    date = date,
                    slots = slots
                )
            )
        }

        // 6. Consolidate shopping list
        val consolidatedShoppingList = consolidateShoppingList(shoppingList)

        return MealPlanResult(
            mealPlans = mealPlans,
            shoppingList = consolidatedShoppingList,
            warnings = warnings
        )
    }

    private fun findPantryAnchors(pantryItems: List<PantryItem>, keywords: Set<String>): List<PantryItem> {
        return pantryItems.filter { item ->
            keywords.any { keyword ->
                item.item.lowercase().contains(keyword)
            } && item.qty > 0
        }
    }

    private fun filterDealsByParams(deals: List<DealItem>, params: Params): List<DealItem> {
        return deals.filter { deal ->
            val name = deal.name.lowercase()

            // GERD-friendly filter
            if (params.gerdFriendly) {
                if (acidicVegetables.any { name.contains(it) }) {
                    return@filter false
                }
            }

            // Avoid peppers filter
            if (params.avoidPeppers) {
                if (name.contains("pepper")) {
                    return@filter false
                }
            }

            true
        }
    }

    private fun calculateFreezerDirective(deal: DealItem, portionSize: Double, days: Int): String? {
        // If buying in bulk (e.g., family pack), suggest freezing portions
        val totalNeeded = portionSize * days
        val packageSize = extractPackageSize(deal)

        if (packageSize != null && packageSize > totalNeeded * 1.5) {
            val portions = ceil(packageSize / portionSize).toInt()
            return "Freeze $portions × ${portionSize} lb packs"
        }

        return null
    }

    private fun extractPackageSize(deal: DealItem): Double? {
        // Try to extract package size from deal text
        val sizePattern = Regex("""(\d+(?:\.\d+)?)\s*lb""")
        val match = sizePattern.find(deal.name + " " + (deal.sizeText ?: ""))
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun calculateMealCost(proteinDeal: DealItem?, vegDeal: DealItem?, proteinQty: Double, vegQty: Double): Double {
        var cost = 0.0

        proteinDeal?.let {
            cost += it.pricePerUnit * proteinQty
        }

        vegDeal?.let {
            cost += it.pricePerUnit * vegQty
        }

        return cost
    }

    private fun addToShoppingList(
        shoppingList: MutableList<ShoppingListItem>,
        deal: DealItem,
        quantity: Double,
        purpose: String
    ) {
        shoppingList.add(
            ShoppingListItem(
                dealItem = deal,
                quantity = quantity,
                purpose = purpose
            )
        )
    }

    private fun consolidateShoppingList(shoppingList: List<ShoppingListItem>): List<ShoppingListItem> {
        // Group by deal item and sum quantities
        val grouped = shoppingList.groupBy { it.dealItem.id }

        return grouped.map { (_, items) ->
            val first = items.first()
            ShoppingListItem(
                dealItem = first.dealItem,
                quantity = items.sumOf { it.quantity },
                purpose = items.joinToString("; ") { it.purpose }
            )
        }
    }

    /**
     * Reorders meals based on perishability (freshness).
     * Items with bestBy dates are prioritized earlier in the week.
     */
    fun reorderByFreshness(mealPlans: List<MealPlan>, pantryItems: List<PantryItem>): List<MealPlan> {
        // Create a map of item names to their best-by dates
        val freshnessMap = pantryItems
            .filter { it.bestBy != null }
            .associateBy({ it.item.lowercase() }, { it.bestBy!! })

        // Sort meal plans - earlier dates for items expiring soon
        return mealPlans.sortedBy { plan ->
            val earliestExpiry = plan.slots.mapNotNull { slot ->
                val proteinDate = slot.protein?.lowercase()?.let { freshnessMap[it] }
                val vegDate = slot.veg?.lowercase()?.let { freshnessMap[it] }
                listOfNotNull(proteinDate, vegDate).minOrNull()
            }.minOrNull()

            earliestExpiry ?: LocalDate.now().plusYears(1) // Items without expiry go last
        }
    }

    /**
     * Auto-freezes proteins nearing their fridge limit (e.g., 3 days).
     */
    fun checkFridgeDays(pantryItems: List<PantryItem>): List<PantryItem> {
        val warnings = mutableListOf<PantryItem>()
        val fridgeLimit = 3L

        pantryItems.forEach { item ->
            if (item.location == "fridge" && item.opened != null) {
                val daysSinceOpened = java.time.temporal.ChronoUnit.DAYS.between(item.opened, LocalDate.now())
                if (daysSinceOpened >= fridgeLimit) {
                    warnings.add(item)
                }
            }
        }

        return warnings
    }
}
