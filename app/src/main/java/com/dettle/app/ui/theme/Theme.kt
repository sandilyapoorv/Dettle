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
    primary = YrbLightPrimary,
    onPrimary = YrbLightOnPrimary,
    primaryContainer = YrbLightPrimaryContainer,
    onPrimaryContainer = YrbLightOnPrimaryContainer,
    secondary = YrbLightSecondary,
    onSecondary = YrbLightOnSecondary,
    background = YrbLightBackground,
    onBackground = YrbLightOnBackground,
    surface = YrbLightSurface,
    onSurface = YrbLightOnSurface,
    surfaceVariant = YrbLightSurfaceVariant,
    onSurfaceVariant = YrbLightOnSurfaceVariant,
    outline = YrbLightOutline,
    outlineVariant = YrbLightOutlineVariant,
    error = YrbLightError,
    onError = YrbLightOnError
)

private val DarkColors = darkColorScheme(
    primary = YrbDarkPrimary,
    onPrimary = YrbDarkOnPrimary,
    primaryContainer = YrbDarkPrimaryContainer,
    onPrimaryContainer = YrbDarkOnPrimaryContainer,
    secondary = YrbDarkSecondary,
    onSecondary = YrbDarkOnSecondary,
    background = YrbDarkBackground,
    onBackground = YrbDarkOnBackground,
    surface = YrbDarkSurface,
    onSurface = YrbDarkOnSurface,
    surfaceVariant = YrbDarkSurfaceVariant,
    onSurfaceVariant = YrbDarkOnSurfaceVariant,
    outline = YrbDarkOutline,
    outlineVariant = YrbDarkOutlineVariant,
    error = YrbDarkError,
    onError = YrbDarkOnError
)

val YrbShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

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
        shapes = YrbShapes,
        content = content
    )
}
