package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupSummary
import com.example.data.backup.DataBackupManager
import com.example.data.local.AppDatabase
import com.example.data.local.entity.DailyFitnessLog
import com.example.data.local.entity.FoodEntry
import com.example.data.local.entity.FoodItem
import com.example.data.local.entity.UserProfile
import com.example.data.local.entity.WeightEntry
import com.example.data.repository.CaloraRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CaloraViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CaloraRepository
    private val backupManager: DataBackupManager
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val _selectedDate = MutableStateFlow(dateFormat.format(Date()))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        val dao = db.caloraDao()
        repository = CaloraRepository(dao)
        backupManager = DataBackupManager(dao)
        viewModelScope.launch {
            repository.ensureInitialized()
        }
    }

    val userProfile: StateFlow<UserProfile> = repository.getUserProfile()
        .map { it ?: UserProfile() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProfile()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val todayFoodEntries: StateFlow<List<FoodEntry>> = _selectedDate
        .flatMapLatest { date -> repository.getFoodEntriesForDate(date) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allFoodEntries: StateFlow<List<FoodEntry>> = repository.getAllFoodEntries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allFoodItems: StateFlow<List<FoodItem>> = repository.getAllFoodItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val todayFitnessLog: StateFlow<DailyFitnessLog?> = _selectedDate
        .flatMapLatest { date -> repository.getFitnessLogForDate(date) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val allFitnessLogs: StateFlow<List<DailyFitnessLog>> = repository.getAllFitnessLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allWeightEntries: StateFlow<List<WeightEntry>> = repository.getAllWeightEntries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun isToday(dateStr: String): Boolean {
        val todayStr = dateFormat.format(Date())
        return dateStr == todayStr
    }

    fun changeDay(offset: Int) {
        try {
            val cal = Calendar.getInstance()
            cal.time = dateFormat.parse(_selectedDate.value) ?: Date()
            cal.add(Calendar.DAY_OF_YEAR, offset)
            _selectedDate.value = dateFormat.format(cal.time)
        } catch (_: Exception) {
            _selectedDate.value = dateFormat.format(Date())
        }
    }

    fun setDate(dateStr: String) {
        _selectedDate.value = dateStr
    }

    fun setToday() {
        _selectedDate.value = dateFormat.format(Date())
    }

    fun addFood(
        mealType: String,
        name: String,
        calories: Int,
        protein: Float,
        carbs: Float,
        fat: Float,
        servingSize: Float = 1f,
        servingUnit: String = "serving",
        saveToDatabase: Boolean = false,
        category: String = "Custom",
        variation: String = "Standard",
        unitConversions: String = ""
    ) {
        viewModelScope.launch {
            repository.addFoodEntry(
                FoodEntry(
                    date = _selectedDate.value,
                    mealType = mealType,
                    name = name.trim(),
                    calories = calories.coerceAtLeast(0),
                    protein = protein.coerceAtLeast(0f),
                    carbs = carbs.coerceAtLeast(0f),
                    fat = fat.coerceAtLeast(0f),
                    servingSize = servingSize.coerceAtLeast(0.01f),
                    servingUnit = servingUnit.trim().ifBlank { "serving" }
                )
            )
            if (saveToDatabase) {
                repository.saveFoodItem(
                    name = name.trim(),
                    category = category.trim().ifBlank { "Custom" },
                    variation = variation.trim().ifBlank { "Standard" },
                    servingSize = servingSize.coerceAtLeast(0.01f),
                    servingUnit = servingUnit.trim().ifBlank { "serving" },
                    calories = calories.coerceAtLeast(0),
                    protein = protein.coerceAtLeast(0f),
                    carbs = carbs.coerceAtLeast(0f),
                    fat = fat.coerceAtLeast(0f),
                    isCustom = true,
                    unitConversions = unitConversions.trim()
                )
            }
        }
    }

    fun saveCustomFood(item: FoodItem) {
        viewModelScope.launch {
            repository.insertFoodItem(item)
        }
    }

    fun saveCustomFoodItem(
        name: String,
        category: String = "Custom",
        variation: String = "Standard",
        servingSize: Float = 100f,
        servingUnit: String = "g",
        calories: Int,
        protein: Float = 0f,
        carbs: Float = 0f,
        fat: Float = 0f,
        unitConversions: String = ""
    ) {
        viewModelScope.launch {
            repository.saveFoodItem(
                name = name,
                category = category,
                variation = variation,
                servingSize = servingSize,
                servingUnit = servingUnit,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                isCustom = true,
                unitConversions = unitConversions.trim()
            )
        }
    }

    fun deleteFoodItem(id: Long) {
        viewModelScope.launch {
            repository.deleteFoodItem(id)
        }
    }

    fun deleteFood(id: Long) {
        viewModelScope.launch {
            repository.deleteFoodEntry(id)
        }
    }

    fun updateFoodEntry(entry: FoodEntry) {
        viewModelScope.launch {
            repository.updateFoodEntry(entry)
        }
    }

    fun addSteps(delta: Int) {
        viewModelScope.launch {
            repository.addSteps(_selectedDate.value, delta)
        }
    }

    fun setSteps(total: Int) {
        viewModelScope.launch {
            repository.updateSteps(_selectedDate.value, total)
        }
    }

    fun toggleWorkout() {
        val currentLog = todayFitnessLog.value
        val newDone = !(currentLog?.isWorkoutDone ?: false)
        viewModelScope.launch {
            repository.updateWorkout(
                date = _selectedDate.value,
                isDone = newDone,
                workoutType = currentLog?.workoutType ?: "Strength",
                minutes = currentLog?.workoutMinutes ?: 45,
                notes = currentLog?.workoutNotes ?: ""
            )
        }
    }

    fun saveWorkout(type: String, minutes: Int, notes: String, isDone: Boolean) {
        viewModelScope.launch {
            repository.updateWorkout(
                date = _selectedDate.value,
                isDone = isDone,
                workoutType = type,
                minutes = minutes,
                notes = notes
            )
        }
    }

    fun addWeight(weight: Float, unit: String, notes: String, date: String = _selectedDate.value) {
        viewModelScope.launch {
            repository.addWeightEntry(
                weight = weight,
                unit = unit,
                notes = notes.trim(),
                date = date
            )
        }
    }

    fun deleteWeight(id: Long) {
        viewModelScope.launch {
            repository.deleteWeightEntry(id)
        }
    }

    fun updateGoals(
        dailyCalories: Int,
        protein: Float,
        carbs: Float,
        fat: Float,
        steps: Int,
        startWeight: Float,
        targetWeight: Float,
        unit: String
    ) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.updateUserProfile(
                current.copy(
                    dailyCaloriesGoal = dailyCalories,
                    dailyProteinGoal = protein,
                    dailyCarbsGoal = carbs,
                    dailyFatGoal = fat,
                    dailyStepsGoal = steps,
                    startWeight = startWeight,
                    targetWeight = targetWeight,
                    weightUnit = unit
                )
            )
        }
    }

    fun updateStartingWeight(weight: Float) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.updateUserProfile(
                current.copy(startWeight = weight)
            )
        }
    }

    suspend fun exportDataJson(): String {
        return backupManager.exportDataAsJson()
    }

    fun inspectBackup(json: String): BackupSummary {
        return backupManager.inspectBackup(json)
    }

    suspend fun importData(json: String, replaceAll: Boolean): Result<BackupSummary> {
        val result = backupManager.restoreDataFromJson(json, replaceAll)
        return result
    }

    fun resetToFreshInstall(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.clearAllUserData()
            onComplete?.invoke()
        }
    }
}
