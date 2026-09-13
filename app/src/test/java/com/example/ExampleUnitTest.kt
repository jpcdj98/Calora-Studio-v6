package com.example

import com.example.data.local.entity.FoodEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testFoodEntryServingDisplayGram() {
    val entry = FoodEntry(
      date = "2026-03-30",
      mealType = "Lunch",
      name = "Chicken Breast",
      calories = 330,
      protein = 62f,
      carbs = 0f,
      fat = 7f,
      servingSize = 200f,
      servingUnit = "g"
    )
    assertEquals("200g", entry.getServingDisplay())
  }

  @Test
  fun testFoodEntryServingDisplayCup() {
    val entry = FoodEntry(
      date = "2026-03-30",
      mealType = "Lunch",
      name = "Rice",
      calories = 410,
      protein = 8f,
      carbs = 90f,
      fat = 1f,
      servingSize = 2f,
      servingUnit = "cup"
    )
    assertEquals("2 cup", entry.getServingDisplay())
  }

  @Test
  fun testFoodEntryServingDisplayDecimal() {
    val entry = FoodEntry(
      date = "2026-03-30",
      mealType = "Dinner",
      name = "Pasta",
      calories = 580,
      protein = 32f,
      carbs = 68f,
      fat = 19f,
      servingSize = 1.5f,
      servingUnit = "cup"
    )
    assertEquals("1 1/2 cup", entry.getServingDisplay())
  }

  @Test
  fun testFormattedTimeCalculation() {
    fun formatTime(h: Int, m: Int): String {
      val amPm = if (h >= 12) "PM" else "AM"
      val displayHour = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
      }
      return String.format(java.util.Locale.US, "%d:%02d %s", displayHour, m, amPm)
    }

    assertEquals("9:30 PM", formatTime(21, 30))
    assertEquals("10:00 PM", formatTime(22, 0))
    assertEquals("8:45 PM", formatTime(20, 45))
    assertEquals("12:00 PM", formatTime(12, 0))
    assertEquals("12:15 AM", formatTime(0, 15))
  }
}
