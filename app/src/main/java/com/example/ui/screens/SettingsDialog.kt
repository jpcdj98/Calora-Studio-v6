package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.backup.BackupSummary
import com.example.data.local.entity.UserProfile
import com.example.data.remote.GeminiApiKeyManager
import com.example.ui.components.BedtimeReminderSection
import com.example.ui.components.GeminiApiKeySetupCard
import com.example.ui.components.TdeeCalculatorSection
import com.example.ui.theme.MacroCarbsColor
import com.example.ui.theme.MacroFatColor
import com.example.ui.theme.MacroProteinColor
import com.example.ui.theme.MetricStepsColor
import com.example.ui.theme.MetricWorkoutColor
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleLightText
import com.example.ui.theme.PurpleMutedText
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurpleSurface
import com.example.ui.theme.PurpleSurfaceBorder
import com.example.ui.theme.PurpleSurfaceVariant
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SettingsDialog(
    currentProfile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (
        calories: Int,
        protein: Float,
        carbs: Float,
        fat: Float,
        steps: Int,
        startWeight: Float,
        targetWeight: Float,
        unit: String
    ) -> Unit,
    onExportJson: suspend () -> String = { "" },
    onInspectBackup: (String) -> BackupSummary = { BackupSummary(isValid = false) },
    onImportData: suspend (String, Boolean) -> Result<BackupSummary> = { _, _ -> Result.failure(Exception("Not implemented")) },
    onResetAllUserData: () -> Unit = {}
) {
    data class MacroPreset(
        val name: String,
        val proteinRatio: Float,
        val carbsRatio: Float,
        val fatRatio: Float,
        val subtitle: String
    )

    val presets = remember {
        listOf(
            MacroPreset("High Protein", 0.35f, 0.40f, 0.25f, "35P / 40C / 25F"),
            MacroPreset("Balanced", 0.25f, 0.50f, 0.25f, "25P / 50C / 25F"),
            MacroPreset("Low Carb", 0.35f, 0.25f, 0.40f, "35P / 25C / 40F")
        )
    }

    var caloriesText by remember { mutableStateOf(currentProfile.dailyCaloriesGoal.toString()) }
    var proteinText by remember { mutableStateOf(currentProfile.dailyProteinGoal.toInt().toString()) }
    var carbsText by remember { mutableStateOf(currentProfile.dailyCarbsGoal.toInt().toString()) }
    var fatText by remember { mutableStateOf(currentProfile.dailyFatGoal.toInt().toString()) }
    var stepsText by remember { mutableStateOf(currentProfile.dailyStepsGoal.toString()) }
    var startWeightText by remember { mutableStateOf(currentProfile.startWeight.toString()) }
    var targetWeightText by remember { mutableStateOf(currentProfile.targetWeight.toString()) }
    var selectedUnit by remember { mutableStateOf(currentProfile.weightUnit) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    var selectedPresetName by remember {
        val pC = currentProfile.dailyProteinGoal * 4f
        val cC = currentProfile.dailyCarbsGoal * 4f
        val fC = currentProfile.dailyFatGoal * 9f
        val sum = pC + cC + fC
        val name = if (sum > 0f) {
            val fR = fC / sum
            val pR = pC / sum
            when {
                fR >= 0.33f -> "Low Carb"
                pR >= 0.30f -> "High Protein"
                else -> "Balanced"
            }
        } else "Balanced"
        mutableStateOf(name)
    }

    var autoFillOtherMacros by remember { mutableStateOf(true) }

    val context = LocalContext.current
    var isAiCameraEnabled by remember { mutableStateOf(GeminiApiKeyManager.isAiCameraEnabled(context)) }
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var isExporting by remember { mutableStateOf(false) }
    var exportedJson by remember { mutableStateOf<String?>(null) }
    var exportSuccessMessage by remember { mutableStateOf<String?>(null) }

    var importJsonText by remember { mutableStateOf("") }
    var showPasteArea by remember { mutableStateOf(false) }
    var inspectedBackup by remember { mutableStateOf<BackupSummary?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var importModeReplace by remember { mutableStateOf(false) }
    var importStatusMessage by remember { mutableStateOf<String?>(null) }
    var isImportSuccess by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && exportedJson != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(exportedJson!!.toByteArray(Charsets.UTF_8))
                }
                exportSuccessMessage = "Saved backup to file successfully!"
                Toast.makeText(context, "Backup exported successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                exportSuccessMessage = "Failed to write file: ${e.localizedMessage}"
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!text.isNullOrBlank()) {
                    importJsonText = text
                    inspectedBackup = onInspectBackup(text)
                    importStatusMessage = null
                } else {
                    importStatusMessage = "Selected file is empty."
                    inspectedBackup = null
                }
            } catch (e: Exception) {
                importStatusMessage = "Error reading file: ${e.localizedMessage}"
                inspectedBackup = null
            }
        }
    }

    val allCategories = remember { listOf("targets", "tdee", "weight", "reminders", "ai", "backup") }
    var expandedCategories by rememberSaveable { mutableStateOf(setOf<String>()) }
    val allCollapsed = expandedCategories.isEmpty()

    fun toggleCategory(cat: String) {
        expandedCategories = if (expandedCategories.contains(cat)) {
            expandedCategories - cat
        } else {
            expandedCategories + cat
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = PurpleSurface,
            border = BorderStroke(1.dp, PurpleSurfaceBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Goals & Settings",
                            color = PurpleLightText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Personalize targets & preferences",
                            color = PurpleMutedText,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = if (allCollapsed) "Expand All" else "Collapse All",
                        color = PurpleAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                expandedCategories = if (allCollapsed) allCategories.toSet() else emptySet()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("toggle_all_settings_collapse")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category 1: Nutrition & Daily Targets
                SettingsCategoryCard(
                    title = "Nutrition & Daily Targets",
                    subtitle = "${caloriesText.ifBlank { "0" }} kcal • ${stepsText.ifBlank { "0" }} steps • P: ${proteinText.ifBlank { "0" }}g C: ${carbsText.ifBlank { "0" }}g F: ${fatText.ifBlank { "0" }}g",
                    icon = Icons.Default.LocalFireDepartment,
                    iconTint = PurpleAccent,
                    isExpanded = expandedCategories.contains("targets"),
                    onToggle = { toggleCategory("targets") },
                    testTag = "settings_cat_targets"
                ) {
                    // Quick TDEE & Goal Strategy link
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MetricWorkoutColor.copy(alpha = 0.12f))
                            .border(1.dp, MetricWorkoutColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .clickable {
                                if (!expandedCategories.contains("tdee")) {
                                    expandedCategories = expandedCategories + "tdee"
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("open_tdee_calculator_helper"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = MetricWorkoutColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Calculate TDEE (Cut, Maintain, Bulk)",
                                    color = PurpleLightText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Align calorie target and macro splits automatically",
                                    color = PurpleMutedText,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Text(
                            text = "Calculate →",
                            color = MetricWorkoutColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Calorie Target
                    OutlinedTextField(
                    value = caloriesText,
                    onValueChange = { newVal ->
                        val filtered = newVal.filter { c -> c.isDigit() }
                        caloriesText = filtered
                        val totalCals = filtered.toIntOrNull()
                        if (totalCals != null && totalCals > 0) {
                            val preset = presets.find { it.name == selectedPresetName } ?: presets[1]
                            val p = (totalCals * preset.proteinRatio / 4f).roundToInt()
                            val c = (totalCals * preset.carbsRatio / 4f).roundToInt()
                            val f = (totalCals * preset.fatRatio / 9f).roundToInt()
                            proteinText = p.toString()
                            carbsText = c.toString()
                            fatText = f.toString()
                        }
                    },
                    label = { Text("Daily Calorie Target (kcal)") },
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
                        .fillMaxWidth()
                        .testTag("target_calories_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Macro Auto Split
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MACRO RATIO PRESETS",
                        color = PurpleAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Active: $selectedPresetName",
                        color = PurpleMutedText,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        val isSelected = selectedPresetName == preset.name
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PurplePrimary else PurpleSurfaceVariant)
                                .border(
                                    1.dp,
                                    if (isSelected) PurpleAccent else PurpleSurfaceBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedPresetName = preset.name
                                    val totalCals = caloriesText.toIntOrNull() ?: currentProfile.dailyCaloriesGoal
                                    val p = (totalCals * preset.proteinRatio / 4f).roundToInt()
                                    val c = (totalCals * preset.carbsRatio / 4f).roundToInt()
                                    val f = (totalCals * preset.fatRatio / 9f).roundToInt()
                                    proteinText = p.toString()
                                    carbsText = c.toString()
                                    fatText = f.toString()
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = preset.name,
                                    color = if (isSelected) Color.White else PurpleLightText,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = preset.subtitle,
                                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else PurpleMutedText,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Auto-fill toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(PurpleSurfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-balance remaining macros",
                            color = PurpleLightText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Filling one macro auto-fills the others to target calories",
                            color = PurpleMutedText,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = autoFillOtherMacros,
                        onCheckedChange = { autoFillOtherMacros = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PurplePrimary,
                            uncheckedThumbColor = PurpleMutedText,
                            uncheckedTrackColor = PurpleSurfaceBorder
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Macros row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = proteinText,
                        onValueChange = { newVal ->
                            val filtered = newVal.filter { c -> c.isDigit() }
                            proteinText = filtered
                            if (autoFillOtherMacros && filtered.isNotEmpty()) {
                                val pGrams = filtered.toIntOrNull() ?: 0
                                val totalCals = caloriesText.toIntOrNull() ?: 2000
                                val pCals = pGrams * 4
                                val remCals = (totalCals - pCals).coerceAtLeast(0)
                                val preset = presets.find { it.name == selectedPresetName } ?: presets[1]
                                val ratioSum = preset.carbsRatio + preset.fatRatio
                                val carbPortion = if (ratioSum > 0f) preset.carbsRatio / ratioSum else 0.6f
                                val fatPortion = if (ratioSum > 0f) preset.fatRatio / ratioSum else 0.4f
                                val cGrams = (remCals * carbPortion / 4f).roundToInt()
                                val fGrams = (remCals * fatPortion / 9f).roundToInt()
                                carbsText = cGrams.toString()
                                fatText = fGrams.toString()
                            }
                        },
                        label = { Text("Protein (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MacroProteinColor,
                            unfocusedBorderColor = PurpleSurfaceBorder,
                            focusedTextColor = PurpleLightText,
                            unfocusedTextColor = PurpleLightText,
                            focusedLabelColor = MacroProteinColor,
                            unfocusedLabelColor = PurpleMutedText
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = carbsText,
                        onValueChange = { newVal ->
                            val filtered = newVal.filter { c -> c.isDigit() }
                            carbsText = filtered
                            if (autoFillOtherMacros && filtered.isNotEmpty()) {
                                val cGrams = filtered.toIntOrNull() ?: 0
                                val totalCals = caloriesText.toIntOrNull() ?: 2000
                                val cCals = cGrams * 4
                                val remCals = (totalCals - cCals).coerceAtLeast(0)
                                val preset = presets.find { it.name == selectedPresetName } ?: presets[1]
                                val ratioSum = preset.proteinRatio + preset.fatRatio
                                val protPortion = if (ratioSum > 0f) preset.proteinRatio / ratioSum else 0.6f
                                val fatPortion = if (ratioSum > 0f) preset.fatRatio / ratioSum else 0.4f
                                val pGrams = (remCals * protPortion / 4f).roundToInt()
                                val fGrams = (remCals * fatPortion / 9f).roundToInt()
                                proteinText = pGrams.toString()
                                fatText = fGrams.toString()
                            }
                        },
                        label = { Text("Carbs (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MacroCarbsColor,
                            unfocusedBorderColor = PurpleSurfaceBorder,
                            focusedTextColor = PurpleLightText,
                            unfocusedTextColor = PurpleLightText,
                            focusedLabelColor = MacroCarbsColor,
                            unfocusedLabelColor = PurpleMutedText
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = fatText,
                        onValueChange = { newVal ->
                            val filtered = newVal.filter { c -> c.isDigit() }
                            fatText = filtered
                            if (autoFillOtherMacros && filtered.isNotEmpty()) {
                                val fGrams = filtered.toIntOrNull() ?: 0
                                val totalCals = caloriesText.toIntOrNull() ?: 2000
                                val fCals = fGrams * 9
                                val remCals = (totalCals - fCals).coerceAtLeast(0)
                                val preset = presets.find { it.name == selectedPresetName } ?: presets[1]
                                val ratioSum = preset.proteinRatio + preset.carbsRatio
                                val protPortion = if (ratioSum > 0f) preset.proteinRatio / ratioSum else 0.4f
                                val carbPortion = if (ratioSum > 0f) preset.carbsRatio / ratioSum else 0.6f
                                val pGrams = (remCals * protPortion / 4f).roundToInt()
                                val cGrams = (remCals * carbPortion / 4f).roundToInt()
                                proteinText = pGrams.toString()
                                carbsText = cGrams.toString()
                            }
                        },
                        label = { Text("Fat (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MacroFatColor,
                            unfocusedBorderColor = PurpleSurfaceBorder,
                            focusedTextColor = PurpleLightText,
                            unfocusedTextColor = PurpleLightText,
                            focusedLabelColor = MacroFatColor,
                            unfocusedLabelColor = PurpleMutedText
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Macro total & match indicator
                val pVal = proteinText.toIntOrNull() ?: 0
                val cVal = carbsText.toIntOrNull() ?: 0
                val fVal = fatText.toIntOrNull() ?: 0
                val calcCalories = pVal * 4 + cVal * 4 + fVal * 9
                val goalCalories = caloriesText.toIntOrNull() ?: 0
                val diff = calcCalories - goalCalories

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(PurpleSurfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Total from macros: ",
                            color = PurpleMutedText,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "$calcCalories kcal",
                            color = PurpleLightText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (goalCalories > 0) {
                        val isBalanced = kotlin.math.abs(diff) <= 15
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isBalanced) MetricStepsColor.copy(alpha = 0.15f)
                                    else PurplePrimary.copy(alpha = 0.15f)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isBalanced) "✓ Matches target" else "${if (diff > 0) "+$diff" else "$diff"} kcal",
                                color = if (isBalanced) MetricStepsColor else PurpleAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Steps Goal
                OutlinedTextField(
                    value = stepsText,
                    onValueChange = { stepsText = it.filter { c -> c.isDigit() } },
                    label = { Text("Daily Step Goal") },
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
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category: TDEE & Energy Strategy (Cut / Maintain / Bulk)
            SettingsCategoryCard(
                title = "TDEE & Energy Strategy",
                subtitle = "Calculate maintenance • Select Cut, Maintain, or Bulk",
                icon = Icons.Default.Bolt,
                iconTint = MetricWorkoutColor,
                isExpanded = expandedCategories.contains("tdee"),
                onToggle = { toggleCategory("tdee") },
                testTag = "settings_cat_tdee"
            ) {
                TdeeCalculatorSection(
                    weightInput = startWeightText,
                    weightUnit = selectedUnit,
                    onWeightChange = { newW -> startWeightText = newW },
                    activeMacroPresetName = selectedPresetName,
                    currentDailyCaloriesTarget = caloriesText.toIntOrNull() ?: currentProfile.dailyCaloriesGoal,
                    onApplyCaloriesTarget = { targetCals ->
                        caloriesText = targetCals.toString()
                        val preset = presets.find { it.name == selectedPresetName } ?: presets[1]
                        val p = (targetCals * preset.proteinRatio / 4f).roundToInt()
                        val c = (targetCals * preset.carbsRatio / 4f).roundToInt()
                        val f = (targetCals * preset.fatRatio / 9f).roundToInt()
                        proteinText = p.toString()
                        carbsText = c.toString()
                        fatText = f.toString()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category 2: Body Weight Targets
            SettingsCategoryCard(
                title = "Body Weight Targets",
                subtitle = "${startWeightText.ifBlank { "-" }} $selectedUnit → ${targetWeightText.ifBlank { "-" }} $selectedUnit",
                icon = Icons.Default.FitnessCenter,
                iconTint = MacroProteinColor,
                isExpanded = expandedCategories.contains("weight"),
                onToggle = { toggleCategory("weight") },
                testTag = "settings_cat_weight"
            ) {
                Text(
                    text = "True starting weight anchors your net change on the Weight tab",
                    color = PurpleMutedText,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = startWeightText,
                        onValueChange = { startWeightText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Starting Weight ($selectedUnit)") },
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
                            .weight(1f)
                            .testTag("starting_weight_input")
                    )

                    OutlinedTextField(
                        value = targetWeightText,
                        onValueChange = { targetWeightText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Goal Weight ($selectedUnit)") },
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
                            .weight(1f)
                            .testTag("target_weight_input")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Weight Unit Selector Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Preferred Unit",
                        color = PurpleLightText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(PurpleSurfaceVariant)
                            .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    ) {
                        listOf("kg", "lbs").forEach { u ->
                            val isSel = u == selectedUnit
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) PurplePrimary else Color.Transparent)
                                    .clickable { selectedUnit = u }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = u,
                                    color = if (isSel) Color.White else PurpleMutedText,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category 3: Bedtime Log Reminder
            SettingsCategoryCard(
                title = "Bedtime Log Reminder",
                subtitle = "Daily notification reminder",
                icon = Icons.Default.Notifications,
                iconTint = MacroCarbsColor,
                isExpanded = expandedCategories.contains("reminders"),
                onToggle = { toggleCategory("reminders") },
                testTag = "settings_cat_reminders"
            ) {
                BedtimeReminderSection()
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category 4: Google Gemini AI Camera
            SettingsCategoryCard(
                title = "Google Gemini AI Camera",
                subtitle = if (isAiCameraEnabled) "Active • Photo recognition enabled" else "Disabled",
                icon = Icons.Default.CameraAlt,
                iconTint = MacroFatColor,
                isExpanded = expandedCategories.contains("ai"),
                onToggle = { toggleCategory("ai") },
                testTag = "settings_cat_ai"
            ) {
                Text(
                    text = "Attach your own Gemini API key for private, unmetered food vision scanning",
                    color = PurpleMutedText,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PurpleSurfaceVariant)
                        .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable AI Camera in Food Logger",
                            color = PurpleLightText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Allow photo meal recognition tab when logging meals",
                            color = PurpleMutedText,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isAiCameraEnabled,
                        onCheckedChange = {
                            isAiCameraEnabled = it
                            GeminiApiKeyManager.setAiCameraEnabled(context, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PurplePrimary,
                            uncheckedThumbColor = PurpleMutedText,
                            uncheckedTrackColor = PurpleSurface
                        )
                    )
                }

                if (isAiCameraEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    GeminiApiKeySetupCard()
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category 5: Data Backup & Restore
            SettingsCategoryCard(
                title = "Data Backup & Restore",
                subtitle = "Export or import JSON backup records",
                icon = Icons.Default.FileUpload,
                iconTint = MetricStepsColor,
                isExpanded = expandedCategories.contains("backup"),
                onToggle = { toggleCategory("backup") },
                testTag = "settings_cat_backup"
            ) {
                Text(
                    text = "Export your history as a JSON backup or restore past records",
                    color = PurpleMutedText,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Export Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PurpleSurfaceVariant)
                        .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PurplePrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Export",
                                    tint = PurpleAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text("Export Backup", color = PurpleLightText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Foods, workouts, steps & goals", color = PurpleMutedText, fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isExporting = true
                                    val json = onExportJson()
                                    exportedJson = json
                                    isExporting = false
                                    val fileName = "calora_backup_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}.json"
                                    exportLauncher.launch(fileName)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("export_backup_button")
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Export File", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Export Quick Actions (Share, Copy JSON)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Share button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PurpleSurface)
                                .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    coroutineScope.launch {
                                        val json = exportedJson ?: onExportJson()
                                        exportedJson = json
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, json)
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, "Share Calora Backup")
                                        context.startActivity(shareIntent)
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = PurpleAccent, modifier = Modifier.size(14.dp))
                                Text("Share", color = PurpleLightText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        // Copy button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PurpleSurface)
                                .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    coroutineScope.launch {
                                        val json = exportedJson ?: onExportJson()
                                        exportedJson = json
                                        clipboardManager.setText(AnnotatedString(json))
                                        exportSuccessMessage = "Copied backup JSON to clipboard!"
                                        Toast.makeText(context, "Copied JSON to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = PurpleAccent, modifier = Modifier.size(14.dp))
                                Text("Copy JSON", color = PurpleLightText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    if (exportSuccessMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = exportSuccessMessage ?: "",
                            color = MacroProteinColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Import Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PurpleSurfaceVariant)
                        .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MacroCarbsColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = "Import",
                                    tint = MacroCarbsColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text("Import Backup", color = PurpleLightText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Restore from file or pasted JSON", color = PurpleMutedText, fontSize = 11.sp)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = {
                                    importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("choose_backup_file_button")
                            ) {
                                Text("Pick File", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = { showPasteArea = !showPasteArea },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleLightText),
                                border = BorderStroke(1.dp, PurpleSurfaceBorder),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(if (showPasteArea) "Hide" else "Paste", fontSize = 12.sp)
                            }
                        }
                    }

                    // Optional paste text area
                    if (showPasteArea) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = importJsonText,
                            onValueChange = {
                                importJsonText = it
                                if (it.isNotBlank()) {
                                    inspectedBackup = onInspectBackup(it)
                                } else {
                                    inspectedBackup = null
                                }
                            },
                            label = { Text("Paste JSON Backup Here") },
                            placeholder = { Text("{\n  \"appName\": \"Calora\",\n  ...\n}") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("paste_json_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurplePrimary,
                                unfocusedBorderColor = PurpleSurfaceBorder,
                                focusedTextColor = PurpleLightText,
                                unfocusedTextColor = PurpleLightText
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                        )
                    }

                    // Inspected backup summary & import execution
                    if (inspectedBackup != null) {
                        val summary = inspectedBackup!!
                        Spacer(modifier = Modifier.height(12.dp))

                        if (summary.isValid) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PurpleSurface)
                                    .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Valid",
                                        tint = MacroProteinColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Valid Calora Backup",
                                        color = MacroProteinColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (summary.exportDate.isNotBlank()) {
                                        Text(
                                            text = "(${summary.exportDate})",
                                            color = PurpleMutedText,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Stats badges
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "Foods" to "${summary.foodCount}",
                                        "Fitness" to "${summary.fitnessCount}",
                                        "Weights" to "${summary.weightCount}",
                                        "Profile" to if (summary.hasProfile) "Yes" else "No"
                                    ).forEach { (label, count) ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(PurpleSurfaceVariant)
                                                .padding(vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(count, color = PurpleLightText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text(label, color = PurpleMutedText, fontSize = 9.sp)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Import mode selection (Merge vs Replace)
                                Text(
                                    text = "Import Strategy:",
                                    color = PurpleMutedText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Merge Option
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (!importModeReplace) PurplePrimary.copy(alpha = 0.25f) else PurpleSurfaceVariant)
                                            .border(
                                                1.dp,
                                                if (!importModeReplace) PurplePrimary else PurpleSurfaceBorder,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { importModeReplace = false }
                                            .padding(vertical = 8.dp, horizontal = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Merge Data",
                                                color = if (!importModeReplace) PurpleAccent else PurpleLightText,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text("Keep current logs", color = PurpleMutedText, fontSize = 10.sp)
                                        }
                                    }

                                    // Replace Option
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (importModeReplace) Color(0xFFEF4444).copy(alpha = 0.2f) else PurpleSurfaceVariant)
                                            .border(
                                                1.dp,
                                                if (importModeReplace) Color(0xFFEF4444) else PurpleSurfaceBorder,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { importModeReplace = true }
                                            .padding(vertical = 8.dp, horizontal = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Replace All",
                                                color = if (importModeReplace) Color(0xFFFF6B6B) else PurpleLightText,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text("Overwrite existing", color = PurpleMutedText, fontSize = 10.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Confirm Import Button
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            isImporting = true
                                            val res = onImportData(importJsonText, importModeReplace)
                                            isImporting = false
                                            res.onSuccess { s ->
                                                isImportSuccess = true
                                                importStatusMessage = "Successfully imported ${s.foodCount} foods, ${s.fitnessCount} fitness logs, and ${s.weightCount} weight records!"
                                                Toast.makeText(context, "Data imported successfully!", Toast.LENGTH_SHORT).show()
                                            }.onFailure { err ->
                                                isImportSuccess = false
                                                importStatusMessage = "Import failed: ${err.localizedMessage}"
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (importModeReplace) Color(0xFFDC2626) else PurplePrimary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("confirm_import_button"),
                                    enabled = !isImporting
                                ) {
                                    if (isImporting) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = if (importModeReplace) "Confirm & Overwrite All Data" else "Confirm & Merge Data",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            // Invalid JSON error
                            Text(
                                text = "Invalid backup format: ${summary.error}",
                                color = Color(0xFFFF6B6B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (importStatusMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = importStatusMessage ?: "",
                            color = if (isImportSuccess) MacroProteinColor else Color(0xFFFF6B6B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Fresh Install / Wipe Data Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(PurpleSurface)
                            .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "RESET / FRESH INSTALL",
                            color = Color(0xFFFF6B6B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Permanently erase all logged food meals, activity logs, and weigh-in records to start with a completely fresh slate.",
                            color = PurpleMutedText,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { showResetConfirmDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFFF6B6B)
                            ),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFF6B6B).copy(alpha = 0.5f))
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reset_fresh_install_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear All Data",
                                tint = Color(0xFFFF6B6B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Clear All Logs & Reset to Fresh Install",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PurpleMutedText
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = androidx.compose.ui.graphics.SolidColor(PurpleSurfaceBorder)
                        )
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val cals = caloriesText.toIntOrNull() ?: currentProfile.dailyCaloriesGoal
                            val p = proteinText.toFloatOrNull() ?: currentProfile.dailyProteinGoal
                            val c = carbsText.toFloatOrNull() ?: currentProfile.dailyCarbsGoal
                            val f = fatText.toFloatOrNull() ?: currentProfile.dailyFatGoal
                            val st = stepsText.toIntOrNull() ?: currentProfile.dailyStepsGoal
                            val sw = startWeightText.toFloatOrNull() ?: currentProfile.startWeight
                            val tw = targetWeightText.toFloatOrNull() ?: currentProfile.targetWeight

                            onSave(cals, p, c, f, st, sw, tw, selectedUnit)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurplePrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("save_settings_button")
                    ) {
                        Text("Save Targets", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            containerColor = Color(0xFF1E1528),
            title = {
                Text(
                    text = "Reset to Fresh Install?",
                    color = PurpleLightText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "This will permanently erase all recorded food entries, fitness logs, and weigh-in history. The food database and your target goals will remain intact.",
                    color = PurpleMutedText,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAllUserData()
                        showResetConfirmDialog = false
                        importStatusMessage = "All user logs have been cleared. Fresh install state active."
                        isImportSuccess = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Clear All Data", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel", color = PurpleMutedText)
                }
            }
        )
    }
}

@Composable
private fun SettingsCategoryCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color = Color.Unspecified,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    testTag: String = "",
    content: @Composable () -> Unit
) {
    val actualIconTint = if (iconTint != Color.Unspecified) iconTint else PurpleAccent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PurpleSurfaceVariant)
            .border(
                width = 1.dp,
                color = if (isExpanded) PurpleAccent.copy(alpha = 0.5f) else PurpleSurfaceBorder,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(actualIconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = actualIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        color = PurpleLightText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        color = PurpleMutedText,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse $title" else "Expand $title",
                tint = PurpleAccent,
                modifier = Modifier.size(22.dp)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(PurpleSurfaceBorder)
                )
                Spacer(modifier = Modifier.height(14.dp))
                content()
            }
        }
    }
}
