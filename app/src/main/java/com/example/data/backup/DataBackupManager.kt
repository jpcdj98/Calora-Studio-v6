package com.example.data.backup

import com.example.data.local.dao.CaloraDao
import com.example.data.local.entity.DailyFitnessLog
import com.example.data.local.entity.FoodEntry
import com.example.data.local.entity.FoodItem
import com.example.data.local.entity.UserProfile
import com.example.data.local.entity.WeightEntry
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupSummary(
    val isValid: Boolean,
    val exportDate: String = "",
    val foodCount: Int = 0,
    val fitnessCount: Int = 0,
    val weightCount: Int = 0,
    val customFoodCount: Int = 0,
    val hasProfile: Boolean = false,
    val error: String? = null
)

class DataBackupManager(private val dao: CaloraDao) {

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    suspend fun exportDataAsJson(): String {
        val profile = dao.getUserProfileDirect() ?: UserProfile()
        val foodList = dao.getAllFoodEntriesDirect()
        val fitnessList = dao.getAllFitnessLogsDirect()
        val weightList = dao.getAllWeightEntriesDirect()
        val allFoods = dao.getAllFoodItemsDirect()
        val customFoods = allFoods.filter { it.isCustom }

        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "Calora")
        root.put("exportDate", isoDateFormat.format(Date()))

        // Profile
        val profileObj = JSONObject().apply {
            put("dailyCaloriesGoal", profile.dailyCaloriesGoal)
            put("dailyProteinGoal", profile.dailyProteinGoal.toDouble())
            put("dailyCarbsGoal", profile.dailyCarbsGoal.toDouble())
            put("dailyFatGoal", profile.dailyFatGoal.toDouble())
            put("dailyStepsGoal", profile.dailyStepsGoal)
            put("startWeight", profile.startWeight.toDouble())
            put("targetWeight", profile.targetWeight.toDouble())
            put("weightUnit", profile.weightUnit)
        }
        root.put("userProfile", profileObj)

        // Foods
        val foodArray = JSONArray()
        foodList.forEach { food ->
            val item = JSONObject().apply {
                put("date", food.date)
                put("mealType", food.mealType)
                put("name", food.name)
                put("calories", food.calories)
                put("protein", food.protein.toDouble())
                put("carbs", food.carbs.toDouble())
                put("fat", food.fat.toDouble())
                put("servingSize", food.servingSize.toDouble())
                put("servingUnit", food.servingUnit)
                put("timestamp", food.timestamp)
            }
            foodArray.put(item)
        }
        root.put("foodEntries", foodArray)

        // Fitness logs
        val fitnessArray = JSONArray()
        fitnessList.forEach { fit ->
            val item = JSONObject().apply {
                put("date", fit.date)
                put("steps", fit.steps)
                put("isWorkoutDone", fit.isWorkoutDone)
                put("workoutType", fit.workoutType)
                put("workoutMinutes", fit.workoutMinutes)
                put("workoutNotes", fit.workoutNotes)
            }
            fitnessArray.put(item)
        }
        root.put("fitnessLogs", fitnessArray)

        // Weight entries
        val weightArray = JSONArray()
        weightList.forEach { wt ->
            val item = JSONObject().apply {
                put("date", wt.date)
                put("weight", wt.weight.toDouble())
                put("unit", wt.unit)
                put("notes", wt.notes)
                put("timestamp", wt.timestamp)
            }
            weightArray.put(item)
        }
        root.put("weightEntries", weightArray)

        // Custom Foods
        val customFoodsArray = JSONArray()
        customFoods.forEach { cf ->
            val item = JSONObject().apply {
                put("name", cf.name)
                put("category", cf.category)
                put("variation", cf.variation)
                put("servingSize", cf.servingSize.toDouble())
                put("servingUnit", cf.servingUnit)
                put("calories", cf.calories)
                put("protein", cf.protein.toDouble())
                put("carbs", cf.carbs.toDouble())
                put("fat", cf.fat.toDouble())
                put("searchKeywords", cf.searchKeywords)
                put("unitConversions", cf.unitConversions)
            }
            customFoodsArray.put(item)
        }
        root.put("customFoods", customFoodsArray)

        return root.toString(2)
    }

    fun inspectBackup(jsonString: String): BackupSummary {
        return try {
            val root = JSONObject(jsonString.trim())
            val exportDate = root.optString("exportDate", "Unknown date")
            val hasProfile = root.has("userProfile")
            val foodCount = root.optJSONArray("foodEntries")?.length() ?: 0
            val fitnessCount = root.optJSONArray("fitnessLogs")?.length() ?: 0
            val weightCount = root.optJSONArray("weightEntries")?.length() ?: 0
            val customFoodCount = root.optJSONArray("customFoods")?.length() ?: 0

            BackupSummary(
                isValid = true,
                exportDate = exportDate,
                foodCount = foodCount,
                fitnessCount = fitnessCount,
                weightCount = weightCount,
                customFoodCount = customFoodCount,
                hasProfile = hasProfile
            )
        } catch (e: Exception) {
            BackupSummary(
                isValid = false,
                error = e.localizedMessage ?: "Invalid JSON format"
            )
        }
    }

    suspend fun restoreDataFromJson(jsonString: String, replaceAll: Boolean): Result<BackupSummary> {
        return try {
            val root = JSONObject(jsonString.trim())
            val exportDate = root.optString("exportDate", "Unknown date")

            val foodsToInsert = mutableListOf<FoodEntry>()
            val rootFoodArray = root.optJSONArray("foodEntries")
            if (rootFoodArray != null) {
                for (i in 0 until rootFoodArray.length()) {
                    val obj = rootFoodArray.getJSONObject(i)
                    foodsToInsert.add(
                        FoodEntry(
                            id = 0, // Auto-generate new IDs so no PK collisions
                            date = obj.optString("date", ""),
                            mealType = obj.optString("mealType", "Meal"),
                            name = obj.optString("name", "Item"),
                            calories = obj.optInt("calories", 0),
                            protein = obj.optDouble("protein", 0.0).toFloat(),
                            carbs = obj.optDouble("carbs", 0.0).toFloat(),
                            fat = obj.optDouble("fat", 0.0).toFloat(),
                            servingSize = obj.optDouble("servingSize", 1.0).toFloat(),
                            servingUnit = obj.optString("servingUnit", "serving"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            val fitnessToInsert = mutableListOf<DailyFitnessLog>()
            val rootFitnessArray = root.optJSONArray("fitnessLogs")
            if (rootFitnessArray != null) {
                for (i in 0 until rootFitnessArray.length()) {
                    val obj = rootFitnessArray.getJSONObject(i)
                    val dateStr = obj.optString("date", "")
                    if (dateStr.isNotBlank()) {
                        fitnessToInsert.add(
                            DailyFitnessLog(
                                date = dateStr,
                                steps = obj.optInt("steps", 0),
                                isWorkoutDone = obj.optBoolean("isWorkoutDone", false),
                                workoutType = obj.optString("workoutType", "Strength"),
                                workoutMinutes = obj.optInt("workoutMinutes", 45),
                                workoutNotes = obj.optString("workoutNotes", "")
                            )
                        )
                    }
                }
            }

            val weightToInsert = mutableListOf<WeightEntry>()
            val rootWeightArray = root.optJSONArray("weightEntries")
            if (rootWeightArray != null) {
                for (i in 0 until rootWeightArray.length()) {
                    val obj = rootWeightArray.getJSONObject(i)
                    val dateStr = obj.optString("date", "")
                    if (dateStr.isNotBlank()) {
                        weightToInsert.add(
                            WeightEntry(
                                id = 0, // Auto-generate IDs
                                date = dateStr,
                                weight = obj.optDouble("weight", 70.0).toFloat(),
                                unit = obj.optString("unit", "kg"),
                                notes = obj.optString("notes", ""),
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                }
            }

            val customFoodsToInsert = mutableListOf<FoodItem>()
            val rootCustomFoodsArray = root.optJSONArray("customFoods")
            if (rootCustomFoodsArray != null) {
                for (i in 0 until rootCustomFoodsArray.length()) {
                    val obj = rootCustomFoodsArray.getJSONObject(i)
                    customFoodsToInsert.add(
                        FoodItem(
                            id = 0,
                            name = obj.optString("name", "Custom Food"),
                            category = obj.optString("category", "Custom"),
                            variation = obj.optString("variation", "Standard"),
                            servingSize = obj.optDouble("servingSize", 100.0).toFloat(),
                            servingUnit = obj.optString("servingUnit", "g"),
                            calories = obj.optInt("calories", 0),
                            protein = obj.optDouble("protein", 0.0).toFloat(),
                            carbs = obj.optDouble("carbs", 0.0).toFloat(),
                            fat = obj.optDouble("fat", 0.0).toFloat(),
                            isCustom = true,
                            searchKeywords = obj.optString("searchKeywords", ""),
                            unitConversions = obj.optString("unitConversions", "")
                        )
                    )
                }
            }

            var profileToSave: UserProfile? = null
            if (root.has("userProfile")) {
                val pObj = root.getJSONObject("userProfile")
                val current = dao.getUserProfileDirect() ?: UserProfile()
                profileToSave = current.copy(
                    id = 1,
                    dailyCaloriesGoal = pObj.optInt("dailyCaloriesGoal", current.dailyCaloriesGoal),
                    dailyProteinGoal = pObj.optDouble("dailyProteinGoal", current.dailyProteinGoal.toDouble()).toFloat(),
                    dailyCarbsGoal = pObj.optDouble("dailyCarbsGoal", current.dailyCarbsGoal.toDouble()).toFloat(),
                    dailyFatGoal = pObj.optDouble("dailyFatGoal", current.dailyFatGoal.toDouble()).toFloat(),
                    dailyStepsGoal = pObj.optInt("dailyStepsGoal", current.dailyStepsGoal),
                    startWeight = pObj.optDouble("startWeight", current.startWeight.toDouble()).toFloat(),
                    targetWeight = pObj.optDouble("targetWeight", current.targetWeight.toDouble()).toFloat(),
                    weightUnit = pObj.optString("weightUnit", current.weightUnit)
                )
            }

            if (replaceAll) {
                dao.clearAllFoodEntries()
                dao.clearAllFitnessLogs()
                dao.clearAllWeightEntries()
                dao.clearAllFoodItems()
                dao.insertFoodItems(com.example.data.catalog.DietFoodCatalog.defaultFoods)
            }

            if (foodsToInsert.isNotEmpty()) {
                dao.insertFoodEntries(foodsToInsert)
            }
            if (fitnessToInsert.isNotEmpty()) {
                dao.upsertFitnessLogs(fitnessToInsert)
            }
            if (weightToInsert.isNotEmpty()) {
                dao.insertWeightEntries(weightToInsert)
            }
            if (customFoodsToInsert.isNotEmpty()) {
                dao.insertFoodItems(customFoodsToInsert)
            }
            if (profileToSave != null) {
                dao.upsertUserProfile(profileToSave)
            }

            val summary = BackupSummary(
                isValid = true,
                exportDate = exportDate,
                foodCount = foodsToInsert.size,
                fitnessCount = fitnessToInsert.size,
                weightCount = weightToInsert.size,
                customFoodCount = customFoodsToInsert.size,
                hasProfile = profileToSave != null
            )
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
