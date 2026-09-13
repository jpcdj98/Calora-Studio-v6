package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkPurpleColorScheme = darkColorScheme(
    primary = DarkCaloraColors.primary,
    onPrimary = Color.White,
    primaryContainer = DarkCaloraColors.surfaceVariant,
    onPrimaryContainer = DarkCaloraColors.accent,
    secondary = DarkCaloraColors.accent,
    onSecondary = Color.Black,
    secondaryContainer = DarkCaloraColors.cardHover,
    onSecondaryContainer = DarkCaloraColors.textPrimary,
    tertiary = DarkCaloraColors.metricSteps,
    onTertiary = Color.Black,
    background = DarkCaloraColors.background,
    onBackground = DarkCaloraColors.textPrimary,
    surface = DarkCaloraColors.surface,
    onSurface = DarkCaloraColors.textPrimary,
    surfaceVariant = DarkCaloraColors.surfaceVariant,
    onSurfaceVariant = DarkCaloraColors.textMuted,
    outline = DarkCaloraColors.surfaceBorder,
    outlineVariant = DarkCaloraColors.textSubtle
)

private val LightPurpleColorScheme = lightColorScheme(
    primary = LightCaloraColors.primary,
    onPrimary = Color.White,
    primaryContainer = LightCaloraColors.surfaceVariant,
    onPrimaryContainer = LightCaloraColors.accent,
    secondary = LightCaloraColors.accent,
    onSecondary = Color.White,
    secondaryContainer = LightCaloraColors.cardHover,
    onSecondaryContainer = LightCaloraColors.textPrimary,
    tertiary = LightCaloraColors.metricSteps,
    onTertiary = Color.White,
    background = LightCaloraColors.background,
    onBackground = LightCaloraColors.textPrimary,
    surface = LightCaloraColors.surface,
    onSurface = LightCaloraColors.textPrimary,
    surfaceVariant = LightCaloraColors.surfaceVariant,
    onSurfaceVariant = LightCaloraColors.textMuted,
    outline = LightCaloraColors.surfaceBorder,
    outlineVariant = LightCaloraColors.textSubtle
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to dark clean purple aesthetic
    dynamicColor: Boolean = false, // Keep intentional purple brand identity
    content: @Composable () -> Unit
) {
    val caloraColors = if (darkTheme) DarkCaloraColors else LightCaloraColors
    val colorScheme = if (darkTheme) DarkPurpleColorScheme else LightPurpleColorScheme

    CompositionLocalProvider(LocalCaloraColors provides caloraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

