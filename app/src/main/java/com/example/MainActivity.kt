package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.CaloraViewModel
import com.example.ui.screens.SettingsDialog
import com.example.ui.screens.TodayScreen
import com.example.ui.screens.WeightScreen
import com.example.ui.screens.WorkoutsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleDarkBackground
import com.example.ui.theme.PurpleLightText
import com.example.ui.theme.PurpleMutedText
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurpleSurface
import com.example.ui.theme.PurpleSurfaceBorder
import com.example.ui.theme.PurpleSurfaceVariant
import com.example.ui.theme.ThemePreferences

enum class CaloraTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    TODAY("Tracker", Icons.Filled.PieChart, Icons.Outlined.PieChart),
    WEIGHT("Weight", Icons.Filled.Scale, Icons.Outlined.Scale),
    WORKOUTS("Workouts", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter)
}

class MainActivity : ComponentActivity() {

    private val viewModel: CaloraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        setContent {
            val themePrefs = remember { ThemePreferences(this) }
            var isDarkTheme by rememberSaveable { mutableStateOf(themePrefs.isDarkMode) }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                CaloraApp(
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = {
                        val nextMode = !isDarkTheme
                        isDarkTheme = nextMode
                        themePrefs.isDarkMode = nextMode
                    }
                )
            }
        }
    }
}

@Composable
fun CaloraApp(
    viewModel: CaloraViewModel,
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableStateOf(CaloraTab.TODAY) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }

    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val foodEntries by viewModel.todayFoodEntries.collectAsStateWithLifecycle()
    val allFoodEntries by viewModel.allFoodEntries.collectAsStateWithLifecycle()
    val fitnessLog by viewModel.todayFitnessLog.collectAsStateWithLifecycle()
    val allFitnessLogs by viewModel.allFitnessLogs.collectAsStateWithLifecycle()
    val allWeightEntries by viewModel.allWeightEntries.collectAsStateWithLifecycle()
    val allFoodItems by viewModel.allFoodItems.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(PurpleDarkBackground),
        topBar = {
            Surface(
                color = PurpleSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = PurpleSurfaceBorder,
                        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                    ),
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PurplePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Calora Studio",
                                color = PurpleLightText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Debloated Fitness Tracker",
                                color = PurpleMutedText,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Moon / Sun Theme Toggle Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PurpleSurfaceVariant)
                                .clickable(onClick = onToggleTheme)
                                .testTag("toggle_theme_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.WbSunny else Icons.Default.Bedtime,
                                contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                                tint = if (isDarkTheme) Color(0xFFFBBF24) else PurpleAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Settings Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PurpleSurfaceVariant)
                                .clickable { showSettingsDialog = true }
                                .testTag("open_settings_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Goals & Settings",
                                tint = PurpleAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = PurpleSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = PurpleSurfaceBorder,
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    ),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                NavigationBar(
                    containerColor = PurpleSurface,
                    modifier = Modifier.navigationBarsPadding(),
                    tonalElevation = 0.dp
                ) {
                    CaloraTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = PurpleAccent,
                                unselectedIconColor = PurpleMutedText,
                                unselectedTextColor = PurpleMutedText,
                                indicatorColor = PurplePrimary
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                CaloraTab.TODAY -> {
                    TodayScreen(
                        selectedDate = selectedDate,
                        userProfile = userProfile,
                        foodEntries = foodEntries,
                        fitnessLog = fitnessLog,
                        allFoodEntries = allFoodEntries,
                        allFitnessLogs = allFitnessLogs,
                        allWeightEntries = allWeightEntries,
                        foodDatabase = allFoodItems,
                        onSaveFoodItem = { item -> viewModel.saveCustomFood(item) },
                        onDeleteFoodItem = { id -> viewModel.deleteFoodItem(id) },
                        onPreviousDay = { viewModel.changeDay(-1) },
                        onNextDay = { viewModel.changeDay(1) },
                        onTodayClicked = { viewModel.setToday() },
                        onDateSelected = { dateStr -> viewModel.setDate(dateStr) },
                        onAddFood = { meal, name, cals, p, c, f, size, unit ->
                            viewModel.addFood(meal, name, cals, p, c, f, size, unit)
                        },
                        onDeleteFood = { id -> viewModel.deleteFood(id) },
                        onUpdateFood = { entry -> viewModel.updateFoodEntry(entry) },
                        onAddSteps = { delta -> viewModel.addSteps(delta) },
                        onSetSteps = { count -> viewModel.setSteps(count) },
                        onToggleWorkout = { viewModel.toggleWorkout() },
                        onSaveWorkout = { type, mins, notes, isDone ->
                            viewModel.saveWorkout(type, mins, notes, isDone)
                        }
                    )
                }

                CaloraTab.WEIGHT -> {
                    WeightScreen(
                        userProfile = userProfile,
                        weightEntries = allWeightEntries,
                        onAddWeight = { w, u, notes, date ->
                            viewModel.addWeight(w, u, notes, date)
                        },
                        onDeleteWeight = { id -> viewModel.deleteWeight(id) },
                        onUpdateStartWeight = { sw -> viewModel.updateStartingWeight(sw) }
                    )
                }

                CaloraTab.WORKOUTS -> {
                    WorkoutsScreen(
                        allFitnessLogs = allFitnessLogs,
                        onSaveWorkout = { date, type, mins, notes, isDone ->
                            viewModel.saveWorkout(type, mins, notes, isDone)
                        }
                    )
                }
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            currentProfile = userProfile,
            onDismiss = { showSettingsDialog = false },
            onSave = { cals, p, c, f, st, sw, tw, u ->
                viewModel.updateGoals(cals, p, c, f, st, sw, tw, u)
                showSettingsDialog = false
            },
            onExportJson = { viewModel.exportDataJson() },
            onInspectBackup = { json -> viewModel.inspectBackup(json) },
            onImportData = { json, replaceAll -> viewModel.importData(json, replaceAll) },
            onResetAllUserData = { viewModel.resetToFreshInstall() }
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
