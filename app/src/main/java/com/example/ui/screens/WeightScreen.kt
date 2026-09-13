package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.UserProfile
import com.example.data.local.entity.WeightEntry
import com.example.ui.components.CleanCard
import com.example.ui.theme.MetricWeightColor
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
import kotlin.math.abs
import kotlin.math.roundToInt

enum class WeightTimeRange(val label: String, val days: Int?) {
    DAYS_7("7d", 7),
    DAYS_30("30d", 30),
    DAYS_90("90d", 90),
    YEAR_1("1y", 365),
    ALL("all", null)
}

@Composable
fun WeightScreen(
    userProfile: UserProfile,
    weightEntries: List<WeightEntry>,
    onAddWeight: (weight: Float, unit: String, notes: String, date: String) -> Unit,
    onDeleteWeight: (id: Long) -> Unit,
    onUpdateStartWeight: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showSetStartWeightDialog by remember { mutableStateOf(false) }
    var selectedRange by remember { mutableStateOf(WeightTimeRange.ALL) }

    val sortedEntries = remember(weightEntries) {
        weightEntries.sortedWith(compareBy({ it.date }, { it.timestamp }))
    }

    val filteredEntries = remember(sortedEntries, selectedRange) {
        if (selectedRange.days == null) {
            sortedEntries
        } else {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -selectedRange.days!!)
            val cutoffStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
            val list = sortedEntries.filter { it.date >= cutoffStr }
            if (list.isEmpty() && sortedEntries.isNotEmpty()) {
                sortedEntries.takeLast(selectedRange.days!!.coerceAtMost(sortedEntries.size))
            } else {
                list
            }
        }
    }

    val latestEntry = sortedEntries.lastOrNull()
    val firstEntry = sortedEntries.firstOrNull()

    val currentWeight = latestEntry?.weight ?: userProfile.startWeight
    val startWeight = userProfile.startWeight
    val targetWeight = userProfile.targetWeight
    val unit = latestEntry?.unit ?: userProfile.weightUnit

    // Net change explicitly calculated from true starting weight
    val deltaFromStart = currentWeight - startWeight
    val deltaToTarget = currentWeight - targetWeight

    val isGoalWeightLoss = targetWeight <= startWeight
    val isLoss = deltaFromStart <= 0
    val isFavorable = if (isGoalWeightLoss) isLoss else deltaFromStart >= 0

    // Goal completion percentage
    val totalGoalSpan = abs(targetWeight - startWeight)
    val progressFraction = if (totalGoalSpan > 0.01f) {
        val achieved = if (isGoalWeightLoss) (startWeight - currentWeight) else (currentWeight - startWeight)
        (achieved / totalGoalSpan).coerceIn(0f, 1f)
    } else 1f
    val progressPct = (progressFraction * 100f).roundToInt()

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
                    Column {
                        Text(
                            text = "WEIGHT & PROGRESS",
                            color = PurpleAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Body Trend Tracker",
                            color = PurpleLightText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("log_weight_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log Weight", fontSize = 13.sp)
                    }
                }
            }

            // Stat Cards - 2 Rows of 2 for optimal mobile layout
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Row 1: Starting & Current
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Starting Weight (Interactive anchor)
                        CleanCard(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showSetStartWeightDialog = true }
                                .testTag("starting_weight_card")
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "STARTING",
                                        color = PurpleAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Starting Weight",
                                        tint = PurpleMutedText,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "%.1f %s".format(startWeight, unit),
                                    color = PurpleLightText,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "True base anchor • tap to edit",
                                    color = PurpleMutedText,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Current Weight
                        CleanCard(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("current_weight_card")
                        ) {
                            Column {
                                Text(
                                    text = "CURRENT",
                                    color = PurpleLightText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "%.1f %s".format(currentWeight, unit),
                                    color = PurpleLightText,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (latestEntry != null) "Latest weigh-in" else "At start baseline",
                                    color = PurpleMutedText,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Row 2: Target Goal & Net Change
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Goal / Target
                        CleanCard(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("target_weight_card")
                        ) {
                            Column {
                                Text(
                                    text = "TARGET GOAL",
                                    color = MetricWeightColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "%.1f %s".format(targetWeight, unit),
                                    color = PurpleLightText,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val remaining = abs(deltaToTarget)
                                Text(
                                    text = "%.1f %s to target".format(remaining, unit),
                                    color = PurpleMutedText,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Net Change (based through starting weight at current weight)
                        CleanCard(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("net_change_card")
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "NET CHANGE",
                                        color = if (isFavorable) Color(0xFF34D399) else Color(0xFFFB7185),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Icon(
                                        imageVector = if (isLoss) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = if (isFavorable) Color(0xFF34D399) else Color(0xFFFB7185),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "%+.1f %s".format(deltaFromStart, unit),
                                    color = if (isFavorable) Color(0xFF34D399) else Color(0xFFFB7185),
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "from start %.1f %s".format(startWeight, unit),
                                    color = PurpleMutedText,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // Progress Journey Milestone Card
            item {
                CleanCard(modifier = Modifier.testTag("journey_progress_card")) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = null,
                                    tint = PurpleAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "GOAL PROGRESS",
                                    color = PurpleAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                            Text(
                                text = "$progressPct% Achieved",
                                color = if (progressPct >= 100) Color(0xFF34D399) else PurpleLightText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress bar track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(PurpleSurfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFraction)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(PurplePrimary, PurpleAccent)
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 3 Milestones: Start, Current, Goal
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("Start", color = PurpleMutedText, fontSize = 10.sp)
                                Text("%.1f %s".format(startWeight, unit), color = PurpleLightText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Current", color = PurpleAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("%.1f %s".format(currentWeight, unit), color = PurpleAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Goal Target", color = MetricWeightColor, fontSize = 10.sp)
                                Text("%.1f %s".format(targetWeight, unit), color = PurpleLightText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Progress Chart Card
            item {
                CleanCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "WEIGHT TRAJECTORY",
                                color = PurpleAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Start: %.1f • Goal: %.1f %s".format(startWeight, targetWeight, unit),
                                color = PurpleMutedText,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Filter Range Selector: 7d, 30d, 90d, 1y, all
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(PurpleSurfaceVariant)
                                .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(10.dp))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            WeightTimeRange.values().forEach { range ->
                                val isSelected = selectedRange == range
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) PurplePrimary else Color.Transparent)
                                        .clickable { selectedRange = range }
                                        .padding(vertical = 6.dp)
                                        .testTag("filter_range_${range.label}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = range.label,
                                        color = if (isSelected) Color.White else PurpleMutedText,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (filteredEntries.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Log weights to visualize your progress curve",
                                    color = PurpleMutedText,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            WeightTrendChart(
                                entries = filteredEntries,
                                startWeight = startWeight,
                                targetWeight = targetWeight,
                                unit = unit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(175.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Legend and bounds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 12.dp, height = 2.dp)
                                                .background(Color(0xFF94A3B8))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Start %.1f".format(startWeight), color = PurpleMutedText, fontSize = 10.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 12.dp, height = 2.dp)
                                                .background(Color(0xFFA855F7))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Target %.1f".format(targetWeight), color = PurpleMutedText, fontSize = 10.sp)
                                    }
                                }

                                val first = filteredEntries.first()
                                val last = filteredEntries.last()
                                Text(
                                    text = "${last.date.drop(5)}: %.1f %s".format(last.weight, unit),
                                    color = PurpleAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // History Header
            item {
                Text(
                    text = "WEIGH-IN LOGS",
                    color = PurpleAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // History List
            if (sortedEntries.isEmpty()) {
                item {
                    Text(
                        text = "No weigh-ins recorded yet.",
                        color = PurpleSubtleText,
                        fontSize = 13.sp
                    )
                }
            } else {
                val reversedList = sortedEntries.reversed()
                items(reversedList, key = { it.id }) { entry ->
                    CleanCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.date,
                                    color = PurpleLightText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (entry.notes.isNotBlank()) {
                                    Text(
                                        text = entry.notes,
                                        color = PurpleMutedText,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val entryDiffFromStart = entry.weight - startWeight
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "%.1f %s".format(entry.weight, entry.unit),
                                        color = PurpleAccent,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "%+.1f vs start".format(entryDiffFromStart),
                                        color = if ((if (isGoalWeightLoss) entryDiffFromStart <= 0 else entryDiffFromStart >= 0)) Color(0xFF34D399) else PurpleMutedText,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { onDeleteWeight(entry.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete weigh-in",
                                        tint = PurpleSubtleText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Set Starting Weight Dialog
    if (showSetStartWeightDialog) {
        var startWeightInput by remember { mutableStateOf(startWeight.toString()) }

        Dialog(
            onDismissRequest = { showSetStartWeightDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = PurpleSurface,
                border = BorderStroke(1.dp, PurpleSurfaceBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .testTag("set_starting_weight_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PurplePrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null,
                                tint = PurpleAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "True Starting Weight",
                                color = PurpleLightText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Base anchor for net weight progress",
                                color = PurpleMutedText,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Net change and goal progress will be measured from this true baseline at your current weigh-in.",
                        color = PurpleMutedText,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = startWeightInput,
                        onValueChange = { startWeightInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Starting Weight ($unit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = PurpleSurfaceBorder,
                            focusedTextColor = PurpleLightText,
                            unfocusedTextColor = PurpleLightText,
                            focusedLabelColor = PurpleAccent,
                            unfocusedLabelColor = PurpleMutedText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("starting_weight_dialog_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick suggestion chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (firstEntry != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PurpleSurfaceVariant)
                                    .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(8.dp))
                                    .clickable { startWeightInput = firstEntry.weight.toString() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "1st Log: %.1f".format(firstEntry.weight),
                                    color = PurpleLightText,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (latestEntry != null && latestEntry.id != firstEntry?.id) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PurpleSurfaceVariant)
                                    .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(8.dp))
                                    .clickable { startWeightInput = latestEntry.weight.toString() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Latest: %.1f".format(latestEntry.weight),
                                    color = PurpleLightText,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { showSetStartWeightDialog = false },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleMutedText),
                            border = BorderStroke(1.dp, PurpleSurfaceBorder)
                        ) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                val w = startWeightInput.toFloatOrNull()
                                if (w != null && w > 0f) {
                                    onUpdateStartWeight(w)
                                    showSetStartWeightDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            modifier = Modifier.testTag("save_starting_weight_button")
                        ) {
                            Text("Save Baseline", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    // Add Weight Dialog
    if (showAddDialog) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var dateInput by remember { mutableStateOf(dateFormat.format(Date())) }
        var weightInput by remember { mutableStateOf(currentWeight.toString()) }
        var notesInput by remember { mutableStateOf("") }

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
                        text = "Log Weight",
                        color = PurpleLightText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Weight ($unit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = PurpleSurfaceBorder,
                            focusedTextColor = PurpleLightText,
                            unfocusedTextColor = PurpleLightText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("weight_input_field")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

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

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Notes (optional, e.g. morning, post-fast)") },
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
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                val w = weightInput.toFloatOrNull()
                                if (w != null && w > 0) {
                                    onAddWeight(w, unit, notesInput, dateInput)
                                    showAddDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                        ) {
                            Text("Save Weigh-in", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeightTrendChart(
    entries: List<WeightEntry>,
    startWeight: Float,
    targetWeight: Float,
    unit: String,
    modifier: Modifier = Modifier
) {
    val chartLineColor = PurplePrimary
    val dotBgColor = PurpleSurface
    val dotInnerColor = PurpleAccent

    Canvas(modifier = modifier) {
        if (entries.isEmpty()) return@Canvas

        val paddingLeft = 40.dp.toPx()
        val paddingRight = 20.dp.toPx()
        val paddingTop = 20.dp.toPx()
        val paddingBottom = 30.dp.toPx()

        val width = size.width - paddingLeft - paddingRight
        val height = size.height - paddingTop - paddingBottom

        val weights = entries.map { it.weight }
        val minWeight = minOf(weights.minOrNull() ?: 60f, targetWeight, startWeight) - 1.5f
        val maxWeight = maxOf(weights.maxOrNull() ?: 80f, targetWeight, startWeight) + 1.5f
        val range = (maxWeight - minWeight).coerceAtLeast(1f)

        // Draw Start Weight Line (dashed gray guideline)
        if (startWeight > 0f) {
            val startY = paddingTop + (1f - (startWeight - minWeight) / range) * height
            drawLine(
                color = Color(0x6694A3B8),
                start = Offset(paddingLeft, startY),
                end = Offset(size.width - paddingRight, startY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )
        }

        // Draw Target Line (dotted purple guideline)
        val targetY = paddingTop + (1f - (targetWeight - minWeight) / range) * height
        drawLine(
            color = Color(0x88A855F7),
            start = Offset(paddingLeft, targetY),
            end = Offset(size.width - paddingRight, targetY),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )

        // Plot points
        val points = entries.mapIndexed { index, entry ->
            val x = if (entries.size > 1) {
                paddingLeft + (index.toFloat() / (entries.size - 1)) * width
            } else {
                paddingLeft + width / 2f
            }
            val y = paddingTop + (1f - (entry.weight - minWeight) / range) * height
            Offset(x, y)
        }

        // Draw filled gradient below the curve
        if (points.size > 1) {
            val fillPath = Path().apply {
                moveTo(points.first().x, size.height - paddingBottom)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, size.height - paddingBottom)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x44A855F7), Color(0x00A855F7)),
                    startY = paddingTop,
                    endY = size.height - paddingBottom
                )
            )

            // Draw line
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
            drawPath(
                path = linePath,
                color = chartLineColor,
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // Draw circular dots
        points.forEach { pt ->
            drawCircle(
                color = dotBgColor,
                radius = 6.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = dotInnerColor,
                radius = 4.dp.toPx(),
                center = pt
            )
        }
    }
}
