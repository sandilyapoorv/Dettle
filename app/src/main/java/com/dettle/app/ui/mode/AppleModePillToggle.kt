package com.dettle.app.ui.mode

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dettle.app.orchestrator.mode.EnvironmentMode

/**
 * Apple Design Horizontal Segmented Pill Toggle:
 *
 * Implements WWDC "Designing Fluid Interfaces" spring physics:
 * - Critically/under-damped spring (damping: 0.82f, stiffness: MediumLow)
 * - Translucent frosted pill track with subtle border
 * - Smooth horizontal sliding indicator from Chat (left) to Build (right)
 * - Touch-down scale compression (0.96f) and tactile haptic feedback
 */
@Composable
fun AppleModePillToggle(
    selected: EnvironmentMode,
    onSelect: (EnvironmentMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pillPressScale"
    )

    // Total track width: 156.dp, padding: 3.dp each side, thumb width: 74.dp
    // Chat (left): offset 3.dp, Build (right): offset 79.dp
    val pillOffsetX by animateDpAsState(
        targetValue = if (selected == EnvironmentMode.CHAT) 3.dp else 79.dp,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pillOffsetX"
    )

    fun triggerHaptic() {
        try {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } catch (_: Exception) {}
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .width(156.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .border(
                BorderStroke(
                    0.5.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                ),
                RoundedCornerShape(17.dp)
            )
    ) {
        // Sliding indicator thumb
        Box(
            modifier = Modifier
                .offset { IntOffset(pillOffsetX.roundToPx(), 0) }
                .padding(vertical = 3.dp)
                .width(74.dp)
                .fillMaxHeight()
                .shadow(2.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    BorderStroke(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    ),
                    RoundedCornerShape(14.dp)
                )
        )

        // Clickable Labels (Chat on Left, Build on Right)
        Row(modifier = Modifier.fillMaxSize()) {
            val isChat = selected == EnvironmentMode.CHAT
            val chatTextColor by animateColorAsState(
                targetValue = if (isChat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                animationSpec = tween(150),
                label = "chatTextColor"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        if (!isChat) {
                            triggerHaptic()
                            onSelect(EnvironmentMode.CHAT)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chat",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = if (isChat) FontWeight.SemiBold else FontWeight.Medium,
                        letterSpacing = (-0.01).sp
                    ),
                    color = chatTextColor
                )
            }

            val isBuild = selected == EnvironmentMode.BUILD
            val buildTextColor by animateColorAsState(
                targetValue = if (isBuild) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                animationSpec = tween(150),
                label = "buildTextColor"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        if (!isBuild) {
                            triggerHaptic()
                            onSelect(EnvironmentMode.BUILD)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Build",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = if (isBuild) FontWeight.SemiBold else FontWeight.Medium,
                        letterSpacing = (-0.01).sp
                    ),
                    color = buildTextColor
                )
            }
        }
    }
}
