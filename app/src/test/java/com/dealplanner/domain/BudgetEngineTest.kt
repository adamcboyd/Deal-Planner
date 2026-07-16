package com.dealplanner.domain

import com.google.common.truth.Truth.assertThat
import com.dealplanner.data.model.BudgetState
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class BudgetEngineTest {

    private lateinit var engine: BudgetEngine

    @Before
    fun setup() {
        engine = BudgetEngine()
    }

    @Test
    fun `analyze budget with surplus`() {
        val budget = BudgetState(
            startingBudget = 300.0,
            spentToDate = 50.0,
            monthStart = LocalDate.now().withDayOfMonth(1)
        )

        val analysis = engine.analyzeBudget(budget, emptyList())

        assertThat(analysis.currentBalance).isEqualTo(250.0)
        assertThat(analysis.onTrack).isTrue()
        assertThat(analysis.surplus).isGreaterThan(0.0)
    }

    @Test
    fun `analyze budget with deficit`() {
        val budget = BudgetState(
            startingBudget = 300.0,
            spentToDate = 280.0,
            monthStart = LocalDate.now().withDayOfMonth(1)
        )

        val analysis = engine.analyzeBudget(budget, emptyList())

        assertThat(analysis.currentBalance).isEqualTo(20.0)
        assertThat(analysis.onTrack).isFalse()
        assertThat(analysis.surplus).isLessThan(0.0)
    }

    @Test
    fun `calculate daily envelope`() {
        val budget = BudgetState(
            startingBudget = 300.0,
            spentToDate = 150.0,
            monthStart = LocalDate.now().withDayOfMonth(1)
        )

        val dailyEnvelope = engine.calculateDailyEnvelope(budget)

        assertThat(dailyEnvelope).isGreaterThan(0.0)
    }

    @Test
    fun `update budget with receipt`() {
        val budget = BudgetState(
            startingBudget = 300.0,
            spentToDate = 50.0,
            monthStart = LocalDate.now().withDayOfMonth(1)
        )

        val updated = engine.updateBudgetWithReceipt(budget, 25.0)

        assertThat(updated.spentToDate).isEqualTo(75.0)
    }

    @Test
    fun `generate suggestions for surplus`() {
        val budget = BudgetState(
            startingBudget = 300.0,
            spentToDate = 50.0,
            monthStart = LocalDate.now().withDayOfMonth(1)
        )

        val analysis = engine.analyzeBudget(budget, emptyList())

        assertThat(analysis.suggestions).isNotEmpty()
        assertThat(analysis.suggestions.any { it.contains("surplus") }).isTrue()
    }

    @Test
    fun `generate suggestions for deficit`() {
        val budget = BudgetState(
            startingBudget = 300.0,
            spentToDate = 280.0,
            monthStart = LocalDate.now().withDayOfMonth(1)
        )

        val analysis = engine.analyzeBudget(budget, emptyList())

        assertThat(analysis.suggestions).isNotEmpty()
        assertThat(analysis.suggestions.any { it.contains("overspend") || it.contains("freezer") }).isTrue()
    }
}
