package com.dettle.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// =============================================================================
// Apple Liquid Glass Design System Palette
// Clean, luminous, translucent materials with system-grade precision
// =============================================================================

// Apple System Accent Colors
val AppleBlue = Color(0xFF0A84FF)
val AppleBlueLight = Color(0xFF007AFF)
val AppleIndigo = Color(0xFF5E5CE6)
val ApplePurple = Color(0xFFBF5AF2)
val ApplePink = Color(0xFFFF375F)
val AppleRed = Color(0xFFFF453A)
val AppleOrange = Color(0xFFFF9F0A)
val AppleYellow = Color(0xFFFFD60A)
val AppleGreen = Color(0xFF30D158)
val AppleMint = Color(0xFF63E6E2)
val AppleTeal = Color(0xFF40C8E0)
val AppleCyan = Color(0xFF64D2FF)

// Semantic Accent Aliases for Backward Compatibility
val DettleGreen = AppleGreen
val DettleGreenDim = Color(0xFF0A3D1B)
val DettleOrange = AppleOrange
val DettleRed = AppleRed
val DettlePurple = ApplePurple
val DettleBlue = AppleBlue
val DettleCyan = AppleCyan
val DettleCyanDim = Color(0xFF0A3044)

// Dark Theme: Deep Space Black & Translucent Glass Surfaces
val AppleDarkBackground = Color(0xFF000000)
val AppleDarkOnBackground = Color(0xFFFFFFFF)
val AppleDarkSurface = Color(0xFF1C1C1E) // System Gray 6
val AppleDarkOnSurface = Color(0xFFFFFFFF)
val AppleDarkSurfaceVariant = Color(0xFF2C2C2E) // System Gray 5
val AppleDarkOnSurfaceVariant = Color(0xFF8E8E93) // System Gray 2

val AppleDarkOutline = Color(0xFF38383A)
val AppleDarkOutlineVariant = Color(0xFF48484A)
val AppleDarkError = AppleRed

// Light Theme: Frosted Silver & Crisp Snow
val AppleLightBackground = Color(0xFFF2F2F7) // System Gray 6 Light
val AppleLightOnBackground = Color(0xFF000000)
val AppleLightSurface = Color(0xFFFFFFFF)
val AppleLightOnSurface = Color(0xFF000000)
val AppleLightSurfaceVariant = Color(0xFFE5E5EA) // System Gray 5 Light
val AppleLightOnSurfaceVariant = Color(0xFF8E8E93)

val AppleLightOutline = Color(0xFFC6C6C8)
val AppleLightOutlineVariant = Color(0xFFD1D1D6)
val AppleLightError = Color(0xFFFF3B30)

// Backward-compatible semantic bridges
val DettleDarkBackground = AppleDarkBackground
val DettleDarkOnBackground = AppleDarkOnBackground
val DettleDarkSurface = AppleDarkSurface
val DettleDarkOnSurface = AppleDarkOnSurface
val DettleDarkSurfaceVariant = AppleDarkSurfaceVariant
val DettleDarkOnSurfaceVariant = AppleDarkOnSurfaceVariant

val DettleDarkPrimary = AppleBlue
val DettleDarkOnPrimary = Color.White
val DettleDarkPrimaryContainer = Color(0xFF1E2640)
val DettleDarkOnPrimaryContainer = AppleBlue
val DettleDarkSecondary = AppleIndigo
val DettleDarkOnSecondary = Color.White
val DettleDarkSecondaryContainer = Color(0xFF262244)
val DettleDarkOnSecondaryContainer = AppleIndigo
val DettleDarkTertiary = AppleOrange
val DettleDarkOnTertiary = Color.Black
val DettleDarkOutline = AppleDarkOutline
val DettleDarkOutlineVariant = AppleDarkOutlineVariant
val DettleDarkError = AppleDarkError
val DettleDarkOnError = Color.White
val DettleDarkErrorContainer = Color(0xFF3D1618)
val DettleDarkOnErrorContainer = AppleRed

val DettleLightBackground = AppleLightBackground
val DettleLightOnBackground = AppleLightOnBackground
val DettleLightSurface = AppleLightSurface
val DettleLightOnSurface = AppleLightOnSurface
val DettleLightSurfaceVariant = AppleLightSurfaceVariant
val DettleLightOnSurfaceVariant = AppleLightOnSurfaceVariant

val DettleLightPrimary = AppleBlueLight
val DettleLightOnPrimary = Color.White
val DettleLightPrimaryContainer = Color(0xFFE5F1FF)
val DettleLightOnPrimaryContainer = AppleBlueLight
val DettleLightSecondary = AppleIndigo
val DettleLightOnSecondary = Color.White
val DettleLightSecondaryContainer = Color(0xFFECEBFC)
val DettleLightOnSecondaryContainer = AppleIndigo
val DettleLightTertiary = AppleOrange
val DettleLightOnTertiary = Color.White
val DettleLightOutline = AppleLightOutline
val DettleLightOutlineVariant = AppleLightOutlineVariant
val DettleLightError = AppleLightError
val DettleLightOnError = Color.White
val DettleLightErrorContainer = Color(0xFFFFE5E5)
val DettleLightOnErrorContainer = AppleLightError

// Additional UI bridges
val DettleDark = AppleDarkBackground
val DettleSurface = AppleDarkSurface
val DettleSurfaceVariant = AppleDarkSurfaceVariant
val DettleCard = AppleDarkSurface
val DettleCardBorder = AppleDarkOutlineVariant
val DettleTextPrimary = AppleDarkOnBackground
val DettleTextSecondary = AppleDarkOnSurfaceVariant
val DettleTextMuted = Color(0xFF636366)

// Chat message bubbles
val UserBubble = AppleBlue
val UserBubbleBorder = AppleBlue
val AiBubble = AppleDarkSurfaceVariant
val AiBubbleBorder = AppleDarkOutline
val ToolCallBg = Color(0xFF242426)
val ToolCallBorder = AppleDarkOutlineVariant
val ApprovalBg = Color(0xFF1C2834)
val ApprovalBorder = AppleBlue.copy(alpha = 0.4f)

// Theme Color Scheme Generator
fun createColorSchemeForTheme(
    theme: AppTheme,
    isDark: Boolean,
    isPureOled: Boolean,
    customAccent: Color?
): androidx.compose.material3.ColorScheme {
    if (isDark) {
        val primary = customAccent ?: when (theme) {
            AppTheme.CYBER_INDIGO -> AppleIndigo
            AppTheme.EMERALD_MATRIX -> AppleGreen
            AppTheme.SUNSET_AMBER -> AppleOrange
            AppTheme.TOKYO_NEON -> ApplePink
            AppTheme.TITANIUM -> Color(0xFF8E8E93)
            else -> AppleBlue
        }
        return darkColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primary.copy(alpha = 0.22f),
            onPrimaryContainer = primary,
            secondary = AppleIndigo,
            onSecondary = Color.White,
            secondaryContainer = AppleIndigo.copy(alpha = 0.2f),
            onSecondaryContainer = AppleIndigo,
            tertiary = AppleOrange,
            onTertiary = Color.Black,
            background = if (isPureOled || theme == AppTheme.OLED_BLACK) Color.Black else AppleDarkBackground,
            onBackground = AppleDarkOnBackground,
            surface = AppleDarkSurface,
            onSurface = AppleDarkOnSurface,
            surfaceVariant = AppleDarkSurfaceVariant,
            onSurfaceVariant = AppleDarkOnSurfaceVariant,
            outline = AppleDarkOutline,
            outlineVariant = AppleDarkOutlineVariant,
            error = AppleDarkError,
            onError = Color.White,
            errorContainer = DettleDarkErrorContainer,
            onErrorContainer = DettleDarkOnErrorContainer
        )
    }

    val primary = customAccent ?: AppleBlueLight
    return lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primary.copy(alpha = 0.15f),
        onPrimaryContainer = primary,
        secondary = AppleIndigo,
        onSecondary = Color.White,
        secondaryContainer = AppleIndigo.copy(alpha = 0.15f),
        onSecondaryContainer = AppleIndigo,
        tertiary = AppleOrange,
        onTertiary = Color.White,
        background = AppleLightBackground,
        onBackground = AppleLightOnBackground,
        surface = AppleLightSurface,
        onSurface = AppleLightOnSurface,
        surfaceVariant = AppleLightSurfaceVariant,
        onSurfaceVariant = AppleLightOnSurfaceVariant,
        outline = AppleLightOutline,
        outlineVariant = AppleLightOutlineVariant,
        error = AppleLightError,
        onError = Color.White,
        errorContainer = DettleLightErrorContainer,
        onErrorContainer = DettleLightOnErrorContainer
    )
}
