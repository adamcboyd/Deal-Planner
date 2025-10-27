package com.snapoptimizer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.snapoptimizer.data.database.Converters
import java.time.LocalDate

@Entity(tableName = "meal_plans")
@TypeConverters(Converters::class)
data class MealPlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: LocalDate,
    val slots: List<MealSlot>,
    val notes: String? = null
)

data class MealSlot(
    val mealType: String, // "breakfast", "lunch", "dinner"
    val protein: String? = null,
    val veg: String? = null,
    val starch: String? = null,
    val proteinQty: Double = 0.0,
    val proteinUnit: String? = null,
    val vegQty: Double = 0.0,
    val vegUnit: String? = null,
    val starchQty: Double = 0.0,
    val starchUnit: String? = null,
    val freezerDirective: String? = null, // e.g., "Freeze 4 × 0.5 lb packs"
    val estimatedCost: Double = 0.0,
    val completed: Boolean = false,
    val skipped: Boolean = false
)
