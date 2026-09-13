package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_fitness_logs")
data class DailyFitnessLog(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val steps: Int = 0,
    val isWorkoutDone: Boolean = false,
    val workoutType: String = "Strength",
    val workoutMinutes: Int = 45,
    val workoutNotes: String = ""
)
