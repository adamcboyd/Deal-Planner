package com.dealplanner.data.dao

import androidx.room.*
import com.dealplanner.data.model.BudgetState
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budget_state WHERE id = 1")
    fun getFlow(): Flow<BudgetState?>

    @Query("SELECT * FROM budget_state WHERE id = 1")
    suspend fun get(): BudgetState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetState)

    @Update
    suspend fun update(budget: BudgetState)

    @Query("UPDATE budget_state SET spentToDate = spentToDate + :amount WHERE id = 1")
    suspend fun addSpending(amount: Double)

    @Query("DELETE FROM budget_state")
    suspend fun deleteAll()
}
