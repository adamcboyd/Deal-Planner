package com.dealplanner.data.dao

import androidx.room.*
import com.dealplanner.data.model.MealPlan
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface MealPlanDao {
    @Query("SELECT * FROM meal_plans ORDER BY date ASC")
    fun getAllFlow(): Flow<List<MealPlan>>

    @Query("SELECT * FROM meal_plans")
    suspend fun getAll(): List<MealPlan>

    @Query("SELECT * FROM meal_plans WHERE id = :id")
    suspend fun getById(id: Long): MealPlan?

    @Query("SELECT * FROM meal_plans WHERE date = :date")
    suspend fun getByDate(date: LocalDate): MealPlan?

    @Query("SELECT * FROM meal_plans WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getDateRange(startDate: LocalDate, endDate: LocalDate): List<MealPlan>

    @Query("SELECT * FROM meal_plans WHERE date >= :startDate ORDER BY date ASC LIMIT 7")
    suspend fun getNextWeek(startDate: LocalDate = LocalDate.now()): List<MealPlan>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: MealPlan): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plans: List<MealPlan>)

    @Update
    suspend fun update(plan: MealPlan)

    @Delete
    suspend fun delete(plan: MealPlan)

    @Query("DELETE FROM meal_plans")
    suspend fun deleteAll()

    @Query("DELETE FROM meal_plans WHERE date < :date")
    suspend fun deleteOlderThan(date: LocalDate)
}
