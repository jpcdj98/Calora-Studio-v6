package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.util.FractionHelper

@Entity(tableName = "food_entries")
data class FoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val mealType: String, // "Breakfast", "Lunch", "Dinner", "Snack"
    val name: String,
    val calories: Int,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val servingSize: Float = 1f,
    val servingUnit: String = "serving",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getServingDisplay(): String {
        return FractionHelper.formatServing(servingSize, servingUnit)
    }
}
