package com.snapoptimizer.data.repository

import com.snapoptimizer.data.dao.*
import com.snapoptimizer.data.model.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class AppRepository(
    private val pantryDao: PantryDao,
    private val dealDao: DealDao,
    private val receiptDao: ReceiptDao,
    private val mealPlanDao: MealPlanDao,
    private val budgetDao: BudgetDao,
    private val paramsDao: ParamsDao,
    private val couponModifierDao: CouponModifierDao
) {
    // Pantry operations
    val allPantryItems: Flow<List<PantryItem>> = pantryDao.getAllFlow()
    val pantryItemsNeedingVerify: Flow<List<PantryItem>> = pantryDao.getNeedsVerifyFlow()

    suspend fun insertPantryItem(item: PantryItem) = pantryDao.insert(item)
    suspend fun insertPantryItems(items: List<PantryItem>) = pantryDao.insertAll(items)
    suspend fun updatePantryItem(item: PantryItem) = pantryDao.update(item)
    suspend fun deletePantryItem(item: PantryItem) = pantryDao.delete(item)
    suspend fun getPantryItem(id: Long) = pantryDao.getById(id)
    suspend fun getAllPantryItems() = pantryDao.getAll()
    suspend fun getPantryByLocation(location: String) = pantryDao.getByLocation(location)
    suspend fun getPantryByBestBy() = pantryDao.getByBestByDate()
    suspend fun searchPantry(term: String) = pantryDao.search(term)
    suspend fun decrementPantryQty(id: Long, amount: Double) = pantryDao.decrementQty(id, amount)
    suspend fun deleteAllPantryItems() = pantryDao.deleteAll()

    // Deal operations
    val allDeals: Flow<List<DealItem>> = dealDao.getAllFlow()
    val lowConfidenceDeals: Flow<List<DealItem>> = dealDao.getLowConfidenceFlow()

    suspend fun insertDeal(item: DealItem) = dealDao.insert(item)
    suspend fun insertDeals(items: List<DealItem>) = dealDao.insertAll(items)
    suspend fun updateDeal(item: DealItem) = dealDao.update(item)
    suspend fun deleteDeal(item: DealItem) = dealDao.delete(item)
    suspend fun getDeal(id: Long) = dealDao.getById(id)
    suspend fun getAllDeals() = dealDao.getAll()
    suspend fun getTopDeals(minScore: Double = 0.5) = dealDao.getTopDeals(minScore)
    suspend fun searchDeals(term: String) = dealDao.search(term)
    suspend fun deleteAllDeals() = dealDao.deleteAll()

    // Receipt operations
    val allReceipts: Flow<List<ReceiptItem>> = receiptDao.getAllFlow()
    val receiptsNeedingReview: Flow<List<ReceiptItem>> = receiptDao.getNeedsReviewFlow()

    suspend fun insertReceipt(item: ReceiptItem) = receiptDao.insert(item)
    suspend fun insertReceipts(items: List<ReceiptItem>) = receiptDao.insertAll(items)
    suspend fun updateReceipt(item: ReceiptItem) = receiptDao.update(item)
    suspend fun deleteReceipt(item: ReceiptItem) = receiptDao.delete(item)
    suspend fun getReceipt(id: Long) = receiptDao.getById(id)

    // Meal plan operations
    val allMealPlans: Flow<List<MealPlan>> = mealPlanDao.getAllFlow()

    suspend fun insertMealPlan(plan: MealPlan) = mealPlanDao.insert(plan)
    suspend fun insertMealPlans(plans: List<MealPlan>) = mealPlanDao.insertAll(plans)
    suspend fun updateMealPlan(plan: MealPlan) = mealPlanDao.update(plan)
    suspend fun deleteMealPlan(plan: MealPlan) = mealPlanDao.delete(plan)
    suspend fun getMealPlan(id: Long) = mealPlanDao.getById(id)
    suspend fun getMealPlanByDate(date: LocalDate) = mealPlanDao.getByDate(date)
    suspend fun getNextWeekMealPlans() = mealPlanDao.getNextWeek()
    suspend fun deleteOldMealPlans(date: LocalDate) = mealPlanDao.deleteOlderThan(date)
    suspend fun deleteAllMealPlans() = mealPlanDao.deleteAll()

    // Budget operations
    val budgetState: Flow<BudgetState?> = budgetDao.getFlow()

    suspend fun getBudget() = budgetDao.get()
    suspend fun insertBudget(budget: BudgetState) = budgetDao.insert(budget)
    suspend fun updateBudget(budget: BudgetState) = budgetDao.update(budget)
    suspend fun addSpending(amount: Double) = budgetDao.addSpending(amount)

    // Params operations
    val params: Flow<Params?> = paramsDao.getFlow()

    suspend fun getParams() = paramsDao.get()
    suspend fun insertParams(params: Params) = paramsDao.insert(params)
    suspend fun updateParams(params: Params) = paramsDao.update(params)

    // Coupon modifier operations
    val allCouponModifiers: Flow<List<CouponModifier>> = couponModifierDao.getAllFlow()

    suspend fun insertCouponModifier(modifier: CouponModifier) = couponModifierDao.insert(modifier)
    suspend fun updateCouponModifier(modifier: CouponModifier) = couponModifierDao.update(modifier)
    suspend fun getCouponModifiersByDeal(dealId: Long) = couponModifierDao.getByDealItem(dealId)
    suspend fun getUnappliedCoupons() = couponModifierDao.getUnapplied()
}
