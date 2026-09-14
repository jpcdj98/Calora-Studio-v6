package com.example.data.util

import com.example.data.local.entity.FoodItem
import com.example.data.local.entity.ScaledNutrition
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Categories of measurement units
 */
enum class UnitCategory(val displayName: String) {
    VOLUME("Volume & Kitchen"),
    WEIGHT("Weight"),
    DISCRETE("Pieces & Items"),
    MEAT_PORTION("Meat Portions")
}

/**
 * Metadata for a measurement unit
 */
data class ServingUnitInfo(
    val code: String,
    val displayName: String,
    val category: UnitCategory,
    val supportsFractions: Boolean = true,
    val defaultStep: Float = 0.25f,
    val isVolumeOrItem: Boolean = true
)

/**
 * Helper to parse and format fractions (e.g. 1/4, 1/3, 1/2, 2/3, 3/4, 1 1/2)
 */
object FractionHelper {

    val COMMON_FRACTIONS = listOf("1/4", "1/3", "1/2", "2/3", "3/4", "1", "1 1/2", "2")

    /**
     * Parses a string that might be a fraction (e.g. "3/4", "1/2", "1 1/2", "0.75", "2") into a Float.
     */
    fun parse(input: String): Float? {
        val trimmed = input.trim().replace(",", ".")
        if (trimmed.isEmpty()) return null

        // Try direct float/decimal parse first
        trimmed.toFloatOrNull()?.let {
            if (it >= 0f) return it
        }

        // Mixed fraction like "1 1/2" or "1-1/2"
        val spaceParts = trimmed.split(Regex("[\\s-]+")).filter { it.isNotBlank() }
        if (spaceParts.size == 2) {
            val whole = spaceParts[0].toFloatOrNull() ?: return null
            val frac = parseSimpleFraction(spaceParts[1]) ?: return null
            return whole + frac
        }

        // Simple fraction like "3/4"
        if (trimmed.contains("/")) {
            return parseSimpleFraction(trimmed)
        }

        return null
    }

    private fun parseSimpleFraction(str: String): Float? {
        val parts = str.split("/")
        if (parts.size == 2) {
            val num = parts[0].trim().toFloatOrNull() ?: return null
            val den = parts[1].trim().toFloatOrNull() ?: return null
            if (den != 0f) return num / den
        }
        return null
    }

    /**
     * Formats amount to fraction string (e.g. 0.75 -> "3/4", 1.5 -> "1 1/2", 0.5 -> "1/2")
     * If preferFraction is false or amount is not near a known fraction, returns standard decimal.
     */
    fun format(amount: Float, preferFraction: Boolean = true): String {
        if (amount <= 0f) return "0"

        if (preferFraction) {
            val whole = amount.toInt()
            val frac = amount - whole
            val eps = 0.045f

            val fracStr = when {
                abs(frac - 0.25f) < eps -> "1/4"
                abs(frac - 0.3333f) < eps -> "1/3"
                abs(frac - 0.5f) < eps -> "1/2"
                abs(frac - 0.6667f) < eps -> "2/3"
                abs(frac - 0.75f) < eps -> "3/4"
                abs(frac - 0.125f) < eps -> "1/8"
                abs(frac - 0.375f) < eps -> "3/8"
                abs(frac - 0.625f) < eps -> "5/8"
                abs(frac - 0.875f) < eps -> "7/8"
                abs(frac) < eps -> ""
                abs(frac - 1.0f) < eps -> ""
                else -> null
            }

            if (fracStr != null) {
                val effectiveWhole = if (abs(frac - 1.0f) < eps) whole + 1 else whole
                return when {
                    effectiveWhole > 0 && fracStr.isNotEmpty() -> "$effectiveWhole $fracStr"
                    fracStr.isNotEmpty() -> fracStr
                    effectiveWhole > 0 -> effectiveWhole.toString()
                    else -> "0"
                }
            }
        }

        return if (amount % 1f == 0f) {
            amount.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", amount)
        }
    }

    /**
     * Formats serving size and unit for display in meal lists and headers.
     * e.g. "3/4 cup", "1/2 tbsp", "1 1/2 slice", "100g"
     */
    fun formatServing(servingSize: Float, servingUnit: String): String {
        if (servingUnit.isBlank()) return ""
        val unitLower = servingUnit.lowercase().trim()
        val isFractionUnit = unitLower in listOf(
            "cup", "cups", "tbsp", "tsp", "piece", "pieces", "slice", "slices",
            "serving", "servings", "packet", "packets", "can", "cans",
            "bottle", "bottles", "clove", "cloves", "bunch", "bunches",
            "small fillet", "medium breast", "large steak", "quarter pounder",
            "palm size", "drumstick", "thigh", "patty", "fillet"
        ) || (servingSize < 1.0f && unitLower !in listOf("g", "ml"))

        val sizeStr = format(servingSize, preferFraction = isFractionUnit)
        val sep = if (unitLower == "g" || unitLower == "ml") "" else " "
        return "$sizeStr$sep$servingUnit"
    }

    /**
     * Sanitizes input for decimal number text fields:
     * - Replaces commas with dots (for international keyboard layouts)
     * - Retains digits and permits at most one decimal point
     */
    fun sanitizeDecimalInput(input: String): String {
        val replaced = input.replace(',', '.')
        var dotSeen = false
        val sb = StringBuilder()
        for (c in replaced) {
            if (c.isDigit()) {
                sb.append(c)
            } else if (c == '.' && !dotSeen) {
                sb.append(c)
                dotSeen = true
            }
        }
        return sb.toString()
    }

    /**
     * Parses a calorie string that might be entered with decimals, rounding to Int.
     * e.g. "250.5" -> 251, "120" -> 120
     */
    fun parseCalories(input: String): Int? {
        val sanitized = input.trim().replace(',', '.')
        if (sanitized.isBlank()) return null
        return sanitized.toDoubleOrNull()?.roundToInt() ?: sanitized.toIntOrNull()
    }

    /**
     * Formats a macro float value for text inputs:
     * - Omits trailing .0 if it's a whole number
     * - Retains decimals if present
     * - Returns empty string if <= 0
     */
    fun formatMacroValue(amount: Float): String {
        if (amount <= 0f) return ""
        return if (amount % 1f == 0f) {
            amount.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", amount)
        }
    }
}

/**
 * Measurement System handling all kitchen units, fractions, and per-food gram conversions.
 */
object ServingMeasurement {

    // Kitchen Volume units
    val VOLUME_UNITS = listOf(
        ServingUnitInfo("cup", "Cup (cup)", UnitCategory.VOLUME, supportsFractions = true, defaultStep = 0.25f),
        ServingUnitInfo("tbsp", "Tablespoon (tbsp)", UnitCategory.VOLUME, supportsFractions = true, defaultStep = 0.25f),
        ServingUnitInfo("tsp", "Teaspoon (tsp)", UnitCategory.VOLUME, supportsFractions = true, defaultStep = 0.25f),
        ServingUnitInfo("ml", "Milliliter (mL)", UnitCategory.VOLUME, supportsFractions = false, defaultStep = 25f, isVolumeOrItem = false),
        ServingUnitInfo("fl oz", "Fluid Ounce (fl oz)", UnitCategory.VOLUME, supportsFractions = true, defaultStep = 0.5f)
    )

    // Weight units
    val WEIGHT_UNITS = listOf(
        ServingUnitInfo("g", "Gram (g)", UnitCategory.WEIGHT, supportsFractions = false, defaultStep = 25f, isVolumeOrItem = false),
        ServingUnitInfo("kg", "Kilogram (kg)", UnitCategory.WEIGHT, supportsFractions = true, defaultStep = 0.1f, isVolumeOrItem = false),
        ServingUnitInfo("oz", "Ounce (oz)", UnitCategory.WEIGHT, supportsFractions = true, defaultStep = 0.5f, isVolumeOrItem = false),
        ServingUnitInfo("lbs", "Pound (lbs)", UnitCategory.WEIGHT, supportsFractions = true, defaultStep = 0.25f, isVolumeOrItem = false)
    )

    // Discrete / Items units
    val DISCRETE_UNITS = listOf(
        ServingUnitInfo("piece", "Piece (pc)", UnitCategory.DISCRETE, supportsFractions = true, defaultStep = 0.5f),
        ServingUnitInfo("slice", "Slice", UnitCategory.DISCRETE, supportsFractions = true, defaultStep = 0.5f),
        ServingUnitInfo("serving", "Serving", UnitCategory.DISCRETE, supportsFractions = true, defaultStep = 0.25f),
        ServingUnitInfo("packet", "Packet", UnitCategory.DISCRETE, supportsFractions = true, defaultStep = 0.5f),
        ServingUnitInfo("can", "Can", UnitCategory.DISCRETE, supportsFractions = true, defaultStep = 0.5f),
        ServingUnitInfo("bottle", "Bottle", UnitCategory.DISCRETE, supportsFractions = true, defaultStep = 0.5f),
        ServingUnitInfo("clove", "Clove", UnitCategory.DISCRETE, supportsFractions = true, defaultStep = 0.5f),
        ServingUnitInfo("bunch", "Bunch", UnitCategory.DISCRETE, supportsFractions = true, defaultStep = 0.25f)
    )

    // Common Meat Portions
    val MEAT_PORTION_UNITS = listOf(
        ServingUnitInfo("small fillet", "Small Fillet (120g)", UnitCategory.MEAT_PORTION, supportsFractions = true, defaultStep = 0.5f),
        ServingUnitInfo("medium breast", "Medium Breast (170g)", UnitCategory.MEAT_PORTION, supportsFractions = true, defaultStep = 0.5f),
        ServingUnitInfo("large steak", "Large Steak (225g / 8oz)", UnitCategory.MEAT_PORTION, supportsFractions = true, defaultStep = 0.5f),
        ServingUnitInfo("quarter pounder", "Quarter Pounder (113g / 4oz)", UnitCategory.MEAT_PORTION, supportsFractions = true, defaultStep = 0.5f),
        ServingUnitInfo("palm size", "Palm Size (~100g)", UnitCategory.MEAT_PORTION, supportsFractions = true, defaultStep = 0.5f),
        ServingUnitInfo("drumstick", "Drumstick (~50g meat)", UnitCategory.MEAT_PORTION, supportsFractions = true, defaultStep = 1.0f),
        ServingUnitInfo("thigh", "Thigh Portion (~100g meat)", UnitCategory.MEAT_PORTION, supportsFractions = true, defaultStep = 0.5f)
    )

    val ALL_STANDARD_UNITS: List<ServingUnitInfo> =
        VOLUME_UNITS + WEIGHT_UNITS + DISCRETE_UNITS + MEAT_PORTION_UNITS

    /**
     * Parses "cup:158,tbsp:10,serving:158" into Map("cup" to 158f, "tbsp" to 10f, ...)
     */
    fun parseConversions(raw: String): Map<String, Float> {
        if (raw.isBlank()) return emptyMap()
        val result = mutableMapOf<String, Float>()
        val pairs = raw.split(',', ';')
        for (pair in pairs) {
            val parts = pair.split(':')
            if (parts.size == 2) {
                val unit = parts[0].trim().lowercase()
                val grams = parts[1].trim().toFloatOrNull()
                if (unit.isNotEmpty() && grams != null && grams > 0f) {
                    result[unit] = grams
                }
            }
        }
        return result
    }

    /**
     * Serializes Map("cup" to 158f, "tbsp" to 10f) into "cup:158,tbsp:10"
     */
    fun encodeConversions(map: Map<String, Float>): String {
        return map.entries.joinToString(",") { (k, v) ->
            val formatted = if (v % 1f == 0f) v.toInt().toString() else String.format(Locale.US, "%.1f", v)
            "${k.trim().lowercase()}:$formatted"
        }
    }

    /**
     * Extracts grams from parenthesized strings like "cup (240ml)", "scoop (30g)", "half (68g)", "slice (38g)"
     */
    fun extractGramsFromString(str: String): Float? {
        val match = Regex("""\((\d+(?:\.\d+)?)\s*(?:g|ml)\)""", RegexOption.IGNORE_CASE).find(str)
        return match?.groupValues?.getOrNull(1)?.toFloatOrNull()
    }

    /**
     * Resolves the grams for 1 unit of a given unit code for a specific food item.
     * Uses the food's specific conversions first, then universal weight conversions,
     * then category/food-specific defaults.
     */
    fun getGramsForUnit(food: FoodItem, unit: String): Float? {
        val u = unit.trim().lowercase()

        // 1. Universal weight conversions
        when (u) {
            "g", "gram", "grams" -> return 1.0f
            "kg", "kilogram", "kilograms" -> return 1000.0f
            "oz", "ounce", "ounces" -> return 28.3495f
            "lbs", "lb", "pound", "pounds" -> return 453.592f
        }

        // 2. Specific per-food conversions stored on the FoodItem
        val foodConversions = parseConversions(food.unitConversions)
        foodConversions[u]?.let { return it }

        // Try normalized match (e.g. "cups" -> "cup")
        val singular = when {
            u.endsWith("s") && !u.equals("lbs", ignoreCase = true) -> u.dropLast(1)
            else -> u
        }
        foodConversions[singular]?.let { return it }

        // 3. Extract grams from unit string if it contains (XXg) or (XXml)
        extractGramsFromString(unit)?.let { return it }

        // 4. Common Meat Portions
        when (u) {
            "small fillet" -> return 120f
            "medium breast" -> return 170f
            "large steak" -> return 225f
            "quarter pounder" -> return 113f
            "palm size" -> return 100f
            "drumstick" -> return 50f
            "thigh" -> return 100f
            "clove" -> return 3f
            "bunch" -> return 300f
            "slice" -> {
                val name = food.name.lowercase()
                return when {
                    name.contains("whole wheat") -> 38f
                    name.contains("white bread") -> 30f
                    name.contains("bread") -> 35f
                    name.contains("cheese") -> 28f
                    else -> 35f
                }
            }
        }

        // 5. Liquid density
        when (u) {
            "ml" -> return 1.0f
            "fl oz" -> return 29.5735f
        }

        // 6. Food-specific discrete items
        val name = food.name.lowercase()
        val varName = food.variation.lowercase()
        val cat = food.category.lowercase()

        when (u) {
            "piece", "egg", "large egg" -> {
                return when {
                    name.contains("egg") && varName.contains("white") -> 33f
                    name.contains("egg") && varName.contains("scrambled") -> 61f
                    name.contains("egg") -> 50f
                    name.contains("banana") -> 118f
                    name.contains("apple") -> 182f
                    name.contains("avocado") -> 136f
                    name.contains("shrimp") -> 15f
                    name.contains("wing") -> 35f
                    name.contains("drumstick") -> 50f
                    name.contains("thigh") -> 100f
                    name.contains("tofu") -> 100f
                    name.contains("potato") -> 150f
                    name.contains("strawberry") || name.contains("strawberries") -> 18f
                    name.contains("broccoli") -> 30f
                    name.contains("asparagus") -> 16f
                    else -> 50f
                }
            }
            "half" -> {
                return when {
                    name.contains("avocado") -> 68f
                    else -> 50f
                }
            }
            "scoop" -> {
                return when {
                    name.contains("casein") -> 33f
                    name.contains("whey") || name.contains("protein") -> 30f
                    else -> 30f
                }
            }
            "cup" -> {
                return when {
                    name.contains("egg") && (varName.contains("liquid") || varName.contains("white")) -> 243f
                    name.contains("egg") -> 136f // 1 cup chopped/mashed hard-boiled eggs (~2.7 large eggs)
                    name.contains("spinach") || name.contains("lettuce") || name.contains("greens") -> 30f
                    name.contains("rice") && name.contains("cooked") -> 158f
                    name.contains("rice") -> 185f
                    name.contains("oats") && name.contains("rolled") -> 80f
                    name.contains("oats") || name.contains("oatmeal") -> 234f
                    name.contains("pasta") || name.contains("spaghetti") -> 140f
                    name.contains("flour") -> 120f
                    name.contains("oil") -> 216f
                    name.contains("butter") -> 227f
                    name.contains("peanut butter") -> 258f
                    name.contains("almond") -> 143f
                    name.contains("milk") || name.contains("yogurt") || cat.contains("dairy") -> 244f
                    name.contains("cottage cheese") -> 226f
                    name.contains("blueberry") || name.contains("blueberries") -> 148f
                    name.contains("strawberry") || name.contains("strawberries") -> 152f
                    name.contains("broccoli") -> 91f
                    name.contains("asparagus") -> 134f
                    name.contains("green bean") || name.contains("green beans") -> 125f
                    name.contains("sweet potato") -> 200f
                    name.contains("potato") -> 210f
                    name.contains("quinoa") -> 185f
                    name.contains("chicken") || name.contains("turkey") || name.contains("beef") -> 140f
                    name.contains("tuna") || name.contains("salmon") -> 150f
                    cat.contains("vegetable") -> 90f
                    cat.contains("fruit") -> 150f
                    cat.contains("meat") || cat.contains("poultry") -> 140f
                    else -> 150f
                }
            }
            "tbsp" -> {
                return when {
                    name.contains("oil") || name.contains("butter") -> 14f
                    name.contains("peanut butter") -> 16f
                    name.contains("egg") -> 15f
                    name.contains("almond") -> 9f
                    name.contains("rice") || name.contains("oats") -> 10f
                    name.contains("quinoa") -> 12f
                    cat.contains("liquid") || cat.contains("dairy") -> 15f
                    else -> 12f
                }
            }
            "tsp" -> {
                return when {
                    name.contains("oil") || name.contains("butter") -> 4.5f
                    name.contains("peanut butter") -> 5.3f
                    else -> 4.0f
                }
            }
            "can" -> return 165f // standard canned tuna (165g)
            "packet" -> return 35f
            "bottle" -> return 330f
        }

        // 7. Base serving unit matching (excluding raw count fallback)
        val baseUnit = food.servingUnit.trim().lowercase()
        val baseUnitClean = baseUnit.replace(Regex("""\s*\(.*\)"""), "").trim()
        if (baseUnitClean == "g" || baseUnitClean == "gram" || baseUnitClean == "grams") {
            return 1.0f
        }
        if (baseUnitClean.isNotEmpty() && baseUnitClean != u) {
            getGramsForUnit(food, baseUnitClean)?.let { return it }
        }

        return null
    }

    /**
     * Determines base grams of a food item definition.
     */
    fun getBaseGrams(food: FoodItem): Float {
        val baseUnit = food.servingUnit.trim().lowercase()
        val baseUnitClean = baseUnit.replace(Regex("""\s*\(.*\)"""), "").trim()

        if (baseUnitClean == "g" || baseUnitClean == "gram" || baseUnitClean == "grams") {
            return food.servingSize.coerceAtLeast(0.1f)
        }

        // 1. Check if food has a conversion for its own base serving unit
        val foodConversions = parseConversions(food.unitConversions)
        foodConversions[baseUnitClean]?.let {
            return (it * food.servingSize).coerceAtLeast(0.1f)
        }
        foodConversions[baseUnit]?.let {
            return (it * food.servingSize).coerceAtLeast(0.1f)
        }
        val singular = if (baseUnitClean.endsWith("s")) baseUnitClean.dropLast(1) else baseUnitClean
        foodConversions[singular]?.let {
            return (it * food.servingSize).coerceAtLeast(0.1f)
        }

        // 2. Check parenthesized grams like "scoop (30g)", "half (68g)", "cup (240ml)"
        extractGramsFromString(food.servingUnit)?.let {
            return (it * food.servingSize).coerceAtLeast(0.1f)
        }

        // 3. Check specific item defaults
        val name = food.name.lowercase()
        val varName = food.variation.lowercase()
        if (name.contains("egg")) {
            val eggWeight = if (varName.contains("white")) 33f else if (varName.contains("scrambled")) 61f else 50f
            val perUnitGrams = when (baseUnitClean) {
                "piece", "egg", "large egg" -> eggWeight
                "cup" -> 136f
                "tbsp" -> 15f
                else -> eggWeight
            }
            return (perUnitGrams * food.servingSize).coerceAtLeast(0.1f)
        }
        if (name.contains("banana")) return (118f * food.servingSize).coerceAtLeast(0.1f)
        if (name.contains("apple")) return (182f * food.servingSize).coerceAtLeast(0.1f)
        if (name.contains("bread")) return (35f * food.servingSize).coerceAtLeast(0.1f)

        // 4. Fallback to getGramsForUnit for base unit
        getGramsForUnit(food, baseUnitClean)?.let {
            return (it * food.servingSize).coerceAtLeast(0.1f)
        }

        return food.servingSize.coerceAtLeast(1f)
    }

    /**
     * Scales nutrition accurately for a specific food using its per-food gram conversion.
     */
    fun scaleNutrition(food: FoodItem, quantity: Float, unit: String): ScaledNutrition {
        val qty = if (quantity <= 0f) 1f else quantity
        val u = unit.trim()

        val gramsPerUnit = getGramsForUnit(food, u)
        val baseGrams = getBaseGrams(food)

        val ratio: Float
        val totalGrams: Float?

        if (gramsPerUnit != null && baseGrams > 0f) {
            val calculatedGrams = qty * gramsPerUnit
            totalGrams = calculatedGrams
            ratio = calculatedGrams / baseGrams
        } else {
            // Fallback if no conversion found
            val baseUnitClean = food.servingUnit.replace(Regex("""\s*\(.*\)"""), "").trim()
            ratio = if (u.equals(food.servingUnit, ignoreCase = true) || u.equals(baseUnitClean, ignoreCase = true)) {
                if (food.servingSize > 0f) qty / food.servingSize else qty
            } else {
                qty
            }
            totalGrams = null
        }

        val cal = (food.calories * ratio).roundToInt()
        val p = food.protein * ratio
        val c = food.carbs * ratio
        val f = food.fat * ratio

        val displayServing = FractionHelper.formatServing(qty, u)

        return ScaledNutrition(
            calories = cal,
            protein = p,
            carbs = c,
            fat = f,
            servingSize = qty,
            servingUnit = u,
            totalGrams = totalGrams,
            displayServing = displayServing
        )
    }

    /**
     * Returns the list of available/recommended units for a specific food.
     * Starts with its own base unit, followed by all units defined in its per-food conversions,
     * then common units appropriate for its category.
     */
    fun getAvailableUnitsForFood(food: FoodItem): List<String> {
        val result = linkedSetOf<String>()

        // 1. Food's own serving unit
        val baseUnit = food.servingUnit.trim()
        val baseUnitClean = baseUnit.replace(Regex("""\s*\(.*\)"""), "").trim()
        if (baseUnitClean.isNotBlank()) {
            result.add(baseUnitClean)
        }

        // 2. Units in per-food unitConversions
        val customMap = parseConversions(food.unitConversions)
        result.addAll(customMap.keys)

        // 3. Category defaults
        val cat = food.category.lowercase()
        val name = food.name.lowercase()

        if (cat.contains("dairy") || cat.contains("egg")) {
            if (name.contains("egg")) {
                result.add("piece")
                result.add("g")
                result.add("cup")
                result.add("tbsp")
                result.add("oz")
            } else if (name.contains("milk")) {
                result.add("cup")
                result.add("fl oz")
                result.add("ml")
                result.add("bottle")
                result.add("tbsp")
                result.add("g")
            } else {
                result.add("cup")
                result.add("tbsp")
                result.add("g")
                result.add("oz")
                result.add("slice")
            }
        } else if (cat.contains("meat") || cat.contains("poultry")) {
            result.add("medium breast")
            result.add("small fillet")
            result.add("large steak")
            result.add("quarter pounder")
            result.add("palm size")
            result.add("oz")
            result.add("g")
            result.add("cup")
        } else if (cat.contains("seafood") || name.contains("tuna") || name.contains("salmon")) {
            if (name.contains("tuna")) result.add("can")
            result.add("small fillet")
            result.add("medium breast")
            result.add("palm size")
            result.add("oz")
            result.add("g")
            result.add("cup")
        } else if (cat.contains("grain") || cat.contains("carb")) {
            result.add("cup")
            result.add("tbsp")
            result.add("g")
            result.add("serving")
            result.add("oz")
            if (name.contains("bread")) result.add("slice")
            if (name.contains("oats")) result.add("packet")
        } else if (cat.contains("fat") || cat.contains("oil") || cat.contains("nut")) {
            result.add("tbsp")
            result.add("tsp")
            result.add("cup")
            result.add("fl oz")
            result.add("g")
            result.add("oz")
        } else if (cat.contains("vegetable")) {
            result.add("cup")
            result.add("tbsp")
            result.add("bunch")
            result.add("clove")
            result.add("g")
            result.add("oz")
        } else if (cat.contains("fruit")) {
            result.add("piece")
            result.add("cup")
            result.add("slice")
            result.add("g")
            result.add("oz")
        }

        // Always ensure grams is present
        result.add("g")

        return result.toList()
    }
}
