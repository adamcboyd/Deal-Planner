package com.dealplanner.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "pantry_items")
data class PantryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val item: String,
    val form: String? = null, // e.g., "canned", "frozen", "fresh"
    val qty: Double = 1.0,
    val unit: String? = null, // e.g., "lb", "oz", "count"
    val size: String? = null, // e.g., "15 oz", "1 lb"
    val brand: String? = null,
    val location: String? = null, // e.g., "pantry", "fridge", "freezer"
    val opened: LocalDate? = null,
    val bestBy: LocalDate? = null,
    val dateAdded: LocalDate = LocalDate.now(),
    val notes: String? = null,
    val needsVerify: Boolean = false
)
