package com.dettle.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// =============================================================================
// Dettle Refined Engineering Palette (Linear / Claude / ChatGPT Standard)
// Calm, restrained, high-legibility dark mode with precise neutral hierarchy
// =============================================================================

// Apple / Developer System Accents (Subtle & purposeful, never garish)
val AppleBlue = Color(0xFF3B82F6)        // Clean technical blue
val AppleBlueLight = Color(0xFF2563EB)
val AppleIndigo = Color(0xFF6366F1)      // Restrained indigo
val ApplePurple = Color(0xFFA855F7)
val ApplePink = Color(0xFFEC4899)
val AppleRed = Color(0xFFEF4444)         // Clear error / destructive red
val AppleOrange = Color(0xFFF97316)      // Warm amber
val AppleYellow = Color(0xFFEAB308)
val AppleGreen = Color(0xFF10B981)       // Precise success emerald
val AppleMint = Color(0xFF14B8A6)
val AppleTeal = Color(0xFF06B6D4)
val AppleCyan = Color(0xFF0EA5E9)

// Semantic Accent Aliases
val DettleGreen = AppleGreen
val DettleGreenDim = Color(0xFF064E3B)
val DettleOrange = AppleOrange
val DettleRed = AppleRed
val DettlePurple = ApplePurple
val DettleBlue = AppleBlue
val DettleCyan = AppleCyan
val DettleCyanDim = Color(0xFF0C4A6E)

// ── Dark Theme: Deep Slate / Carbon (Engineered for prolonged coding sessions) ──
val AppleDarkBackground = Color(0xFF09090B)       // Deep obsidian carbon
val AppleDarkOnBackground = Color(0xFFF4F4F5)     // High-contrast clean white
val AppleDarkSurface = Color(0xFF121215)          // Primary card surface
val AppleDarkOnSurface = Color(0xFFF4F4F5)        // Primary text
val AppleDarkSurfaceVariant = Color(0xFF1C1C21)   // Elevated surface / interactive
val AppleDarkOnSurfaceVariant = Color(0xFFA1A1AA) // Secondary muted text (Zinc 400)

val AppleDarkOutline = Color(0xFF27272A)          // Hairline structural border
val AppleDarkOutlineVariant = Color(0xFF323238)   // Secondary subtle border
val AppleDarkError = AppleRed

// ── Light Theme: Clean Minimalist Frost ────────────────────────────────────────
val AppleLightBackground = Color(0xFFF8F9FA)      // Pure neutral off-white
val AppleLightOnBackground = Color(0xFF09090B)
val AppleLightSurface = Color(0xFFFFFFFF)         // Clean card white
val AppleLightOnSurface = Color(0xFF09090B)
val AppleLightSurfaceVariant = Color(0xFFF1F2F4)
val AppleLightOnSurfaceVariant = Color(0xFF71717A)

val AppleLightOutline = Color(0xFFE4E4E7)
val AppleLightOutlineVariant = Color(0xFFD4D4D8)
val AppleLightError = Color(0xFFDC2626)

// Backward-compatible semantic bridges
val DettleDarkBackground = AppleDarkBackground
val DettleDarkOnBackground = AppleDarkOnBackground
val DettleDarkSurface = AppleDarkSurface
val DettleDarkOnSurface = AppleDarkOnSurface
val DettleDarkSurfaceVariant = AppleDarkSurfaceVariant
val DettleDarkOnSurfaceVariant = AppleDarkOnSurfaceVariant

val DettleDarkPrimary = AppleBlue
val DettleDarkOnPrimary = Color.White
val DettleDarkPrimaryContainer = Color(0xFF1E293B)
val DettleDarkOnPrimaryContainer = Color(0xFF93C5FD)
val DettleDarkSecondary = AppleIndigo
val DettleDarkOnSecondary = Color.White
val DettleDarkSecondaryContainer = Color(0xFF232242)
val DettleDarkOnSecondaryContainer = Color(0xFFC7D2FE)
val DettleDarkTertiary = AppleOrange
val DettleDarkOnTertiary = Color.Black
val DettleDarkOutline = AppleDarkOutline
val DettleDarkOutlineVariant = AppleDarkOutlineVariant
val DettleDarkError = AppleDarkError
val DettleDarkOnError = Color.White
val DettleDarkErrorContainer = Color(0xFF450A0A)
val DettleDarkOnErrorContainer = Color(0xFFFCA5A5)

val DettleLightBackground = AppleLightBackground
val DettleLightOnBackground = AppleLightOnBackground
val DettleLightSurface = AppleLightSurface
val DettleLightOnSurface = AppleLightOnSurface
val DettleLightSurfaceVariant = AppleLightSurfaceVariant
val DettleLightOnSurfaceVariant = AppleLightOnSurfaceVariant

val DettleLightPrimary = AppleBlueLight
val DettleLightOnPrimary = Color.White
val DettleLightPrimaryContainer = Color(0xFFEFF6FF)
val DettleLightOnPrimaryContainer = Color(0xFF1D4ED8)
val DettleLightSecondary = AppleIndigo
val DettleLightOnSecondary = Color.White
val DettleLightSecondaryContainer = Color(0xFFEEF2FF)
val DettleLightOnSecondaryContainer = Color(0xFF4338CA)
val DettleLightTertiary = AppleOrange
val DettleLightOnTertiary = Color.White
val DettleLightOutline = AppleLightOutline
val DettleLightOutlineVariant = AppleLightOutlineVariant
val DettleLightError = AppleLightError
val DettleLightOnError = Color.White
val DettleLightErrorContainer = Color(0xFFFEF2F2)
val DettleLightOnErrorContainer = AppleLightError

// Additional UI bridges
val DettleDark = AppleDarkBackground
val DettleSurface = AppleDarkSurface
val DettleSurfaceVariant = AppleDarkSurfaceVariant
val DettleCard = AppleDarkSurface
val DettleCardBorder = AppleDarkOutlineVariant
val DettleTextPrimary = AppleDarkOnBackground
val DettleTextSecondary = AppleDarkOnSurfaceVariant
val DettleTextMuted = Color(0xFF71717A)

// ── Chat message bubbles (ChatGPT / Claude Style: Elevated neutral surfaces) ──
val UserBubble = Color(0xFF222227)                // Elevated neutral charcoal (NOT harsh blue)
val UserBubbleBorder = Color(0xFF33333B)          // Crisp hairline border
val AiBubble = Color(0xFF141417)                  // Soft card surface on canvas
val AiBubbleBorder = Color(0xFF24242A)            // Subtle boundary
val ToolCallBg = Color(0xFF101013)                // Embedded code/tool card
val ToolCallBorder = Color(0xFF27272F)
val ApprovalBg = Color(0xFF1A1D24)
val ApprovalBorder = Color(0xFF2E3B4E)

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
            AppTheme.TITANIUM -> Color(0xFFA1A1AA)
            else -> AppleBlue
        }
        val bg = if (isPureOled || theme == AppTheme.OLED_BLACK) Color.Black else AppleDarkBackground
        return darkColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primary.copy(alpha = 0.18f),
            onPrimaryContainer = primary,
            secondary = AppleIndigo,
            onSecondary = Color.White,
            secondaryContainer = AppleIndigo.copy(alpha = 0.15f),
            onSecondaryContainer = AppleIndigo,
            tertiary = AppleOrange,
            onTertiary = Color.Black,
            background = bg,
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
        primaryContainer = primary.copy(alpha = 0.12f),
        onPrimaryContainer = primary,
        secondary = AppleIndigo,
        onSecondary = Color.White,
        secondaryContainer = AppleIndigo.copy(alpha = 0.12f),
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
