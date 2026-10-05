package com.dettle.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Apple Liquid Glass Design System
 * Provides frosted glassmorphism surfaces with specular light catching borders,
 * depth layering, and smooth squircle curvature.
 */
object LiquidGlassTokens {
    // Dark mode glass colors
    val DarkGlassBackground = Color(0xFF1C1C1E).copy(alpha = 0.72f)
    val DarkGlassSubtle = Color(0xFF2C2C2E).copy(alpha = 0.55f)
    val DarkGlassElevated = Color(0xFF3A3A3C).copy(alpha = 0.65f)

    val DarkGlassBorderBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.28f), // Specular light catching top
            Color.White.copy(alpha = 0.08f), // Body reflection
            Color.White.copy(alpha = 0.03f)  // Ambient bottom
        )
    )

    // Light mode frosted frost colors
    val LightGlassBackground = Color.White.copy(alpha = 0.82f)
    val LightGlassSubtle = Color(0xFFF2F2F7).copy(alpha = 0.70f)
    val LightGlassElevated = Color.White.copy(alpha = 0.92f)

    val LightGlassBorderBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.90f),
            Color(0xFFD1D1D6).copy(alpha = 0.50f),
            Color(0xFFE5E5EA).copy(alpha = 0.30f)
        )
    )
}

/**
 * Modifier to apply Apple Liquid Glass surface effect.
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(16.dp),
    isDark: Boolean = true,
    elevation: Dp = 0.dp,
    tint: Color? = null,
    borderWidth: Dp = 1.dp
): Modifier = this
    .then(if (elevation > 0.dp) Modifier.shadow(elevation, shape, clip = false) else Modifier)
    .clip(shape)
    .background(
        color = tint ?: if (isDark) LiquidGlassTokens.DarkGlassBackground else LiquidGlassTokens.LightGlassBackground,
        shape = shape
    )
    .border(
        width = borderWidth,
        brush = if (isDark) LiquidGlassTokens.DarkGlassBorderBrush else LiquidGlassTokens.LightGlassBorderBrush,
        shape = shape
    )

/**
 * Liquid Glass Container Card for high-end Apple aesthetic.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    isDark: Boolean = true,
    tint: Color? = null,
    elevation: Dp = 4.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .liquidGlass(
                shape = shape,
                isDark = isDark,
                elevation = elevation,
                tint = tint
            )
            .padding(16.dp),
        content = content
    )
}
