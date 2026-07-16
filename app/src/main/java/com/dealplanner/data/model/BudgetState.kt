package com.dealplanner.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "budget_state")
data class BudgetState(
    @PrimaryKey
    val id: Long = 1, // Single row
    val startingBudget: Double,
    val spentToDate: Double = 0.0,
    val dailyEnvelope: Double = 10.0,
    val breakfastAnchorCost: Double = 0.55,
    val monthStart: LocalDate = LocalDate.now().withDayOfMonth(1),
    val projectedSpend: Double = 0.0,
    val surplus: Double = 0.0
)
