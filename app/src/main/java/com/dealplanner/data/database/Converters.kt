package com.dealplanner.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.dealplanner.data.model.MealSlot
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class Converters {
    private val gson = Gson()
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? {
        return value?.format(dateFormatter)
    }

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it, dateFormatter) }
    }

    @TypeConverter
    fun fromMealSlotList(value: List<MealSlot>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toMealSlotList(value: String?): List<MealSlot>? {
        val listType = object : TypeToken<List<MealSlot>>() {}.type
        return gson.fromJson(value, listType)
    }
}
