package com.dealplanner.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "params")
data class Params(
    @PrimaryKey
    val id: Long = 1, // Single row
    val gerdFriendly: Boolean = false,
    val avoidPeppers: Boolean = false,
    val breakfastAnchor: Boolean = true,
    val proteinPerMealLb: Double = 0.5,
    val prefersOrganic: Boolean = false,
    val dietaryRestrictions: String? = null // Normalized comma-separated avoid terms.
)
