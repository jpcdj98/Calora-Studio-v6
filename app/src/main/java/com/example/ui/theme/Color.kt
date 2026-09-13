package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class CaloraColors(
    val primary: Color,
    val primaryVariant: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceBorder: Color,
    val cardHover: Color,
    val accent: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val textSubtle: Color,
    val macroProtein: Color,
    val macroCarbs: Color,
    val macroFat: Color,
    val metricSteps: Color,
    val metricWorkout: Color,
    val metricWeight: Color,
    val isDark: Boolean
)

// The current theme as Dark Mode
val DarkCaloraColors = CaloraColors(
    primary = Color(0xFFA855F7),
    primaryVariant = Color(0xFF9333EA),
    background = Color(0xFF0C0913),
    surface = Color(0xFF161220),
    surfaceVariant = Color(0xFF221B32),
    surfaceBorder = Color(0xFF33294B),
    cardHover = Color(0xFF2B2240),
    accent = Color(0xFFC084FC),
    textPrimary = Color(0xFFF3EEFA),
    textMuted = Color(0xFF9E92B5),
    textSubtle = Color(0xFF6B5F84),
    macroProtein = Color(0xFFA855F7),
    macroCarbs = Color(0xFF38BDF8),
    macroFat = Color(0xFFFB7185),
    metricSteps = Color(0xFF34D399),
    metricWorkout = Color(0xFFF59E0B),
    metricWeight = Color(0xFF818CF8),
    isDark = true
)

// Light Mode theme: Clean, crisp, high-contrast purple & lilac palette
val LightCaloraColors = CaloraColors(
    primary = Color(0xFF8B2FC9),
    primaryVariant = Color(0xFF7E22CE),
    background = Color(0xFFF7F5FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEFEAF6),
    surfaceBorder = Color(0xFFE2DBEC),
    cardHover = Color(0xFFE6DFF2),
    accent = Color(0xFF7E22CE),
    textPrimary = Color(0xFF191226),
    textMuted = Color(0xFF655B77),
    textSubtle = Color(0xFF9288A5),
    macroProtein = Color(0xFF8B2FC9),
    macroCarbs = Color(0xFF0284C7),
    macroFat = Color(0xFFE11D48),
    metricSteps = Color(0xFF059669),
    metricWorkout = Color(0xFFD97706),
    metricWeight = Color(0xFF4F46E5),
    isDark = false
)

val LocalCaloraColors = staticCompositionLocalOf { DarkCaloraColors }

// Dynamic theme-aware accessors matching existing code
val PurplePrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCaloraColors.current.primary

val PurplePrimaryVariant: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCaloraColors.current.primaryVariant

val PurpleDarkBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCaloraColors.current.background

val PurpleSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCaloraColors.current.surface

val PurpleSurfaceVariant: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCaloraColors.current.surfaceVariant

val PurpleSurfaceBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCaloraColors.current.surfaceBorder

val PurpleCardHover: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCaloraColors.current.cardHover

val PurpleAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCaloraColors.current.accent

val PurpleLightText: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCaloraColors.current.textPrimary

val PurpleMutedText: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCaloraColors.current.textMuted

val PurpleSubtleText: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCaloraColors.current.textSubtle

// Macro & Metric semantic colors
val MacroProteinColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCaloraColors.current.macroProtein

val MacroCarbsColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCaloraColors.current.macroCarbs

val MacroFatColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCaloraColors.current.macroFat

val MetricStepsColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCaloraColors.current.metricSteps

val MetricWorkoutColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCaloraColors.current.metricWorkout

val MetricWeightColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCaloraColors.current.metricWeight

val isAppInDarkTheme: Boolean
    @Composable
    @ReadOnlyComposable
    get() = LocalCaloraColors.current.isDark

// Material defaults
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

