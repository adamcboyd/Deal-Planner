package com.snapoptimizer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "receipt_items")
data class ReceiptItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawLine: String,
    val matchedItemId: Long? = null, // references PantryItem or DealItem
    val matchedType: String? = null, // "pantry" or "deal"
    val qty: Double? = null,
    val totalCost: Double,
    val date: LocalDate = LocalDate.now(),
    val confidence: Double = 1.0,
    val store: String? = null,
    val needsReview: Boolean = false
)
