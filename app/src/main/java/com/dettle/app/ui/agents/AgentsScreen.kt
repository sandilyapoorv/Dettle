package com.dettle.app.ui.agents

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dettle.app.orchestrator.AgentEvent
import com.dettle.app.orchestrator.AgentRole
import com.dettle.app.orchestrator.AgentState
import com.dettle.app.orchestrator.AgentStatus
import com.dettle.app.ui.theme.DettleGreen
import com.dettle.app.ui.theme.DettleOrange

fun getAgentRoleIcon(role: AgentRole): ImageVector = when (role) {
    AgentRole.ORCHESTRATOR -> Icons.Outlined.Psychology
    AgentRole.READER -> Icons.Outlined.AutoStories
    AgentRole.CODER -> Icons.Outlined.Terminal
    AgentRole.REVIEWER -> Icons.Outlined.RateReview
    AgentRole.DEPLOYER -> Icons.Outlined.CloudUpload
    AgentRole.RESEARCHER -> Icons.Outlined.Search
}

@Composable
fun AgentsScreen(
    viewModel: AgentsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Agent Fleet",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            if (state.busyCount > 0) "${state.busyCount} agent${if (state.busyCount > 1) "s" else ""} active"
                            else "All 6 specialized agents idle and ready",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (state.busyCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (state.busyCount > 0) {
                        PulsingDot(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Agent roles
            item {
                Text(
                    "SPECIALIZED ROLES",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(AgentRole.entries, key = { it.name }) { role ->
                val agentState = state.agentStates[role] ?: AgentState(role = role)
                AgentRoleCard(agentState)
            }

            // MCP servers
            item {
                Text(
                    "MCP EXTENSIONS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    McpStatusChip(
                        name = "Serena",
                        description = "Codebase navigation",
                        isConnected = state.mcpStatus.serenaConfigured,
                        detail = if (state.mcpStatus.serenaConfigured) state.mcpStatus.serenaUrl
                                 else "Configure in Settings",
                        modifier = Modifier.weight(1f)
                    )
                    McpStatusChip(
                        name = "Context7",
                        description = "Live docs indexing",
                        isConnected = state.mcpStatus.context7Ready,
                        detail = if (state.mcpStatus.context7Ready) "Connected" else "Configure in Settings",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Event log
            if (state.recentEvents.isNotEmpty()) {
                item {
                    Text(
                        "RECENT FLEET ACTIVITY",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(state.recentEvents) { event ->
                    AgentEventRow(event)
                }
            } else {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Box(
                            modifier = Modifier.padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No fleet events yet. Start a task in Chat to dispatch agents.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun AgentRoleCard(agentState: AgentState) {
    val role = agentState.role
    val isWorking = agentState.status == AgentStatus.WORKING

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (isWorking) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        getAgentRoleIcon(role),
                        contentDescription = null,
                        tint = if (isWorking) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        role.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(8.dp))
                    StatusBadge(agentState.status)
                }
                Text(
                    if (isWorking && agentState.currentTask.isNotBlank()) agentState.currentTask
                    else role.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isWorking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: AgentStatus) {
    val label = when (status) {
        AgentStatus.IDLE -> "Idle"
        AgentStatus.WORKING -> "Working"
        AgentStatus.WAITING -> "Waiting"
        AgentStatus.DONE -> "Done"
        AgentStatus.FAILED -> "Failed"
    }
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = when (status) {
            AgentStatus.WORKING -> MaterialTheme.colorScheme.primaryContainer
            AgentStatus.DONE -> DettleGreen.copy(alpha = 0.15f)
            AgentStatus.FAILED -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = when (status) {
                AgentStatus.WORKING -> MaterialTheme.colorScheme.onPrimaryContainer
                AgentStatus.DONE -> DettleGreen
                AgentStatus.FAILED -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun McpStatusChip(
    name: String,
    description: String,
    isConnected: Boolean,
    detail: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) DettleGreen else MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1)
        }
    }
}

@Composable
fun AgentEventRow(event: AgentEvent) {
    val (icon, text, color) = when (event) {
        is AgentEvent.TaskAssigned -> Triple(Icons.Outlined.Assignment, "${event.role.displayName}: ${event.task.take(60)}", MaterialTheme.colorScheme.onSurface)
        is AgentEvent.TaskCompleted -> Triple(Icons.Outlined.CheckCircle, "${event.role.displayName} completed task", DettleGreen)
        is AgentEvent.TaskFailed -> Triple(Icons.Outlined.Cancel, "${event.role.displayName} failed: ${event.error.take(50)}", MaterialTheme.colorScheme.error)
        is AgentEvent.DebateRound -> Triple(Icons.Outlined.CompareArrows, "Debate round ${event.round} (Coder vs Reviewer)", MaterialTheme.colorScheme.onSurface)
        is AgentEvent.DebateResolved -> Triple(Icons.Outlined.DoneAll, "Debate resolved in ${event.rounds} round(s)", DettleGreen)
        is AgentEvent.DeploymentStarted -> Triple(Icons.Outlined.CloudUpload, "Deploying to ${event.target}...", DettleOrange)
        is AgentEvent.DeploymentComplete -> Triple(Icons.Outlined.Public, "Deployment live: ${event.url}", DettleGreen)
        is AgentEvent.Message -> Triple(Icons.Outlined.ChatBubbleOutline, "${event.from.displayName} to ${event.to.displayName}: ${event.content.take(60)}", MaterialTheme.colorScheme.onSurfaceVariant)
        is AgentEvent.Interrupt -> Triple(Icons.Outlined.Block, "Interrupted: ${event.reason}", MaterialTheme.colorScheme.error)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = color,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun PulsingDot(color: Color, modifier: Modifier = Modifier.size(10.dp)) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale = infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                val s = scale.value
                scaleX = s
                scaleY = s
            }
            .clip(CircleShape)
            .background(color)
    )
}
