package com.dettle.app.ui.mode

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dettle.app.orchestrator.mode.EnvironmentMode

/**
 * Clean, centered horizontal segmented control for switching between Chat and Build modes.
 * Built to the highest standards of modern engineering tools (ChatGPT / Linear / Cursor).
 */
@Composable
fun TopEnvironmentSegmentedControl(
    selected: EnvironmentMode,
    onSelect: (EnvironmentMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val isChat = selected == EnvironmentMode.CHAT
            val chatBg by animateColorAsState(
                targetValue = if (isChat) MaterialTheme.colorScheme.surface else Color.Transparent,
                animationSpec = tween(180),
                label = "chatBg"
            )
            Surface(
                onClick = { onSelect(EnvironmentMode.CHAT) },
                shape = RoundedCornerShape(18.dp),
                color = chatBg,
                shadowElevation = if (isChat) 2.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        tint = if (isChat) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Chat",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isChat) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 12.sp
                        ),
                        color = if (isChat) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val isBuild = selected == EnvironmentMode.BUILD
            val buildBg by animateColorAsState(
                targetValue = if (isBuild) MaterialTheme.colorScheme.surface else Color.Transparent,
                animationSpec = tween(180),
                label = "buildBg"
            )
            Surface(
                onClick = { onSelect(EnvironmentMode.BUILD) },
                shape = RoundedCornerShape(18.dp),
                color = buildBg,
                shadowElevation = if (isBuild) 2.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Code,
                        contentDescription = null,
                        tint = if (isBuild) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Build",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isBuild) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 12.sp
                        ),
                        color = if (isBuild) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
