package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val dailyCaloriesGoal: Int = 2200,
    val dailyProteinGoal: Float = 160f,
    val dailyCarbsGoal: Float = 220f,
    val dailyFatGoal: Float = 70f,
    val dailyStepsGoal: Int = 10000,
    val startWeight: Float = 78.5f,
    val targetWeight: Float = 72.0f,
    val weightUnit: String = "kg"
)
