package com.dettle.app.ui.agents

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dettle.app.orchestrator.AgentEvent
import com.dettle.app.orchestrator.AgentRole
import com.dettle.app.orchestrator.AgentState
import com.dettle.app.orchestrator.AgentStatus
import com.dettle.app.ui.theme.DettleCard
import com.dettle.app.ui.theme.DettleCardBorder
import com.dettle.app.ui.theme.DettleCyan
import com.dettle.app.ui.theme.DettleDark
import com.dettle.app.ui.theme.DettleGreen
import com.dettle.app.ui.theme.DettleOrange
import com.dettle.app.ui.theme.DettlePurple
import com.dettle.app.ui.theme.DettleRed
import com.dettle.app.ui.theme.DettleSurface
import com.dettle.app.ui.theme.DettleTextMuted
import com.dettle.app.ui.theme.DettleTextSecondary

/**
 * Real-time multi-agent status screen.
 *
 * Shows:
 * - All 6 agent roles with live status (IDLE / WORKING / DONE / FAILED)
 * - Animated pulse on WORKING agents
 * - Recent event log (debate rounds, tool calls, deployments)
 * - MCP server connection status (Serena + Context7)
 */
@Composable
fun AgentsScreen(
    viewModel: AgentsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DettleDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Agent Fleet",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (state.busyCount > 0) "${state.busyCount} agent${if (state.busyCount > 1) "s" else ""} working..."
                        else "All agents idle",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.busyCount > 0) DettleCyan else DettleTextMuted
                    )
                }
                // Overall pulse indicator
                if (state.busyCount > 0) {
                    PulsingDot(color = DettleCyan)
                }
            }
        }

        // ── Agent role cards ─────────────────────────────────────────────
        item {
            Text(
                "AGENTS",
                style = MaterialTheme.typography.labelSmall,
                color = DettleTextMuted,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        items(AgentRole.values()) { role ->
            val agentState = state.agentStates[role] ?: AgentState(role = role)
            AgentRoleCard(agentState)
        }

        // ── MCP servers ──────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(4.dp))
            Text(
                "MCP SERVERS",
                style = MaterialTheme.typography.labelSmall,
                color = DettleTextMuted,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                McpStatusChip(
                    name = "Serena",
                    description = "Codebase navigation",
                    isConnected = state.mcpStatus.serenaConfigured,
                    detail = if (state.mcpStatus.serenaConfigured) state.mcpStatus.serenaUrl
                             else "Set URL in Settings",
                    modifier = Modifier.weight(1f)
                )
                McpStatusChip(
                    name = "Context7",
                    description = "Live documentation",
                    isConnected = state.mcpStatus.context7Ready,
                    detail = if (state.mcpStatus.context7Ready) "Connected" else "Set API key in Settings",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── Event log ────────────────────────────────────────────────────
        if (state.recentEvents.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    "RECENT EVENTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = DettleTextMuted,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(state.recentEvents) { event ->
                AgentEventRow(event)
            }
        } else {
            item {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DettleSurface)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No activity yet. Send a task in Chat to see agents work.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DettleTextMuted
                    )
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ── Agent role card ────────────────────────────────────────────────────────

@Composable
fun AgentRoleCard(agentState: AgentState) {
    val role = agentState.role
    val isWorking = agentState.status == AgentStatus.WORKING

    val statusColor = when (agentState.status) {
        AgentStatus.WORKING -> DettleCyan
        AgentStatus.DONE -> DettleGreen
        AgentStatus.FAILED -> DettleRed
        AgentStatus.WAITING -> DettleOrange
        AgentStatus.IDLE -> DettleTextMuted
    }

    val borderColor by animateColorAsState(
        if (isWorking) DettleCyan.copy(alpha = 0.5f) else DettleCardBorder,
        animationSpec = tween(500),
        label = "border"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DettleCard)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Emoji + pulse
        Box(contentAlignment = Alignment.Center) {
            Text(role.emoji, style = MaterialTheme.typography.titleLarge)
            if (isWorking) {
                PulsingDot(
                    color = DettleCyan,
                    modifier = Modifier.align(Alignment.TopEnd).size(8.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    role.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(8.dp))
                StatusBadge(agentState.status, statusColor)
            }
            Text(
                if (isWorking && agentState.currentTask.isNotBlank()) agentState.currentTask
                else role.description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isWorking) DettleCyan else DettleTextSecondary,
                maxLines = 2
            )
        }
    }
}

@Composable
fun StatusBadge(status: AgentStatus, color: Color) {
    val label = when (status) {
        AgentStatus.IDLE -> "idle"
        AgentStatus.WORKING -> "working"
        AgentStatus.WAITING -> "waiting"
        AgentStatus.DONE -> "done"
        AgentStatus.FAILED -> "failed"
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

// ── MCP status chip ───────────────────────────────────────────────────────

@Composable
fun McpStatusChip(
    name: String,
    description: String,
    isConnected: Boolean,
    detail: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DettleCard)
            .border(
                1.dp,
                if (isConnected) DettleGreen.copy(alpha = 0.3f) else DettleCardBorder,
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isConnected) DettleGreen else DettleTextMuted)
            )
            Spacer(Modifier.width(6.dp))
            Text(name, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Text(description, style = MaterialTheme.typography.bodySmall, color = DettleTextSecondary)
        Text(detail, style = MaterialTheme.typography.labelSmall, color = DettleTextMuted, maxLines = 1)
    }
}

// ── Event log row ──────────────────────────────────────────────────────────

@Composable
fun AgentEventRow(event: AgentEvent) {
    val (emoji, text, color) = when (event) {
        is AgentEvent.TaskAssigned -> Triple("📋", "${event.role.emoji} ${event.role.displayName}: ${event.task.take(60)}", DettleCyan)
        is AgentEvent.TaskCompleted -> Triple("✅", "${event.role.emoji} ${event.role.displayName} finished", DettleGreen)
        is AgentEvent.TaskFailed -> Triple("❌", "${event.role.emoji} ${event.role.displayName} failed: ${event.error.take(50)}", DettleRed)
        is AgentEvent.DebateRound -> Triple("⚔️", "Debate round ${event.round} — Coder vs Reviewer", DettlePurple)
        is AgentEvent.DebateResolved -> Triple("🏆", "Debate resolved in ${event.rounds} round(s)", DettleGreen)
        is AgentEvent.DeploymentStarted -> Triple("🚀", "Deploying to ${event.target}...", DettleOrange)
        is AgentEvent.DeploymentComplete -> Triple("🌐", "Live: ${event.url}", DettleGreen)
        is AgentEvent.Message -> Triple("💬", "${event.from.emoji}→${event.to.emoji} ${event.content.take(60)}", DettleTextSecondary)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DettleSurface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = color, modifier = Modifier.weight(1f))
    }
}

// ── Pulsing dot ───────────────────────────────────────────────────────────

@Composable
fun PulsingDot(color: Color, modifier: Modifier = Modifier.size(10.dp)) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
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
            .scale(scale)
            .clip(CircleShape)
            .background(color)
    )
}
