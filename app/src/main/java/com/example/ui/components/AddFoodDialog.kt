package com.example.ui.components

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.FoodItem
import com.example.data.remote.GeminiApiKeyManager
import com.example.data.remote.GeminiFoodService
import com.example.data.util.FractionHelper
import com.example.data.util.ServingMeasurement
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
import com.example.ui.theme.PurpleSurface
import com.example.ui.theme.PurpleSurfaceBorder
import com.example.ui.theme.PurpleSurfaceVariant
import com.example.ui.theme.isAppInDarkTheme
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

enum class FoodLogMode {
    DATABASE,
    MANUAL,
    AI_CAMERA
}

@Composable
fun AddFoodDialog(
    initialMealType: String = "Breakfast",
    foodItems: List<FoodItem> = emptyList(),
    onSaveCustomFood: ((FoodItem) -> Unit)? = null,
    onDeleteCustomFood: ((Long) -> Unit)? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        mealType: String,
        name: String,
        calories: Int,
        protein: Float,
        carbs: Float,
        fat: Float,
        servingSize: Float,
        servingUnit: String
    ) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isAiCameraFeatureEnabled = remember { GeminiApiKeyManager.isAiCameraEnabled(context) }
    var isApiKeyConfigured by remember { mutableStateOf(GeminiApiKeyManager.isApiKeyConfigured(context)) }
    var showApiKeySetupDialog by remember { mutableStateOf(false) }

    val mealTypes = listOf("Breakfast", "Lunch", "Dinner", "Snack")
    var selectedMeal by rememberSaveable(initialMealType) { mutableStateOf(initialMealType) }
    var currentMode by rememberSaveable { mutableStateOf(FoodLogMode.DATABASE) }

    // Form inputs
    var foodName by rememberSaveable { mutableStateOf("") }
    var servingSizeText by rememberSaveable { mutableStateOf("1") }
    var servingUnit by rememberSaveable { mutableStateOf("serving") }
    var caloriesText by rememberSaveable { mutableStateOf("") }
    var proteinText by rememberSaveable { mutableStateOf("") }
    var carbsText by rememberSaveable { mutableStateOf("") }
    var fatText by rememberSaveable { mutableStateOf("") }

    // Save to Food Database checkbox & attributes
    var saveToDbChecked by rememberSaveable { mutableStateOf(false) }
    var dbCategory by rememberSaveable { mutableStateOf("Custom") }
    var dbVariation by rememberSaveable { mutableStateOf("Standard") }
    var customGramsPerUnit by rememberSaveable { mutableStateOf("") }

    // AI Camera state
    val selectedBitmaps = remember { mutableStateListOf<Bitmap>() }
    var userDescription by rememberSaveable { mutableStateOf("") }
    var isAnalyzing by rememberSaveable { mutableStateOf(false) }
    var analysisNote by rememberSaveable { mutableStateOf<String?>(null) }
    var analysisError by rememberSaveable { mutableStateOf<String?>(null) }
    val isDark = isAppInDarkTheme

    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    fun triggerAiAnalysis() {
        if (!GeminiApiKeyManager.isApiKeyConfigured(context)) {
            analysisError = "Please attach your Google Gemini API key first."
            isApiKeyConfigured = false
            return
        }

        if (selectedBitmaps.isEmpty() && userDescription.isBlank()) {
            analysisError = "Please add at least one photo or describe your meal first."
            return
        }

        errorMessage = null
        analysisError = null
        isAnalyzing = true
        coroutineScope.launch {
            val result = GeminiFoodService.analyzeFoodImages(
                bitmaps = selectedBitmaps.toList(),
                userDescription = userDescription,
                context = context
            )
            isAnalyzing = false
            result.onSuccess { analysis ->
                foodName = analysis.foodName
                val formattedSize = if (analysis.servingSize % 1f == 0f) {
                    analysis.servingSize.toInt().toString()
                } else {
                    String.format(Locale.US, "%.1f", analysis.servingSize)
                }
                servingSizeText = formattedSize
                servingUnit = analysis.servingUnit
                caloriesText = analysis.calories.toString()
                proteinText = FractionHelper.formatMacroValue(analysis.protein)
                carbsText = FractionHelper.formatMacroValue(analysis.carbs)
                fatText = FractionHelper.formatMacroValue(analysis.fat)
                analysisNote = analysis.notes
            }.onFailure { error ->
                analysisError = error.message ?: "Failed to estimate meal nutrition"
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            if (selectedBitmaps.size < 5) {
                selectedBitmaps.add(bitmap)
                analysisError = null
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            analysisError = "Camera permission was denied. You can select photos from your gallery instead."
        }
    }

    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        for (uri in uris) {
            if (selectedBitmaps.size >= 5) break
            try {
                val stream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(stream)
                stream?.close()
                if (bitmap != null) {
                    selectedBitmaps.add(bitmap)
                    analysisError = null
                }
            } catch (e: Exception) {
                analysisError = "Unable to read one of the selected photos."
            }
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
                .padding(horizontal = 14.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Log Food",
                            color = PurpleLightText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (currentMode) {
                                FoodLogMode.DATABASE -> "Search & log common diet foods & variations"
                                FoodLogMode.MANUAL -> "Quick manual calorie & macro logging"
                                FoodLogMode.AI_CAMERA -> "AI meal visual scan with editable macros"
                            },
                            color = PurpleMutedText,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = PurpleMutedText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mode Tabs Switcher (Database vs Quick Manual vs AI Camera)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PurpleSurfaceVariant)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Database Tab
                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (currentMode == FoodLogMode.DATABASE) PurplePrimary else Color.Transparent)
                            .clickable { currentMode = FoodLogMode.DATABASE }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = if (currentMode == FoodLogMode.DATABASE) Color.White else PurpleAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Database",
                                color = if (currentMode == FoodLogMode.DATABASE) Color.White else PurpleLightText,
                                fontSize = 12.sp,
                                fontWeight = if (currentMode == FoodLogMode.DATABASE) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }

                    // Manual Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (currentMode == FoodLogMode.MANUAL) PurplePrimary else Color.Transparent)
                            .clickable { currentMode = FoodLogMode.MANUAL }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = if (currentMode == FoodLogMode.MANUAL) Color.White else PurpleMutedText,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Manual",
                                color = if (currentMode == FoodLogMode.MANUAL) Color.White else PurpleLightText,
                                fontSize = 12.sp,
                                fontWeight = if (currentMode == FoodLogMode.MANUAL) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }

                    if (isAiCameraFeatureEnabled) {
                        // AI Camera Tab
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (currentMode == FoodLogMode.AI_CAMERA) PurplePrimary else Color.Transparent)
                                .clickable { currentMode = FoodLogMode.AI_CAMERA }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (currentMode == FoodLogMode.AI_CAMERA) Color.White else PurpleAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "AI Camera",
                                    color = if (currentMode == FoodLogMode.AI_CAMERA) Color.White else PurpleLightText,
                                    fontSize = 11.sp,
                                    fontWeight = if (currentMode == FoodLogMode.AI_CAMERA) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Meal Selector (Breakfast, Lunch, Dinner, Snack)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    mealTypes.forEach { meal ->
                        val isSelected = meal == selectedMeal
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PurplePrimary.copy(alpha = 0.85f) else PurpleSurfaceVariant)
                                .border(
                                    1.dp,
                                    if (isSelected) PurpleAccent else PurpleSurfaceBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedMeal = meal }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = meal,
                                color = if (isSelected) Color.White else PurpleMutedText,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                when (currentMode) {
                    FoodLogMode.DATABASE -> {
                        // Embedded Food Database search, variations & quick logger
                        FoodDatabaseView(
                            foodItems = foodItems,
                            selectedMeal = selectedMeal,
                            onLogFood = { meal, name, cals, p, c, f, size, unit ->
                                onConfirm(meal, name, cals, p, c, f, size, unit)
                                onDismiss()
                            },
                            onCustomizeInManual = { name, cals, p, c, f, size, unit ->
                                foodName = name
                                caloriesText = cals.toString()
                                proteinText = if (p % 1f == 0f) p.toInt().toString() else String.format(Locale.US, "%.1f", p)
                                carbsText = if (c % 1f == 0f) c.toInt().toString() else String.format(Locale.US, "%.1f", c)
                                fatText = if (f % 1f == 0f) f.toInt().toString() else String.format(Locale.US, "%.1f", f)
                                servingSizeText = if (size % 1f == 0f) size.toInt().toString() else String.format(Locale.US, "%.1f", size)
                                servingUnit = unit
                                currentMode = FoodLogMode.MANUAL
                            },
                            onSaveCustomFood = { customItem ->
                                onSaveCustomFood?.invoke(customItem)
                            },
                            onDeleteCustomFood = { customFoodId ->
                                onDeleteCustomFood?.invoke(customFoodId)
                            },
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    FoodLogMode.MANUAL, FoodLogMode.AI_CAMERA -> {
                        // Scrollable manual form & AI camera contents
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (currentMode == FoodLogMode.AI_CAMERA) {
                                if (!isApiKeyConfigured) {
                                    GeminiApiKeySetupCard(
                                        onKeyUpdated = {
                                            isApiKeyConfigured = GeminiApiKeyManager.isApiKeyConfigured(context)
                                            analysisError = null
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp)
                                    )
                                } else {
                                    // AI Camera Box
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (isDark) {
                                                    Brush.horizontalGradient(
                                                        colors = listOf(Color(0xFF2E1065), Color(0xFF1E1B4B))
                                                    )
                                                } else {
                                                    Brush.horizontalGradient(
                                                        colors = listOf(Color(0xFFF5EEFC), Color(0xFFEDE3FA))
                                                    )
                                                }
                                            )
                                            .border(
                                                1.dp,
                                                if (isDark) PurpleAccent.copy(alpha = 0.4f) else PurpleSurfaceBorder,
                                                RoundedCornerShape(14.dp)
                                            )
                                            .padding(14.dp)
                                    ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "MEAL PHOTOS (${selectedBitmaps.size}/5)",
                                                color = PurpleAccent,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            )
                                            if (selectedBitmaps.isNotEmpty()) {
                                                Text(
                                                    text = "Clear all",
                                                    color = MacroFatColor,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.clickable { selectedBitmaps.clear() }
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (selectedBitmaps.size < 5) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(68.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(if (isDark) PurpleSurfaceVariant else PurpleSurface)
                                                        .border(
                                                            1.dp,
                                                            if (isDark) PurpleAccent.copy(alpha = 0.5f) else PurplePrimary.copy(alpha = 0.35f),
                                                            RoundedCornerShape(12.dp)
                                                        )
                                                        .clickable { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Icon(
                                                            imageVector = Icons.Default.CameraAlt,
                                                            contentDescription = "Take Photo",
                                                            tint = PurpleAccent,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text("Camera", color = PurpleLightText, fontSize = 10.sp)
                                                    }
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .size(68.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(if (isDark) PurpleSurfaceVariant else PurpleSurface)
                                                        .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(12.dp))
                                                        .clickable {
                                                            multiplePhotoPickerLauncher.launch(
                                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                            )
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Icon(
                                                            imageVector = Icons.Default.PhotoLibrary,
                                                            contentDescription = "Select from Gallery",
                                                            tint = if (isDark) PurpleMutedText else PurpleAccent,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text("Gallery", color = PurpleLightText, fontSize = 10.sp)
                                                    }
                                                }
                                            }

                                            selectedBitmaps.forEachIndexed { index, bitmap ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(68.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .border(1.dp, PurpleAccent, RoundedCornerShape(12.dp))
                                                ) {
                                                    Image(
                                                        bitmap = bitmap.asImageBitmap(),
                                                        contentDescription = "Captured Meal photo ${index + 1}",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.size(68.dp)
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(3.dp)
                                                            .size(18.dp)
                                                            .clip(CircleShape)
                                                            .background(Color.Black.copy(alpha = 0.7f))
                                                            .clickable { selectedBitmaps.removeAt(index) },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Remove photo",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                            value = userDescription,
                                            onValueChange = { userDescription = it },
                                            placeholder = {
                                                Text(
                                                    "Add meal hints (e.g., grilled chicken breast, 200g, olive oil)",
                                                    color = PurpleMutedText,
                                                    fontSize = 11.sp
                                                )
                                            },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = if (isDark) Color.Transparent else PurpleSurface,
                                                unfocusedContainerColor = if (isDark) Color.Transparent else PurpleSurface,
                                                focusedBorderColor = PurplePrimary,
                                                unfocusedBorderColor = PurpleSurfaceBorder,
                                                focusedTextColor = PurpleLightText,
                                                unfocusedTextColor = PurpleLightText
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Button(
                                            onClick = { triggerAiAnalysis() },
                                            enabled = !isAnalyzing && (selectedBitmaps.isNotEmpty() || userDescription.isNotBlank()),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = PurplePrimary,
                                                disabledContainerColor = if (isDark) PurpleSurfaceVariant else PurpleSurfaceBorder
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (isAnalyzing) {
                                                CircularProgressIndicator(
                                                    color = Color.White,
                                                    strokeWidth = 2.dp,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Analyzing meal...", fontSize = 12.sp)
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Analyze Photos with Gemini", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }

                                        if (analysisNote != null) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "✨ ${analysisNote ?: ""}",
                                                color = MetricStepsColor,
                                                fontSize = 11.sp
                                            )
                                        }

                                        if (analysisError != null && !isAnalyzing) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = analysisError ?: "",
                                                color = MacroFatColor,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Key: ${GeminiApiKeyManager.getMaskedKey(context)}",
                                                color = PurpleMutedText,
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = "Manage Key",
                                                color = PurpleAccent,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .clickable { isApiKeyConfigured = false }
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                            // Food Name Input
                            OutlinedTextField(
                                value = foodName,
                                onValueChange = {
                                    foodName = it
                                    errorMessage = null
                                },
                                label = { Text("Food / Meal Name") },
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
                                    .testTag("food_name_input")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Portion & Measurement Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = servingSizeText,
                                    onValueChange = { input ->
                                        val sanitized = input.replace(',', '.')
                                        if (sanitized.all { it.isDigit() || it == '.' || it == '/' || it == ' ' }) {
                                            servingSizeText = sanitized
                                            errorMessage = null
                                        }
                                    },
                                    label = { Text("Quantity (e.g. 1, 3/4)") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PurplePrimary,
                                        unfocusedBorderColor = PurpleSurfaceBorder,
                                        focusedTextColor = PurpleLightText,
                                        unfocusedTextColor = PurpleLightText
                                    ),
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .testTag("serving_size_input")
                                )

                                OutlinedTextField(
                                    value = servingUnit,
                                    onValueChange = {
                                        servingUnit = it
                                        errorMessage = null
                                    },
                                    label = { Text("Unit (g, cup, tbsp)") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PurplePrimary,
                                        unfocusedBorderColor = PurpleSurfaceBorder,
                                        focusedTextColor = PurpleLightText,
                                        unfocusedTextColor = PurpleLightText
                                    ),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .testTag("serving_unit_input")
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Quick Common Fractions Chips
                            Text(
                                text = "Quick fractions:",
                                color = PurpleMutedText,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val commonFractions = listOf("1/4", "1/3", "1/2", "2/3", "3/4", "1", "1 1/2", "2", "3")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                commonFractions.forEach { frac ->
                                    val isSelected = servingSizeText.trim() == frac
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) PurplePrimary else PurpleSurfaceVariant)
                                            .border(
                                                1.dp,
                                                if (isSelected) PurpleAccent else PurpleSurfaceBorder,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable {
                                                servingSizeText = frac
                                                errorMessage = null
                                            }
                                            .padding(horizontal = 7.dp, vertical = 3.dp)
                                            .testTag("fraction_chip_$frac")
                                    ) {
                                        Text(
                                            text = frac,
                                            color = if (isSelected) Color.White else PurpleLightText,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Kitchen Measurement Units
                            Text(
                                text = "Kitchen units:",
                                color = PurpleMutedText,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val commonUnits = listOf(
                                "g", "cup", "tbsp", "tsp", "oz", "ml", "fl oz",
                                "slice", "piece", "serving", "packet", "can", "bottle",
                                "clove", "bunch", "kg", "lbs",
                                "small fillet", "medium breast", "large steak"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                commonUnits.forEach { unitOption ->
                                    val isSelected = servingUnit.equals(unitOption, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) PurplePrimary else PurpleSurfaceVariant)
                                            .border(
                                                1.dp,
                                                if (isSelected) PurpleAccent else PurpleSurfaceBorder,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                servingUnit = unitOption
                                                errorMessage = null
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                            .testTag("unit_chip_$unitOption")
                                    ) {
                                        Text(
                                            text = unitOption,
                                            color = if (isSelected) Color.White else PurpleLightText,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Calories Input (High contrast, clean)
                            OutlinedTextField(
                                value = caloriesText,
                                onValueChange = {
                                    caloriesText = FractionHelper.sanitizeDecimalInput(it)
                                    errorMessage = null
                                },
                                label = { Text("Calories (kcal)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = MetricWorkoutColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MetricWorkoutColor,
                                    unfocusedBorderColor = PurpleSurfaceBorder,
                                    focusedTextColor = PurpleLightText,
                                    unfocusedTextColor = PurpleLightText,
                                    focusedLabelColor = MetricWorkoutColor,
                                    unfocusedLabelColor = PurpleMutedText
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("calories_input")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Macros Row (Protein, Carbs, Fat)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = proteinText,
                                    onValueChange = { proteinText = FractionHelper.sanitizeDecimalInput(it) },
                                    label = { Text("Protein (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MacroProteinColor,
                                        unfocusedBorderColor = PurpleSurfaceBorder,
                                        focusedTextColor = PurpleLightText,
                                        unfocusedTextColor = PurpleLightText,
                                        focusedLabelColor = MacroProteinColor,
                                        unfocusedLabelColor = PurpleMutedText
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("protein_input")
                                )

                                OutlinedTextField(
                                    value = carbsText,
                                    onValueChange = { carbsText = FractionHelper.sanitizeDecimalInput(it) },
                                    label = { Text("Carbs (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MacroCarbsColor,
                                        unfocusedBorderColor = PurpleSurfaceBorder,
                                        focusedTextColor = PurpleLightText,
                                        unfocusedTextColor = PurpleLightText,
                                        focusedLabelColor = MacroCarbsColor,
                                        unfocusedLabelColor = PurpleMutedText
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("carbs_input")
                                )

                                OutlinedTextField(
                                    value = fatText,
                                    onValueChange = { fatText = FractionHelper.sanitizeDecimalInput(it) },
                                    label = { Text("Fat (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MacroFatColor,
                                        unfocusedBorderColor = PurpleSurfaceBorder,
                                        focusedTextColor = PurpleLightText,
                                        unfocusedTextColor = PurpleLightText,
                                        focusedLabelColor = MacroFatColor,
                                        unfocusedLabelColor = PurpleMutedText
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("fat_input")
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // "Save to Food Database for future search" Card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PurpleSurfaceVariant)
                                    .border(1.dp, if (saveToDbChecked) PurpleAccent else PurpleSurfaceBorder, RoundedCornerShape(10.dp))
                                    .clickable { saveToDbChecked = !saveToDbChecked }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (saveToDbChecked) Icons.Default.Star else Icons.Default.BookmarkBorder,
                                            contentDescription = null,
                                            tint = if (saveToDbChecked) MetricWorkoutColor else PurpleMutedText,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Save to Food Database",
                                                color = PurpleLightText,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Search and log it again in seconds later",
                                                color = PurpleMutedText,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Switch(
                                        checked = saveToDbChecked,
                                        onCheckedChange = { saveToDbChecked = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = PurplePrimary,
                                            checkedBorderColor = PurpleAccent,
                                            uncheckedThumbColor = PurpleMutedText,
                                            uncheckedTrackColor = PurpleDarkBackground,
                                            uncheckedBorderColor = PurpleSurfaceBorder
                                        )
                                    )
                                }
                            }

                            // Sub-fields if Save to Database is checked
                            AnimatedVisibility(
                                visible = saveToDbChecked,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    OutlinedTextField(
                                        value = dbVariation,
                                        onValueChange = { dbVariation = it },
                                        label = { Text("Variation (e.g. Air-fried, Homemade)") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = PurplePrimary,
                                            unfocusedBorderColor = PurpleSurfaceBorder,
                                            focusedTextColor = PurpleLightText,
                                            unfocusedTextColor = PurpleLightText
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    if (!servingUnit.equals("g", ignoreCase = true) && !servingUnit.equals("grams", ignoreCase = true)) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedTextField(
                                            value = customGramsPerUnit,
                                            onValueChange = { customGramsPerUnit = FractionHelper.sanitizeDecimalInput(it) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            label = { Text("Grams per 1 $servingUnit (optional, e.g. 158)") },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PurplePrimary,
                                                unfocusedBorderColor = PurpleSurfaceBorder,
                                                focusedTextColor = PurpleLightText,
                                                unfocusedTextColor = PurpleLightText
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            if (errorMessage != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    color = MacroFatColor,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Bottom Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleMutedText),
                                    border = BorderStroke(1.dp, PurpleSurfaceBorder),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Cancel")
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Button(
                                    onClick = {
                                        val cals = FractionHelper.parseCalories(caloriesText)
                                        if (foodName.isBlank()) {
                                            errorMessage = "Please enter food or meal name"
                                            return@Button
                                        }
                                        if (cals == null || cals <= 0) {
                                            errorMessage = "Please enter valid calories"
                                            return@Button
                                        }
                                        val p = proteinText.toFloatOrNull() ?: 0f
                                        val c = carbsText.toFloatOrNull() ?: 0f
                                        val f = fatText.toFloatOrNull() ?: 0f
                                        val size = FractionHelper.parse(servingSizeText) ?: 1f
                                        val unit = servingUnit.trim().ifBlank { "serving" }

                                        if (saveToDbChecked) {
                                            val conversions = if (customGramsPerUnit.isNotBlank()) "${unit.trim()}:${customGramsPerUnit.trim()}" else ""
                                            onSaveCustomFood?.invoke(
                                                FoodItem(
                                                    id = 0,
                                                    name = foodName.trim(),
                                                    category = dbCategory.trim(),
                                                    variation = dbVariation.trim().ifBlank { "Standard" },
                                                    servingSize = size,
                                                    servingUnit = unit,
                                                    calories = cals,
                                                    protein = p,
                                                    carbs = c,
                                                    fat = f,
                                                    isCustom = true,
                                                    searchKeywords = "$foodName $dbCategory $dbVariation $unit".lowercase(),
                                                    unitConversions = conversions
                                                )
                                            )
                                        }

                                        onConfirm(
                                            selectedMeal,
                                            foodName,
                                            cals,
                                            p,
                                            c,
                                            f,
                                            size,
                                            unit
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PurplePrimary,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("confirm_add_food")
                                ) {
                                    Text("Log Food", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
