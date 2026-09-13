package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.ui.components.CleanCard
import com.example.ui.theme.MetricWorkoutColor
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleDarkBackground
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

@Composable
fun WorkoutsScreen(
    allFitnessLogs: List<DailyFitnessLog>,
    onSaveWorkout: (date: String, type: String, minutes: Int, notes: String, isDone: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val dayNameFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val dayNumFormat = remember { SimpleDateFormat("d", Locale.getDefault()) }

    var showAddDialog by remember { mutableStateOf(false) }

    val todayStr = remember { dateFormat.format(Date()) }

    // Last 14 days list for consistency calendar
    val recentDays = remember {
        val list = mutableListOf<Triple<String, String, String>>() // dateStr, dayName, dayNum
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -13)
        for (i in 0..13) {
            val dStr = dateFormat.format(cal.time)
            val dName = dayNameFormat.format(cal.time)
            val dNum = dayNumFormat.format(cal.time)
            list.add(Triple(dStr, dName, dNum))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val workoutLogs = remember(allFitnessLogs) {
        allFitnessLogs.filter { it.isWorkoutDone }.sortedByDescending { it.date }
    }

    val totalWorkoutsThisMonth = workoutLogs.count {
        it.date.startsWith(todayStr.take(7))
    }
    val totalMinutes = workoutLogs.sumOf { it.workoutMinutes }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PurpleDarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Text(
                            text = "WORKOUT DAYS",
                            color = PurpleAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Consistency & Training",
                            color = PurpleLightText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("log_workout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Workout",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Stats Summary - Uniform size for all 3 tiles
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WorkoutStatTile(
                        label = "THIS MONTH",
                        labelColor = MetricWorkoutColor,
                        value = "$totalWorkoutsThisMonth",
                        unit = "days",
                        modifier = Modifier.weight(1f)
                    )

                    WorkoutStatTile(
                        label = "TOTAL LOGGED",
                        labelColor = PurpleAccent,
                        value = "${workoutLogs.size}",
                        unit = "sessions",
                        modifier = Modifier.weight(1f)
                    )

                    WorkoutStatTile(
                        label = "TRAINING TIME",
                        labelColor = PurpleMutedText,
                        value = "$totalMinutes",
                        unit = "min",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 14-day Habit & Consistency Matrix
            item {
                CleanCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PAST 14 DAYS CONSISTENCY",
                                color = PurpleAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Purple = Workout Day",
                                color = PurpleMutedText,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Render in 2 rows of 7 days
                        val row1 = recentDays.take(7)
                        val row2 = recentDays.drop(7)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            row1.forEach { (dStr, dName, dNum) ->
                                val isDone = allFitnessLogs.any { it.date == dStr && it.isWorkoutDone }
                                DayBubble(
                                    dateNum = dNum,
                                    dayName = dName,
                                    isDone = isDone,
                                    isToday = dStr == todayStr,
                                    onClick = {
                                        // Quick toggle
                                        val existing = allFitnessLogs.find { it.date == dStr }
                                        onSaveWorkout(
                                            dStr,
                                            existing?.workoutType ?: "Strength",
                                            existing?.workoutMinutes ?: 45,
                                            existing?.workoutNotes ?: "",
                                            !(existing?.isWorkoutDone ?: false)
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            row2.forEach { (dStr, dName, dNum) ->
                                val isDone = allFitnessLogs.any { it.date == dStr && it.isWorkoutDone }
                                DayBubble(
                                    dateNum = dNum,
                                    dayName = dName,
                                    isDone = isDone,
                                    isToday = dStr == todayStr,
                                    onClick = {
                                        val existing = allFitnessLogs.find { it.date == dStr }
                                        onSaveWorkout(
                                            dStr,
                                            existing?.workoutType ?: "Strength",
                                            existing?.workoutMinutes ?: 45,
                                            existing?.workoutNotes ?: "",
                                            !(existing?.isWorkoutDone ?: false)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Recent Sessions Header
            item {
                Text(
                    text = "RECENT WORKOUT SESSIONS",
                    color = PurpleAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (workoutLogs.isEmpty()) {
                item {
                    Text(
                        text = "No workouts logged yet. Tap '+ Workout' or tap any day bubble above to record a session.",
                        color = PurpleSubtleText,
                        fontSize = 13.sp
                    )
                }
            } else {
                items(workoutLogs, key = { it.date }) { log ->
                    CleanCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(PurpleSurfaceVariant)
                                    .border(1.dp, PurpleAccent.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = PurpleAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Text(
                                    text = log.workoutType.ifBlank { "Workout" },
                                    color = PurpleLightText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${log.date} • ${log.workoutMinutes} min",
                                    color = PurpleMutedText,
                                    fontSize = 12.sp
                                )
                                if (log.workoutNotes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = log.workoutNotes,
                                        color = PurpleSubtleText,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            // Fixed size Trash Bin Button
                            IconButton(
                                onClick = {
                                    onSaveWorkout(
                                        log.date,
                                        log.workoutType,
                                        log.workoutMinutes,
                                        log.workoutNotes,
                                        false
                                    )
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE11D48).copy(alpha = 0.12f))
                                    .testTag("delete_workout_${log.date}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove workout",
                                    tint = Color(0xFFFB7185),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var dateInput by remember { mutableStateOf(todayStr) }
        var typeInput by remember { mutableStateOf("Strength (Upper Body)") }
        var minutesInput by remember { mutableStateOf("45") }
        var notesInput by remember { mutableStateOf("") }

        val presets = listOf("Strength", "Cardio Run", "HIIT", "Calisthenics", "Mobility / Yoga")

        Dialog(
            onDismissRequest = { showAddDialog = false },
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
                        text = "Log Workout Session",
                        color = PurpleLightText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = dateInput,
                        onValueChange = { dateInput = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = PurpleSurfaceBorder,
                            focusedTextColor = PurpleLightText,
                            unfocusedTextColor = PurpleLightText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.take(3).forEach { p ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (typeInput == p) PurplePrimary else PurpleSurfaceVariant)
                                    .clickable { typeInput = p }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = p,
                                    color = if (typeInput == p) Color.White else PurpleMutedText,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = typeInput,
                        onValueChange = { typeInput = it },
                        label = { Text("Workout Focus / Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = PurpleSurfaceBorder,
                            focusedTextColor = PurpleLightText,
                            unfocusedTextColor = PurpleLightText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = minutesInput,
                        onValueChange = { minutesInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Duration (minutes)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = PurpleSurfaceBorder,
                            focusedTextColor = PurpleLightText,
                            unfocusedTextColor = PurpleLightText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Notes (sets, exercises, reps)") },
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = PurpleSurfaceBorder,
                            focusedTextColor = PurpleLightText,
                            unfocusedTextColor = PurpleLightText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showAddDialog = false },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PurpleMutedText
                            ),
                            border = BorderStroke(1.dp, PurpleSurfaceBorder),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                val mins = minutesInput.toIntOrNull() ?: 45
                                onSaveWorkout(dateInput, typeInput, mins, notesInput, true)
                                showAddDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save Workout", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutStatTile(
    label: String,
    labelColor: Color,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(PurpleSurface)
            .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = labelColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    color = PurpleLightText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 20.sp
                )
                if (unit.isNotBlank()) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = unit,
                        color = PurpleMutedText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayBubble(
    dateNum: String,
    dayName: String,
    isDone: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Text(
            text = dayName.take(2),
            color = PurpleMutedText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isDone) PurplePrimary else PurpleSurfaceVariant)
                .border(
                    1.dp,
                    if (isToday) PurpleAccent else if (isDone) PurplePrimary else PurpleSurfaceBorder,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = dateNum,
                    color = if (isToday) PurpleAccent else PurpleMutedText,
                    fontSize = 11.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
