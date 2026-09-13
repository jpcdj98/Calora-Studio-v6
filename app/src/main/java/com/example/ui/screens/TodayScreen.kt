package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.DailyFitnessLog
import com.example.data.local.entity.FoodEntry
import com.example.data.local.entity.FoodItem
import com.example.data.local.entity.UserProfile
import com.example.data.local.entity.WeightEntry
import com.example.ui.components.AddFoodDialog
import com.example.ui.components.CalorieProgressRing
import com.example.ui.components.CleanCard
import com.example.ui.components.DateNavigationBar
import com.example.ui.components.EditFoodDialog
import com.example.ui.components.EnergyDetailsDialog
import com.example.ui.components.MacroProgressBar
import com.example.ui.components.StepsDetailsDialog
import com.example.ui.theme.MacroCarbsColor
import com.example.ui.theme.MacroFatColor
import com.example.ui.theme.MacroProteinColor
import com.example.ui.theme.MetricStepsColor
import com.example.ui.theme.MetricWorkoutColor
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleCardHover
import com.example.ui.theme.PurpleDarkBackground
import com.example.ui.theme.PurpleLightText
import com.example.ui.theme.PurpleMutedText
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurpleSubtleText
import com.example.ui.theme.PurpleSurface
import com.example.ui.theme.PurpleSurfaceBorder
import com.example.ui.theme.PurpleSurfaceVariant
import java.util.Calendar

@Composable
fun TodayScreen(
    selectedDate: String,
    userProfile: UserProfile,
    foodEntries: List<FoodEntry>,
    fitnessLog: DailyFitnessLog?,
    allFoodEntries: List<FoodEntry> = emptyList(),
    allFitnessLogs: List<DailyFitnessLog> = emptyList(),
    allWeightEntries: List<WeightEntry> = emptyList(),
    foodDatabase: List<FoodItem> = emptyList(),
    onSaveFoodItem: ((FoodItem) -> Unit)? = null,
    onDeleteFoodItem: ((Long) -> Unit)? = null,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onTodayClicked: () -> Unit,
    onDateSelected: (String) -> Unit = {},
    onAddFood: (
        mealType: String,
        name: String,
        calories: Int,
        protein: Float,
        carbs: Float,
        fat: Float,
        servingSize: Float,
        servingUnit: String
    ) -> Unit,
    onDeleteFood: (id: Long) -> Unit,
    onUpdateFood: (entry: FoodEntry) -> Unit = {},
    onAddSteps: (delta: Int) -> Unit,
    onSetSteps: (count: Int) -> Unit,
    onToggleWorkout: () -> Unit,
    onSaveWorkout: (type: String, minutes: Int, notes: String, isDone: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    fun getDefaultMealType(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 4..10 -> "Breakfast"
            in 11..15 -> "Lunch"
            in 16..20 -> "Dinner"
            else -> "Snack"
        }
    }

    var showAddFoodDialog by rememberSaveable { mutableStateOf(false) }
    var selectedMealForAdd by rememberSaveable { mutableStateOf(getDefaultMealType()) }
    var editingFoodEntry by remember { mutableStateOf<FoodEntry?>(null) }
    var collapsedMeals by rememberSaveable { mutableStateOf(setOf("Breakfast", "Lunch", "Dinner", "Snack")) }

    var showEditStepsDialog by rememberSaveable { mutableStateOf(false) }
    var showEditWorkoutDialog by rememberSaveable { mutableStateOf(false) }
    var showEnergyDetailsDialog by rememberSaveable { mutableStateOf(false) }
    var showStepsDetailsDialog by rememberSaveable { mutableStateOf(false) }

    val totalCalories = foodEntries.sumOf { it.calories }
    val totalProtein = foodEntries.sumOf { it.protein.toDouble() }.toFloat()
    val totalCarbs = foodEntries.sumOf { it.carbs.toDouble() }.toFloat()
    val totalFat = foodEntries.sumOf { it.fat.toDouble() }.toFloat()

    val currentSteps = fitnessLog?.steps ?: 0
    val stepGoal = userProfile.dailyStepsGoal
    val isWorkoutDone = fitnessLog?.isWorkoutDone ?: false

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PurpleDarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date Switcher
            item {
                DateNavigationBar(
                    selectedDateStr = selectedDate,
                    onPreviousDay = onPreviousDay,
                    onNextDay = onNextDay,
                    onTodayClicked = onTodayClicked,
                    onDateSelected = onDateSelected
                )
            }

            // Calorie & Macro Dashboard Card
            item {
                CleanCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "ENERGY SUMMARY",
                                    color = PurpleAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$totalCalories / ${userProfile.dailyCaloriesGoal} kcal",
                                    color = PurpleLightText,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PurpleSurfaceVariant)
                                        .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${((totalCalories.toFloat() / userProfile.dailyCaloriesGoal.coerceAtLeast(1)) * 100).toInt()}%",
                                        color = PurpleAccent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PurpleSurfaceVariant)
                                        .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(8.dp))
                                        .clickable { showEnergyDetailsDialog = true }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("energy_more_details_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Insights,
                                                contentDescription = "More",
                                                tint = PurpleAccent,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "More",
                                                color = PurpleLightText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CalorieProgressRing(
                                consumed = totalCalories,
                                goal = userProfile.dailyCaloriesGoal,
                                size = 120.dp,
                                strokeWidth = 10.dp
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MacroProgressBar(
                                    label = "Protein",
                                    current = totalProtein,
                                    goal = userProfile.dailyProteinGoal,
                                    color = MacroProteinColor
                                )
                                MacroProgressBar(
                                    label = "Carbs",
                                    current = totalCarbs,
                                    goal = userProfile.dailyCarbsGoal,
                                    color = MacroCarbsColor
                                )
                                MacroProgressBar(
                                    label = "Fat",
                                    current = totalFat,
                                    goal = userProfile.dailyFatGoal,
                                    color = MacroFatColor
                                )
                            }
                        }
                    }
                }
            }

            // Activity Row: Steps & Workout
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Steps Card
                    CleanCard(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentModifier = Modifier.fillMaxHeight(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f, fill = false),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                            contentDescription = "Steps",
                                            tint = MetricStepsColor,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "STEPS",
                                            color = MetricStepsColor,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .height(24.dp)
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(PurpleSurfaceVariant)
                                            .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(7.dp))
                                            .clickable { showStepsDetailsDialog = true }
                                            .padding(horizontal = 6.dp)
                                            .testTag("steps_more_details_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Insights,
                                                contentDescription = "More",
                                                tint = PurpleAccent,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "More",
                                                color = PurpleLightText,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "%,d".format(currentSteps),
                                    color = PurpleLightText,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Goal: %,d".format(stepGoal),
                                    color = PurpleMutedText,
                                    fontSize = 11.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Step progress bar
                                val stepProgress = (currentSteps.toFloat() / stepGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(PurpleSurfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(stepProgress)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(MetricStepsColor)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Bottom actions: Quick step buttons and Edit button below
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Quick step buttons (+500, +1000)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(500, 1000).forEach { delta ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(PurpleSurfaceVariant)
                                                .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(6.dp))
                                                .clickable { onAddSteps(delta) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "+$delta",
                                                color = PurpleLightText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                // Edit button moved below +500 +1000 buttons
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PurpleSurfaceVariant)
                                        .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(8.dp))
                                        .clickable { showEditStepsDialog = true }
                                        .testTag("edit_steps_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = PurpleAccent,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Edit Steps",
                                            color = PurpleAccent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Workout Day Card
                    CleanCard(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentModifier = Modifier.fillMaxHeight(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f, fill = false),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FitnessCenter,
                                            contentDescription = "Workout",
                                            tint = MetricWorkoutColor,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "WORKOUT",
                                            color = MetricWorkoutColor,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .height(24.dp)
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(PurpleSurfaceVariant)
                                            .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(7.dp))
                                            .clickable { showEditWorkoutDialog = true }
                                            .padding(horizontal = 6.dp)
                                            .testTag("workout_more_details_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Insights,
                                                contentDescription = "More",
                                                tint = PurpleAccent,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "More",
                                                color = PurpleLightText,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = if (isWorkoutDone) "Done!" else "Rest Day",
                                    color = if (isWorkoutDone) MetricWorkoutColor else PurpleLightText,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isWorkoutDone) "${fitnessLog?.workoutType ?: "Workout"} • ${fitnessLog?.workoutMinutes ?: 45}m" else "Tap below to log",
                                    color = PurpleMutedText,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Workout indicator bar matching steps bar height and position
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (isWorkoutDone) MetricWorkoutColor else PurpleSurfaceVariant)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Bottom actions: status pill and Toggle button matching Edit button height
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(PurpleSurfaceVariant)
                                        .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isWorkoutDone) "${fitnessLog?.workoutMinutes ?: 45} min logged" else "Planned: 45 min",
                                        color = if (isWorkoutDone) MetricWorkoutColor else PurpleLightText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Workout Toggle Button
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isWorkoutDone) MetricWorkoutColor else PurpleSurfaceVariant)
                                        .border(
                                            1.dp,
                                            if (isWorkoutDone) MetricWorkoutColor else PurpleSurfaceBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onToggleWorkout() }
                                        .testTag("toggle_workout_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (isWorkoutDone) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                        Text(
                                            text = if (isWorkoutDone) "Completed" else "Mark Done",
                                            color = if (isWorkoutDone) Color.Black else PurpleLightText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Meal Breakdown Header
            val meals = listOf("Breakfast", "Lunch", "Dinner", "Snack")
            item {
                val allCollapsed = collapsedMeals.size == meals.size
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MEALS & FOOD",
                        color = PurpleAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (allCollapsed) "Expand All" else "Collapse All",
                        color = PurpleAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                collapsedMeals = if (allCollapsed) emptySet() else meals.toSet()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("toggle_all_meals_collapse")
                    )
                }
            }

            // Meals
            meals.forEach { meal ->
                val mealItems = foodEntries.filter { it.mealType.equals(meal, ignoreCase = true) }
                val mealCals = mealItems.sumOf { it.calories }
                val mealProtein = mealItems.sumOf { it.protein.toDouble() }.toFloat()
                val mealCarbs = mealItems.sumOf { it.carbs.toDouble() }.toFloat()
                val mealFat = mealItems.sumOf { it.fat.toDouble() }.toFloat()
                val isCollapsed = collapsedMeals.contains(meal)

                item(key = "meal_$meal") {
                    CleanCard {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        collapsedMeals = if (isCollapsed) {
                                            collapsedMeals - meal
                                        } else {
                                            collapsedMeals + meal
                                        }
                                    }
                                    .padding(vertical = 4.dp)
                                    .testTag("meal_header_$meal"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                        contentDescription = if (isCollapsed) "Expand $meal" else "Collapse $meal",
                                        tint = PurpleAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = meal,
                                                color = PurpleLightText,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(PurpleSurfaceVariant)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "${mealItems.size}",
                                                    color = PurpleMutedText,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Text(
                                                text = "$mealCals kcal",
                                                color = PurpleMutedText,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (mealItems.isNotEmpty()) {
                                                Text(
                                                    text = "•",
                                                    color = PurpleMutedText.copy(alpha = 0.5f),
                                                    fontSize = 11.sp
                                                )
                                                Text(
                                                    text = "P: ${mealProtein.toInt()}g",
                                                    color = MacroProteinColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "C: ${mealCarbs.toInt()}g",
                                                    color = MacroCarbsColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "F: ${mealFat.toInt()}g",
                                                    color = MacroFatColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        selectedMealForAdd = meal
                                        showAddFoodDialog = true
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(PurpleSurfaceVariant)
                                        .testTag("add_food_to_$meal")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add food to $meal",
                                        tint = PurpleAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = !isCollapsed,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column {
                                    if (mealItems.isEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "No food logged yet",
                                            color = PurpleSubtleText,
                                            fontSize = 12.sp
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            mealItems.forEach { item ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(PurpleSurfaceVariant)
                                                        .clickable { editingFoodEntry = item }
                                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                                        .testTag("food_entry_${item.id}"),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = item.name,
                                                            color = PurpleLightText,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        Text(
                                                            text = "${item.getServingDisplay()} • P: ${item.protein.toInt()}g • C: ${item.carbs.toInt()}g • F: ${item.fat.toInt()}g",
                                                            color = PurpleMutedText,
                                                            fontSize = 11.sp
                                                        )
                                                    }

                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = "${item.calories} kcal",
                                                            color = PurpleLightText,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        IconButton(
                                                            onClick = { editingFoodEntry = item },
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .testTag("edit_food_btn_${item.id}")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Edit,
                                                                contentDescription = "Edit ${item.name}",
                                                                tint = PurpleAccent,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                        IconButton(
                                                            onClick = { onDeleteFood(item.id) },
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .testTag("delete_food_btn_${item.id}")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "Remove ${item.name}",
                                                                tint = PurpleSubtleText,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button to Quick Add Food
        FloatingActionButton(
            onClick = {
                selectedMealForAdd = getDefaultMealType()
                showAddFoodDialog = true
            },
            containerColor = PurplePrimary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 20.dp)
                .testTag("fab_add_food")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Log Food",
                modifier = Modifier.size(28.dp)
            )
        }
    }

    // Add Food Dialog
    if (showAddFoodDialog) {
        AddFoodDialog(
            initialMealType = selectedMealForAdd,
            foodItems = foodDatabase,
            onSaveCustomFood = onSaveFoodItem,
            onDeleteCustomFood = onDeleteFoodItem,
            onDismiss = { showAddFoodDialog = false },
            onConfirm = { meal, name, cals, p, c, f, size, unit ->
                onAddFood(meal, name, cals, p, c, f, size, unit)
                showAddFoodDialog = false
            }
        )
    }

    // Edit Food Dialog
    if (editingFoodEntry != null) {
        EditFoodDialog(
            entry = editingFoodEntry!!,
            onDismiss = { editingFoodEntry = null },
            onSave = { updated ->
                onUpdateFood(updated)
                editingFoodEntry = null
            },
            onDelete = { id ->
                onDeleteFood(id)
                editingFoodEntry = null
            }
        )
    }

    // Edit Steps Dialog
    if (showEditStepsDialog) {
        var stepsInput by remember { mutableStateOf(currentSteps.toString()) }
        Dialog(
            onDismissRequest = { showEditStepsDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = PurpleSurface,
                border = BorderStroke(1.dp, PurpleSurfaceBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Update Steps",
                        color = PurpleLightText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = stepsInput,
                        onValueChange = { stepsInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Total steps for today") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MetricStepsColor,
                            unfocusedBorderColor = PurpleSurfaceBorder,
                            focusedTextColor = PurpleLightText,
                            unfocusedTextColor = PurpleLightText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                val st = stepsInput.toIntOrNull() ?: currentSteps
                                onSetSteps(st)
                                showEditStepsDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MetricStepsColor)
                        ) {
                            Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Edit Workout Dialog
    if (showEditWorkoutDialog) {
        var workoutTypeInput by remember { mutableStateOf(fitnessLog?.workoutType ?: "Strength") }
        var workoutMinutesInput by remember { mutableStateOf((fitnessLog?.workoutMinutes ?: 45).toString()) }
        var workoutNotesInput by remember { mutableStateOf(fitnessLog?.workoutNotes ?: "") }
        var isDoneState by remember { mutableStateOf(fitnessLog?.isWorkoutDone ?: true) }

        val workoutPresets = listOf("Strength", "Cardio", "HIIT", "Yoga / Stretch", "Run", "Walk")

        Dialog(
            onDismissRequest = { showEditWorkoutDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = PurpleSurface,
                border = BorderStroke(1.dp, PurpleSurfaceBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Workout Details",
                        color = PurpleLightText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(PurpleSurfaceVariant)
                            .clickable { isDoneState = !isDoneState }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isDoneState,
                            onCheckedChange = { isDoneState = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MetricWorkoutColor,
                                checkmarkColor = Color.Black
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Workout completed today",
                            color = PurpleLightText,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        workoutPresets.take(3).forEach { type ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (workoutTypeInput == type) PurplePrimary else PurpleSurfaceVariant)
                                    .clickable { workoutTypeInput = type }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = type,
                                    color = if (workoutTypeInput == type) Color.White else PurpleMutedText,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        workoutPresets.drop(3).forEach { type ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (workoutTypeInput == type) PurplePrimary else PurpleSurfaceVariant)
                                    .clickable { workoutTypeInput = type }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = type,
                                    color = if (workoutTypeInput == type) Color.White else PurpleMutedText,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = workoutTypeInput,
                        onValueChange = { workoutTypeInput = it },
                        label = { Text("Workout Name / Focus") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MetricWorkoutColor,
                            unfocusedBorderColor = PurpleSurfaceBorder,
                            focusedTextColor = PurpleLightText,
                            unfocusedTextColor = PurpleLightText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = workoutMinutesInput,
                        onValueChange = { workoutMinutesInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Duration (minutes)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MetricWorkoutColor,
                            unfocusedBorderColor = PurpleSurfaceBorder,
                            focusedTextColor = PurpleLightText,
                            unfocusedTextColor = PurpleLightText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = workoutNotesInput,
                        onValueChange = { workoutNotesInput = it },
                        label = { Text("Notes (optional)") },
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MetricWorkoutColor,
                            unfocusedBorderColor = PurpleSurfaceBorder,
                            focusedTextColor = PurpleLightText,
                            unfocusedTextColor = PurpleLightText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                val mins = workoutMinutesInput.toIntOrNull() ?: 45
                                onSaveWorkout(workoutTypeInput, mins, workoutNotesInput, isDoneState)
                                showEditWorkoutDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MetricWorkoutColor)
                        ) {
                            Text("Save Workout", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // 7-Day Energy Details Dialog
    if (showEnergyDetailsDialog) {
        EnergyDetailsDialog(
            anchorDate = selectedDate,
            userProfile = userProfile,
            allFoodEntries = allFoodEntries,
            onDismiss = { showEnergyDetailsDialog = false }
        )
    }

    // 7-Day Activity / Steps Details Dialog
    if (showStepsDetailsDialog) {
        StepsDetailsDialog(
            anchorDate = selectedDate,
            userProfile = userProfile,
            allFitnessLogs = allFitnessLogs,
            weightEntries = allWeightEntries,
            onDismiss = { showStepsDetailsDialog = false }
        )
    }
}
