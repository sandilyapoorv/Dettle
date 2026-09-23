package com.dettle.app.ui.overnight

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dettle.app.orchestrator.overnight.OvernightPhase
import com.dettle.app.orchestrator.overnight.OvernightTask
import com.dettle.app.orchestrator.overnight.OvernightTaskStatus
import com.dettle.app.orchestrator.overnight.OvernightTaskType
import com.dettle.app.orchestrator.overnight.TaskOutcome
import com.dettle.app.orchestrator.overnight.TaskPriority
import com.dettle.app.ui.agents.PulsingDot
import com.dettle.app.ui.theme.DettleGreen
import com.dettle.app.ui.theme.DettleOrange
import com.dettle.app.ui.theme.DettlePurple
import com.dettle.app.ui.theme.DettleRed

@Composable
fun OvernightScreen(
    viewModel: OvernightViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val addState by viewModel.addTaskState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (!state.isRunning) {
                FloatingActionButton(
                    onClick = viewModel::showAddTask,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Outlined.Add, "Add task")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Overnight Mode",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "Autonomous background execution. Queue engineering goals before sleep.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (state.isRunning) {
                        PulsingDot(MaterialTheme.colorScheme.primary, Modifier.size(12.dp))
                    }
                }
            }

            // Status banner
            item { StatusBanner(state) }

            // Loop Control Button
            item {
                LoopControlButton(
                    isRunning = state.isRunning,
                    pendingCount = state.pendingCount,
                    phase = state.loopState.phase,
                    onStart = viewModel::startOvernight,
                    onStop = viewModel::interruptOvernight
                )
            }

            // Task queue
            if (state.tasks.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "TASK QUEUE (${state.tasks.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (state.tasks.any { it.status == OvernightTaskStatus.FAILED }) {
                                Text(
                                    "Retry failed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DettleOrange,
                                    modifier = Modifier.clickable { viewModel.retryFailed() }
                                )
                            }
                            Text(
                                "Clear finished",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable { viewModel.clearDone() }
                            )
                        }
                    }
                }

                items(state.tasks, key = { it.id }) { task ->
                    TaskQueueCard(
                        task = task,
                        isCurrentTask = state.loopState.currentTask?.id == task.id,
                        onRemove = if (!state.isRunning) ({ viewModel.removeTask(task.id) }) else null
                    )
                }
            } else {
                item { EmptyQueueCard() }
            }

            // Live log
            if (state.isRunning && state.loopState.log.isNotEmpty()) {
                item {
                    Text(
                        "LIVE EXECUTION LOG",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                item {
                    val logs = remember(state.loopState.log) {
                        state.loopState.log.takeLast(40).map { entry ->
                            val cleanMsg = entry.message
                                .replace("✅", "")
                                .replace("❌", "")
                                .replace("⚠️", "")
                                .replace("▶", "")
                                .replace("🌅", "")
                                .trim()
                            val colorType = when {
                                entry.message.contains("succeeded", ignoreCase = true) || entry.message.contains("complete", ignoreCase = true) -> 1
                                entry.message.contains("failed", ignoreCase = true) || entry.message.contains("error", ignoreCase = true) -> 2
                                entry.message.contains("warning", ignoreCase = true) -> 3
                                else -> 0
                            }
                            cleanMsg to colorType
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            logs.forEach { (cleanMsg, colorType) ->
                                Text(
                                    "> $cleanMsg",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = when (colorType) {
                                        1 -> DettleGreen
                                        2 -> MaterialTheme.colorScheme.error
                                        3 -> DettleOrange
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        // Add Task Sheet
        if (addState.showing) {
            AddTaskSheet(
                state = addState,
                onTitleChange = viewModel::updateAddTaskTitle,
                onDescChange = viewModel::updateAddTaskDesc,
                onTypeChange = viewModel::updateAddTaskType,
                onPriorityChange = viewModel::updateAddTaskPriority,
                onRepoChange = viewModel::updateAddTaskRepo,
                onAdd = {
                    viewModel.addTask(
                        addState.title, addState.description,
                        addState.type, addState.priority, addState.repoKey
                    )
                },
                onDismiss = viewModel::hideAddTask
            )
        }
    }
}

// ── Status banner ──────────────────────────────────────────────────────────

@Composable
fun StatusBanner(state: OvernightUiState) {
    val phase = state.loopState.phase
    val icon: ImageVector = when (phase) {
        OvernightPhase.IDLE -> Icons.Outlined.Bedtime
        OvernightPhase.STARTING -> Icons.Outlined.PlayArrow
        OvernightPhase.RUNNING -> Icons.Outlined.PlayArrow
        OvernightPhase.WRAPPING_UP -> Icons.Outlined.Description
        OvernightPhase.DONE -> Icons.Outlined.CheckCircle
        OvernightPhase.INTERRUPTED -> Icons.Outlined.Pause
    }
    val (title, sub) = when (phase) {
        OvernightPhase.IDLE -> "Queue Ready" to "${state.pendingCount} task(s) waiting to run"
        OvernightPhase.STARTING -> "Starting Fleet" to "Preparing orchestration loop..."
        OvernightPhase.RUNNING -> "Running Overnight" to (state.loopState.currentTask?.title ?: "Executing task...")
        OvernightPhase.WRAPPING_UP -> "Wrapping Up" to "Writing summary to GitHub and logs..."
        OvernightPhase.DONE -> "Run Completed" to "${state.loopState.successCount}/${state.loopState.completedCount} tasks succeeded"
        OvernightPhase.INTERRUPTED -> "Interrupted" to "${state.loopState.completedCount} tasks completed before stop"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (phase == OvernightPhase.DONE) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LoopControlButton(
    isRunning: Boolean,
    pendingCount: Int,
    phase: OvernightPhase,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    if (!isRunning) {
        Button(
            onClick = onStart,
            enabled = pendingCount > 0 && phase != OvernightPhase.WRAPPING_UP,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (pendingCount > 0) "Start Overnight Run ($pendingCount tasks)" else "Add tasks to start",
                fontWeight = FontWeight.SemiBold
            )
        }
    } else {
        Button(
            onClick = onStop,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) {
            Icon(Icons.Outlined.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Stop Overnight Run", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun TaskQueueCard(
    task: OvernightTask,
    isCurrentTask: Boolean,
    onRemove: (() -> Unit)?
) {
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
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskTypeBadge(task.type)
                    Text(
                        task.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (task.description.isNotBlank()) {
                    Text(
                        task.description.take(100),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }

            if (onRemove != null) {
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TaskTypeBadge(type: OvernightTaskType) {
    val icon: ImageVector = when (type) {
        OvernightTaskType.CODE_FEATURE -> Icons.Outlined.AutoAwesome
        OvernightTaskType.CODE_REFACTOR -> Icons.Outlined.Sync
        OvernightTaskType.CODE_FIX -> Icons.Outlined.Build
        OvernightTaskType.WRITE_TESTS -> Icons.Outlined.FactCheck
        OvernightTaskType.DEPLOY -> Icons.Outlined.CloudUpload
        OvernightTaskType.DOCS -> Icons.Outlined.Description
        OvernightTaskType.RESEARCH -> Icons.Outlined.Search
        OvernightTaskType.CUSTOM -> Icons.Outlined.Terminal
    }

    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(12.dp)
            )
            Text(
                type.name.replace('_', ' '),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun EmptyQueueCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Outlined.Bedtime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            Text(
                "Queue is empty",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Tap + to schedule engineering tasks for tonight.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(
    state: AddTaskState,
    onTitleChange: (String) -> Unit,
    onDescChange: (String) -> Unit,
    onTypeChange: (OvernightTaskType) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    onRepoChange: (String) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Add Overnight Task",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                "Queue an autonomous task for overnight execution.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = state.title,
                onValueChange = onTitleChange,
                label = { Text("Task Title") },
                placeholder = { Text("e.g. Implement user profile screen") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = onDescChange,
                label = { Text("Description & Constraints") },
                placeholder = { Text("Details, requirements, or acceptance criteria...") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                maxLines = 4
            )

            // Task Type Chips
            Text(
                "Task Type",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    OvernightTaskType.CODE_FEATURE to "Feature",
                    OvernightTaskType.CODE_FIX to "Bug Fix",
                    OvernightTaskType.CODE_REFACTOR to "Refactor",
                    OvernightTaskType.WRITE_TESTS to "Test"
                ).forEach { (type, label) ->
                    val isSelected = state.type == type
                    Surface(
                        onClick = { onTypeChange(type) },
                        shape = MaterialTheme.shapes.small,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = onAdd,
                    enabled = state.title.isNotBlank(),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text("Add to Queue")
                }
            }
        }
    }
}

