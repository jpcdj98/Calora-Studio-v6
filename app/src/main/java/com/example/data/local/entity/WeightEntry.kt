package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_entries")
data class WeightEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val weight: Float,
    val unit: String = "kg",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
