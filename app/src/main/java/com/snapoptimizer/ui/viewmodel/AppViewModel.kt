package com.snapoptimizer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.snapoptimizer.data.database.AppDatabase
import com.snapoptimizer.data.model.*
import com.snapoptimizer.data.repository.AppRepository
import com.snapoptimizer.domain.*
import com.snapoptimizer.parser.DealsParser
import com.snapoptimizer.parser.PantryPhraseParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = AppRepository(
        pantryDao = database.pantryDao(),
        dealDao = database.dealDao(),
        receiptDao = database.receiptDao(),
        mealPlanDao = database.mealPlanDao(),
        budgetDao = database.budgetDao(),
        paramsDao = database.paramsDao(),
        couponModifierDao = database.couponModifierDao()
    )

    private val pantryParser = PantryPhraseParser()
    private val dealsParser = DealsParser()
    private val mealPlanningEngine = MealPlanningEngine()
    private val budgetEngine = BudgetEngine()
    private val receiptReconciler = ReceiptReconciler()

    // Flows
    val pantryItems = repository.allPantryItems.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val deals = repository.allDeals.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val mealPlans = repository.allMealPlans.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val budgetState = repository.budgetState.stateIn(viewModelScope, SharingStarted.Lazily, null)
    val params = repository.params.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _budgetAnalysis = MutableStateFlow<BudgetEngine.BudgetAnalysis?>(null)
    val budgetAnalysis: StateFlow<BudgetEngine.BudgetAnalysis?> = _budgetAnalysis.asStateFlow()

    private val _shoppingList = MutableStateFlow<List<MealPlanningEngine.ShoppingListItem>>(emptyList())
    val shoppingList: StateFlow<List<MealPlanningEngine.ShoppingListItem>> = _shoppingList.asStateFlow()

    init {
        viewModelScope.launch {
            initializeDefaults()
            updateBudgetAnalysis()
        }
    }

    // Pantry operations
    fun addPantryPhrase(phrase: String) {
        viewModelScope.launch {
            val result = pantryParser.parse(phrase)
            repository.insertPantryItem(result.item)
        }
    }

    fun updatePantryItem(item: PantryItem) {
        viewModelScope.launch {
            repository.updatePantryItem(item)
        }
    }

    fun deletePantryItem(item: PantryItem) {
        viewModelScope.launch {
            repository.deletePantryItem(item)
        }
    }

    // Deals operations
    fun processDealsOCR(ocrText: String, store: String = "Unknown") {
        viewModelScope.launch {
            val result = dealsParser.parse(ocrText, store)
            repository.insertDeals(result.deals)
        }
    }

    fun updateDeal(deal: DealItem) {
        viewModelScope.launch {
            repository.updateDeal(deal)
        }
    }

    fun deleteDeal(deal: DealItem) {
        viewModelScope.launch {
            repository.deleteDeal(deal)
        }
    }

    // Meal planning
    fun generateMealPlan() {
        viewModelScope.launch {
            val currentParams = repository.getParams() ?: Params()
            val pantry = repository.getAllPantryItems()
            val currentDeals = repository.getAllDeals()

            val request = MealPlanningEngine.MealPlanRequest(
                params = currentParams,
                pantryItems = pantry,
                deals = currentDeals
            )

            val result = mealPlanningEngine.generateMealPlan(request)

            // Clear old plans and insert new ones
            repository.deleteOldMealPlans(LocalDate.now().minusDays(1))
            repository.insertMealPlans(result.mealPlans)

            _shoppingList.value = result.shoppingList
        }
    }

    fun updateMealPlan(plan: MealPlan) {
        viewModelScope.launch {
            repository.updateMealPlan(plan)
        }
    }

    // Budget operations
    fun updateBudget(budget: BudgetState) {
        viewModelScope.launch {
            repository.updateBudget(budget)
            updateBudgetAnalysis()
        }
    }

    private suspend fun updateBudgetAnalysis() {
        val budget = repository.getBudget() ?: return
        val receipts = emptyList<ReceiptItem>() // Would load actual receipts
        val analysis = budgetEngine.analyzeBudget(budget, receipts)
        _budgetAnalysis.value = analysis
    }

    // Receipt reconciliation
    fun processReceiptOCR(ocrText: String, store: String) {
        viewModelScope.launch {
            val currentDeals = repository.getAllDeals()
            val pantry = repository.getAllPantryItems()

            val result = receiptReconciler.reconcileReceipt(ocrText, currentDeals, pantry, store)

            // Insert receipt items
            repository.insertReceipts(result.receiptItems)

            // Update pantry quantities
            result.pantryUpdates.forEach { updatedItem ->
                repository.updatePantryItem(updatedItem)
            }

            // Update budget
            val currentBudget = repository.getBudget()
            if (currentBudget != null) {
                val updatedBudget = budgetEngine.updateBudgetWithReceipt(currentBudget, result.total)
                repository.updateBudget(updatedBudget)
                updateBudgetAnalysis()
            }
        }
    }

    // Params operations
    fun updateParams(params: Params) {
        viewModelScope.launch {
            repository.updateParams(params)
        }
    }

    // Demo data seeding
    fun loadDemoData() {
        viewModelScope.launch {
            // Clear existing data
            repository.deleteAllPantryItems()
            repository.deleteAllDeals()
            repository.deleteAllMealPlans()

            // Add pantry anchors
            val pantryAnchors = listOf(
                PantryItem(item = "rice", qty = 5.0, unit = "lb", location = "pantry", form = "dried"),
                PantryItem(item = "pasta", qty = 3.0, unit = "lb", location = "pantry", form = "dried"),
                PantryItem(item = "oats", qty = 2.0, unit = "lb", location = "pantry", form = "dried"),
                PantryItem(item = "black beans", qty = 4.0, unit = "can", location = "pantry", form = "canned", size = "15 oz"),
                PantryItem(item = "olive oil", qty = 1.0, unit = "bottle", location = "pantry")
            )
            repository.insertPantryItems(pantryAnchors)

            // Add demo deals
            val demoDeals = listOf(
                DealItem(
                    name = "Pork Shoulder",
                    price = 3.99,
                    unit = "lb",
                    dealType = "per_pound",
                    store = "Kroger",
                    dealScore = 0.85,
                    pricePerUnit = 3.99,
                    discountPercent = 30.0
                ),
                DealItem(
                    name = "Broccoli Crowns",
                    price = 1.99,
                    unit = "lb",
                    dealType = "per_pound",
                    store = "Kroger",
                    dealScore = 0.75,
                    pricePerUnit = 1.99,
                    discountPercent = 20.0
                ),
                DealItem(
                    name = "Mandarin Oranges",
                    price = 3.99,
                    unit = "bag",
                    dealType = "per_unit",
                    store = "Kroger",
                    dealScore = 0.70,
                    pricePerUnit = 3.99,
                    sizeText = "3 lb bag"
                ),
                DealItem(
                    name = "Chicken Breast",
                    price = 2.99,
                    unit = "lb",
                    dealType = "per_pound",
                    store = "Walmart",
                    dealScore = 0.80,
                    pricePerUnit = 2.99,
                    discountPercent = 25.0
                )
            )
            repository.insertDeals(demoDeals)

            // Initialize params if not exists
            if (repository.getParams() == null) {
                repository.insertParams(Params())
            }

            // Initialize budget if not exists
            if (repository.getBudget() == null) {
                repository.insertBudget(
                    BudgetState(
                        startingSnap = 292.0,
                        spentToDate = 45.0,
                        dailyEnvelope = 10.0,
                        breakfastAnchorCost = 0.55
                    )
                )
            }

            // Generate meal plan
            generateMealPlan()
        }
    }

    private suspend fun initializeDefaults() {
        if (repository.getParams() == null) {
            repository.insertParams(Params())
        }
        if (repository.getBudget() == null) {
            repository.insertBudget(
                BudgetState(
                    startingSnap = 292.0,
                    dailyEnvelope = 10.0,
                    breakfastAnchorCost = 0.55
                )
            )
        }
    }
}
