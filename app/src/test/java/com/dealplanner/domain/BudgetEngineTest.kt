package com.dealplanner.domain

import com.google.common.truth.Truth.assertThat
import com.dealplanner.data.model.BudgetState
import com.dealplanner.data.model.ReceiptItem
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
    fun `analyze budget uses current month receipts when budget spend is stale`() {
        val budget = BudgetState(
            startingBudget = 100.0,
            spentToDate = 0.0,
            monthStart = LocalDate.now().withDayOfMonth(1)
        )
        val receipts = listOf(
            ReceiptItem(rawLine = "milk 6.00", totalCost = 6.0, date = LocalDate.now()),
            ReceiptItem(rawLine = "eggs 4.00", totalCost = 4.0, date = LocalDate.now())
        )

        val analysis = engine.analyzeBudget(budget, receipts)

        assertThat(analysis.currentBalance).isEqualTo(90.0)
        assertThat(analysis.projectedSpend).isGreaterThan(0.0)
    }

    @Test
    fun `analyze budget ignores receipts outside current month`() {
        val budget = BudgetState(
            startingBudget = 100.0,
            spentToDate = 5.0,
            monthStart = LocalDate.now().withDayOfMonth(1)
        )
        val receipts = listOf(
            ReceiptItem(
                rawLine = "old receipt 80.00",
                totalCost = 80.0,
                date = budget.monthStart.minusDays(1)
            )
        )

        val analysis = engine.analyzeBudget(budget, receipts)

        assertThat(analysis.currentBalance).isEqualTo(95.0)
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
            projectedSpend = 50.0,
            monthStart = LocalDate.now().withDayOfMonth(1)
        )

        val updated = engine.updateBudgetWithReceipt(budget, 25.0)

        assertThat(updated.spentToDate).isEqualTo(75.0)
        assertThat(updated.projectedSpend).isEqualTo(75.0)
        assertThat(updated.dailyEnvelope).isEqualTo(engine.calculateDailyEnvelope(updated))
    }

    @Test
    fun `adjust budget when receipt total changes`() {
        val budget = BudgetState(
            startingBudget = 300.0,
            spentToDate = 75.0,
            projectedSpend = 75.0,
            monthStart = LocalDate.now().withDayOfMonth(1)
        )

        val updated = engine.adjustBudgetForReceiptChange(budget, -10.0)

        assertThat(updated.spentToDate).isEqualTo(65.0)
        assertThat(updated.projectedSpend).isEqualTo(65.0)
        assertThat(updated.dailyEnvelope).isEqualTo(engine.calculateDailyEnvelope(updated))
    }

    @Test
    fun `receipt budget adjustment does not go negative`() {
        val budget = BudgetState(
            startingBudget = 300.0,
            spentToDate = 5.0,
            projectedSpend = 5.0,
            monthStart = LocalDate.now().withDayOfMonth(1)
        )

        val updated = engine.adjustBudgetForReceiptChange(budget, -10.0)

        assertThat(updated.spentToDate).isEqualTo(0.0)
        assertThat(updated.projectedSpend).isEqualTo(0.0)
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
