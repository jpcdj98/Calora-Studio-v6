package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.DailyFitnessLog
import com.example.data.local.entity.FoodEntry
import com.example.data.local.entity.UserProfile
import com.example.data.local.entity.WeightEntry
import com.example.ui.theme.MetricStepsColor
import com.example.ui.theme.MetricWorkoutColor
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleLightText
import com.example.ui.theme.PurpleMutedText
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurpleSubtleText
import com.example.ui.theme.PurpleSurface
import com.example.ui.theme.PurpleSurfaceBorder
import com.example.ui.theme.PurpleSurfaceVariant
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class DayEnergyStat(
    val dateStr: String,
    val dayLabel: String,
    val dateLabel: String,
    val calories: Int,
    val isAnchorDay: Boolean
)

data class DayStepsStat(
    val dateStr: String,
    val dayLabel: String,
    val dateLabel: String,
    val steps: Int,
    val caloriesBurned: Int,
    val isAnchorDay: Boolean
)

/**
 * Calculates 7 calendar dates ending on the anchor date (inclusive).
 */
fun getPast7Dates(anchorDateStr: String): List<String> {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val parsed = try {
        format.parse(anchorDateStr) ?: Date()
    } catch (_: Exception) {
        Date()
    }
    val dates = mutableListOf<String>()
    for (i in 6 downTo 0) {
        val cal = Calendar.getInstance()
        cal.time = parsed
        cal.add(Calendar.DAY_OF_YEAR, -i)
        dates.add(format.format(cal.time))
    }
    return dates
}

/**
 * Calculates the 7 calendar dates of the week (Monday through Sunday, inclusive) containing the anchor date.
 * The days constantly start on Monday and end on Sunday.
 */
fun getMondayToSundayDates(anchorDateStr: String): List<String> {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val parsed = try {
        format.parse(anchorDateStr) ?: Date()
    } catch (_: Exception) {
        Date()
    }
    val cal = Calendar.getInstance().apply {
        time = parsed
        // Normalize time fields to noon to avoid edge-cases around Daylight Saving Time shifts
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val dow = cal.get(Calendar.DAY_OF_WEEK)
    val daysSinceMonday = if (dow == Calendar.SUNDAY) 6 else (dow - Calendar.MONDAY)
    cal.add(Calendar.DAY_OF_YEAR, -daysSinceMonday)

    val dates = mutableListOf<String>()
    for (i in 0 until 7) {
        dates.add(format.format(cal.time))
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return dates
}

fun formatFriendlyDateRange(startStr: String, endStr: String): String {
    val inFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val outFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    return try {
        val startDate = inFormat.parse(startStr) ?: Date()
        val endDate = inFormat.parse(endStr) ?: Date()
        "${outFormat.format(startDate)} – ${outFormat.format(endDate)}"
    } catch (_: Exception) {
        "$startStr to $endStr"
    }
}

fun formatDayOfWeek(dateStr: String): String {
    val inFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val outFormat = SimpleDateFormat("EEE", Locale.getDefault())
    return try {
        val date = inFormat.parse(dateStr) ?: Date()
        outFormat.format(date)
    } catch (_: Exception) {
        ""
    }
}

fun formatShortDate(dateStr: String): String {
    val inFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val outFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    return try {
        val date = inFormat.parse(dateStr) ?: Date()
        outFormat.format(date)
    } catch (_: Exception) {
        dateStr
    }
}

/**
 * Resolves user body weight in kg. If weight is recorded in lbs, converts to kg.
 */
fun resolveBodyWeightKg(userProfile: UserProfile, weightEntries: List<WeightEntry>): Float {
    val latestEntry = weightEntries.maxByOrNull { it.timestamp }
    val rawWeight = latestEntry?.weight ?: userProfile.startWeight
    val unit = latestEntry?.unit ?: userProfile.weightUnit
    return if (unit.equals("lbs", ignoreCase = true)) {
        rawWeight / 2.20462f
    } else {
        rawWeight
    }
}

/**
 * Step calories calculation formula requested:
 * (2.74 * bodyweight in kilogram * steps / 8368)
 */
fun calculateStepCaloriesBurned(steps: Int, bodyWeightKg: Float): Int {
    if (steps <= 0 || bodyWeightKg <= 0f) return 0
    val burned = (2.74 * bodyWeightKg.toDouble() * steps.toDouble()) / 8368.0
    return burned.roundToInt().coerceAtLeast(0)
}

/**
 * Dialog displaying 7-Day Energy Breakdown.
 */
@Composable
fun EnergyDetailsDialog(
    anchorDate: String,
    userProfile: UserProfile,
    allFoodEntries: List<FoodEntry>,
    onDismiss: () -> Unit
) {
    val weekDates = getMondayToSundayDates(anchorDate)
    val dayStats = weekDates.map { dateStr ->
        val cals = allFoodEntries.filter { it.date == dateStr }.sumOf { it.calories }
        DayEnergyStat(
            dateStr = dateStr,
            dayLabel = formatDayOfWeek(dateStr),
            dateLabel = formatShortDate(dateStr),
            calories = cals,
            isAnchorDay = (dateStr == anchorDate)
        )
    }

    val totalConsumed = dayStats.sumOf { it.calories }
    val dailyGoal = userProfile.dailyCaloriesGoal
    val totalGoal = dailyGoal * 7
    val dailyAverage = (totalConsumed.toFloat() / 7f).roundToInt()
    val weeklyDifference = totalConsumed - totalGoal

    // Highest and lowest day (by calories)
    val highestDay = dayStats.maxByOrNull { it.calories } ?: dayStats.first()
    val lowestDay = dayStats.minByOrNull { it.calories } ?: dayStats.first()

    val dateRangeDisplay = formatFriendlyDateRange(weekDates.first(), weekDates.last())

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(24.dp))
                .testTag("energy_details_dialog"),
            color = PurpleSurface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = PurpleAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "7-Day Energy",
                                color = PurpleLightText,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dateRangeDisplay,
                            color = PurpleMutedText,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PurpleSurfaceVariant)
                            .border(1.dp, PurpleSurfaceBorder, CircleShape)
                            .testTag("close_energy_details_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = PurpleMutedText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Primary Highlight Card: 7-Day Calories Consumed / Goal
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PurpleSurfaceVariant),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PurpleSurfaceBorder)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "7-DAY CALORIES / GOAL",
                                color = PurpleAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            val goalPct = ((totalConsumed.toFloat() / totalGoal.coerceAtLeast(1)) * 100).toInt()
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PurpleSurface)
                                    .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$goalPct% of goal",
                                    color = PurpleAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "%,d".format(totalConsumed),
                                color = PurpleLightText,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = " / %,d kcal".format(totalGoal),
                                color = PurpleMutedText,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress Bar
                        val progress = (totalConsumed.toFloat() / totalGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(PurpleSurface)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PurplePrimary)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Two column grid: Daily Average & Weekly Difference
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Daily Average
                    MetricDetailTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Speed,
                        label = "DAILY AVERAGE",
                        value = "%,d".format(dailyAverage),
                        unit = "kcal / day",
                        subtext = "Goal: %,d".format(dailyGoal),
                        accentColor = PurpleAccent
                    )

                    // Weekly Difference
                    val diffPrefix = if (weeklyDifference > 0) "+" else if (weeklyDifference < 0) "-" else ""
                    val diffColor = if (weeklyDifference > 0) Color(0xFFFB7185) else if (weeklyDifference < 0) MetricStepsColor else PurpleAccent
                    val diffDesc = when {
                        weeklyDifference > 0 -> "Surplus vs goal"
                        weeklyDifference < 0 -> "Deficit vs goal"
                        else -> "Exactly on goal"
                    }

                    MetricDetailTile(
                        modifier = Modifier.weight(1f),
                        icon = if (weeklyDifference >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        label = "WEEKLY DIFFERENCE",
                        value = "$diffPrefix%,d".format(abs(weeklyDifference)),
                        unit = "kcal net",
                        subtext = diffDesc,
                        accentColor = diffColor
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Highest and Lowest Day Tile
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PurpleSurfaceVariant),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PurpleSurfaceBorder)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "EXTREMES (HIGHEST / LOWEST)",
                            color = PurpleMutedText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Highest Day
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFB7185).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = Color(0xFFFB7185),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Highest: ${highestDay.dayLabel}, ${highestDay.dateLabel}",
                                        color = PurpleLightText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "%,d kcal".format(highestDay.calories),
                                        color = Color(0xFFFB7185),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Lowest Day
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MetricStepsColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                        contentDescription = null,
                                        tint = MetricStepsColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Lowest: ${lowestDay.dayLabel}, ${lowestDay.dateLabel}",
                                        color = PurpleLightText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "%,d kcal".format(lowestDay.calories),
                                        color = MetricStepsColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 7-Day Day-by-Day Visual Bar Chart
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "7-Day Caloric Intake",
                        color = PurpleLightText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Mon – Sun",
                        color = PurpleMutedText,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                DailyEnergyBarChart(
                    dayStats = dayStats,
                    dailyGoal = dailyGoal
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text("Done", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * Dialog displaying 7-Day Activity & Steps Breakdown.
 */
@Composable
fun StepsDetailsDialog(
    anchorDate: String,
    userProfile: UserProfile,
    allFitnessLogs: List<DailyFitnessLog>,
    weightEntries: List<WeightEntry>,
    onDismiss: () -> Unit
) {
    val weekDates = getMondayToSundayDates(anchorDate)
    val bodyWeightKg = resolveBodyWeightKg(userProfile, weightEntries)

    val dayStats = weekDates.map { dateStr ->
        val log = allFitnessLogs.find { it.date == dateStr }
        val steps = log?.steps ?: 0
        val burned = calculateStepCaloriesBurned(steps, bodyWeightKg)
        DayStepsStat(
            dateStr = dateStr,
            dayLabel = formatDayOfWeek(dateStr),
            dateLabel = formatShortDate(dateStr),
            steps = steps,
            caloriesBurned = burned,
            isAnchorDay = (dateStr == anchorDate)
        )
    }

    val totalSteps = dayStats.sumOf { it.steps }
    val stepGoal = userProfile.dailyStepsGoal
    val totalStepGoal = stepGoal * 7
    val averageSteps = (totalSteps.toFloat() / 7f).roundToInt()

    // Step calories for today (anchor day)
    val todayStepsStat = dayStats.find { it.isAnchorDay } ?: dayStats.last()
    val todayStepCalories = calculateStepCaloriesBurned(todayStepsStat.steps, bodyWeightKg)

    // 7-day calories burned
    val total7DayCaloriesBurned = dayStats.sumOf { it.caloriesBurned }

    val dateRangeDisplay = formatFriendlyDateRange(weekDates.first(), weekDates.last())

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(24.dp))
                .testTag("steps_details_dialog"),
            color = PurpleSurface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = null,
                                tint = MetricStepsColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Activity",
                                color = PurpleLightText,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dateRangeDisplay,
                            color = PurpleMutedText,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PurpleSurfaceVariant)
                            .border(1.dp, PurpleSurfaceBorder, CircleShape)
                            .testTag("close_steps_details_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = PurpleMutedText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Primary Highlight Card: 7-Day Steps / Goal
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PurpleSurfaceVariant),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PurpleSurfaceBorder)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "7-DAY STEPS / GOAL",
                                color = MetricStepsColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            val goalPct = ((totalSteps.toFloat() / totalStepGoal.coerceAtLeast(1)) * 100).toInt()
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PurpleSurface)
                                    .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$goalPct% of goal",
                                    color = MetricStepsColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "%,d".format(totalSteps),
                                color = PurpleLightText,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = " / %,d steps".format(totalStepGoal),
                                color = PurpleMutedText,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress Bar
                        val progress = (totalSteps.toFloat() / totalStepGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(PurpleSurface)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MetricStepsColor)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Two column grid: Average Steps & 7-Day Calorie Burned
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Average Steps
                    MetricDetailTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.DateRange,
                        label = "AVERAGE STEPS",
                        value = "%,d".format(averageSteps),
                        unit = "steps / day",
                        subtext = "Goal: %,d".format(stepGoal),
                        accentColor = MetricStepsColor
                    )

                    // 7-day Calorie Burned
                    MetricDetailTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.LocalFireDepartment,
                        label = "7-DAY CALORIES BURNED",
                        value = "%,d".format(total7DayCaloriesBurned),
                        unit = "kcal burned",
                        subtext = "~%,d kcal / day".format((total7DayCaloriesBurned.toFloat() / 7f).roundToInt()),
                        accentColor = Color(0xFFFB923C)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Step Calories for Today (Explicit Formula Tile)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PurpleSurfaceVariant),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PurpleSurfaceBorder)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Color(0xFFFB7185),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "STEP CALORIES FOR TODAY",
                                    color = PurpleLightText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFB7185).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Today",
                                    color = Color(0xFFFB7185),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "%,d".format(todayStepCalories),
                                color = Color(0xFFFB7185),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = " kcal burned today (%,d steps)".format(todayStepsStat.steps),
                                color = PurpleMutedText,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 3.dp, start = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Explicit formula breakdown
                        Text(
                            text = "Formula: (2.74 × %.1f kg × %,d steps) / 8368".format(
                                bodyWeightKg,
                                todayStepsStat.steps
                            ),
                            color = PurpleSubtleText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 7-Day Day-by-Day Visual Step Chart
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "7-Day Steps & Burned Energy",
                        color = PurpleLightText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Mon – Sun",
                        color = PurpleMutedText,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                DailyStepsBarChart(
                    dayStats = dayStats,
                    dailyStepGoal = stepGoal
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text("Done", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun MetricDetailTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    subtext: String,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = PurpleSurfaceVariant),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PurpleSurfaceBorder)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    color = PurpleMutedText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = PurpleLightText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = unit,
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtext,
                color = PurpleSubtleText,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun DailyEnergyBarChart(
    dayStats: List<DayEnergyStat>,
    dailyGoal: Int
) {
    val maxVal = (dayStats.maxOfOrNull { it.calories } ?: dailyGoal).coerceAtLeast(dailyGoal).coerceAtLeast(1)

    val goalLineColor = PurpleAccent.copy(alpha = 0.4f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PurpleSurfaceVariant),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PurpleSurfaceBorder)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                // Goal reference line
                Canvas(modifier = Modifier.fillMaxWidth().height(110.dp)) {
                    val goalY = size.height * (1f - (dailyGoal.toFloat() / maxVal.toFloat()).coerceIn(0.05f, 0.95f))
                    drawLine(
                        color = goalLineColor,
                        start = Offset(0f, goalY),
                        end = Offset(size.width, goalY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                // Bars
                Row(
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    dayStats.forEach { stat ->
                        val barRatio = (stat.calories.toFloat() / maxVal.toFloat()).coerceIn(0.02f, 1f)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .fillMaxWidth(0.6f)
                                    .height((90 * barRatio).dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        if (stat.isAnchorDay) PurplePrimary
                                        else if (stat.calories >= dailyGoal) PurpleAccent
                                        else PurpleMutedText.copy(alpha = 0.6f)
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Day labels row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayStats.forEach { stat ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stat.dayLabel,
                            color = if (stat.isAnchorDay) PurpleAccent else PurpleMutedText,
                            fontSize = 11.sp,
                            fontWeight = if (stat.isAnchorDay) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = "%,d".format(stat.calories),
                            color = if (stat.isAnchorDay) PurpleLightText else PurpleSubtleText,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyStepsBarChart(
    dayStats: List<DayStepsStat>,
    dailyStepGoal: Int
) {
    val maxVal = (dayStats.maxOfOrNull { it.steps } ?: dailyStepGoal).coerceAtLeast(dailyStepGoal).coerceAtLeast(1)

    val goalLineColor = MetricStepsColor.copy(alpha = 0.4f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PurpleSurfaceVariant),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PurpleSurfaceBorder)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                // Goal reference line
                Canvas(modifier = Modifier.fillMaxWidth().height(110.dp)) {
                    val goalY = size.height * (1f - (dailyStepGoal.toFloat() / maxVal.toFloat()).coerceIn(0.05f, 0.95f))
                    drawLine(
                        color = goalLineColor,
                        start = Offset(0f, goalY),
                        end = Offset(size.width, goalY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                // Bars
                Row(
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    dayStats.forEach { stat ->
                        val barRatio = (stat.steps.toFloat() / maxVal.toFloat()).coerceIn(0.02f, 1f)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .fillMaxWidth(0.6f)
                                    .height((90 * barRatio).dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        if (stat.isAnchorDay) MetricStepsColor
                                        else if (stat.steps >= dailyStepGoal) MetricStepsColor.copy(alpha = 0.85f)
                                        else PurpleMutedText.copy(alpha = 0.6f)
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Day labels row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayStats.forEach { stat ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stat.dayLabel,
                            color = if (stat.isAnchorDay) MetricStepsColor else PurpleMutedText,
                            fontSize = 11.sp,
                            fontWeight = if (stat.isAnchorDay) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = "%,d".format(stat.steps),
                            color = if (stat.isAnchorDay) PurpleLightText else PurpleSubtleText,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}
