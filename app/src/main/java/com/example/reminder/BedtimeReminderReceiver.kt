package com.example.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity

class BedtimeReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = ReminderPreferences(context)
        if (!prefs.isReminderEnabled) {
            return
        }

        showBedtimeNotification(context, isTest = false)

        // Reschedule for tomorrow evening
        BedtimeReminderScheduler.scheduleNextReminder(context)
    }

    companion object {
        const val ACTION_BEDTIME_REMINDER = "com.example.action.BEDTIME_REMINDER"
        const val CHANNEL_ID = "calora_bedtime_reminders_v2"
        private const val OLD_CHANNEL_ID = "calora_bedtime_reminders"
        private const val NOTIFICATION_ID = 4018

        val VIBRATION_PATTERN = longArrayOf(0, 350, 150, 350)

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                
                // Clear obsolete default channel to upgrade user settings
                try {
                    manager?.deleteNotificationChannel(OLD_CHANNEL_ID)
                } catch (_: Exception) {}

                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                    .build()

                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Bedtime Log Reminder",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Bedtime heads-up reminder with sound & vibration to log meals and steps."
                    setShowBadge(true)
                    enableVibration(true)
                    vibrationPattern = VIBRATION_PATTERN
                    setSound(soundUri, audioAttributes)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    enableLights(true)
                    lightColor = Color.MAGENTA
                }
                manager?.createNotificationChannel(channel)
            }
        }

        /**
         * Triggers device vibration directly to ensure immediate haptic feedback
         */
        fun triggerHapticFeedback(context: Context) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator?.vibrate(
                        VibrationEffect.createWaveform(VIBRATION_PATTERN, -1)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(VIBRATION_PATTERN, -1)
                    }
                }
            } catch (_: Exception) {}
        }

        /**
         * Plays the notification chime audio
         */
        fun playAudibleChime(context: Context) {
            try {
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context.applicationContext, soundUri)
                ringtone?.play()
            } catch (_: Exception) {}
        }

        fun showBedtimeNotification(context: Context, isTest: Boolean = false) {
            createNotificationChannel(context)

            // Verify notification permission on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasPermission) {
                    return
                }
            }

            // Immediately trigger physical haptic feedback & chime
            triggerHapticFeedback(context)
            playAudibleChime(context)

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "today")
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                flags
            )

            val title = if (isTest) "🌙 Bedtime Reminder (Preview)" else "🌙 Time to wrap up today's log?"
            val body = if (isTest) {
                "Take 30 seconds to log dinner and check your step count before turning in."
            } else {
                "Take 30 seconds to log dinner and check your step count before turning in."
            }

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setSound(soundUri)
                .setVibrate(VIBRATION_PATTERN)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            } catch (e: SecurityException) {
                // Permission revoked
            }
        }
    }
}
