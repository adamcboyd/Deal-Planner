package com.dealplanner.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class ShoppingListExportFormatter {

    data class Report(
        val title: String,
        val generatedOnText: String,
        val rows: List<Row>,
        val totalEstimatedCostText: String
    )

    data class Row(
        val name: String,
        val quantityText: String,
        val estimatedCostText: String,
        val detailLines: List<String>
    )

    fun buildReport(
        items: List<MealPlanningEngine.ShoppingListItem>,
        generatedOn: LocalDate = LocalDate.now()
    ): Report {
        val rows = items.map { item ->
            val deal = item.dealItem
            val unit = deal.unit.displayOr("unit")
            val pricePerUnit = deal.pricePerUnit.takeIf { it > 0.0 } ?: deal.price
            val detailLines = buildList {
                add("Store: ${deal.store.ifBlank { "Unknown" }}")
                add("Brand: ${deal.brand.displayOr("Any")} | Size: ${deal.sizeText.displayOr("N/A")}")
                add("Price: ${formatCurrency(pricePerUnit)}/$unit | Deal: ${deal.dealType}")
                if (deal.couponFlag) {
                    add("Coupon: clip before checkout")
                }
                deal.limit?.let { limit ->
                    add("Limit: $limit")
                }
                add("For: ${item.purpose.ifBlank { "meal plan" }}")
            }

            Row(
                name = deal.name.ifBlank { "Unnamed item" },
                quantityText = "${formatQuantity(item.quantity)} $unit",
                estimatedCostText = formatCurrency(item.estimatedCost),
                detailLines = detailLines
            )
        }

        return Report(
            title = "Deal Planner Shopping List",
            generatedOnText = generatedOn.format(DateTimeFormatter.ISO_LOCAL_DATE),
            rows = rows,
            totalEstimatedCostText = formatCurrency(items.sumOf { it.estimatedCost })
        )
    }

    private fun formatCurrency(value: Double): String {
        return "$" + String.format(Locale.US, "%.2f", value)
    }

    private fun formatQuantity(value: Double): String {
        return String.format(Locale.US, "%.2f", value)
            .trimEnd('0')
            .trimEnd('.')
            .ifBlank { "0" }
    }

    private fun String?.displayOr(defaultValue: String): String {
        return this?.trim()?.takeIf { it.isNotBlank() } ?: defaultValue
    }
}
