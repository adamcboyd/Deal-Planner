package com.dealplanner.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "deal_items")
data class DealItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val brand: String? = null,
    val sizeText: String? = null,
    val price: Double,
    val unit: String? = null, // e.g., "lb", "ea"
    val dealType: String, // e.g., "per_pound", "n_for_x", "buy_n_get_m", "percent_off"
    val limit: Int? = null,
    val couponFlag: Boolean = false,
    val store: String,
    val confidence: Double = 1.0, // 0.0 to 1.0
    val dealScore: Double = 0.0,
    val pricePerUnit: Double = 0.0, // normalized $/lb or $/oz
    val discountPercent: Double = 0.0,
    val validUntil: LocalDate? = null,
    val rawText: String? = null
)
