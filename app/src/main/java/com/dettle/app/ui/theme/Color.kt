package com.dettle.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// =============================================================================
// Linear / Raycast / Apple Inspired Color Palettes
// =============================================================================

// Default Dark Theme Palette (Obsidian Void)
val DettleDarkBackground = Color(0xFF0C0D10)
val DettleDarkOnBackground = Color(0xFFF3F4F6)
val DettleDarkSurface = Color(0xFF13151A)
val DettleDarkOnSurface = Color(0xFFF3F4F6)
val DettleDarkSurfaceVariant = Color(0xFF191C23)
val DettleDarkOnSurfaceVariant = Color(0xFF8E95A2)

val DettleDarkPrimary = Color(0xFF5E6AD2)
val DettleDarkOnPrimary = Color(0xFFFFFFFF)
val DettleDarkPrimaryContainer = Color(0xFF1F2338)
val DettleDarkOnPrimaryContainer = Color(0xFFC5CBFF)

val DettleDarkSecondary = Color(0xFF949AA4)
val DettleDarkOnSecondary = Color(0xFF13151A)
val DettleDarkSecondaryContainer = Color(0xFF222631)
val DettleDarkOnSecondaryContainer = Color(0xFFE2E4E9)

val DettleDarkTertiary = Color(0xFF38BDF8)
val DettleDarkOnTertiary = Color(0xFF0B1926)

val DettleDarkOutline = Color(0xFF2E333F)
val DettleDarkOutlineVariant = Color(0xFF1E212A)

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
val DettlePurple = Color(0xFF8B5CF6)
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
val UserBubbleBorder = Color(0xFF2E3554)
val AiBubble = DettleDarkSurface
val AiBubbleBorder = DettleDarkOutlineVariant
val ToolCallBg = DettleDarkSurfaceVariant
val ToolCallBorder = DettleDarkOutlineVariant
val ApprovalBg = Color(0xFF1B1D28)
val ApprovalBorder = Color(0xFF3B4168)

// =============================================================================
// Dynamic Color Scheme Builders for App Themes
// =============================================================================

fun createColorSchemeForTheme(
    theme: AppTheme,
    isDark: Boolean,
    isPureOled: Boolean,
    customAccent: Color?
): ColorScheme {
    return when (theme) {
        AppTheme.OBSIDIAN -> {
            val primary = customAccent ?: DettleDarkPrimary
            val bg = if (isPureOled && isDark) Color(0xFF000000) else DettleDarkBackground
            val surface = if (isPureOled && isDark) Color(0xFF0D0E12) else DettleDarkSurface
            darkColorScheme(
                primary = primary,
                onPrimary = Color.White,
                primaryContainer = primary.copy(alpha = 0.22f),
                onPrimaryContainer = Color(0xFFD6DCFF),
                secondary = DettleDarkSecondary,
                onSecondary = Color(0xFF13151A),
                secondaryContainer = Color(0xFF222631),
                onSecondaryContainer = Color(0xFFE2E4E9),
                tertiary = DettleDarkTertiary,
                onTertiary = Color(0xFF0B1926),
                background = bg,
                onBackground = DettleDarkOnBackground,
                surface = surface,
                onSurface = DettleDarkOnSurface,
                surfaceVariant = if (isPureOled && isDark) Color(0xFF141720) else DettleDarkSurfaceVariant,
                onSurfaceVariant = DettleDarkOnSurfaceVariant,
                outline = DettleDarkOutline,
                outlineVariant = DettleDarkOutlineVariant,
                error = DettleDarkError,
                onError = DettleDarkOnError,
                errorContainer = DettleDarkErrorContainer,
                onErrorContainer = DettleDarkOnErrorContainer
            )
        }
        AppTheme.OAT_LIGHT, AppTheme.APPLE_LIGHT -> {
            val primary = customAccent ?: DettlePlumPrimary
            lightColorScheme(
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
        AppTheme.TITANIUM -> {
            val primary = customAccent ?: Color(0xFFB0B5C0)
            val bg = if (isPureOled && isDark) Color(0xFF000000) else Color(0xFF141518)
            val surface = if (isPureOled && isDark) Color(0xFF0E0F12) else Color(0xFF1A1C21)
            darkColorScheme(
                primary = primary,
                onPrimary = Color(0xFF141518),
                primaryContainer = Color(0xFF2A2D35),
                onPrimaryContainer = Color(0xFFF0F2F6),
                secondary = Color(0xFF8A909D),
                onSecondary = Color(0xFF141518),
                secondaryContainer = Color(0xFF242730),
                onSecondaryContainer = Color(0xFFDFE2E8),
                tertiary = Color(0xFF4A90E2),
                onTertiary = Color.White,
                background = bg,
                onBackground = Color(0xFFF0F2F6),
                surface = surface,
                onSurface = Color(0xFFF0F2F6),
                surfaceVariant = Color(0xFF23262E),
                onSurfaceVariant = Color(0xFF9EA4B1),
                outline = Color(0xFF353945),
                outlineVariant = Color(0xFF242730),
                error = DettleDarkError,
                onError = DettleDarkOnError,
                errorContainer = DettleDarkErrorContainer,
                onErrorContainer = DettleDarkOnErrorContainer
            )
        }
        AppTheme.CYBER_INDIGO -> {
            val primary = customAccent ?: Color(0xFF7C3AED)
            val bg = if (isPureOled && isDark) Color(0xFF000000) else Color(0xFF090D1A)
            val surface = if (isPureOled && isDark) Color(0xFF0F1426) else Color(0xFF11172E)
            darkColorScheme(
                primary = primary,
                onPrimary = Color.White,
                primaryContainer = Color(0xFF26194D),
                onPrimaryContainer = Color(0xFFDDD6FE),
                secondary = Color(0xFF38BDF8),
                onSecondary = Color(0xFF0B1926),
                secondaryContainer = Color(0xFF1E2E4A),
                onSecondaryContainer = Color(0xFFBAE6FD),
                tertiary = Color(0xFFEC4899),
                onTertiary = Color.White,
                background = bg,
                onBackground = Color(0xFFF8FAFC),
                surface = surface,
                onSurface = Color(0xFFF8FAFC),
                surfaceVariant = Color(0xFF1A213D),
                onSurfaceVariant = Color(0xFF94A3B8),
                outline = Color(0xFF2E385D),
                outlineVariant = Color(0xFF1C2442),
                error = DettleDarkError,
                onError = DettleDarkOnError,
                errorContainer = DettleDarkErrorContainer,
                onErrorContainer = DettleDarkOnErrorContainer
            )
        }
        AppTheme.EMERALD_MATRIX -> {
            val primary = customAccent ?: Color(0xFF10B981)
            val bg = if (isPureOled && isDark) Color(0xFF000000) else Color(0xFF08100C)
            val surface = if (isPureOled && isDark) Color(0xFF0E1A14) else Color(0xFF0F1F17)
            darkColorScheme(
                primary = primary,
                onPrimary = Color(0xFF022C22),
                primaryContainer = Color(0xFF064E3B),
                onPrimaryContainer = Color(0xFFA7F3D0),
                secondary = Color(0xFF34D399),
                onSecondary = Color(0xFF064E3B),
                secondaryContainer = Color(0xFF132F23),
                onSecondaryContainer = Color(0xFFD1FAE5),
                tertiary = Color(0xFF2DD4BF),
                onTertiary = Color(0xFF042F2E),
                background = bg,
                onBackground = Color(0xFFF0FDF4),
                surface = surface,
                onSurface = Color(0xFFF0FDF4),
                surfaceVariant = Color(0xFF152A20),
                onSurfaceVariant = Color(0xFF86A594),
                outline = Color(0xFF204433),
                outlineVariant = Color(0xFF142B20),
                error = DettleDarkError,
                onError = DettleDarkOnError,
                errorContainer = DettleDarkErrorContainer,
                onErrorContainer = DettleDarkOnErrorContainer
            )
        }
        AppTheme.SUNSET_AMBER -> {
            val primary = customAccent ?: Color(0xFFF59E0B)
            val bg = if (isPureOled && isDark) Color(0xFF000000) else Color(0xFF12100E)
            val surface = if (isPureOled && isDark) Color(0xFF1A1612) else Color(0xFF1E1A15)
            darkColorScheme(
                primary = primary,
                onPrimary = Color(0xFF451A03),
                primaryContainer = Color(0xFF451A03),
                onPrimaryContainer = Color(0xFFFDE68A),
                secondary = Color(0xFFFB923C),
                onSecondary = Color(0xFF431407),
                secondaryContainer = Color(0xFF382012),
                onSecondaryContainer = Color(0xFFFFEDD5),
                tertiary = Color(0xFFF43F5E),
                onTertiary = Color.White,
                background = bg,
                onBackground = Color(0xFFFFFBEB),
                surface = surface,
                onSurface = Color(0xFFFFFBEB),
                surfaceVariant = Color(0xFF28221B),
                onSurfaceVariant = Color(0xFFA89F91),
                outline = Color(0xFF42372A),
                outlineVariant = Color(0xFF292219),
                error = DettleDarkError,
                onError = DettleDarkOnError,
                errorContainer = DettleDarkErrorContainer,
                onErrorContainer = DettleDarkOnErrorContainer
            )
        }
        AppTheme.TOKYO_NEON -> {
            val primary = customAccent ?: Color(0xFF06B6D4)
            val bg = if (isPureOled && isDark) Color(0xFF000000) else Color(0xFF100C1A)
            val surface = if (isPureOled && isDark) Color(0xFF171126) else Color(0xFF1C1430)
            darkColorScheme(
                primary = primary,
                onPrimary = Color(0xFF083344),
                primaryContainer = Color(0xFF164E63),
                onPrimaryContainer = Color(0xFFCFFAFE),
                secondary = Color(0xFFEC4899),
                onSecondary = Color(0xFF500724),
                secondaryContainer = Color(0xFF451230),
                onSecondaryContainer = Color(0xFFFCE7F3),
                tertiary = Color(0xFFA855F7),
                onTertiary = Color.White,
                background = bg,
                onBackground = Color(0xFFFAF5FF),
                surface = surface,
                onSurface = Color(0xFFFAF5FF),
                surfaceVariant = Color(0xFF261B42),
                onSurfaceVariant = Color(0xFFA197B8),
                outline = Color(0xFF432E6E),
                outlineVariant = Color(0xFF2B1C47),
                error = DettleDarkError,
                onError = DettleDarkOnError,
                errorContainer = DettleDarkErrorContainer,
                onErrorContainer = DettleDarkOnErrorContainer
            )
        }
        AppTheme.OLED_BLACK -> {
            val primary = customAccent ?: Color(0xFF5E6AD2)
            darkColorScheme(
                primary = primary,
                onPrimary = Color.White,
                primaryContainer = primary.copy(alpha = 0.25f),
                onPrimaryContainer = Color(0xFFC5CBFF),
                secondary = Color(0xFF9CA3AF),
                onSecondary = Color.Black,
                secondaryContainer = Color(0xFF161616),
                onSecondaryContainer = Color(0xFFE5E7EB),
                tertiary = Color(0xFF38BDF8),
                onTertiary = Color.Black,
                background = Color(0xFF000000),
                onBackground = Color(0xFFFFFFFF),
                surface = Color(0xFF080808),
                onSurface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFF121212),
                onSurfaceVariant = Color(0xFF9E9E9E),
                outline = Color(0xFF282828),
                outlineVariant = Color(0xFF1C1C1C),
                error = DettleDarkError,
                onError = DettleDarkOnError,
                errorContainer = DettleDarkErrorContainer,
                onErrorContainer = DettleDarkOnErrorContainer
            )
        }
    }
}
