package com.example.data.repository

import com.example.data.catalog.DietFoodCatalog
import com.example.data.local.dao.CaloraDao
import com.example.data.local.entity.DailyFitnessLog
import com.example.data.local.entity.FoodEntry
import com.example.data.local.entity.FoodItem
import com.example.data.local.entity.UserProfile
import com.example.data.local.entity.WeightEntry
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CaloraRepository(private val dao: CaloraDao) {

    fun getFoodEntriesForDate(date: String): Flow<List<FoodEntry>> =
        dao.getFoodEntriesForDate(date)

    fun getAllFoodEntries(): Flow<List<FoodEntry>> =
        dao.getAllFoodEntries()

    suspend fun addFoodEntry(entry: FoodEntry): Long =
        dao.insertFoodEntry(entry)

    suspend fun updateFoodEntry(entry: FoodEntry) =
        dao.updateFoodEntry(entry)

    suspend fun deleteFoodEntry(id: Long) =
        dao.deleteFoodEntryById(id)

    // Food Database Catalog & User Saved Foods
    fun getAllFoodItems(): Flow<List<FoodItem>> =
        dao.getAllFoodItems()

    fun searchFoodItems(query: String): Flow<List<FoodItem>> {
        val trimmed = query.trim()
        return if (trimmed.isBlank()) {
            dao.getAllFoodItems()
        } else {
            dao.searchFoodItems(trimmed)
        }
    }

    suspend fun saveFoodItem(
        name: String,
        category: String,
        variation: String,
        servingSize: Float,
        servingUnit: String,
        calories: Int,
        protein: Float,
        carbs: Float,
        fat: Float,
        isCustom: Boolean = true,
        unitConversions: String = ""
    ): Long {
        val item = FoodItem(
            name = name.trim(),
            category = category.trim().ifBlank { "Custom" },
            variation = variation.trim().ifBlank { "Standard" },
            servingSize = if (servingSize <= 0f) 1f else servingSize,
            servingUnit = servingUnit.trim().ifBlank { "serving" },
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            isCustom = isCustom,
            searchKeywords = "$name $category $variation $servingUnit".lowercase(),
            unitConversions = unitConversions.trim()
        )
        return dao.insertFoodItem(item)
    }

    suspend fun insertFoodItem(item: FoodItem): Long {
        return dao.insertFoodItem(item)
    }

    suspend fun deleteFoodItem(id: Long) {
        dao.deleteFoodItemById(id)
    }

    fun getFitnessLogForDate(date: String): Flow<DailyFitnessLog?> =
        dao.getFitnessLogForDate(date)

    suspend fun updateSteps(date: String, steps: Int) {
        val current = dao.getFitnessLogDirect(date) ?: DailyFitnessLog(date = date)
        dao.upsertFitnessLog(current.copy(steps = steps.coerceAtLeast(0)))
    }

    suspend fun addSteps(date: String, delta: Int) {
        val current = dao.getFitnessLogDirect(date) ?: DailyFitnessLog(date = date)
        val newSteps = (current.steps + delta).coerceAtLeast(0)
        dao.upsertFitnessLog(current.copy(steps = newSteps))
    }

    suspend fun updateWorkout(
        date: String,
        isDone: Boolean,
        workoutType: String,
        minutes: Int,
        notes: String
    ) {
        val current = dao.getFitnessLogDirect(date) ?: DailyFitnessLog(date = date)
        dao.upsertFitnessLog(
            current.copy(
                isWorkoutDone = isDone,
                workoutType = workoutType,
                workoutMinutes = minutes,
                workoutNotes = notes
            )
        )
    }

    fun getAllFitnessLogs(): Flow<List<DailyFitnessLog>> =
        dao.getAllFitnessLogs()

    fun getAllWeightEntries(): Flow<List<WeightEntry>> =
        dao.getAllWeightEntries()

    suspend fun addWeightEntry(weight: Float, unit: String, notes: String, date: String): Long {
        val entry = WeightEntry(
            date = date,
            weight = weight,
            unit = unit,
            notes = notes
        )
        return dao.insertWeightEntry(entry)
    }

    suspend fun deleteWeightEntry(id: Long) =
        dao.deleteWeightEntryById(id)

    fun getUserProfile(): Flow<UserProfile?> =
        dao.getUserProfile()

    suspend fun updateUserProfile(profile: UserProfile) =
        dao.upsertUserProfile(profile)

    suspend fun clearAllUserData() {
        dao.clearAllFoodEntries()
        dao.clearAllFitnessLogs()
        dao.clearAllWeightEntries()
    }

    suspend fun ensureInitialized() {
        val catalogItems = dao.getAllFoodItemsDirect().filter { !it.isCustom }
        val needsRefresh = catalogItems.isEmpty() ||
                catalogItems.none { it.unitConversions.isNotBlank() } ||
                catalogItems.any { it.name.equals("Eggs", ignoreCase = true) && !it.unitConversions.contains("cup:136") }

        if (needsRefresh) {
            dao.clearCatalogFoodItems()
            dao.insertFoodItems(DietFoodCatalog.defaultFoods)
        }

        val existingProfile = dao.getUserProfileDirect()
        if (existingProfile == null) {
            dao.upsertUserProfile(
                UserProfile(
                    id = 1,
                    dailyCaloriesGoal = 2000,
                    dailyProteinGoal = 140f,
                    dailyCarbsGoal = 220f,
                    dailyFatGoal = 65f,
                    dailyStepsGoal = 10000,
                    startWeight = 70.0f,
                    targetWeight = 65.0f,
                    weightUnit = "kg"
                )
            )
        }

        // Clean up any legacy seeded sample records for a clean fresh install experience
        cleanupLegacySeededData()
    }

    private suspend fun cleanupLegacySeededData() {
        val seededFoodNames = setOf(
            "Oatmeal with Whey & Berries",
            "Grilled Chicken Breast & Brown Rice",
            "Greek Yogurt & Almonds"
        )
        val foodEntries = dao.getAllFoodEntriesDirect()
        foodEntries.forEach { entry ->
            if (entry.name in seededFoodNames) {
                dao.deleteFoodEntryById(entry.id)
            }
        }

        val weightEntries = dao.getAllWeightEntriesDirect()
        weightEntries.forEach { entry ->
            if (entry.notes == "Morning weigh-in, feeling energetic" || entry.notes == "Weekly check-in") {
                dao.deleteWeightEntryById(entry.id)
            }
        }

        val fitnessLogs = dao.getAllFitnessLogsDirect()
        fitnessLogs.forEach { log ->
            if (log.workoutNotes == "Bench press, pull-ups, dumbbell shoulder press." ||
                log.workoutNotes == "Consistent routine"
            ) {
                dao.upsertFitnessLog(
                    log.copy(
                        steps = 0,
                        isWorkoutDone = false,
                        workoutType = "Rest / Recovery",
                        workoutMinutes = 0,
                        workoutNotes = ""
                    )
                )
            }
        }
    }
}
