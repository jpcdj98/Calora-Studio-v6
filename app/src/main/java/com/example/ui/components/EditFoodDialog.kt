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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.FoodEntry
import com.example.data.util.FractionHelper
import com.example.ui.theme.MacroCarbsColor
import com.example.ui.theme.MacroFatColor
import com.example.ui.theme.MacroProteinColor
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleLightText
import com.example.ui.theme.PurpleMutedText
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurpleSurface
import com.example.ui.theme.PurpleSurfaceBorder
import com.example.ui.theme.PurpleSurfaceVariant

@Composable
fun EditFoodDialog(
    entry: FoodEntry,
    onDismiss: () -> Unit,
    onSave: (FoodEntry) -> Unit,
    onDelete: (Long) -> Unit
) {
    val mealTypes = listOf("Breakfast", "Lunch", "Dinner", "Snack")
    var selectedMeal by rememberSaveable { mutableStateOf(entry.mealType) }
    var foodName by rememberSaveable { mutableStateOf(entry.name) }
    var servingSizeText by rememberSaveable { mutableStateOf(FractionHelper.format(entry.servingSize)) }
    var servingUnit by rememberSaveable { mutableStateOf(entry.servingUnit) }
    var caloriesText by rememberSaveable { mutableStateOf(entry.calories.toString()) }
    var proteinText by rememberSaveable { mutableStateOf(FractionHelper.formatMacroValue(entry.protein)) }
    var carbsText by rememberSaveable { mutableStateOf(FractionHelper.formatMacroValue(entry.carbs)) }
    var fatText by rememberSaveable { mutableStateOf(FractionHelper.formatMacroValue(entry.fat)) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = PurpleSurface,
            border = BorderStroke(1.dp, PurpleSurfaceBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("edit_food_dialog")
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Edit Logged Food",
                            color = PurpleLightText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Change meal type, serving, or nutrition",
                            color = PurpleMutedText,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(PurpleSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = PurpleMutedText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Meal Type selector
                Text(
                    text = "MEAL TYPE",
                    color = PurpleAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    mealTypes.forEach { meal ->
                        val isSelected = selectedMeal.equals(meal, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) PurplePrimary else PurpleSurfaceVariant)
                                .border(
                                    1.dp,
                                    if (isSelected) PurpleAccent else PurpleSurfaceBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedMeal = meal }
                                .testTag("edit_meal_type_$meal"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = meal,
                                color = if (isSelected) Color.White else PurpleLightText,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Food Name
                OutlinedTextField(
                    value = foodName,
                    onValueChange = {
                        foodName = it
                        errorMessage = null
                    },
                    label = { Text("Food Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = PurpleSurfaceBorder,
                        focusedTextColor = PurpleLightText,
                        unfocusedTextColor = PurpleLightText,
                        focusedLabelColor = PurpleAccent,
                        unfocusedLabelColor = PurpleMutedText
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_food_name_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Serving size and unit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = servingSizeText,
                        onValueChange = { servingSizeText = it.replace(',', '.') },
                        label = { Text("Serving Size") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = PurpleSurfaceBorder,
                            focusedTextColor = PurpleLightText,
                            unfocusedTextColor = PurpleLightText,
                            focusedLabelColor = PurpleAccent,
                            unfocusedLabelColor = PurpleMutedText
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = servingUnit,
                        onValueChange = { servingUnit = it },
                        label = { Text("Unit") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = PurpleSurfaceBorder,
                            focusedTextColor = PurpleLightText,
                            unfocusedTextColor = PurpleLightText,
                            focusedLabelColor = PurpleAccent,
                            unfocusedLabelColor = PurpleMutedText
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Calories
                OutlinedTextField(
                    value = caloriesText,
                    onValueChange = {
                        caloriesText = FractionHelper.sanitizeDecimalInput(it)
                        errorMessage = null
                    },
                    label = { Text("Calories (kcal)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = PurpleSurfaceBorder,
                        focusedTextColor = PurpleLightText,
                        unfocusedTextColor = PurpleLightText,
                        focusedLabelColor = PurpleAccent,
                        unfocusedLabelColor = PurpleMutedText
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_food_calories_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Macros
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = proteinText,
                        onValueChange = { proteinText = FractionHelper.sanitizeDecimalInput(it) },
                        label = { Text("Protein (g)", color = MacroProteinColor) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MacroProteinColor,
                            unfocusedBorderColor = PurpleSurfaceBorder,
                            focusedTextColor = PurpleLightText,
                            unfocusedTextColor = PurpleLightText
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = carbsText,
                        onValueChange = { carbsText = FractionHelper.sanitizeDecimalInput(it) },
                        label = { Text("Carbs (g)", color = MacroCarbsColor) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MacroCarbsColor,
                            unfocusedBorderColor = PurpleSurfaceBorder,
                            focusedTextColor = PurpleLightText,
                            unfocusedTextColor = PurpleLightText
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = fatText,
                        onValueChange = { fatText = FractionHelper.sanitizeDecimalInput(it) },
                        label = { Text("Fat (g)", color = MacroFatColor) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MacroFatColor,
                            unfocusedBorderColor = PurpleSurfaceBorder,
                            focusedTextColor = PurpleLightText,
                            unfocusedTextColor = PurpleLightText
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFFB7185),
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions: Delete, Cancel, Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            onDelete(entry.id)
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFB7185)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFFB7185).copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("edit_food_delete_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete entry",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontSize = 12.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, PurpleSurfaceBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleLightText)
                        ) {
                            Text("Cancel", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                val trimmedName = foodName.trim()
                                if (trimmedName.isBlank()) {
                                    errorMessage = "Please enter food name"
                                    return@Button
                                }
                                val cals = FractionHelper.parseCalories(caloriesText)
                                if (cals == null || cals < 0) {
                                    errorMessage = "Please enter valid calories"
                                    return@Button
                                }
                                val parsedSize = FractionHelper.parse(servingSizeText) ?: 1f
                                val updated = entry.copy(
                                    mealType = selectedMeal,
                                    name = trimmedName,
                                    calories = cals,
                                    protein = proteinText.toFloatOrNull() ?: 0f,
                                    carbs = carbsText.toFloatOrNull() ?: 0f,
                                    fat = fatText.toFloatOrNull() ?: 0f,
                                    servingSize = parsedSize,
                                    servingUnit = servingUnit.trim().ifBlank { "serving" }
                                )
                                onSave(updated)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PurplePrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("edit_food_save_btn")
                        ) {
                            Text("Save", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
