package com.dettle.app.ui.mode

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dettle.app.orchestrator.mode.EnvironmentMode

/**
 * Apple-inspired vertical toggle docked on the right side of the screen.
 *
 * Implements WWDC "Designing Fluid Interfaces" spring physics:
 * - Critically/Under-damped spring (damping: 0.82, snappy response)
 * - Translucent frosted material layer with tactile feedback
 * - Instant touch-down press feedback (scale 0.95f)
 * - Smooth vertical sliding pill between Chat and Build modes
 */
@Composable
fun VerticalModeToggle(
    selected: EnvironmentMode,
    onSelect: (EnvironmentMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Apple fluid spring press feedback
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )

    // Vertical sliding pill position: 3dp for CHAT (top), 47dp for BUILD (bottom)
    val pillOffsetY by animateDpAsState(
        targetValue = if (selected == EnvironmentMode.CHAT) 3.dp else 47.dp,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pillOffsetY"
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
            .shadow(6.dp, RoundedCornerShape(26.dp))
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            .border(
                BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                ),
                RoundedCornerShape(26.dp)
            )
            .width(46.dp)
            .height(94.dp)
            .pointerInput(selected) {
                detectTapGestures { offset ->
                    val isTopHalf = offset.y < size.height / 2f
                    val newMode = if (isTopHalf) EnvironmentMode.CHAT else EnvironmentMode.BUILD
                    if (newMode != selected) {
                        triggerHaptic()
                        onSelect(newMode)
                    }
                }
            }
    ) {
        // Sliding Active Indicator Pill
        Box(
            modifier = Modifier
                .padding(horizontal = 3.dp)
                .offset { IntOffset(0, pillOffsetY.roundToPx()) }
                .width(40.dp)
                .height(44.dp)
                .shadow(2.dp, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    RoundedCornerShape(22.dp)
                )
        )

        // Slot contents
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Chat slot (Top)
            val isChat = selected == EnvironmentMode.CHAT
            Box(
                modifier = Modifier
                    .width(46.dp)
                    .height(47.dp)
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Chat Mode",
                        tint = if (isChat) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Chat",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = if (isChat) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = (-0.02).sp
                        ),
                        color = if (isChat) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // Build slot (Bottom)
            val isBuild = selected == EnvironmentMode.BUILD
            Box(
                modifier = Modifier
                    .width(46.dp)
                    .height(47.dp)
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Construction,
                        contentDescription = "Build Mode",
                        tint = if (isBuild) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Build",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = if (isBuild) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = (-0.02).sp
                        ),
                        color = if (isBuild) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
