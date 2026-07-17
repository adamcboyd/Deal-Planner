package com.dealplanner.domain

import com.dealplanner.data.model.MealPlan
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class MealPlanRefreshPolicyTest {

    @Test
    fun `scope is absent when no generated meal plan exists`() {
        val scope = MealPlanRefreshPolicy.scopeFor(emptyList())

        assertThat(scope).isNull()
    }

    @Test
    fun `scope preserves existing generated week start and length`() {
        val startDate = LocalDate.of(2026, 7, 16)
        val existingPlans = (0 until 7).map { dayOffset ->
            MealPlan(
                date = startDate.plusDays(dayOffset.toLong()),
                slots = emptyList()
            )
        }

        val scope = MealPlanRefreshPolicy.scopeFor(existingPlans)

        assertThat(scope?.startDate).isEqualTo(startDate)
        assertThat(scope?.daysToGenerate).isEqualTo(7)
    }

    @Test
    fun `scope covers gaps in existing generated plan dates`() {
        val startDate = LocalDate.of(2026, 7, 16)
        val existingPlans = listOf(
            MealPlan(date = startDate.plusDays(6), slots = emptyList()),
            MealPlan(date = startDate, slots = emptyList())
        )

        val scope = MealPlanRefreshPolicy.scopeFor(existingPlans)

        assertThat(scope?.startDate).isEqualTo(startDate)
        assertThat(scope?.daysToGenerate).isEqualTo(7)
    }
}
