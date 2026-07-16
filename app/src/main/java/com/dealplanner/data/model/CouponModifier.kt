package com.dealplanner.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coupon_modifiers")
data class CouponModifier(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dealItemId: Long, // references DealItem
    val triggerDesc: String, // e.g., "Buy 2", "Member Only"
    val type: String, // "fixed", "percent", "bogo"
    val value: Double, // dollar amount or percent
    val appliesTo: String, // "all", "matching_items"
    val applied: Boolean = false,
    val notes: String? = null
)
