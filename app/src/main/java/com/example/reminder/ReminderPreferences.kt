package com.example.reminder

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale

class ReminderPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("calora_bedtime_reminder_prefs", Context.MODE_PRIVATE)

    var isReminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_REMINDER_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_REMINDER_ENABLED, value).apply()

    var reminderHour: Int
        get() = prefs.getInt(KEY_REMINDER_HOUR, 21) // 9 PM default
        set(value) = prefs.edit().putInt(KEY_REMINDER_HOUR, value).apply()

    var reminderMinute: Int
        get() = prefs.getInt(KEY_REMINDER_MINUTE, 30) // 9:30 PM default
        set(value) = prefs.edit().putInt(KEY_REMINDER_MINUTE, value).apply()

    fun getFormattedTime(): String {
        val h = reminderHour
        val m = reminderMinute
        val amPm = if (h >= 12) "PM" else "AM"
        val displayHour = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return String.format(Locale.US, "%d:%02d %s", displayHour, m, amPm)
    }

    companion object {
        private const val KEY_REMINDER_ENABLED = "key_reminder_enabled"
        private const val KEY_REMINDER_HOUR = "key_reminder_hour"
        private const val KEY_REMINDER_MINUTE = "key_reminder_minute"
    }
}
