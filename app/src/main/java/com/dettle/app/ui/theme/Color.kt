package com.dettle.app.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// YRB Inspired Color Palette (Warm Organic Editorial Design System)
// =============================================================================

// Light Theme Palette
val YrbLightPrimary = Color(0xFF1B1B18)
val YrbLightOnPrimary = Color(0xFFFFFFFF)
val YrbLightPrimaryContainer = Color(0xFFE8E5DE)
val YrbLightOnPrimaryContainer = Color(0xFF1B1B18)
val YrbLightSecondary = Color(0xFF67635C)
val YrbLightOnSecondary = Color(0xFFFFFFFF)
val YrbLightBackground = Color(0xFFF6F3ED)
val YrbLightOnBackground = Color(0xFF1B1B18)
val YrbLightSurface = Color(0xFFFFFCF7)
val YrbLightOnSurface = Color(0xFF1B1B18)
val YrbLightSurfaceVariant = Color(0xFFEDE9E1)
val YrbLightOnSurfaceVariant = Color(0xFF625F58)
val YrbLightOutline = Color(0xFF817D75)
val YrbLightOutlineVariant = Color(0xFFD5D0C6)
val YrbLightError = Color(0xFFB3261E)
val YrbLightOnError = Color(0xFFFFFFFF)

// Dark Theme Palette
val YrbDarkPrimary = Color(0xFFF2EFE8)
val YrbDarkOnPrimary = Color(0xFF22221F)
val YrbDarkPrimaryContainer = Color(0xFF34332F)
val YrbDarkOnPrimaryContainer = Color(0xFFF2EFE8)
val YrbDarkSecondary = Color(0xFFC9C4BA)
val YrbDarkOnSecondary = Color(0xFF302F2B)
val YrbDarkBackground = Color(0xFF11110F)
val YrbDarkOnBackground = Color(0xFFF0EDE6)
val YrbDarkSurface = Color(0xFF191917)
val YrbDarkOnSurface = Color(0xFFF0EDE6)
val YrbDarkSurfaceVariant = Color(0xFF282723)
val YrbDarkOnSurfaceVariant = Color(0xFFC9C4BA)
val YrbDarkOutline = Color(0xFF928D84)
val YrbDarkOutlineVariant = Color(0xFF403E38)
val YrbDarkError = Color(0xFFFFB4AB)
val YrbDarkOnError = Color(0xFF690005)

// Status & Accents (Subtle, desaturated to match YRB)
val DettleGreen = Color(0xFF4E8E5D)
val DettleGreenDim = Color(0xFF356340)
val DettleOrange = Color(0xFFD47C3B)
val DettleRed = Color(0xFFC84C4C)
val DettlePurple = Color(0xFF8E7AB5)
val DettleBlue = Color(0xFF4A7C9D)

// Backward-compatible semantic bridges (Dark warm defaults for legacy references)
val DettleDark = YrbDarkBackground
val DettleSurface = YrbDarkSurface
val DettleSurfaceVariant = YrbDarkSurfaceVariant
val DettleCard = YrbDarkSurface
val DettleCardBorder = YrbDarkOutlineVariant

val DettleCyan = YrbDarkPrimary
val DettleCyanDim = YrbDarkPrimaryContainer

val DettleTextPrimary = YrbDarkOnBackground
val DettleTextSecondary = YrbDarkOnSurfaceVariant
val DettleTextMuted = YrbDarkOutline

// Chat message bubbles
val UserBubble = YrbDarkPrimaryContainer
val UserBubbleBorder = YrbDarkOutlineVariant

val AiBubble = YrbDarkSurface
val AiBubbleBorder = YrbDarkSurfaceVariant

// Tool call card
val ToolCallBg = Color(0xFF1F1E1B)
val ToolCallBorder = YrbDarkOutlineVariant

// Approval card
val ApprovalBg = Color(0xFF24221C)
val ApprovalBorder = Color(0xFF4A4435)
