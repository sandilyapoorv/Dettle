package com.dettle.app.ui.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dettle.app.orchestrator.mode.AgentMode
import com.dettle.app.orchestrator.mode.ModeId
import com.dettle.app.ui.deployments.DeploymentsScreen
import com.dettle.app.ui.mode.ModePillBar
import com.dettle.app.ui.overnight.OvernightScreen
import com.dettle.app.ui.projects.ProjectsScreen
import com.dettle.app.ui.repos.ReposScreen

enum class BuildWorkspaceTab(val label: String, val icon: ImageVector) {
    PROJECTS("Projects", Icons.Outlined.FolderCopy),
    REPOS("Repos", Icons.Outlined.Code),
    DEPLOY("Deploy", Icons.Outlined.CloudUpload),
    OVERNIGHT("Overnight", Icons.Outlined.Bedtime),
    CHAT_STREAM("Build Tasks", Icons.Outlined.ChatBubbleOutline)
}

/**
 * Integrated Build Mode Workspace:
 * Consolidates Projects, Repositories, Deployments, and Overnight execution
 * directly inside Build mode with zero clutter.
 */
@Composable
fun BuildWorkspaceView(
    hasActiveMessages: Boolean,
    workModes: List<AgentMode>,
    activeModeId: ModeId,
    lockedModeId: ModeId?,
    onModeTap: (ModeId) -> Unit,
    onModeSettings: (AgentMode) -> Unit,
    messageStreamContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember(hasActiveMessages) {
        mutableStateOf(if (hasActiveMessages) BuildWorkspaceTab.CHAT_STREAM else BuildWorkspaceTab.PROJECTS)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Sub-mode pills (Plan, Goal, Review, Deploy, Code)
        if (workModes.isNotEmpty()) {
            ModePillBar(
                modes = workModes,
                activeModeId = activeModeId,
                lockedModeId = lockedModeId,
                onModeTap = { id ->
                    onModeTap(id)
                    selectedTab = BuildWorkspaceTab.CHAT_STREAM
                },
                onModeSettings = onModeSettings
            )
        }

        // Workspace Segmented Navigation Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BuildWorkspaceTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                Surface(
                    onClick = { selectedTab = tab },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            tint = if (isSelected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Selected Tab Content
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn().togetherWith(fadeOut())
                },
                label = "buildWorkspaceTabContent"
            ) { tab ->
                when (tab) {
                    BuildWorkspaceTab.PROJECTS -> {
                        ProjectsScreen(
                            onProjectSelected = {
                                selectedTab = BuildWorkspaceTab.REPOS
                            }
                        )
                    }
                    BuildWorkspaceTab.REPOS -> {
                        ReposScreen()
                    }
                    BuildWorkspaceTab.DEPLOY -> {
                        DeploymentsScreen()
                    }
                    BuildWorkspaceTab.OVERNIGHT -> {
                        OvernightScreen()
                    }
                    BuildWorkspaceTab.CHAT_STREAM -> {
                        messageStreamContent()
                    }
                }
            }
        }
    }
}
