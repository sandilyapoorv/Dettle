package com.dettle.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = DettleLightPrimary,
    onPrimary = DettleLightOnPrimary,
    primaryContainer = DettleLightPrimaryContainer,
    onPrimaryContainer = DettleLightOnPrimaryContainer,
    secondary = DettleLightSecondary,
    onSecondary = DettleLightOnSecondary,
    secondaryContainer = DettleLightSecondaryContainer,
    onSecondaryContainer = DettleLightOnSecondaryContainer,
    tertiary = DettleLightTertiary,
    onTertiary = DettleLightOnTertiary,
    background = DettleLightBackground,
    onBackground = DettleLightOnBackground,
    surface = DettleLightSurface,
    onSurface = DettleLightOnSurface,
    surfaceVariant = DettleLightSurfaceVariant,
    onSurfaceVariant = DettleLightOnSurfaceVariant,
    outline = DettleLightOutline,
    outlineVariant = DettleLightOutlineVariant,
    error = DettleLightError,
    onError = DettleLightOnError,
    errorContainer = DettleLightErrorContainer,
    onErrorContainer = DettleLightOnErrorContainer
)

private val DarkColors = darkColorScheme(
    primary = DettleDarkPrimary,
    onPrimary = DettleDarkOnPrimary,
    primaryContainer = DettleDarkPrimaryContainer,
    onPrimaryContainer = DettleDarkOnPrimaryContainer,
    secondary = DettleDarkSecondary,
    onSecondary = DettleDarkOnSecondary,
    secondaryContainer = DettleDarkSecondaryContainer,
    onSecondaryContainer = DettleDarkOnSecondaryContainer,
    tertiary = DettleDarkTertiary,
    onTertiary = DettleDarkOnTertiary,
    background = DettleDarkBackground,
    onBackground = DettleDarkOnBackground,
    surface = DettleDarkSurface,
    onSurface = DettleDarkOnSurface,
    surfaceVariant = DettleDarkSurfaceVariant,
    onSurfaceVariant = DettleDarkOnSurfaceVariant,
    outline = DettleDarkOutline,
    outlineVariant = DettleDarkOutlineVariant,
    error = DettleDarkError,
    onError = DettleDarkOnError,
    errorContainer = DettleDarkErrorContainer,
    onErrorContainer = DettleDarkOnErrorContainer
)

val DettleShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

// Backward-compatible alias
val YrbShapes = DettleShapes

@Composable
fun DettleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current

    if (!view.isInEditMode && view.context is Activity) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = DettleTypography,
        shapes = DettleShapes,
        content = content
    )
}
