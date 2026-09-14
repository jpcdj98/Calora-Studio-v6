package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.example.data.util.FractionHelper
import com.example.data.util.ServingMeasurement
import kotlin.math.abs
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.FoodItem
import com.example.ui.theme.MacroCarbsColor
import com.example.ui.theme.MacroFatColor
import com.example.ui.theme.MacroProteinColor
import com.example.ui.theme.MetricWorkoutColor
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleCardHover
import com.example.ui.theme.PurpleLightText
import com.example.ui.theme.PurpleMutedText
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurpleSurface
import com.example.ui.theme.PurpleSurfaceBorder
import com.example.ui.theme.PurpleSurfaceVariant
import java.util.Locale

@Composable
fun FoodDatabaseView(
    foodItems: List<FoodItem>,
    selectedMeal: String,
    onLogFood: (
        meal: String,
        name: String,
        calories: Int,
        protein: Float,
        carbs: Float,
        fat: Float,
        size: Float,
        unit: String
    ) -> Unit,
    onCustomizeInManual: (
        name: String,
        calories: Int,
        protein: Float,
        carbs: Float,
        fat: Float,
        size: Float,
        unit: String
    ) -> Unit,
    onSaveCustomFood: (FoodItem) -> Unit,
    onDeleteCustomFood: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var showAddCustomFoodDialog by rememberSaveable { mutableStateOf(false) }
    var loggedToastMessage by remember { mutableStateOf<String?>(null) }

    val categories = listOf(
        "All",
        "Meat & Poultry",
        "Eggs & Dairy",
        "Seafood",
        "Grains & Carbs",
        "Vegetables",
        "Fruits",
        "Fats & Nuts",
        "Supplements",
        "Saved Custom"
    )

    // Smart ranking and grouping of foods based on search query and category
    val sortedAndFilteredGroups = remember(foodItems, searchQuery, selectedCategory) {
        val query = searchQuery.trim().lowercase()

        // 1. Filter by category
        val categoryFiltered = foodItems.filter { item ->
            when (selectedCategory) {
                "All" -> true
                "Saved Custom" -> item.isCustom
                else -> item.category.equals(selectedCategory, ignoreCase = true)
            }
        }

        // 2. Filter matching items
        val matches = if (query.isBlank()) {
            categoryFiltered
        } else {
            categoryFiltered.filter { item ->
                item.name.lowercase().contains(query) ||
                        item.variation.lowercase().contains(query) ||
                        item.searchKeywords.lowercase().contains(query) ||
                        item.category.lowercase().contains(query)
            }
        }

        // 3. Group by Food Name to manage variations together
        val grouped = matches.groupBy { it.name }

        // 4. Rank items closest to search query or category
        if (query.isNotBlank()) {
            grouped.entries.sortedByDescending { (foodName, variations) ->
                val lowerName = foodName.lowercase()
                var score = 0
                if (lowerName == query) score += 1000
                else if (lowerName.startsWith(query)) score += 500
                else if (lowerName.split(" ", "-", "/").any { it.startsWith(query) }) score += 300
                else if (lowerName.contains(query)) score += 200

                // Variation match
                if (variations.any { it.variation.lowercase().contains(query) }) score += 120
                // Keyword match
                if (variations.any { it.searchKeywords.lowercase().contains(query) }) score += 60
                // Category match
                if (variations.any { it.category.lowercase().contains(query) }) score += 40
                // Custom food bonus
                if (variations.any { it.isCustom }) score += 15

                score
            }
        } else {
            // Default ordering: Custom foods first, then staple diet foods, then alphabetical
            val staples = listOf(
                "Chicken Breast", "Eggs", "Egg Whites", "White Rice", "Brown Rice",
                "Salmon", "Ground Beef (93/7)", "Sweet Potato", "Oats / Oatmeal",
                "Greek Yogurt", "Whey Protein Powder", "Tuna", "Steak (Sirloin)",
                "Avocado", "Broccoli", "Banana", "Apple", "Peanut Butter", "Almonds"
            )
            grouped.entries.sortedWith(
                compareByDescending<Map.Entry<String, List<FoodItem>>> { it.value.any { item -> item.isCustom } }
                    .thenByDescending { staples.contains(it.key) }
                    .thenBy {
                        val idx = staples.indexOf(it.key)
                        if (idx >= 0) idx else Int.MAX_VALUE
                    }
                    .thenBy { it.key }
            )
        }
    }

    // Pagination: Max 5 items visible at a time to prevent slow opening and cramped view
    val maxPageStep = 5
    var visibleLimit by rememberSaveable(searchQuery, selectedCategory) { mutableIntStateOf(maxPageStep) }
    val totalGroupsCount = sortedAndFilteredGroups.size
    val visibleGroups = remember(sortedAndFilteredGroups, visibleLimit) {
        sortedAndFilteredGroups.take(visibleLimit)
    }
    val remainingCount = (totalGroupsCount - visibleLimit).coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search food", color = PurpleMutedText, fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = PurpleAccent,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = PurpleMutedText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PurplePrimary,
                unfocusedBorderColor = PurpleSurfaceBorder,
                focusedTextColor = PurpleLightText,
                unfocusedTextColor = PurpleLightText,
                focusedContainerColor = PurpleSurfaceVariant.copy(alpha = 0.6f),
                unfocusedContainerColor = PurpleSurfaceVariant.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("food_database_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { category ->
                val isSelected = selectedCategory == category
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) PurplePrimary else PurpleSurfaceVariant)
                        .border(
                            1.dp,
                            if (isSelected) PurpleAccent else PurpleSurfaceBorder,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedCategory = category }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("cat_chip_$category")
                ) {
                    Text(
                        text = category,
                        color = if (isSelected) Color.White else PurpleLightText,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Top Actions Bar: count & "Save Custom Food"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (totalGroupsCount == 0) "0 foods found" else "Showing ${visibleGroups.size} of $totalGroupsCount foods",
                color = PurpleMutedText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Button(
                onClick = { showAddCustomFoodDialog = !showAddCustomFoodDialog },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PurpleSurfaceVariant,
                    contentColor = PurpleAccent
                ),
                border = BorderStroke(1.dp, PurpleSurfaceBorder),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("add_custom_food_btn")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (showAddCustomFoodDialog) Icons.Default.Remove else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showAddCustomFoodDialog) "Close Form" else "Save Custom Food",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Inline Add Custom Food Expansion
        AnimatedVisibility(
            visible = showAddCustomFoodDialog,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            AddCustomFoodCard(
                onSave = { newItem ->
                    onSaveCustomFood(newItem)
                    showAddCustomFoodDialog = false
                    loggedToastMessage = "Saved '${newItem.getFullDisplayName()}' to Database!"
                },
                onCancel = { showAddCustomFoodDialog = false }
            )
        }

        // Toast feedback pill if logged
        AnimatedVisibility(
            visible = loggedToastMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (loggedToastMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF065F46))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = loggedToastMessage ?: "",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Food Items List (Ranked Top 5 closest, minimal data until tapped)
        if (sortedAndFilteredGroups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PurpleSurfaceVariant.copy(alpha = 0.5f))
                    .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = PurpleMutedText,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No foods found for \"$searchQuery\"",
                        color = PurpleLightText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap 'Save Custom Food' above to add it to your database.",
                        color = PurpleMutedText,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                visibleGroups.forEach { (foodName, variations) ->
                    FoodGroupCard(
                        foodName = foodName,
                        variations = variations,
                        selectedMeal = selectedMeal,
                        onLogFood = { name, cals, p, c, f, size, unit ->
                            onLogFood(selectedMeal, name, cals, p, c, f, size, unit)
                            loggedToastMessage = "Logged $name ($size $unit) to $selectedMeal!"
                        },
                        onCustomizeInManual = onCustomizeInManual,
                        onDeleteCustomFood = onDeleteCustomFood
                    )
                }
            }

            // Show More Results controls if there are additional matching items
            if (remainingCount > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val nextCount = minOf(maxPageStep, remainingCount)
                    Button(
                        onClick = { visibleLimit += maxPageStep },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurpleSurfaceVariant,
                            contentColor = PurpleAccent
                        ),
                        border = BorderStroke(1.dp, PurpleSurfaceBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("show_more_foods_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Show More Results ($nextCount more)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (remainingCount > maxPageStep) {
                        OutlinedButton(
                            onClick = { visibleLimit = totalGroupsCount },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PurpleLightText
                            ),
                            border = BorderStroke(1.dp, PurpleSurfaceBorder),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("show_all_foods_btn")
                        ) {
                            Text(
                                text = "Show All ($totalGroupsCount)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else if (visibleLimit > maxPageStep && totalGroupsCount > maxPageStep) {
                // Option to collapse back to top 5
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(
                        onClick = { visibleLimit = maxPageStep },
                        colors = ButtonDefaults.textButtonColors(contentColor = PurpleMutedText)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExpandLess,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Show Less (Top 5 only)", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun FoodGroupCard(
    foodName: String,
    variations: List<FoodItem>,
    selectedMeal: String,
    onLogFood: (name: String, calories: Int, protein: Float, carbs: Float, fat: Float, size: Float, unit: String) -> Unit,
    onCustomizeInManual: (name: String, calories: Int, protein: Float, carbs: Float, fat: Float, size: Float, unit: String) -> Unit,
    onDeleteCustomFood: (Long) -> Unit
) {
    var isExpanded by rememberSaveable(foodName) { mutableStateOf(false) }
    var selectedIndex by rememberSaveable(foodName) { mutableIntStateOf(0) }
    val activeItem = variations.getOrNull(selectedIndex) ?: variations.first()

    val availableUnits = remember(activeItem.id) { ServingMeasurement.getAvailableUnitsForFood(activeItem) }
    var selectedUnit by remember(activeItem.id) {
        mutableStateOf(
            if (activeItem.servingUnit.isNotBlank() && !activeItem.servingUnit.contains("(")) {
                activeItem.servingUnit
            } else if (availableUnits.isNotEmpty()) {
                availableUnits.first()
            } else {
                "g"
            }
        )
    }

    fun defaultQtyForUnit(u: String): Float = when (u.lowercase().trim()) {
        "g", "ml" -> 100f
        "oz" -> 4f
        "kg", "lbs" -> 0.25f
        else -> 1f
    }

    var quantity by remember(activeItem.id, selectedUnit) {
        mutableFloatStateOf(defaultQtyForUnit(selectedUnit))
    }
    val scaled = activeItem.scaledForUnit(quantity, selectedUnit)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isExpanded) PurpleSurfaceVariant else PurpleSurface)
            .border(
                1.dp,
                if (isExpanded) PurplePrimary else PurpleSurfaceBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable { isExpanded = !isExpanded }
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .testTag("food_group_${foodName.lowercase().replace(" ", "_")}")
    ) {
        Column {
            // Minimal Header Row: Always visible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 38.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Title + minimal subtitle info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = foodName,
                            color = PurpleLightText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (activeItem.isCustom) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PurpleAccent.copy(alpha = 0.2f))
                                    .border(0.5.dp, PurpleAccent.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
                            ) {
                                Text(
                                    text = "Custom",
                                    color = PurpleAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (variations.size > 1) {
                            Text(
                                text = "${variations.size} variations",
                                color = PurpleAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                text = " • ",
                                color = PurpleMutedText,
                                fontSize = 11.sp
                            )
                        } else if (activeItem.variation.isNotBlank() && !activeItem.variation.equals("Standard", ignoreCase = true)) {
                            Text(
                                text = activeItem.variation,
                                color = PurpleAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = " • ",
                                color = PurpleMutedText,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = activeItem.category,
                            color = PurpleMutedText,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right: Minimal stats & expand indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.widthIn(max = 115.dp)
                    ) {
                        Text(
                            text = "${activeItem.calories} kcal",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.0f", activeItem.protein)}g P • ${activeItem.getServingDescription()}",
                            color = PurpleMutedText,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (isExpanded) PurplePrimary.copy(alpha = 0.25f) else PurpleSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand details",
                            tint = if (isExpanded) PurpleAccent else PurpleMutedText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Expanded Details: Serving size, variations, macros, and logging actions
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    // Subtle divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(PurpleSurfaceBorder)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Full Title Banner: Displays complete food name & variation if it was truncated in the collapsed row
                    if (foodName.length > 22 || (activeItem.variation.isNotBlank() && !activeItem.variation.equals("Standard", ignoreCase = true))) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(PurpleSurfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activeItem.getFullDisplayName(),
                                color = PurpleLightText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            if (activeItem.isCustom) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(PurpleAccent.copy(alpha = 0.2f))
                                        .border(0.5.dp, PurpleAccent.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "Custom",
                                        color = PurpleAccent,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Variations Chips (Crucial for chicken breast skin on/off, eggs boiled/fried, etc)
                    if (variations.size > 1) {
                        Text(
                            text = "SELECT VARIATION:",
                            color = PurpleAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(5.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            variations.forEachIndexed { index, varItem ->
                                val isVarSelected = (index == selectedIndex)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isVarSelected) PurplePrimary else PurpleCardHover)
                                        .border(
                                            1.dp,
                                            if (isVarSelected) PurpleAccent else PurpleSurfaceBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            selectedIndex = index
                                            quantity = defaultQtyForUnit(selectedUnit)
                                        }
                                        .padding(horizontal = 9.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = varItem.variation,
                                        color = if (isVarSelected) Color.White else PurpleLightText,
                                        fontSize = 11.sp,
                                        fontWeight = if (isVarSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Base Nutrition Info Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(PurpleSurfaceVariant.copy(alpha = 0.7f))
                            .border(1.dp, PurpleSurfaceBorder.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Base: ${activeItem.getServingDescription()}",
                            color = PurpleMutedText,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${activeItem.calories} kcal  •  ${activeItem.protein}g P  •  ${activeItem.carbs}g C  •  ${activeItem.fat}g F",
                            color = PurpleLightText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Serving Unit Selection Row
                    Text(
                        text = "SERVING UNIT",
                        color = PurpleMutedText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        availableUnits.forEach { unitCode ->
                            val isSelected = selectedUnit.equals(unitCode, ignoreCase = true)
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
                                        selectedUnit = unitCode
                                        quantity = defaultQtyForUnit(unitCode)
                                    }
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = unitCode,
                                    color = if (isSelected) Color.White else PurpleLightText,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Portion Stepper & Quick Fraction / Metric Presets
                    val isFractionFriendly = remember(selectedUnit) {
                        val u = selectedUnit.lowercase().trim()
                        u !in listOf("g", "ml")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Stepper: [-] [Formatted Quantity] [+]
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PurpleSurfaceVariant)
                                .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    quantity = when {
                                        selectedUnit.equals("g", true) -> (quantity - 25f).coerceAtLeast(10f)
                                        selectedUnit.equals("ml", true) -> (quantity - 25f).coerceAtLeast(10f)
                                        selectedUnit.equals("oz", true) -> (quantity - 0.5f).coerceAtLeast(0.5f)
                                        else -> (quantity - 0.25f).coerceAtLeast(0.25f)
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease portion",
                                    tint = PurpleLightText,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            val formattedDisplay = FractionHelper.formatServing(quantity, selectedUnit)
                            Text(
                                text = formattedDisplay,
                                color = PurpleAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )

                            IconButton(
                                onClick = {
                                    quantity = when {
                                        selectedUnit.equals("g", true) -> (quantity + 25f).coerceAtMost(2000f)
                                        selectedUnit.equals("ml", true) -> (quantity + 25f).coerceAtMost(2000f)
                                        selectedUnit.equals("oz", true) -> (quantity + 0.5f).coerceAtMost(50f)
                                        else -> (quantity + 0.25f).coerceAtMost(20f)
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase portion",
                                    tint = PurpleLightText,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Quick presets: Fraction buttons or weight buttons
                        val quickPresets = when {
                            isFractionFriendly -> listOf(
                                0.25f to "1/4",
                                0.333f to "1/3",
                                0.5f to "1/2",
                                0.667f to "2/3",
                                0.75f to "3/4",
                                1.0f to "1",
                                1.5f to "1 1/2",
                                2.0f to "2"
                            )
                            selectedUnit.equals("oz", true) -> listOf(
                                2.0f to "2 oz",
                                4.0f to "4 oz",
                                6.0f to "6 oz",
                                8.0f to "8 oz"
                            )
                            else -> listOf(
                                50f to "50g",
                                100f to "100g",
                                150f to "150g",
                                200f to "200g"
                            )
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .padding(start = 6.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            quickPresets.forEach { (m, label) ->
                                val isCurr = abs(quantity - m) < 0.04f
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isCurr) PurplePrimary else PurpleSurfaceVariant)
                                        .border(
                                            1.dp,
                                            if (isCurr) PurpleAccent else PurpleSurfaceBorder,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { quantity = m }
                                        .padding(horizontal = 7.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isCurr) Color.White else PurpleMutedText,
                                        fontSize = 10.sp,
                                        fontWeight = if (isCurr) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Calculated Totals Summary Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(PurplePrimary.copy(alpha = 0.15f))
                            .border(1.dp, PurplePrimary.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "PORTION TOTAL",
                                        color = PurpleMutedText,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    if (scaled.totalGrams != null && !selectedUnit.equals("g", ignoreCase = true)) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "(~${scaled.totalGrams.toInt()}g)",
                                            color = PurpleAccent,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Text(
                                    text = "${scaled.calories} kcal",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${String.format(Locale.US, "%.1f", scaled.protein)}g P  •  ${String.format(Locale.US, "%.1f", scaled.carbs)}g C  •  ${String.format(Locale.US, "%.1f", scaled.fat)}g F",
                                color = PurpleAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Buttons: "Log to Meal" and "Customize"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (activeItem.isCustom) {
                            IconButton(
                                onClick = { onDeleteCustomFood(activeItem.id) },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PurpleSurfaceVariant)
                                    .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(8.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete custom food",
                                    tint = Color(0xFFFB7185),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val fullName = activeItem.getFullDisplayName()
                                onCustomizeInManual(
                                    fullName,
                                    scaled.calories,
                                    scaled.protein,
                                    scaled.carbs,
                                    scaled.fat,
                                    scaled.servingSize,
                                    scaled.servingUnit
                                )
                            },
                            border = BorderStroke(1.dp, PurpleSurfaceBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleLightText),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(36.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = PurpleMutedText
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Edit",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val fullName = activeItem.getFullDisplayName()
                                onLogFood(
                                    fullName,
                                    scaled.calories,
                                    scaled.protein,
                                    scaled.carbs,
                                    scaled.fat,
                                    scaled.servingSize,
                                    scaled.servingUnit
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PurplePrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("log_${foodName.lowercase().replace(" ", "_")}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Log to $selectedMeal",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddCustomFoodCard(
    onSave: (FoodItem) -> Unit,
    onCancel: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Meat & Poultry") }
    var variation by rememberSaveable { mutableStateOf("Standard") }
    var sizeText by rememberSaveable { mutableStateOf("100") }
    var unit by rememberSaveable { mutableStateOf("g") }
    var caloriesText by rememberSaveable { mutableStateOf("") }
    var proteinText by rememberSaveable { mutableStateOf("") }
    var carbsText by rememberSaveable { mutableStateOf("") }
    var fatText by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    val categoryOptions = listOf(
        "Meat & Poultry",
        "Eggs & Dairy",
        "Seafood",
        "Grains & Carbs",
        "Vegetables",
        "Fruits",
        "Fats & Nuts",
        "Supplements",
        "Custom"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PurpleSurfaceVariant)
            .border(1.dp, PurpleAccent, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Save New Food to Database",
                    color = PurpleLightText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Cancel",
                        tint = PurpleMutedText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Food Name & Variation
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                label = { Text("Food Name (e.g. Chicken Breast)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurplePrimary,
                    unfocusedBorderColor = PurpleSurfaceBorder,
                    focusedTextColor = PurpleLightText,
                    unfocusedTextColor = PurpleLightText
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = variation,
                    onValueChange = { variation = it },
                    label = { Text("Variation (e.g. Air-fried, Skinless)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = PurpleSurfaceBorder,
                        focusedTextColor = PurpleLightText,
                        unfocusedTextColor = PurpleLightText
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category selector chips
            Text(
                text = "CATEGORY:",
                color = PurpleMutedText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                categoryOptions.forEach { opt ->
                    val isSel = (category == opt)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) PurplePrimary else PurpleSurface)
                            .border(
                                1.dp,
                                if (isSel) PurpleAccent else PurpleSurfaceBorder,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { category = opt }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = opt,
                            color = if (isSel) Color.White else PurpleLightText,
                            fontSize = 10.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Portion row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = sizeText,
                    onValueChange = { input ->
                        val sanitized = input.replace(',', '.')
                        if (sanitized.all { it.isDigit() || it == '.' || it == '/' || it == ' ' }) {
                            sizeText = sanitized
                        }
                    },
                    label = { Text("Serving Size (e.g. 1, 3/4)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = PurpleSurfaceBorder,
                        focusedTextColor = PurpleLightText,
                        unfocusedTextColor = PurpleLightText
                    ),
                    modifier = Modifier.weight(1.1f)
                )

                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit (g, cup, tbsp)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = PurpleSurfaceBorder,
                        focusedTextColor = PurpleLightText,
                        unfocusedTextColor = PurpleLightText
                    ),
                    modifier = Modifier.weight(1.1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Quick kitchen unit chips
            val quickKitchenUnits = listOf("g", "cup", "tbsp", "tsp", "oz", "ml", "fl oz", "slice", "piece", "serving", "can", "packet")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                quickKitchenUnits.forEach { u ->
                    val isSel = unit.equals(u, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) PurplePrimary else PurpleSurface)
                            .border(
                                1.dp,
                                if (isSel) PurpleAccent else PurpleSurfaceBorder,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { unit = u }
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = u,
                            color = if (isSel) Color.White else PurpleLightText,
                            fontSize = 10.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calories & Macros
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = caloriesText,
                    onValueChange = {
                        caloriesText = FractionHelper.sanitizeDecimalInput(it)
                        error = null
                    },
                    label = { Text("Calories") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MetricWorkoutColor,
                        unfocusedBorderColor = PurpleSurfaceBorder,
                        focusedTextColor = PurpleLightText,
                        unfocusedTextColor = PurpleLightText
                    ),
                    modifier = Modifier.weight(1.1f)
                )

                OutlinedTextField(
                    value = proteinText,
                    onValueChange = { proteinText = FractionHelper.sanitizeDecimalInput(it) },
                    label = { Text("P (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MacroProteinColor,
                        unfocusedBorderColor = PurpleSurfaceBorder,
                        focusedTextColor = PurpleLightText,
                        unfocusedTextColor = PurpleLightText
                    ),
                    modifier = Modifier.weight(0.9f)
                )

                OutlinedTextField(
                    value = carbsText,
                    onValueChange = { carbsText = FractionHelper.sanitizeDecimalInput(it) },
                    label = { Text("C (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MacroCarbsColor,
                        unfocusedBorderColor = PurpleSurfaceBorder,
                        focusedTextColor = PurpleLightText,
                        unfocusedTextColor = PurpleLightText
                    ),
                    modifier = Modifier.weight(0.9f)
                )

                OutlinedTextField(
                    value = fatText,
                    onValueChange = { fatText = FractionHelper.sanitizeDecimalInput(it) },
                    label = { Text("F (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MacroFatColor,
                        unfocusedBorderColor = PurpleSurfaceBorder,
                        focusedTextColor = PurpleLightText,
                        unfocusedTextColor = PurpleLightText
                    ),
                    modifier = Modifier.weight(0.9f)
                )
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = error ?: "", color = Color(0xFFFB7185), fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    if (name.isBlank()) {
                        error = "Please enter food name"
                        return@Button
                    }
                    val cals = FractionHelper.parseCalories(caloriesText)
                    if (cals == null || cals < 0) {
                        error = "Please enter valid calories"
                        return@Button
                    }
                    val p = proteinText.toFloatOrNull() ?: 0f
                    val c = carbsText.toFloatOrNull() ?: 0f
                    val f = fatText.toFloatOrNull() ?: 0f
                    val sz = FractionHelper.parse(sizeText) ?: 100f

                    onSave(
                        FoodItem(
                            id = 0,
                            name = name.trim(),
                            category = category.trim(),
                            variation = variation.trim().ifBlank { "Standard" },
                            servingSize = sz,
                            servingUnit = unit.trim().ifBlank { "g" },
                            calories = cals,
                            protein = p,
                            carbs = c,
                            fat = f,
                            isCustom = true,
                            searchKeywords = "$name $category $variation $unit".lowercase()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PurplePrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            ) {
                Text("Save to Food Database", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
