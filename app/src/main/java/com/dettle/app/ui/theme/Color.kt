package com.dettle.app.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// Linear / Raycast Inspired Color Palette (Precision Dark Slate & Electric Indigo)
// =============================================================================

// Dark Theme Palette (Primary Developer Experience)
val DettleDarkBackground = Color(0xFF0C0D10) // Deep obsidian slate void
val DettleDarkOnBackground = Color(0xFFF3F4F6) // High contrast clean white/gray
val DettleDarkSurface = Color(0xFF13151A) // Elevated dark slate container
val DettleDarkOnSurface = Color(0xFFF3F4F6)
val DettleDarkSurfaceVariant = Color(0xFF191C23) // Secondary surface (cards, inputs, tools)
val DettleDarkOnSurfaceVariant = Color(0xFF8E95A2) // Refined cool slate for metadata/subtitles

val DettleDarkPrimary = Color(0xFF5E6AD2) // Linear Electric Indigo accent
val DettleDarkOnPrimary = Color(0xFFFFFFFF)
val DettleDarkPrimaryContainer = Color(0xFF1F2338) // Deep subtle indigo container
val DettleDarkOnPrimaryContainer = Color(0xFFC5CBFF)

val DettleDarkSecondary = Color(0xFF949AA4) // Cool neutral secondary
val DettleDarkOnSecondary = Color(0xFF13151A)
val DettleDarkSecondaryContainer = Color(0xFF222631)
val DettleDarkOnSecondaryContainer = Color(0xFFE2E4E9)

val DettleDarkTertiary = Color(0xFF38BDF8) // Electric cyan accent
val DettleDarkOnTertiary = Color(0xFF0B1926)

val DettleDarkOutline = Color(0xFF2E333F) // Crisp visible divider/border
val DettleDarkOutlineVariant = Color(0xFF1E212A) // Faint hairline border (1dp)

val DettleDarkError = Color(0xFFEF4444)
val DettleDarkOnError = Color(0xFF450A0A)
val DettleDarkErrorContainer = Color(0xFF2D1214)
val DettleDarkOnErrorContainer = Color(0xFFFCA5A5)

// Light Theme Palette (Clean Studio Light)
val DettleLightBackground = Color(0xFFF8F9FA)
val DettleLightOnBackground = Color(0xFF0F172A)
val DettleLightSurface = Color(0xFFFFFFFF)
val DettleLightOnSurface = Color(0xFF0F172A)
val DettleLightSurfaceVariant = Color(0xFFF1F3F5)
val DettleLightOnSurfaceVariant = Color(0xFF64748B)

val DettleLightPrimary = Color(0xFF4F5BD5)
val DettleLightOnPrimary = Color(0xFFFFFFFF)
val DettleLightPrimaryContainer = Color(0xFFEEF0FF)
val DettleLightOnPrimaryContainer = Color(0xFF1E2568)

val DettleLightSecondary = Color(0xFF475569)
val DettleLightOnSecondary = Color(0xFFFFFFFF)
val DettleLightSecondaryContainer = Color(0xFFE2E8F0)
val DettleLightOnSecondaryContainer = Color(0xFF1E293B)

val DettleLightTertiary = Color(0xFF0284C7)
val DettleLightOnTertiary = Color(0xFFFFFFFF)

val DettleLightOutline = Color(0xFF94A3B8)
val DettleLightOutlineVariant = Color(0xFFE2E8F0)

val DettleLightError = Color(0xFFDC2626)
val DettleLightOnError = Color(0xFFFFFFFF)
val DettleLightErrorContainer = Color(0xFFFEE2E2)
val DettleLightOnErrorContainer = Color(0xFF991B1B)

// Status & Accents (Vibrant yet disciplined developer tones)
val DettleGreen = Color(0xFF10B981) // Crisp Emerald (Active/Unleashed/Connected)
val DettleGreenDim = Color(0xFF064E3B)
val DettleOrange = Color(0xFFF59E0B) // Amber (Warning/Pending)
val DettleRed = Color(0xFFEF4444) // Rose Crimson (Error/Failed)
val DettlePurple = Color(0xFF8B5CF6) // Violet (Secondary AI/Agent)
val DettleBlue = Color(0xFF38BDF8) // Electric Cyan (Info/Tool)

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

// Tool call card
val ToolCallBg = DettleDarkSurfaceVariant
val ToolCallBorder = DettleDarkOutlineVariant

// Approval card
val ApprovalBg = Color(0xFF1B1D28)
val ApprovalBorder = Color(0xFF3B4168)
