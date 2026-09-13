package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(tableName = "food_items")
data class FoodItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String, // "Poultry & Meat", "Eggs & Dairy", "Seafood", "Grains & Carbs", "Vegetables", "Fruits", "Fats & Nuts", "Supplements", "Custom"
    val variation: String = "Standard", // e.g. "Boneless Skinless (Cooked)", "Skin-on", "Bone-in", "Boiled", "Fried", etc.
    val servingSize: Float = 100f,
    val servingUnit: String = "g",
    val calories: Int,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val isCustom: Boolean = false,
    val searchKeywords: String = "",
    val unitConversions: String = ""
) {
    fun getFullDisplayName(): String {
        return if (variation.isNotBlank() && !variation.equals("Standard", ignoreCase = true)) {
            "$name ($variation)"
        } else {
            name
        }
    }

    fun getServingDescription(): String {
        val sizeFormatted = com.example.data.util.FractionHelper.format(servingSize)
        val sep = if (servingUnit.equals("g", ignoreCase = true) || servingUnit.equals("ml", ignoreCase = true)) "" else " "
        return "$sizeFormatted$sep$servingUnit"
    }

    /**
     * Computes scaled nutritional values based on serving multiplier
     */
    fun scaledFor(multiplier: Float): ScaledNutrition {
        val mult = if (multiplier <= 0f) 1f else multiplier
        val cal = (calories * mult).toInt()
        val p = protein * mult
        val c = carbs * mult
        val f = fat * mult
        val size = servingSize * mult
        return ScaledNutrition(cal, p, c, f, size, servingUnit)
    }

    /**
     * Computes scaled nutritional values based on a specific serving unit and fraction/quantity
     */
    fun scaledForUnit(quantity: Float, unit: String): ScaledNutrition {
        return com.example.data.util.ServingMeasurement.scaleNutrition(this, quantity, unit)
    }
}

data class ScaledNutrition(
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val servingSize: Float,
    val servingUnit: String,
    val totalGrams: Float? = null,
    val displayServing: String = ""
)
