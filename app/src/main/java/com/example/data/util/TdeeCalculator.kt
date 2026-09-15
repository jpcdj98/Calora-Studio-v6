package com.example.data.util

import android.content.Context
import android.content.SharedPreferences
import kotlin.math.roundToInt

enum class BiologicalSex(val displayName: String) {
    MALE("Male"),
    FEMALE("Female")
}

enum class ActivityLevel(
    val id: String,
    val displayName: String,
    val multiplier: Float,
    val shortDesc: String,
    val stepGuide: String
) {
    SEDENTARY("sedentary", "Sedentary", 1.20f, "Desk job, minimal movement", "< 5,000 steps/day"),
    LIGHT("light", "Lightly Active", 1.375f, "Light workouts 1-3 days/wk", "5,000 - 7,500 steps/day"),
    MODERATE("moderate", "Moderately Active", 1.55f, "Moderate exercise 3-5 days/wk", "8,000 - 10,000 steps/day"),
    VERY_ACTIVE("very_active", "Very Active", 1.725f, "Heavy training 6-7 days/wk", "11,000 - 14,000 steps/day"),
    EXTRA_ACTIVE("extra_active", "Extra Active", 1.90f, "Competitive athlete / intense labor", "15,000+ steps/day")
}

enum class GoalStrategy(
    val id: String,
    val displayName: String,
    val shortLabel: String,
    val description: String
) {
    CUT("cut", "Cut", "Fat Loss", "Deficit to drop body fat while preserving lean muscle"),
    MAINTAIN("maintain", "Maintain", "Maintenance", "Maintain current weight and body composition"),
    BULK("bulk", "Bulk", "Muscle Gain", "Caloric surplus to support hypertrophy and strength")
}

data class TdeeCalculation(
    val bmr: Int,
    val tdee: Int,
    val targetCalories: Int,
    val strategy: GoalStrategy,
    val calorieAdjustment: Int,
    val weeklyWeightChangeKg: Float,
    val weeklyWeightChangeLbs: Float
)

object TdeeCalculator {
    /**
     * Mifflin-St Jeor equation (gold standard BMR formula)
     */
    fun calculateBmr(
        weightKg: Float,
        heightCm: Float,
        age: Int,
        sex: BiologicalSex
    ): Float {
        val safeWeight = weightKg.coerceAtLeast(30f)
        val safeHeight = heightCm.coerceAtLeast(90f)
        val safeAge = age.coerceIn(10, 110)
        return when (sex) {
            BiologicalSex.MALE -> (10f * safeWeight) + (6.25f * safeHeight) - (5f * safeAge) + 5f
            BiologicalSex.FEMALE -> (10f * safeWeight) + (6.25f * safeHeight) - (5f * safeAge) - 161f
        }
    }

    fun calculateTdee(bmr: Float, activityLevel: ActivityLevel): Int {
        return (bmr * activityLevel.multiplier).roundToInt().coerceAtLeast(1000)
    }

    fun calculate(
        weightKg: Float,
        heightCm: Float,
        age: Int,
        sex: BiologicalSex,
        activityLevel: ActivityLevel,
        strategy: GoalStrategy,
        calorieAdjustment: Int
    ): TdeeCalculation {
        val bmrFloat = calculateBmr(weightKg, heightCm, age, sex)
        val bmr = bmrFloat.roundToInt()
        val tdee = calculateTdee(bmrFloat, activityLevel)

        val targetCalories = when (strategy) {
            GoalStrategy.CUT -> (tdee - kotlin.math.abs(calorieAdjustment)).coerceAtLeast(1000)
            GoalStrategy.MAINTAIN -> tdee
            GoalStrategy.BULK -> tdee + kotlin.math.abs(calorieAdjustment)
        }

        val netDailyDiff = targetCalories - tdee
        // 7700 kcal ≈ 1 kg body fat, 3500 kcal ≈ 1 lb body fat
        val weeklyWeightChangeKg = (netDailyDiff * 7f) / 7700f
        val weeklyWeightChangeLbs = (netDailyDiff * 7f) / 3500f

        return TdeeCalculation(
            bmr = bmr,
            tdee = tdee,
            targetCalories = targetCalories,
            strategy = strategy,
            calorieAdjustment = calorieAdjustment,
            weeklyWeightChangeKg = weeklyWeightChangeKg,
            weeklyWeightChangeLbs = weeklyWeightChangeLbs
        )
    }

    fun lbsToKg(lbs: Float): Float = lbs / 2.20462f
    fun kgToLbs(kg: Float): Float = kg * 2.20462f
    fun feetInchesToCm(feet: Int, inches: Float): Float = (feet * 30.48f) + (inches * 2.54f)
    fun cmToFeetInches(cm: Float): Pair<Int, Float> {
        val totalInches = cm / 2.54f
        val feet = (totalInches / 12f).toInt()
        val inches = totalInches - (feet * 12)
        return Pair(feet, inches)
    }
}

class TdeePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("calora_tdee_prefs", Context.MODE_PRIVATE)

    var sex: BiologicalSex
        get() = if (prefs.getString(KEY_SEX, "MALE") == "FEMALE") BiologicalSex.FEMALE else BiologicalSex.MALE
        set(value) = prefs.edit().putString(KEY_SEX, value.name).apply()

    var age: Int
        get() = prefs.getInt(KEY_AGE, 28)
        set(value) = prefs.edit().putInt(KEY_AGE, value).apply()

    var heightCm: Float
        get() = prefs.getFloat(KEY_HEIGHT_CM, 175f)
        set(value) = prefs.edit().putFloat(KEY_HEIGHT_CM, value).apply()

    var heightUnit: String // "cm" or "ft_in"
        get() = prefs.getString(KEY_HEIGHT_UNIT, "cm") ?: "cm"
        set(value) = prefs.edit().putString(KEY_HEIGHT_UNIT, value).apply()

    var activityLevel: ActivityLevel
        get() {
            val id = prefs.getString(KEY_ACTIVITY, ActivityLevel.MODERATE.id)
            return ActivityLevel.entries.find { it.id == id } ?: ActivityLevel.MODERATE
        }
        set(value) = prefs.edit().putString(KEY_ACTIVITY, value.id).apply()

    var strategy: GoalStrategy
        get() {
            val id = prefs.getString(KEY_STRATEGY, GoalStrategy.CUT.id)
            return GoalStrategy.entries.find { it.id == id } ?: GoalStrategy.CUT
        }
        set(value) = prefs.edit().putString(KEY_STRATEGY, value.id).apply()

    var cutDeficitKcal: Int
        get() = prefs.getInt(KEY_CUT_DEFICIT, 500)
        set(value) = prefs.edit().putInt(KEY_CUT_DEFICIT, value).apply()

    var bulkSurplusKcal: Int
        get() = prefs.getInt(KEY_BULK_SURPLUS, 300)
        set(value) = prefs.edit().putInt(KEY_BULK_SURPLUS, value).apply()

    var autoSyncWithNutrition: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SYNC, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SYNC, value).apply()

    companion object {
        private const val KEY_SEX = "key_tdee_sex"
        private const val KEY_AGE = "key_tdee_age"
        private const val KEY_HEIGHT_CM = "key_tdee_height_cm"
        private const val KEY_HEIGHT_UNIT = "key_tdee_height_unit"
        private const val KEY_ACTIVITY = "key_tdee_activity"
        private const val KEY_STRATEGY = "key_tdee_strategy"
        private const val KEY_CUT_DEFICIT = "key_tdee_cut_deficit"
        private const val KEY_BULK_SURPLUS = "key_tdee_bulk_surplus"
        private const val KEY_AUTO_SYNC = "key_tdee_auto_sync"
    }
}
