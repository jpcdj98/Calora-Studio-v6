package com.example.ui.theme

import android.content.Context
import android.content.SharedPreferences

class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("calora_theme_prefs", Context.MODE_PRIVATE)

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_IS_DARK_MODE, true) // Default is dark mode (the current theme)
        set(value) = prefs.edit().putBoolean(KEY_IS_DARK_MODE, value).apply()

    companion object {
        private const val KEY_IS_DARK_MODE = "key_is_dark_mode"
    }
}
