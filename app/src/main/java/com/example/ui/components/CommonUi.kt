package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleCardHover
import com.example.ui.theme.PurpleLightText
import com.example.ui.theme.PurpleMutedText
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurpleSubtleText
import com.example.ui.theme.PurpleSurface
import com.example.ui.theme.PurpleSurfaceBorder
import com.example.ui.theme.PurpleSurfaceVariant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateNavigationBar(
    selectedDateStr: String,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onTodayClicked: () -> Unit,
    onDateSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showCalendarDialog by rememberSaveable { mutableStateOf(false) }

    val displayLabel = try {
        val inFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        val todayStr = inFormat.format(Date())
        if (selectedDateStr == todayStr) {
            "Today (${outFormat.format(Date())})"
        } else {
            val date = inFormat.parse(selectedDateStr)
            date?.let { outFormat.format(it) } ?: selectedDateStr
        }
    } catch (_: Exception) {
        selectedDateStr
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PurpleSurface)
            .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousDay,
            modifier = Modifier.testTag("prev_day_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous Day",
                tint = PurpleLightText
            )
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { showCalendarDialog = true }
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("current_date_button"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = "Open Calendar",
                tint = PurpleAccent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = displayLabel,
                color = PurpleLightText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }

        IconButton(
            onClick = onNextDay,
            modifier = Modifier.testTag("next_day_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Next Day",
                tint = PurpleLightText
            )
        }
    }

    if (showCalendarDialog) {
        val initialMillis = remember(selectedDateStr) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                sdf.parse(selectedDateStr)?.time
            } catch (_: Exception) {
                null
            } ?: System.currentTimeMillis()
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )

        DatePickerDialog(
            onDismissRequest = { showCalendarDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            onDateSelected(sdf.format(Date(millis)))
                        }
                        showCalendarDialog = false
                    },
                    modifier = Modifier.testTag("calendar_confirm_button")
                ) {
                    Text("Select", color = PurpleAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            onTodayClicked()
                            showCalendarDialog = false
                        },
                        modifier = Modifier.testTag("calendar_today_button")
                    ) {
                        Text("Today", color = PurpleAccent)
                    }
                    TextButton(
                        onClick = { showCalendarDialog = false },
                        modifier = Modifier.testTag("calendar_cancel_button")
                    ) {
                        Text("Cancel", color = PurpleMutedText)
                    }
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = PurpleSurface
            ),
            modifier = Modifier.testTag("calendar_dialog")
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = PurpleSurface,
                    titleContentColor = PurpleLightText,
                    headlineContentColor = PurpleLightText,
                    weekdayContentColor = PurpleMutedText,
                    subheadContentColor = PurpleAccent,
                    yearContentColor = PurpleLightText,
                    currentYearContentColor = PurpleAccent,
                    selectedYearContentColor = Color.White,
                    selectedYearContainerColor = PurplePrimary,
                    dayContentColor = PurpleLightText,
                    selectedDayContentColor = Color.White,
                    selectedDayContainerColor = PurplePrimary,
                    todayContentColor = PurpleAccent,
                    todayDateBorderColor = PurpleAccent
                )
            )
        }
    }
}

@Composable
fun CalorieProgressRing(
    consumed: Int,
    goal: Int,
    size: Dp = 150.dp,
    strokeWidth: Dp = 12.dp,
    modifier: Modifier = Modifier
) {
    val progress = if (goal > 0) (consumed.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800),
        label = "calorie_progress"
    )

    val remaining = goal - consumed

    val trackBgColor = PurpleSurfaceVariant
    val primaryColor = PurplePrimary
    val accentColor = PurpleAccent

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            // Track background ring
            drawArc(
                color = trackBgColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            // Progress ring
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(primaryColor, accentColor, primaryColor)
                ),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (remaining >= 0) "$remaining" else "+${-remaining}",
                color = PurpleLightText,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (remaining >= 0) "kcal left" else "over goal",
                color = if (remaining >= 0) PurpleMutedText else Color(0xFFFB7185),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun MacroProgressBar(
    label: String,
    current: Float,
    goal: Float,
    color: Color,
    unit: String = "g",
    modifier: Modifier = Modifier
) {
    val progress = if (goal > 0) (current / goal).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(600),
        label = "macro_progress"
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = PurpleLightText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${current.toInt()} / ${goal.toInt()}$unit",
                color = PurpleMutedText,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(PurpleSurfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun CleanCard(
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = PurpleSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.padding(contentPadding).then(contentModifier)) {
            content()
        }
    }
}
