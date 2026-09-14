package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entity.UserProfile
import com.example.data.local.entity.WeightEntry
import com.example.ui.components.calculateStepCaloriesBurned
import com.example.ui.components.getMondayToSundayDates
import com.example.ui.components.getPast7Dates
import com.example.ui.components.resolveBodyWeightKg
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Calora Studio", appName)
  }

  @Test
  fun `verify step calories formula exactness`() {
    // Formula: 2.74 * bodyweight_in_kg * steps / 8368
    val bodyWeightKg = 75.0f
    val steps = 10000
    // Expected: (2.74 * 75.0 * 10000) / 8368 = 2055000 / 8368 = 245.578... -> 246
    val expected = ((2.74 * bodyWeightKg.toDouble() * steps.toDouble()) / 8368.0).roundToInt()
    val calculated = calculateStepCaloriesBurned(steps, bodyWeightKg)
    assertEquals(expected, calculated)
    assertEquals(246, calculated)
  }

  @Test
  fun `verify bodyweight resolution in kg and lbs`() {
    val profileKg = UserProfile(startWeight = 70.0f, weightUnit = "kg")
    assertEquals(70.0f, resolveBodyWeightKg(profileKg, emptyList()))

    val profileLbs = UserProfile(startWeight = 154.32f, weightUnit = "lbs")
    val resolvedKg = resolveBodyWeightKg(profileLbs, emptyList())
    assertEquals(70.0f, resolvedKg, 0.1f)

    val weightEntries = listOf(
        WeightEntry(date = "2026-09-08", weight = 176.37f, unit = "lbs", timestamp = 1000L),
        WeightEntry(date = "2026-09-09", weight = 165.35f, unit = "lbs", timestamp = 2000L) // latest
    )
    val latestResolvedKg = resolveBodyWeightKg(profileLbs, weightEntries)
    assertEquals(75.0f, latestResolvedKg, 0.1f)
  }

  @Test
  fun `verify past 7 dates generation`() {
    val dates = getPast7Dates("2026-09-10")
    assertEquals(7, dates.size)
    assertEquals("2026-09-04", dates.first())
    assertEquals("2026-09-10", dates.last())
  }

  @Test
  fun `verify monday to sunday week dates generation`() {
    // 2026-09-10 is a Thursday
    val thursdayDates = getMondayToSundayDates("2026-09-10")
    assertEquals(7, thursdayDates.size)
    assertEquals("2026-09-07", thursdayDates.first()) // Monday
    assertEquals("2026-09-13", thursdayDates.last())  // Sunday

    // 2026-09-14 is a Monday
    val mondayDates = getMondayToSundayDates("2026-09-14")
    assertEquals(7, mondayDates.size)
    assertEquals("2026-09-14", mondayDates.first()) // Monday
    assertEquals("2026-09-20", mondayDates.last())  // Sunday

    // 2026-09-20 is a Sunday
    val sundayDates = getMondayToSundayDates("2026-09-20")
    assertEquals(7, sundayDates.size)
    assertEquals("2026-09-14", sundayDates.first()) // Monday
    assertEquals("2026-09-20", sundayDates.last())  // Sunday
  }

  @Test
  fun `verify backup inspect and json parsing`() {
    val sampleJson = """
      {
        "version": 1,
        "appName": "Calora",
        "exportDate": "2026-09-11 12:00:00",
        "userProfile": {
          "dailyCaloriesGoal": 2400,
          "dailyProteinGoal": 160.0,
          "dailyCarbsGoal": 250.0,
          "dailyFatGoal": 70.0,
          "dailyStepsGoal": 12000,
          "startWeight": 80.0,
          "targetWeight": 75.0,
          "weightUnit": "kg"
        },
        "foodEntries": [
          {
            "date": "2026-09-11",
            "mealType": "Breakfast",
            "name": "Eggs & Avocado Toast",
            "calories": 450,
            "protein": 24.0,
            "carbs": 35.0,
            "fat": 22.0,
            "servingSize": 1.0,
            "servingUnit": "plate",
            "timestamp": 1000
          }
        ],
        "fitnessLogs": [
          {
            "date": "2026-09-11",
            "steps": 9500,
            "isWorkoutDone": true,
            "workoutType": "HIIT",
            "workoutMinutes": 30,
            "workoutNotes": "Tabata"
          }
        ],
        "weightEntries": [
          {
            "date": "2026-09-11",
            "weight": 79.5,
            "unit": "kg",
            "notes": "Morning",
            "timestamp": 1000
          }
        ]
      }
    """.trimIndent()

    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java).allowMainThreadQueries().build()
    val manager = com.example.data.backup.DataBackupManager(db.caloraDao())

    val summary = manager.inspectBackup(sampleJson)
    assertEquals(true, summary.isValid)
    assertEquals(1, summary.foodCount)
    assertEquals(1, summary.fitnessCount)
    assertEquals(1, summary.weightCount)
    assertEquals(true, summary.hasProfile)
    assertEquals("2026-09-11 12:00:00", summary.exportDate)
    db.close()
  }

  @Test
  fun `verify date selection updates selectedDate`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.CaloraViewModel(app)
    vm.setDate("2026-09-05")
    assertEquals("2026-09-05", vm.selectedDate.value)

    vm.changeDay(1)
    assertEquals("2026-09-06", vm.selectedDate.value)

    vm.setToday()
    val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    assertEquals(todayStr, vm.selectedDate.value)
  }

  @Test
  fun `verify custom food item with long name and serving description`() {
    val customFood = com.example.data.local.entity.FoodItem(
        name = "Organic Chocolate Peanut Butter Whey Protein Shake with Chia Seeds",
        variation = "Large 24oz Double Scoop",
        category = "Custom",
        calories = 620,
        protein = 52.0f,
        carbs = 40.0f,
        fat = 24.0f,
        servingSize = 1.0f,
        servingUnit = "bottle (700ml)",
        isCustom = true
    )

    assertEquals("Organic Chocolate Peanut Butter Whey Protein Shake with Chia Seeds (Large 24oz Double Scoop)", customFood.getFullDisplayName())
    assertEquals("1 bottle (700ml)", customFood.getServingDescription())
    assertEquals(true, customFood.isCustom)
  }

  @Test
  fun `verify decimal input sanitization and calorie parsing with decimals`() {
    // Sanitization replaces commas and allows single decimal point
    assertEquals("12.5", com.example.data.util.FractionHelper.sanitizeDecimalInput("12.5"))
    assertEquals("12.5", com.example.data.util.FractionHelper.sanitizeDecimalInput("12,5"))
    assertEquals("12.55", com.example.data.util.FractionHelper.sanitizeDecimalInput("12.5.5"))
    assertEquals("0.75", com.example.data.util.FractionHelper.sanitizeDecimalInput("0.75"))

    // Calorie parsing handles decimals and integers properly
    assertEquals(250, com.example.data.util.FractionHelper.parseCalories("250"))
    assertEquals(251, com.example.data.util.FractionHelper.parseCalories("250.6"))
    assertEquals(250, com.example.data.util.FractionHelper.parseCalories("250.4"))
    assertEquals(185, com.example.data.util.FractionHelper.parseCalories("184,8"))
    assertEquals(null, com.example.data.util.FractionHelper.parseCalories(""))

    // Macro formatting retains decimals when present
    assertEquals("24.5", com.example.data.util.FractionHelper.formatMacroValue(24.5f))
    assertEquals("30", com.example.data.util.FractionHelper.formatMacroValue(30.0f))
    assertEquals("", com.example.data.util.FractionHelper.formatMacroValue(0f))

    // Serving size parsing handles decimals
    assertEquals(1.5f, com.example.data.util.FractionHelper.parse("1.5"))
    assertEquals(0.75f, com.example.data.util.FractionHelper.parse("3/4"))
    assertEquals(2.25f, com.example.data.util.FractionHelper.parse("2,25"))
  }

  @Test
  fun `verify bedtime reminder notification channel configured for heads-up sound and vibration`() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    com.example.reminder.BedtimeReminderReceiver.createNotificationChannel(context)

    val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val channel = notificationManager.getNotificationChannel(com.example.reminder.BedtimeReminderReceiver.CHANNEL_ID)
    assertNotNull(channel)
    assertEquals(android.app.NotificationManager.IMPORTANCE_HIGH, channel.importance)
    assertEquals(true, channel.shouldVibrate())
    assertNotNull(channel.sound)
  }
}
