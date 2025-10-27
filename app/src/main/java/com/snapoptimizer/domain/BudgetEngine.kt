package com.snapoptimizer.domain

import com.snapoptimizer.data.model.BudgetState
import com.snapoptimizer.data.model.ReceiptItem
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Budget tracking engine.
 * Manages SNAP balance, daily envelope, and projections.
 */
class BudgetEngine {

    data class BudgetAnalysis(
        val currentBalance: Double,
        val dailyBudget: Double,
        val projectedSpend: Double,
        val surplus: Double,
        val daysRemaining: Int,
        val onTrack: Boolean,
        val suggestions: List<String>
    )

    /**
     * Analyzes current budget state and provides recommendations.
     */
    fun analyzeBudget(
        budgetState: BudgetState,
        receipts: List<ReceiptItem>
    ): BudgetAnalysis {
        val currentBalance = budgetState.startingSnap - budgetState.spentToDate
        val monthStart = budgetState.monthStart
        val monthEnd = monthStart.plusMonths(1)
        val daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), monthEnd).toInt()

        // Calculate projected spend based on current rate
        val daysElapsed = ChronoUnit.DAYS.between(monthStart, LocalDate.now()).toInt().coerceAtLeast(1)
        val avgDailySpend = budgetState.spentToDate / daysElapsed
        val projectedSpend = avgDailySpend * daysRemaining

        // Calculate surplus/deficit
        val surplus = currentBalance - projectedSpend
        val onTrack = surplus >= 0

        // Generate suggestions
        val suggestions = mutableListOf<String>()

        when {
            surplus > 50 -> {
                suggestions.add("Great! You have $${"%.2f".format(surplus)} surplus.")
                suggestions.add("Consider stocking up on pantry anchors (rice, pasta, oats).")
                suggestions.add("Look for protein deals to freeze for next month.")
            }
            surplus in 0.0..50.0 -> {
                suggestions.add("You're on track with $${"%.2f".format(surplus)} surplus.")
                suggestions.add("Stick to your meal plan to maintain balance.")
            }
            surplus < 0 -> {
                suggestions.add("Warning: Projected to overspend by $${"%.2f".format(-surplus)}.")
                suggestions.add("Pull from freezer stock to reduce spending.")
                suggestions.add("Focus on pantry anchors and skip protein purchases this week.")
            }
        }

        return BudgetAnalysis(
            currentBalance = currentBalance,
            dailyBudget = budgetState.dailyEnvelope,
            projectedSpend = projectedSpend,
            surplus = surplus,
            daysRemaining = daysRemaining,
            onTrack = onTrack,
            suggestions = suggestions
        )
    }

    /**
     * Calculates daily envelope based on remaining balance and days.
     */
    fun calculateDailyEnvelope(budgetState: BudgetState): Double {
        val currentBalance = budgetState.startingSnap - budgetState.spentToDate
        val monthStart = budgetState.monthStart
        val monthEnd = monthStart.plusMonths(1)
        val daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), monthEnd).toInt().coerceAtLeast(1)

        return currentBalance / daysRemaining
    }

    /**
     * Updates budget after receipt reconciliation.
     */
    fun updateBudgetWithReceipt(
        budgetState: BudgetState,
        receiptTotal: Double
    ): BudgetState {
        return budgetState.copy(
            spentToDate = budgetState.spentToDate + receiptTotal,
            projectedSpend = budgetState.projectedSpend + receiptTotal
        )
    }
}
