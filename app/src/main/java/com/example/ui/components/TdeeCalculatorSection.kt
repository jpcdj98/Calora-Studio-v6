package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.util.ActivityLevel
import com.example.data.util.BiologicalSex
import com.example.data.util.GoalStrategy
import com.example.data.util.TdeeCalculator
import com.example.data.util.TdeePreferences
import com.example.ui.theme.MacroCarbsColor
import com.example.ui.theme.MacroFatColor
import com.example.ui.theme.MacroProteinColor
import com.example.ui.theme.MetricStepsColor
import com.example.ui.theme.MetricWorkoutColor
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleLightText
import com.example.ui.theme.PurpleMutedText
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurpleSurfaceBorder
import com.example.ui.theme.PurpleSurfaceVariant
import kotlin.math.roundToInt

@Composable
fun TdeeCalculatorSection(
    weightInput: String,
    weightUnit: String,
    onWeightChange: (String) -> Unit,
    activeMacroPresetName: String,
    currentDailyCaloriesTarget: Int,
    onApplyCaloriesTarget: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tdeePrefs = remember { TdeePreferences(context) }

    var sex by remember { mutableStateOf(tdeePrefs.sex) }
    var ageText by remember { mutableStateOf(tdeePrefs.age.toString()) }
    var heightUnit by remember { mutableStateOf(tdeePrefs.heightUnit) } // "cm" or "ft_in"
    var heightCmText by remember { mutableStateOf(tdeePrefs.heightCm.roundToInt().toString()) }

    // Feet & inches state
    val initialFeetInches = remember(tdeePrefs.heightCm) {
        TdeeCalculator.cmToFeetInches(tdeePrefs.heightCm)
    }
    var feetText by remember { mutableStateOf(initialFeetInches.first.toString()) }
    var inchesText by remember { mutableStateOf(initialFeetInches.second.roundToInt().toString()) }

    var activityLevel by remember { mutableStateOf(tdeePrefs.activityLevel) }
    var selectedStrategy by remember { mutableStateOf(tdeePrefs.strategy) }
    var cutDeficitKcal by remember { mutableStateOf(tdeePrefs.cutDeficitKcal) }
    var bulkSurplusKcal by remember { mutableStateOf(tdeePrefs.bulkSurplusKcal) }
    var autoSyncWithNutrition by remember { mutableStateOf(tdeePrefs.autoSyncWithNutrition) }

    // Resolve weight in kg for the BMR formula
    val rawWeight = weightInput.toFloatOrNull() ?: if (weightUnit == "lbs") 170f else 75f
    val weightKg = if (weightUnit.equals("lbs", ignoreCase = true)) {
        TdeeCalculator.lbsToKg(rawWeight)
    } else {
        rawWeight
    }

    // Resolve height in cm
    val heightCm = if (heightUnit == "ft_in") {
        val ft = feetText.toIntOrNull() ?: 5
        val inc = inchesText.toFloatOrNull() ?: 9f
        TdeeCalculator.feetInchesToCm(ft, inc)
    } else {
        heightCmText.toFloatOrNull() ?: 175f
    }

    val age = ageText.toIntOrNull()?.coerceIn(12, 100) ?: 28

    val calorieAdjustment = when (selectedStrategy) {
        GoalStrategy.CUT -> cutDeficitKcal
        GoalStrategy.MAINTAIN -> 0
        GoalStrategy.BULK -> bulkSurplusKcal
    }

    // Compute calculation
    val calculation = remember(weightKg, heightCm, age, sex, activityLevel, selectedStrategy, calorieAdjustment) {
        TdeeCalculator.calculate(
            weightKg = weightKg,
            heightCm = heightCm,
            age = age,
            sex = sex,
            activityLevel = activityLevel,
            strategy = selectedStrategy,
            calorieAdjustment = calorieAdjustment
        )
    }

    // Save preferences and auto-sync when values change
    LaunchedEffect(sex, age, heightCm, heightUnit, activityLevel, selectedStrategy, cutDeficitKcal, bulkSurplusKcal, autoSyncWithNutrition) {
        tdeePrefs.sex = sex
        tdeePrefs.age = age
        tdeePrefs.heightCm = heightCm
        tdeePrefs.heightUnit = heightUnit
        tdeePrefs.activityLevel = activityLevel
        tdeePrefs.strategy = selectedStrategy
        tdeePrefs.cutDeficitKcal = cutDeficitKcal
        tdeePrefs.bulkSurplusKcal = bulkSurplusKcal
        tdeePrefs.autoSyncWithNutrition = autoSyncWithNutrition

        if (autoSyncWithNutrition && calculation.targetCalories > 0 && calculation.targetCalories != currentDailyCaloriesTarget) {
            onApplyCaloriesTarget(calculation.targetCalories)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tdee_calculator_section")
    ) {
        Text(
            text = "Calculate your Total Daily Energy Expenditure (TDEE) and select your fitness goal strategy to align nutrition targets automatically.",
            color = PurpleMutedText,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Biological Sex selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BIOLOGICAL SEX",
                color = PurpleAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(PurpleSurfaceVariant)
                    .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(10.dp))
                    .padding(3.dp)
            ) {
                BiologicalSex.entries.forEach { s ->
                    val isSel = s == sex
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) PurplePrimary else Color.Transparent)
                            .clickable { sex = s }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("sex_toggle_${s.name.lowercase()}")
                    ) {
                        Text(
                            text = if (s == BiologicalSex.MALE) "Male ♂" else "Female ♀",
                            color = if (isSel) Color.White else PurpleMutedText,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Age & Weight & Height Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Age
            OutlinedTextField(
                value = ageText,
                onValueChange = { newVal ->
                    val filtered = newVal.filter { it.isDigit() }.take(3)
                    ageText = filtered
                },
                label = { Text("Age") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    .weight(0.9f)
                    .testTag("tdee_age_input")
            )

            // Weight (synced with starting weight)
            OutlinedTextField(
                value = weightInput,
                onValueChange = { onWeightChange(it.filter { c -> c.isDigit() || c == '.' }) },
                label = { Text("Weight ($weightUnit)") },
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
                    .weight(1.1f)
                    .testTag("tdee_weight_input")
            )

            // Height input
            if (heightUnit == "cm") {
                OutlinedTextField(
                    value = heightCmText,
                    onValueChange = { newVal ->
                        val filtered = newVal.filter { it.isDigit() }.take(3)
                        heightCmText = filtered
                    },
                    label = { Text("Height (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                        .weight(1.1f)
                        .testTag("tdee_height_cm_input")
                )
            } else {
                Row(
                    modifier = Modifier.weight(1.3f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedTextField(
                        value = feetText,
                        onValueChange = { feetText = it.filter { c -> c.isDigit() }.take(1) },
                        label = { Text("Ft") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = PurpleSurfaceBorder,
                            focusedTextColor = PurpleLightText,
                            unfocusedTextColor = PurpleLightText,
                            focusedLabelColor = PurpleAccent,
                            unfocusedLabelColor = PurpleMutedText
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = inchesText,
                        onValueChange = { inchesText = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("In") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = PurpleSurfaceBorder,
                            focusedTextColor = PurpleLightText,
                            unfocusedTextColor = PurpleLightText,
                            focusedLabelColor = PurpleAccent,
                            unfocusedLabelColor = PurpleMutedText
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Height Unit toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (heightUnit == "cm") "Switch to ft/in" else "Switch to cm",
                color = PurpleAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        if (heightUnit == "cm") {
                            val cm = heightCmText.toFloatOrNull() ?: 175f
                            val (f, i) = TdeeCalculator.cmToFeetInches(cm)
                            feetText = f.toString()
                            inchesText = i.roundToInt().toString()
                            heightUnit = "ft_in"
                        } else {
                            val f = feetText.toIntOrNull() ?: 5
                            val i = inchesText.toFloatOrNull() ?: 9f
                            heightCmText = TdeeCalculator.feetInchesToCm(f, i).roundToInt().toString()
                            heightUnit = "cm"
                        }
                    }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Activity Level
        Text(
            text = "ACTIVITY LEVEL",
            color = PurpleAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ActivityLevel.entries.forEach { level ->
                val isSel = level == activityLevel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSel) PurplePrimary.copy(alpha = 0.2f) else PurpleSurfaceVariant)
                        .border(
                            1.dp,
                            if (isSel) PurplePrimary else PurpleSurfaceBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { activityLevel = level }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("activity_level_${level.id}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = level.displayName,
                                color = if (isSel) Color.White else PurpleLightText,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "×${level.multiplier}",
                                color = PurpleAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "${level.shortDesc} • ${level.stepGuide}",
                            color = PurpleMutedText,
                            fontSize = 10.sp
                        )
                    }
                    if (isSel) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PurplePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // BMR & TDEE Stat Highlights
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // BMR Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PurpleSurfaceVariant)
                    .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "BMR (At Rest)",
                        color = PurpleMutedText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${calculation.bmr} kcal",
                        color = PurpleLightText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Basal calorie burn",
                        color = PurpleMutedText,
                        fontSize = 9.sp
                    )
                }
            }

            // TDEE Maintenance Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PurpleSurfaceVariant)
                    .border(1.dp, PurpleAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "TDEE (Maintenance)",
                            color = PurpleAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${calculation.tdee} kcal",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Daily burn with activity",
                        color = PurpleMutedText,
                        fontSize = 9.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Goal Strategy (Cut / Maintain / Bulk)
        Text(
            text = "SELECT GOAL STRATEGY",
            color = PurpleAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GoalStrategy.entries.forEach { strat ->
                val isSel = strat == selectedStrategy
                val color = when (strat) {
                    GoalStrategy.CUT -> MacroCarbsColor
                    GoalStrategy.MAINTAIN -> MetricStepsColor
                    GoalStrategy.BULK -> MetricWorkoutColor
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSel) color.copy(alpha = 0.22f) else PurpleSurfaceVariant)
                        .border(
                            1.dp,
                            if (isSel) color else PurpleSurfaceBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedStrategy = strat }
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .testTag("strategy_tab_${strat.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (strat) {
                                GoalStrategy.CUT -> "✂️ Cut"
                                GoalStrategy.MAINTAIN -> "⚖️ Maintain"
                                GoalStrategy.BULK -> "🚀 Bulk"
                            },
                            color = if (isSel) Color.White else PurpleLightText,
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = strat.shortLabel,
                            color = if (isSel) color else PurpleMutedText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Strategy Sub-options
        when (selectedStrategy) {
            GoalStrategy.CUT -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MacroCarbsColor.copy(alpha = 0.08f))
                        .border(1.dp, MacroCarbsColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Caloric Deficit Intensity",
                        color = MacroCarbsColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val deficitPresets = listOf(
                            Triple(250, "Mild", "-250 kcal"),
                            Triple(500, "Standard", "-500 kcal"),
                            Triple(750, "Aggressive", "-750 kcal")
                        )
                        deficitPresets.forEach { (def, title, subtitle) ->
                            val isChosen = cutDeficitKcal == def
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isChosen) MacroCarbsColor.copy(alpha = 0.22f) else PurpleSurfaceVariant)
                                    .border(
                                        1.dp,
                                        if (isChosen) MacroCarbsColor else PurpleSurfaceBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { cutDeficitKcal = def }
                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                                    .testTag("deficit_intensity_${title.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = title,
                                        color = if (isChosen) Color.White else PurpleLightText,
                                        fontSize = 11.sp,
                                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = subtitle,
                                        color = if (isChosen) MacroCarbsColor else PurpleMutedText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Target: ${calculation.targetCalories} kcal • Est. weekly loss: ${String.format(java.util.Locale.US, "%.2f", kotlin.math.abs(calculation.weeklyWeightChangeKg))} kg (${String.format(java.util.Locale.US, "%.1f", kotlin.math.abs(calculation.weeklyWeightChangeLbs))} lbs) / week",
                        color = PurpleLightText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            GoalStrategy.MAINTAIN -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MetricStepsColor.copy(alpha = 0.08f))
                        .border(1.dp, MetricStepsColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Steady State Maintenance",
                            color = MetricStepsColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Target: ${calculation.targetCalories} kcal • Keeps your current body weight stable while optimizing workout recovery.",
                            color = PurpleLightText,
                            fontSize = 10.sp
                        )
                    }
                }
            }
            GoalStrategy.BULK -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MetricWorkoutColor.copy(alpha = 0.08f))
                        .border(1.dp, MetricWorkoutColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Caloric Surplus Intensity",
                        color = MetricWorkoutColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val surplusPresets = listOf(
                            Triple(250, "Lean", "+250 kcal"),
                            Triple(350, "Moderate", "+350 kcal"),
                            Triple(500, "Aggressive", "+500 kcal")
                        )
                        surplusPresets.forEach { (sur, title, subtitle) ->
                            val isChosen = bulkSurplusKcal == sur
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isChosen) MetricWorkoutColor.copy(alpha = 0.22f) else PurpleSurfaceVariant)
                                    .border(
                                        1.dp,
                                        if (isChosen) MetricWorkoutColor else PurpleSurfaceBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { bulkSurplusKcal = sur }
                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                                    .testTag("surplus_intensity_${title.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = title,
                                        color = if (isChosen) Color.White else PurpleLightText,
                                        fontSize = 11.sp,
                                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = subtitle,
                                        color = if (isChosen) MetricWorkoutColor else PurpleMutedText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Target: ${calculation.targetCalories} kcal • Est. weekly gain: +${String.format(java.util.Locale.US, "%.2f", calculation.weeklyWeightChangeKg)} kg (+${String.format(java.util.Locale.US, "%.1f", calculation.weeklyWeightChangeLbs)} lbs) / week",
                        color = PurpleLightText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Seamless Nutrition Integration Card
        val isTargetSynced = currentDailyCaloriesTarget == calculation.targetCalories

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(PurpleSurfaceVariant)
                .border(
                    1.dp,
                    if (isTargetSynced) MetricStepsColor.copy(alpha = 0.5f) else PurpleSurfaceBorder,
                    RoundedCornerShape(14.dp)
                )
                .padding(12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "RECOMMENDED NUTRITION TARGET",
                            color = PurpleAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${calculation.targetCalories}",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = " kcal / day",
                                color = PurpleMutedText,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }

                    if (isTargetSynced) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MetricStepsColor.copy(alpha = 0.15f))
                                .border(1.dp, MetricStepsColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MetricStepsColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Synced",
                                    color = MetricStepsColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Recommended Macros for active preset
                val (protRatio, carbRatio, fatRatio) = when (activeMacroPresetName) {
                    "High Protein" -> Triple(0.35f, 0.40f, 0.25f)
                    "Low Carb" -> Triple(0.35f, 0.25f, 0.40f)
                    else -> Triple(0.25f, 0.50f, 0.25f) // Balanced
                }
                val pG = (calculation.targetCalories * protRatio / 4f).roundToInt()
                val cG = (calculation.targetCalories * carbRatio / 4f).roundToInt()
                val fG = (calculation.targetCalories * fatRatio / 9f).roundToInt()

                val macroCards = listOf(
                    Triple("Protein", "${pG}g", Pair((protRatio * 100).roundToInt(), MacroProteinColor)),
                    Triple("Carbs", "${cG}g", Pair((carbRatio * 100).roundToInt(), MacroCarbsColor)),
                    Triple("Fat", "${fG}g", Pair((fatRatio * 100).roundToInt(), MacroFatColor))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    macroCards.forEach { (name, amount, details) ->
                        val (pct, color) = details
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(68.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(color.copy(alpha = 0.12f))
                                .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                                .testTag("macro_target_${name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = name,
                                        color = color,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = amount,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "$pct% cals",
                                    color = color.copy(alpha = 0.85f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Auto-sync toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-sync with Nutrition Targets",
                            color = PurpleLightText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Calculations continuously update daily target & macros",
                            color = PurpleMutedText,
                            fontSize = 9.sp
                        )
                    }
                    Switch(
                        checked = autoSyncWithNutrition,
                        onCheckedChange = {
                            autoSyncWithNutrition = it
                            if (it) {
                                onApplyCaloriesTarget(calculation.targetCalories)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PurplePrimary,
                            uncheckedThumbColor = PurpleMutedText,
                            uncheckedTrackColor = PurpleSurfaceBorder
                        ),
                        modifier = Modifier.testTag("tdee_auto_sync_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // One-tap Apply Button
                Button(
                    onClick = {
                        onApplyCaloriesTarget(calculation.targetCalories)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTargetSynced) MetricStepsColor else PurplePrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("apply_tdee_to_nutrition_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isTargetSynced) Icons.Default.Check else Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isTargetSynced) "Targets Up-To-Date (${calculation.targetCalories} kcal)" else "Apply ${calculation.targetCalories} kcal to Nutrition Targets",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
