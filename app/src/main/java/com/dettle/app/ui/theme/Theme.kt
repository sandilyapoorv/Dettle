package com.dettle.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

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
    themeConfig: ThemeConfig? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val isEffectiveDark = when (themeConfig?.mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM, null -> darkTheme
    }

    val activeTheme = themeConfig?.theme ?: if (isEffectiveDark) AppTheme.OBSIDIAN else AppTheme.APPLE_LIGHT
    val customAccent = themeConfig?.customAccentHex?.let { Color(it) }
    val isPureOled = themeConfig?.isPureOled == true || activeTheme == AppTheme.OLED_BLACK

    val colors = createColorSchemeForTheme(
        theme = activeTheme,
        isDark = isEffectiveDark,
        isPureOled = isPureOled,
        customAccent = customAccent
    )

    val view = LocalView.current
    if (!view.isInEditMode && view.context is Activity) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isEffectiveDark
            insetsController.isAppearanceLightNavigationBars = !isEffectiveDark
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = DettleTypography,
        shapes = DettleShapes,
        content = content
    )
}
