package com.dealplanner.domain

import com.dealplanner.data.model.MealPlan
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object MealPlanRefreshPolicy {
    data class RefreshScope(
        val startDate: LocalDate,
        val daysToGenerate: Int
    )

    fun scopeFor(existingPlans: List<MealPlan>): RefreshScope? {
        if (existingPlans.isEmpty()) return null

        val dates = existingPlans.map { it.date }
        val startDate = dates.minOrNull() ?: return null
        val endDate = dates.maxOrNull() ?: startDate
        val inclusiveDateRangeDays = ChronoUnit.DAYS.between(startDate, endDate)
            .toInt()
            .coerceAtLeast(0) + 1

        return RefreshScope(
            startDate = startDate,
            daysToGenerate = maxOf(existingPlans.size, inclusiveDateRangeDays)
        )
    }
}
