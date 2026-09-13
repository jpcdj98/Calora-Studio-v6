package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val prefs = ReminderPreferences(context)
            if (prefs.isReminderEnabled) {
                BedtimeReminderScheduler.scheduleNextReminder(context)
            }
        }
    }
}
