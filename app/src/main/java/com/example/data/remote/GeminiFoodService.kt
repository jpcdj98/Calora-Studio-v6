package com.example.data.remote

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class FoodAnalysisResult(
    val foodName: String,
    val servingSize: Float = 1f,
    val servingUnit: String = "serving",
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val notes: String,
    val isDemo: Boolean = false
)

object GeminiFoodService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val CANDIDATE_MODELS = listOf("gemini-3.5-flash", "gemini-3.1-flash-lite-preview", "gemini-flash-latest")
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    fun isApiKeyConfigured(context: android.content.Context? = null): Boolean {
        return context?.let { GeminiApiKeyManager.isApiKeyConfigured(it) } ?: false
    }

    suspend fun analyzeFoodImage(bitmap: Bitmap, apiKey: String? = null): Result<FoodAnalysisResult> =
        analyzeFoodImages(listOf(bitmap), "", apiKey = apiKey)

    suspend fun analyzeFoodImages(
        bitmaps: List<Bitmap>,
        userDescription: String = "",
        apiKey: String? = null,
        context: android.content.Context? = null
    ): Result<FoodAnalysisResult> = withContext(Dispatchers.IO) {
        val effectiveApiKey = apiKey
            ?: (context?.let { GeminiApiKeyManager.getActiveApiKey(it) })

        if (effectiveApiKey.isNullOrBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Google Gemini API key is not configured. Please enter your free Gemini API key in Settings > Gemini AI Vision.")
            )
        }

        if (bitmaps.isEmpty() && userDescription.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Please add at least one photo or a description of your meal.")
            )
        }

        try {
            val promptBuilder = StringBuilder()
            promptBuilder.append(
                """
                You are an expert nutritionist and food vision AI.
                Analyze the provided photo(s) of this meal/food.
                """.trimIndent()
            )

            if (userDescription.isNotBlank()) {
                promptBuilder.append("\n\nUser Context / Description of the meal: \"${userDescription.trim()}\"")
                promptBuilder.append("\nUse this user description to understand specific ingredients, dressings, portion sizes, or cooking methods.")
            }

            promptBuilder.append(
                """

                Instructions:
                1. Identify all food items and dishes present across the photo(s) and description.
                2. Estimate the realistic portion amount (servingSize, number e.g. 200, 1.5, 1) and unit of measurement (servingUnit, e.g. 'g', 'cup', 'oz', 'serving', 'bowl', 'slice', 'piece', 'tbsp', 'ml').
                3. Estimate the total combined calories (kcal) for the entire meal shown/described.
                4. Estimate the total combined macronutrients: protein (grams), carbohydrates (grams), fat (grams).
                5. Provide a brief 1-sentence note detailing the breakdown of ingredients and portion sizes.

                Return ONLY valid JSON in this exact structure without markdown backticks:
                {
                  "foodName": "Steak & Sweet Potato with Garden Salad",
                  "servingSize": 250,
                  "servingUnit": "g",
                  "calories": 620,
                  "protein": 46.0,
                  "carbs": 52.0,
                  "fat": 18.0,
                  "notes": "Estimated 200g grilled steak, 1 medium sweet potato, and green salad with light vinaigrette"
                }
                """.trimIndent()
            )

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            // Text prompt with context
                            put(JSONObject().put("text", promptBuilder.toString()))

                            // Add each image as inlineData
                            for (bitmap in bitmaps) {
                                val base64Image = bitmapToBase64(bitmap)
                                val inlineData = JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                }
                                put(JSONObject().put("inlineData", inlineData))
                            }
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)

                val generationConfig = JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                }
                put("generationConfig", generationConfig)
            }

            val requestBodyString = requestJson.toString()
            var lastError = "Unable to connect to Gemini API"

            for (model in CANDIDATE_MODELS) {
                val requestBody = requestBodyString.toRequestBody("application/json; charset=utf-8".toMediaType())
                val url = "$BASE_URL/$model:generateContent?key=$effectiveApiKey"

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val response = try {
                    client.newCall(request).execute()
                } catch (e: Exception) {
                    lastError = e.message ?: "Network error"
                    continue
                }

                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val errMsg = try {
                        val errJson = JSONObject(responseBody)
                        errJson.getJSONObject("error").getString("message")
                    } catch (e: Exception) {
                        "HTTP ${response.code}: $responseBody"
                    }
                    lastError = errMsg
                    if (response.code == 404 || errMsg.contains("not found", ignoreCase = true) || errMsg.contains("no longer available", ignoreCase = true) || errMsg.contains("deprecated", ignoreCase = true)) {
                        continue
                    } else {
                        return@withContext Result.failure(Exception("Gemini API error: $errMsg"))
                    }
                }

                val root = JSONObject(responseBody)
                val candidates = root.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text") ?: ""

                if (text.isNotBlank()) {
                    val cleanedJson = text.trim()
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()

                    val foodObj = JSONObject(cleanedJson)
                    val foodName = foodObj.optString("foodName", if (userDescription.isNotBlank()) userDescription.take(30) else "Estimated Meal")
                    val servingSize = foodObj.optDouble("servingSize", 1.0).toFloat().coerceAtLeast(0.1f)
                    val servingUnit = foodObj.optString("servingUnit", "serving").ifBlank { "serving" }
                    val calories = foodObj.optInt("calories", 450)
                    val protein = foodObj.optDouble("protein", 28.0).toFloat()
                    val carbs = foodObj.optDouble("carbs", 45.0).toFloat()
                    val fat = foodObj.optDouble("fat", 14.0).toFloat()
                    val notes = foodObj.optString("notes", "AI estimated nutrition breakdown")

                    return@withContext Result.success(
                        FoodAnalysisResult(
                            foodName = foodName,
                            servingSize = servingSize,
                            servingUnit = servingUnit,
                            calories = calories,
                            protein = protein,
                            carbs = carbs,
                            fat = fat,
                            notes = notes
                        )
                    )
                }
            }

            Result.failure(Exception("Gemini error: $lastError"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val maxDimension = 1024
        val scaled = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val newWidth = if (ratio > 1) maxDimension else (maxDimension * ratio).toInt()
            val newHeight = if (ratio > 1) (maxDimension / ratio).toInt() else maxDimension
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * Realistic sample estimates for quick testing or fallback.
     */
    fun getDemoAnalysis(dishType: String = "Salmon Bowl", userDescription: String = ""): FoodAnalysisResult {
        if (userDescription.isNotBlank()) {
            val lower = userDescription.lowercase()
            return when {
                lower.contains("steak") || lower.contains("beef") -> FoodAnalysisResult(
                    foodName = userDescription.take(35).replaceFirstChar { it.uppercase() },
                    servingSize = 250f,
                    servingUnit = "g",
                    calories = 640,
                    protein = 54f,
                    carbs = 38f,
                    fat = 26f,
                    notes = "Estimated based on context: ~$userDescription",
                    isDemo = true
                )
                lower.contains("chicken") || lower.contains("rice") -> FoodAnalysisResult(
                    foodName = userDescription.take(35).replaceFirstChar { it.uppercase() },
                    servingSize = 200f,
                    servingUnit = "g",
                    calories = 520,
                    protein = 46f,
                    carbs = 55f,
                    fat = 10f,
                    notes = "Estimated based on context: ~$userDescription",
                    isDemo = true
                )
                lower.contains("salad") -> FoodAnalysisResult(
                    foodName = userDescription.take(35).replaceFirstChar { it.uppercase() },
                    servingSize = 1f,
                    servingUnit = "bowl",
                    calories = 360,
                    protein = 28f,
                    carbs = 18f,
                    fat = 19f,
                    notes = "Estimated based on context: ~$userDescription",
                    isDemo = true
                )
                lower.contains("pizza") || lower.contains("burger") -> FoodAnalysisResult(
                    foodName = userDescription.take(35).replaceFirstChar { it.uppercase() },
                    servingSize = 1f,
                    servingUnit = "serving",
                    calories = 720,
                    protein = 32f,
                    carbs = 76f,
                    fat = 32f,
                    notes = "Estimated based on context: ~$userDescription",
                    isDemo = true
                )
                else -> FoodAnalysisResult(
                    foodName = userDescription.take(35).replaceFirstChar { it.uppercase() },
                    servingSize = 1f,
                    servingUnit = "serving",
                    calories = 490,
                    protein = 35f,
                    carbs = 48f,
                    fat = 16f,
                    notes = "Estimated based on context: ~$userDescription",
                    isDemo = true
                )
            }
        }

        return when (dishType.lowercase()) {
            "steak" -> FoodAnalysisResult(
                foodName = "Grilled Sirloin & Roasted Potatoes",
                servingSize = 250f,
                servingUnit = "g",
                calories = 620,
                protein = 52f,
                carbs = 35f,
                fat = 28f,
                notes = "Estimated ~250g sirloin steak with roasted potatoes and herb butter",
                isDemo = true
            )
            "salad" -> FoodAnalysisResult(
                foodName = "Chicken Caesar Salad",
                servingSize = 1f,
                servingUnit = "bowl",
                calories = 390,
                protein = 36f,
                carbs = 14f,
                fat = 22f,
                notes = "Estimated 1 bowl: 150g grilled chicken, romaine, shaved parmesan, light caesar dressing",
                isDemo = true
            )
            "pasta" -> FoodAnalysisResult(
                foodName = "Pasta Bolognese",
                servingSize = 1.5f,
                servingUnit = "cup",
                calories = 580,
                protein = 32f,
                carbs = 68f,
                fat = 19f,
                notes = "Estimated 1.5 cups pasta with lean beef bolognese and grated parmesan",
                isDemo = true
            )
            else -> FoodAnalysisResult(
                foodName = "Grilled Salmon with Quinoa & Greens",
                servingSize = 200f,
                servingUnit = "g",
                calories = 510,
                protein = 42f,
                carbs = 36f,
                fat = 21f,
                notes = "Estimated ~200g pan-seared salmon, 1 cup cooked quinoa, and asparagus",
                isDemo = true
            )
        }
    }
}
