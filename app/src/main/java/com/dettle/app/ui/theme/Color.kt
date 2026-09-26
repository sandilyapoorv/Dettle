package com.dettle.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// =============================================================================
// Linear / Raycast / Apple Inspired Color Palettes
// =============================================================================

// Pure Pitch Black & Mascot Periwinkle Dark Theme Palette
val DettleDarkBackground = Color(0xFF000000)
val DettleDarkOnBackground = Color(0xFFF6F3EE)
val DettleDarkSurface = Color(0xFF08090E)
val DettleDarkOnSurface = Color(0xFFF6F3EE)
val DettleDarkSurfaceVariant = Color(0xFF131522)
val DettleDarkOnSurfaceVariant = Color(0xFFA3A8BF)

// Periwinkle Lavender (from the mascot's floral pajamas)
val DettleLavenderPrimary = Color(0xFFA4B0E8)
val DettleLavenderOnPrimary = Color(0xFF14172B)
val DettleLavenderContainer = Color(0xFF1E2338) // Midnight indigo from the cozy sofa
val DettleLavenderOnContainer = Color(0xFFE5E9FF)

val DettleDarkPrimary = DettleLavenderPrimary
val DettleDarkOnPrimary = DettleLavenderOnPrimary
val DettleDarkPrimaryContainer = DettleLavenderContainer
val DettleDarkOnPrimaryContainer = DettleLavenderOnContainer

// Sweet Blush Rose (from the floating hearts and rosy cheeks)
val DettleHeartRose = Color(0xFFE87A82)
val DettleDarkSecondary = DettleHeartRose
val DettleDarkOnSecondary = Color(0xFF2E0E14)
val DettleDarkSecondaryContainer = Color(0xFF2C161C)
val DettleDarkOnSecondaryContainer = Color(0xFFFFDADE)

// Warm Honey Gold (from the golden bangle and savory curry plate)
val DettleHoneyGold = Color(0xFFF5B041)
val DettleDarkTertiary = DettleHoneyGold
val DettleDarkOnTertiary = Color(0xFF2C1B03)

val DettleDarkOutline = Color(0xFF282D42)
val DettleDarkOutlineVariant = Color(0xFF1B1E2E)

val DettleDarkError = Color(0xFFE87A82)
val DettleDarkOnError = Color(0xFF2E0E14)
val DettleDarkErrorContainer = Color(0xFF2C161C)
val DettleDarkOnErrorContainer = Color(0xFFFFDADE)

// Light Theme Palette (Warm Cream Ivory Canvas from mascot badge)
val DettleOatBackground = Color(0xFFFAF0E3)
val DettleOatOnBackground = Color(0xFF241E24)
val DettleOatSurface = Color(0xFFFFFFFF)
val DettleOatOnSurface = Color(0xFF241E24)
val DettleOatSurfaceVariant = Color(0xFFF2E6D5)
val DettleOatOnSurfaceVariant = Color(0xFF6B6258)

val DettlePlumPrimary = Color(0xFF52609A) // Deep periwinkle
val DettlePlumOnPrimary = Color(0xFFFFFFFF)
val DettlePlumPrimaryContainer = Color(0xFFE0E4FC)
val DettlePlumOnPrimaryContainer = Color(0xFF0F1738)

val DettleTangerineTertiary = Color(0xFFD97706) // Golden honey
val DettleTangerineOnTertiary = Color(0xFFFFFFFF)

val DettleOatSecondary = Color(0xFFD14F60) // Heart coral rose
val DettleOatOnSecondary = Color(0xFFFFFFFF)
val DettleOatSecondaryContainer = Color(0xFFFFD9DC)
val DettleOatOnSecondaryContainer = Color(0xFF3F0410)

val DettleOatOutline = Color(0xFFD8CEBF)
val DettleOatOutlineVariant = Color(0xFFE8DFD1)

// Backward-compatible semantic aliases
val DettleLightBackground = DettleOatBackground
val DettleLightOnBackground = DettleOatOnBackground
val DettleLightSurface = DettleOatSurface
val DettleLightOnSurface = DettleOatOnSurface
val DettleLightSurfaceVariant = DettleOatSurfaceVariant
val DettleLightOnSurfaceVariant = DettleOatOnSurfaceVariant

val DettleLightPrimary = DettlePlumPrimary
val DettleLightOnPrimary = DettlePlumOnPrimary
val DettleLightPrimaryContainer = DettlePlumPrimaryContainer
val DettleLightOnPrimaryContainer = DettlePlumOnPrimaryContainer

val DettleLightSecondary = DettleOatSecondary
val DettleLightOnSecondary = DettleOatOnSecondary
val DettleLightSecondaryContainer = DettleOatSecondaryContainer
val DettleLightOnSecondaryContainer = DettleOatOnSecondaryContainer

val DettleLightTertiary = DettleTangerineTertiary
val DettleLightOnTertiary = DettleTangerineOnTertiary

val DettleLightOutline = DettleOatOutline
val DettleLightOutlineVariant = DettleOatOutlineVariant

val DettleLightError = Color(0xFFD14F60)
val DettleLightOnError = Color(0xFFFFFFFF)
val DettleLightErrorContainer = Color(0xFFFFD9DC)
val DettleLightOnErrorContainer = Color(0xFF8B0000)

// Status & Accents (Extracted from the mascot illustration)
val DettleGreen = Color(0xFF10B981)
val DettleGreenDim = Color(0xFF064E3B)
val DettleOrange = Color(0xFFF5B041)
val DettleRed = Color(0xFFE87A82)
val DettlePurple = Color(0xFFA4B0E8)
val DettleBlue = Color(0xFF7986CB)

// Backward-compatible semantic bridges & shortcuts
val DettleDark = DettleDarkBackground
val DettleSurface = DettleDarkSurface
val DettleSurfaceVariant = DettleDarkSurfaceVariant
val DettleCard = DettleDarkSurface
val DettleCardBorder = DettleDarkOutlineVariant

val DettleCyan = DettleDarkPrimary
val DettleCyanDim = DettleDarkPrimaryContainer

val DettleTextPrimary = DettleDarkOnBackground
val DettleTextSecondary = DettleDarkOnSurfaceVariant
val DettleTextMuted = Color(0xFF6B7288)

// Chat message bubbles
val UserBubble = DettleDarkPrimaryContainer
val UserBubbleBorder = Color(0xFF353D5C)
val AiBubble = DettleDarkSurface
val AiBubbleBorder = DettleDarkOutlineVariant
val ToolCallBg = DettleDarkSurfaceVariant
val ToolCallBorder = DettleDarkOutlineVariant
val ApprovalBg = Color(0xFF24151C)
val ApprovalBorder = Color(0xFF4A242E)

// =============================================================================
// Dynamic Color Scheme Builders for App Themes
// =============================================================================

fun createColorSchemeForTheme(
    theme: AppTheme,
    isDark: Boolean,
    isPureOled: Boolean,
    customAccent: Color?
): ColorScheme {
    if (isDark) {
        // Pure Pitch Black (#000000) & Mascot Palette (Periwinkle #A4B0E8 + Blush #E87A82 + Midnight #1E2338)
        val primary = customAccent ?: DettleLavenderPrimary
        return darkColorScheme(
            primary = primary,
            onPrimary = DettleLavenderOnPrimary,
            primaryContainer = DettleLavenderContainer,
            onPrimaryContainer = DettleLavenderOnContainer,
            secondary = DettleDarkSecondary,
            onSecondary = DettleDarkOnSecondary,
            secondaryContainer = DettleDarkSecondaryContainer,
            onSecondaryContainer = DettleDarkOnSecondaryContainer,
            tertiary = DettleDarkTertiary,
            onTertiary = DettleDarkOnTertiary,
            background = DettleDarkBackground, // 0xFF000000 Pure Pitch Black
            onBackground = DettleDarkOnBackground,
            surface = DettleDarkSurface,       // 0xFF08090E Obsidian Surface
            onSurface = DettleDarkOnSurface,
            surfaceVariant = DettleDarkSurfaceVariant, // 0xFF131522 Midnight Sofa
            onSurfaceVariant = DettleDarkOnSurfaceVariant,
            outline = DettleDarkOutline,
            outlineVariant = DettleDarkOutlineVariant,
            error = DettleDarkError,
            onError = DettleDarkOnError,
            errorContainer = DettleDarkErrorContainer,
            onErrorContainer = DettleDarkOnErrorContainer
        )
    }

    // Light Theme: Warm Mascot Ivory Canvas, Rich Periwinkle & Sweet Coral Rose
    val primary = customAccent ?: DettlePlumPrimary
    return lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = DettlePlumPrimaryContainer,
        onPrimaryContainer = DettlePlumOnPrimaryContainer,
        secondary = DettleOatSecondary,
        onSecondary = Color.White,
        secondaryContainer = DettleOatSecondaryContainer,
        onSecondaryContainer = DettleOatOnSecondaryContainer,
        tertiary = DettleTangerineTertiary,
        onTertiary = Color.White,
        background = DettleOatBackground,
        onBackground = DettleOatOnBackground,
        surface = DettleOatSurface,
        onSurface = DettleOatOnSurface,
        surfaceVariant = DettleOatSurfaceVariant,
        onSurfaceVariant = DettleOatOnSurfaceVariant,
        outline = DettleOatOutline,
        outlineVariant = DettleOatOutlineVariant,
        error = DettleLightError,
        onError = DettleLightOnError,
        errorContainer = DettleLightErrorContainer,
        onErrorContainer = DettleLightOnErrorContainer
    )
}
