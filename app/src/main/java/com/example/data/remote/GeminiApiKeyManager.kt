package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiApiKeyManager {
    private const val PREFS_NAME = "calora_gemini_api_key_prefs"
    private const val KEY_USER_API_KEY = "key_user_gemini_api_key"
    private const val KEY_AI_CAMERA_ENABLED = "key_ai_camera_enabled"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Returns user-entered API key from local preferences if present.
     */
    fun getUserApiKey(context: Context): String? {
        val key = getPrefs(context).getString(KEY_USER_API_KEY, null)?.trim()
        return if (key.isNullOrBlank()) null else key
    }

    /**
     * Saves user-entered Gemini API key locally on device.
     */
    fun saveUserApiKey(context: Context, key: String) {
        getPrefs(context).edit().putString(KEY_USER_API_KEY, key.trim()).apply()
    }

    /**
     * Clears user-entered Gemini API key.
     */
    fun clearUserApiKey(context: Context) {
        getPrefs(context).edit().remove(KEY_USER_API_KEY).apply()
    }

    /**
     * Returns true if user has manually attached their own API key.
     */
    fun isUserKeyAttached(context: Context): Boolean {
        return !getUserApiKey(context).isNullOrBlank()
    }

    /**
     * Returns the active API key to use:
     * Strictly user-provided key. Internal secret keys have been removed
     * so each user inputs their own free API key from Google AI Studio.
     */
    fun getActiveApiKey(context: Context): String? {
        return getUserApiKey(context)
    }

    /**
     * Whether a valid Gemini API key is configured and ready to use.
     */
    fun isApiKeyConfigured(context: Context): Boolean {
        return !getActiveApiKey(context).isNullOrBlank()
    }

    /**
     * Returns masked key for display in UI, e.g. "AIzaSy...x4K9".
     */
    fun getMaskedKey(context: Context): String {
        val key = getActiveApiKey(context) ?: return "None"
        return if (key.length > 10) {
            "${key.take(6)}...${key.takeLast(4)}"
        } else {
            "••••••••"
        }
    }

    /**
     * Whether the AI Camera tab is enabled in the food logger.
     */
    fun isAiCameraEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AI_CAMERA_ENABLED, true)
    }

    fun setAiCameraEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AI_CAMERA_ENABLED, enabled).apply()
    }

    /**
     * Validates API key by testing access to the Google Generative Language API.
     */
    suspend fun testApiKey(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter or paste your Gemini API key first"))
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$trimmedKey"
        val request = Request.Builder().url(url).get().build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                Result.success("API key verified! Google Gemini is connected and ready.")
            } else {
                val errorMsg = try {
                    val errJson = JSONObject(body)
                    val errObj = errJson.optJSONObject("error")
                    val message = errObj?.optString("message")
                    if (!message.isNullOrBlank()) {
                        message
                    } else if (response.code == 400 || response.code == 403) {
                        "Invalid API Key. Please verify the key from Google AI Studio."
                    } else {
                        "HTTP ${response.code}: $body"
                    }
                } catch (e: Exception) {
                    if (response.code == 400 || response.code == 403) {
                        "Invalid API Key. Please check the key and try again."
                    } else {
                        "Error (HTTP ${response.code})"
                    }
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Connection failed: ${e.message ?: "Please check internet connection"}"))
        }
    }
}

