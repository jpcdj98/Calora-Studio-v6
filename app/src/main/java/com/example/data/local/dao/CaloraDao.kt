package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.DailyFitnessLog
import com.example.data.local.entity.FoodEntry
import com.example.data.local.entity.FoodItem
import com.example.data.local.entity.UserProfile
import com.example.data.local.entity.WeightEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface CaloraDao {

    // Food Entries
    @Query("SELECT * FROM food_entries WHERE date = :date ORDER BY timestamp ASC")
    fun getFoodEntriesForDate(date: String): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entries ORDER BY date ASC, timestamp ASC")
    fun getAllFoodEntries(): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entries ORDER BY date ASC, timestamp ASC")
    suspend fun getAllFoodEntriesDirect(): List<FoodEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodEntry(entry: FoodEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodEntries(entries: List<FoodEntry>)

    @Update
    suspend fun updateFoodEntry(entry: FoodEntry)

    @Query("DELETE FROM food_entries WHERE id = :id")
    suspend fun deleteFoodEntryById(id: Long)

    @Query("DELETE FROM food_entries")
    suspend fun clearAllFoodEntries()

    // Daily Fitness (Steps & Workouts)
    @Query("SELECT * FROM daily_fitness_logs WHERE date = :date")
    fun getFitnessLogForDate(date: String): Flow<DailyFitnessLog?>

    @Query("SELECT * FROM daily_fitness_logs WHERE date = :date")
    suspend fun getFitnessLogDirect(date: String): DailyFitnessLog?

    @Query("SELECT * FROM daily_fitness_logs ORDER BY date DESC")
    fun getAllFitnessLogs(): Flow<List<DailyFitnessLog>>

    @Query("SELECT * FROM daily_fitness_logs ORDER BY date DESC")
    suspend fun getAllFitnessLogsDirect(): List<DailyFitnessLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFitnessLog(log: DailyFitnessLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFitnessLogs(logs: List<DailyFitnessLog>)

    @Query("DELETE FROM daily_fitness_logs")
    suspend fun clearAllFitnessLogs()

    // Weight Entries
    @Query("SELECT * FROM weight_entries ORDER BY date ASC, timestamp ASC")
    fun getAllWeightEntries(): Flow<List<WeightEntry>>

    @Query("SELECT * FROM weight_entries ORDER BY date ASC, timestamp ASC")
    suspend fun getAllWeightEntriesDirect(): List<WeightEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightEntry(entry: WeightEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightEntries(entries: List<WeightEntry>)

    @Query("DELETE FROM weight_entries WHERE id = :id")
    suspend fun deleteWeightEntryById(id: Long)

    @Query("DELETE FROM weight_entries")
    suspend fun clearAllWeightEntries()

    // User Profile / Goals
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getUserProfileDirect(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProfile(profile: UserProfile)

    // Food Database Items (Catalog & Custom User Saved Foods)
    @Query("SELECT * FROM food_items ORDER BY isCustom DESC, name ASC, variation ASC")
    fun getAllFoodItems(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items ORDER BY isCustom DESC, name ASC, variation ASC")
    suspend fun getAllFoodItemsDirect(): List<FoodItem>

    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%' OR variation LIKE '%' || :query || '%' OR searchKeywords LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY isCustom DESC, name ASC, variation ASC")
    fun searchFoodItems(query: String): Flow<List<FoodItem>>

    @Query("SELECT COUNT(*) FROM food_items")
    suspend fun getFoodItemCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItem(item: FoodItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItems(items: List<FoodItem>)

    @Query("DELETE FROM food_items WHERE id = :id")
    suspend fun deleteFoodItemById(id: Long)

    @Query("DELETE FROM food_items WHERE isCustom = 0")
    suspend fun clearCatalogFoodItems()

    @Query("DELETE FROM food_items")
    suspend fun clearAllFoodItems()
}
