package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.reminder.BedtimeReminderScheduler
import com.example.reminder.ReminderPreferences
import com.example.ui.theme.MetricStepsColor
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleLightText
import com.example.ui.theme.PurpleMutedText
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurpleSurface
import com.example.ui.theme.PurpleSurfaceBorder
import com.example.ui.theme.PurpleSurfaceVariant

data class TimePreset(val label: String, val hour: Int, val minute: Int)

@Composable
fun BedtimeReminderSection(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { ReminderPreferences(context) }

    var isEnabled by remember { mutableStateOf(prefs.isReminderEnabled) }
    var selectedHour by remember { mutableIntStateOf(prefs.reminderHour) }
    var selectedMinute by remember { mutableIntStateOf(prefs.reminderMinute) }

    var testNotificationSent by remember { mutableStateOf(false) }

    val presets = remember {
        listOf(
            TimePreset("8:30 PM", 20, 30),
            TimePreset("9:00 PM", 21, 0),
            TimePreset("9:30 PM", 21, 30),
            TimePreset("10:00 PM", 22, 0),
            TimePreset("10:30 PM", 22, 30)
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isEnabled = true
            prefs.isReminderEnabled = true
            BedtimeReminderScheduler.scheduleNextReminder(context)
            Toast.makeText(context, "Bedtime reminder set for ${prefs.getFormattedTime()}", Toast.LENGTH_SHORT).show()
        } else {
            isEnabled = false
            prefs.isReminderEnabled = false
            Toast.makeText(context, "Notification permission is needed for reminders", Toast.LENGTH_LONG).show()
        }
    }

    fun handleToggle(newChecked: Boolean) {
        if (newChecked) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasPermission) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    return
                }
            }
            isEnabled = true
            prefs.isReminderEnabled = true
            BedtimeReminderScheduler.scheduleNextReminder(context)
            Toast.makeText(context, "Bedtime reminder active at ${prefs.getFormattedTime()}", Toast.LENGTH_SHORT).show()
        } else {
            isEnabled = false
            prefs.isReminderEnabled = false
            BedtimeReminderScheduler.cancelReminder(context)
            Toast.makeText(context, "Bedtime reminder turned off", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "BEDTIME LOG REMINDER",
            color = PurpleAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = "Optional gentle evening nudge to log dinner & daily steps before bed",
            color = PurpleMutedText,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Main Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PurpleSurfaceVariant)
                .border(
                    1.dp,
                    if (isEnabled) PurpleAccent.copy(alpha = 0.5f) else PurpleSurfaceBorder,
                    RoundedCornerShape(12.dp)
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isEnabled) PurplePrimary.copy(alpha = 0.35f)
                                else PurpleSurface
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = "Bedtime Reminder",
                            tint = if (isEnabled) PurpleAccent else PurpleMutedText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Evening Log Reminder",
                            color = PurpleLightText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEnabled) "Active at ${prefs.getFormattedTime()} daily" else "Off (Optional)",
                            color = if (isEnabled) MetricStepsColor else PurpleMutedText,
                            fontSize = 11.sp,
                            fontWeight = if (isEnabled) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = { handleToggle(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PurplePrimary,
                        uncheckedThumbColor = PurpleMutedText,
                        uncheckedTrackColor = PurpleSurface
                    ),
                    modifier = Modifier.testTag("bedtime_reminder_switch")
                )
            }

            AnimatedVisibility(
                visible = isEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(PurpleSurfaceBorder)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "CHOOSE REMINDER TIME",
                        color = PurpleAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Preset Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { preset ->
                            val isSelected = selectedHour == preset.hour && selectedMinute == preset.minute
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) PurplePrimary else PurpleSurface)
                                    .border(
                                        1.dp,
                                        if (isSelected) PurpleAccent else PurpleSurfaceBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedHour = preset.hour
                                        selectedMinute = preset.minute
                                        prefs.reminderHour = preset.hour
                                        prefs.reminderMinute = preset.minute
                                        BedtimeReminderScheduler.scheduleNextReminder(context)
                                        Toast.makeText(
                                            context,
                                            "Updated reminder to ${preset.label}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else PurpleMutedText,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = preset.label,
                                        color = if (isSelected) Color.White else PurpleLightText,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Test Notification & Reassurance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🌙 Non-nagging guarantee",
                                color = PurpleLightText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Notifies only once per day before sleep. Tap to log dinner & steps.",
                                color = PurpleMutedText,
                                fontSize = 10.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!hasPermission) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        return@OutlinedButton
                                    }
                                }
                                BedtimeReminderScheduler.sendTestNotification(context)
                                testNotificationSent = true
                                Toast.makeText(context, "Test reminder sent! Check your notification bar.", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PurpleAccent
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("send_test_notification_btn")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Preview", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
