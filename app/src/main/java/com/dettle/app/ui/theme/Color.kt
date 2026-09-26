package com.dettle.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// =============================================================================
// Linear / Raycast / Apple Inspired Color Palettes
// =============================================================================

// Pure Pitch Black & Lavender Dark Theme Palette
val DettleDarkBackground = Color(0xFF000000)
val DettleDarkOnBackground = Color(0xFFF3F4F6)
val DettleDarkSurface = Color(0xFF070709)
val DettleDarkOnSurface = Color(0xFFF3F4F6)
val DettleDarkSurfaceVariant = Color(0xFF101014)
val DettleDarkOnSurfaceVariant = Color(0xFF9E9EA8)

val DettleLavenderPrimary = Color(0xFFA78BFA)
val DettleLavenderOnPrimary = Color(0xFF130924)
val DettleLavenderContainer = Color(0xFF26183B)
val DettleLavenderOnContainer = Color(0xFFEDE9FE)

val DettleDarkPrimary = DettleLavenderPrimary
val DettleDarkOnPrimary = DettleLavenderOnPrimary
val DettleDarkPrimaryContainer = DettleLavenderContainer
val DettleDarkOnPrimaryContainer = DettleLavenderOnContainer

val DettleDarkSecondary = Color(0xFF9CA3AF)
val DettleDarkOnSecondary = Color(0xFF000000)
val DettleDarkSecondaryContainer = Color(0xFF181520)
val DettleDarkOnSecondaryContainer = Color(0xFFE5E7EB)

val DettleDarkTertiary = Color(0xFF38BDF8)
val DettleDarkOnTertiary = Color(0xFF0B1926)

val DettleDarkOutline = Color(0xFF2E2A38)
val DettleDarkOutlineVariant = Color(0xFF1D1A24)

val DettleDarkError = Color(0xFFEF4444)
val DettleDarkOnError = Color(0xFF450A0A)
val DettleDarkErrorContainer = Color(0xFF2D1214)
val DettleDarkOnErrorContainer = Color(0xFFFCA5A5)

// Light Theme Palette (Oat, Plum Purple & Tangerine)
val DettleOatBackground = Color(0xFFF7F4EE)
val DettleOatOnBackground = Color(0xFF1C1917)
val DettleOatSurface = Color(0xFFFFFFFF)
val DettleOatOnSurface = Color(0xFF1C1917)
val DettleOatSurfaceVariant = Color(0xFFEDE8DF)
val DettleOatOnSurfaceVariant = Color(0xFF78716C)

val DettlePlumPrimary = Color(0xFF6B21A8)
val DettlePlumOnPrimary = Color(0xFFFFFFFF)
val DettlePlumPrimaryContainer = Color(0xFFF3E8FF)
val DettlePlumOnPrimaryContainer = Color(0xFF3B0764)

val DettleTangerineTertiary = Color(0xFFF97316)
val DettleTangerineOnTertiary = Color(0xFFFFFFFF)

val DettleOatSecondary = Color(0xFF78716C)
val DettleOatOnSecondary = Color(0xFFFFFFFF)
val DettleOatSecondaryContainer = Color(0xFFE7E1D5)
val DettleOatOnSecondaryContainer = Color(0xFF292524)

val DettleOatOutline = Color(0xFFD4CEBF)
val DettleOatOutlineVariant = Color(0xFFE2DDD2)

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

val DettleLightError = Color(0xFFFF3B30)
val DettleLightOnError = Color(0xFFFFFFFF)
val DettleLightErrorContainer = Color(0xFFFFE5E5)
val DettleLightOnErrorContainer = Color(0xFF8B0000)

// Status & Accents (Vibrant yet disciplined developer tones)
val DettleGreen = Color(0xFF10B981)
val DettleGreenDim = Color(0xFF064E3B)
val DettleOrange = Color(0xFFF59E0B)
val DettleRed = Color(0xFFEF4444)
val DettlePurple = Color(0xFFA78BFA)
val DettleBlue = Color(0xFF38BDF8)

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
val DettleTextMuted = Color(0xFF5C6270)

// Chat message bubbles
val UserBubble = DettleDarkPrimaryContainer
val UserBubbleBorder = Color(0xFF352452)
val AiBubble = DettleDarkSurface
val AiBubbleBorder = DettleDarkOutlineVariant
val ToolCallBg = DettleDarkSurfaceVariant
val ToolCallBorder = DettleDarkOutlineVariant
val ApprovalBg = Color(0xFF130924)
val ApprovalBorder = Color(0xFF352452)

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
        // Pure Pitch Black (#000000) & Lavender (#A78BFA) Dark Theme
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
            surface = DettleDarkSurface,       // 0xFF070709 Pitch Black Surface
            onSurface = DettleDarkOnSurface,
            surfaceVariant = DettleDarkSurfaceVariant, // 0xFF101014
            onSurfaceVariant = DettleDarkOnSurfaceVariant,
            outline = DettleDarkOutline,
            outlineVariant = DettleDarkOutlineVariant,
            error = DettleDarkError,
            onError = DettleDarkOnError,
            errorContainer = DettleDarkErrorContainer,
            onErrorContainer = DettleDarkOnErrorContainer
        )
    }

    // Light Theme: Oat Canvas, Velvet Plum & Radiant Tangerine
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
