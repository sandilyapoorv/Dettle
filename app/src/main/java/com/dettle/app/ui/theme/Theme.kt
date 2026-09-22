package com.dettle.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

private val DettleColorScheme = darkColorScheme(
    primary = DettleCyan,
    onPrimary = DettleDark,
    primaryContainer = DettleCyanDim,
    onPrimaryContainer = DettleTextPrimary,
    secondary = DettlePurple,
    onSecondary = DettleTextPrimary,
    tertiary = DettleGreen,
    onTertiary = DettleDark,
    background = DettleDark,
    onBackground = DettleTextPrimary,
    surface = DettleSurface,
    onSurface = DettleTextPrimary,
    surfaceVariant = DettleSurfaceVariant,
    onSurfaceVariant = DettleTextSecondary,
    error = DettleRed,
    onError = DettleTextPrimary,
    outline = DettleCardBorder,
    outlineVariant = DettleTextMuted,
)

@Composable
fun DettleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DettleColorScheme,
        typography = DettleTypography,
        content = content
    )
}
