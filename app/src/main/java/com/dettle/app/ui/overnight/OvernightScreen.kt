package com.dettle.app.ui.overnight

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.dettle.app.ui.theme.DettleCard
import com.dettle.app.ui.theme.DettleCardBorder
import com.dettle.app.ui.theme.DettleCyan
import com.dettle.app.ui.theme.DettleCyanDim
import com.dettle.app.ui.theme.DettleDark
import com.dettle.app.ui.theme.DettleGreen
import com.dettle.app.ui.theme.DettleOrange
import com.dettle.app.ui.theme.DettlePurple
import com.dettle.app.ui.theme.DettleRed
import com.dettle.app.ui.theme.DettleSurface
import com.dettle.app.ui.theme.DettleSurfaceVariant
import com.dettle.app.ui.theme.DettleTextMuted
import com.dettle.app.ui.theme.DettleTextSecondary

/**
 * Overnight mode screen.
 *
 * Sections:
 * 1. Status banner — IDLE / RUNNING (with current task) / DONE summary
 * 2. START / STOP button with task count
 * 3. Task queue — pending, running, done, failed cards
 * 4. Live log scroll — last 50 log lines while running
 * 5. FAB → Add task sheet
 */
@Composable
fun OvernightScreen(
    viewModel: OvernightViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val addState by viewModel.addTaskState.collectAsState()

    Scaffold(
        containerColor = DettleDark,
        floatingActionButton = {
            if (!state.isRunning) {
                FloatingActionButton(
                    onClick = viewModel::showAddTask,
                    containerColor = DettleCyan,
                    contentColor = DettleDark
                ) {
                    Icon(Icons.Filled.Add, "Add task")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Header ──────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Overnight Mode",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Queue tasks • Agent works while you sleep • Wake up to done",
                            style = MaterialTheme.typography.bodySmall,
                            color = DettleTextMuted
                        )
                    }
                    if (state.isRunning) PulsingDot(DettleCyan, Modifier.size(12.dp))
                }
            }

            // ── Status banner ────────────────────────────────────────────
            item { StatusBanner(state) }

            // ── Start/Stop control ───────────────────────────────────────
            item {
                LoopControlButton(
                    isRunning = state.isRunning,
                    pendingCount = state.pendingCount,
                    phase = state.loopState.phase,
                    onStart = viewModel::startOvernight,
                    onStop = viewModel::interruptOvernight
                )
            }

            // ── Task queue ───────────────────────────────────────────────
            if (state.tasks.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "QUEUE (${state.tasks.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = DettleTextMuted
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (state.tasks.any { it.status == OvernightTaskStatus.FAILED }) {
                                Text(
                                    "Retry failed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DettleOrange,
                                    modifier = Modifier.clickable { viewModel.retryFailed() }
                                )
                            }
                            Text(
                                "Clear done",
                                style = MaterialTheme.typography.labelSmall,
                                color = DettleTextMuted,
                                modifier = Modifier.clickable { viewModel.clearDone() }
                            )
                        }
                    }
                }

                items(state.tasks) { task ->
                    TaskQueueCard(
                        task = task,
                        isCurrentTask = state.loopState.currentTask?.id == task.id,
                        onRemove = if (!state.isRunning) ({ viewModel.removeTask(task.id) }) else null
                    )
                }
            } else {
                item { EmptyQueueCard() }
            }

            // ── Live log ─────────────────────────────────────────────────
            if (state.isRunning && state.loopState.log.isNotEmpty()) {
                item {
                    Text(
                        "LIVE LOG",
                        style = MaterialTheme.typography.labelSmall,
                        color = DettleTextMuted,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                item {
                    val logState = rememberLazyListState()
                    val logs = state.loopState.log.takeLast(40)

                    LaunchedEffect(logs.size) {
                        logState.animateScrollToItem(logs.size.coerceAtLeast(1) - 1)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF050810))
                            .border(1.dp, DettleCardBorder, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        logs.forEach { entry ->
                            Text(
                                "> ${entry.message}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = when {
                                    entry.message.startsWith("✅") -> DettleGreen
                                    entry.message.startsWith("❌") -> DettleRed
                                    entry.message.startsWith("⚠️") -> DettleOrange
                                    entry.message.startsWith("▶") -> DettleCyan
                                    entry.message.startsWith("🌅") -> DettleGreen
                                    else -> DettleTextSecondary
                                }
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }

        // ── Add Task Sheet ───────────────────────────────────────────────
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
    val (bgColor, borderColor, icon, title, sub) = when (phase) {
        OvernightPhase.IDLE -> BannerConfig(
            DettleSurface, DettleCardBorder, "🌙",
            "Ready to run", "${state.pendingCount} task(s) queued"
        )
        OvernightPhase.STARTING -> BannerConfig(
            DettleCyan.copy(alpha = 0.08f), DettleCyan.copy(alpha = 0.3f), "⚡",
            "Starting...", "Preparing agent fleet"
        )
        OvernightPhase.RUNNING -> BannerConfig(
            DettleCyan.copy(alpha = 0.08f), DettleCyan.copy(alpha = 0.5f), "⚡",
            "Running overnight", state.loopState.currentTask?.title ?: "Working..."
        )
        OvernightPhase.WRAPPING_UP -> BannerConfig(
            DettleGreen.copy(alpha = 0.08f), DettleGreen.copy(alpha = 0.3f), "📝",
            "Wrapping up", "Writing summary to GitHub + Drive..."
        )
        OvernightPhase.DONE -> BannerConfig(
            DettleGreen.copy(alpha = 0.08f), DettleGreen.copy(alpha = 0.4f), "🌅",
            "Run complete", "${state.loopState.successCount}/${state.loopState.completedCount} tasks succeeded"
        )
        OvernightPhase.INTERRUPTED -> BannerConfig(
            DettleOrange.copy(alpha = 0.08f), DettleOrange.copy(alpha = 0.3f), "⏸",
            "Interrupted", "${state.loopState.completedCount} tasks completed before stop"
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = DettleTextSecondary)
        }
    }
}

data class BannerConfig(
    val bgColor: Color, val borderColor: Color, val icon: String,
    val title: String, val sub: String
)

// ── Loop control button ────────────────────────────────────────────────────

@Composable
fun LoopControlButton(
    isRunning: Boolean,
    pendingCount: Int,
    phase: OvernightPhase,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (isRunning) DettleRed.copy(alpha = 0.15f) else DettleCyan.copy(alpha = 0.15f),
        animationSpec = tween(400), label = "btn"
    )
    val fgColor by animateColorAsState(
        if (isRunning) DettleRed else DettleCyan,
        animationSpec = tween(400), label = "fg"
    )

    Button(
        onClick = if (isRunning) onStop else onStart,
        enabled = isRunning || pendingCount > 0,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = bgColor,
            contentColor = fgColor,
            disabledContainerColor = DettleSurface,
            disabledContentColor = DettleTextMuted
        )
    ) {
        Icon(
            if (isRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            if (isRunning) "Stop Overnight Run"
            else if (pendingCount == 0) "Add tasks to start"
            else "Start Overnight Run ($pendingCount tasks)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── Task queue card ────────────────────────────────────────────────────────

@Composable
fun TaskQueueCard(
    task: OvernightTask,
    isCurrentTask: Boolean,
    onRemove: (() -> Unit)?
) {
    val statusColor = when (task.status) {
        OvernightTaskStatus.PENDING -> DettleTextMuted
        OvernightTaskStatus.RUNNING -> DettleCyan
        OvernightTaskStatus.DONE -> DettleGreen
        OvernightTaskStatus.FAILED -> DettleRed
        OvernightTaskStatus.PARTIAL -> DettleOrange
        OvernightTaskStatus.SKIPPED -> DettleTextMuted
    }

    val priorityColor = when (task.priority) {
        TaskPriority.CRITICAL -> DettleRed
        TaskPriority.HIGH -> DettleOrange
        TaskPriority.NORMAL -> DettleCyan
        TaskPriority.LOW -> DettleTextMuted
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DettleCard)
            .border(
                1.dp,
                if (isCurrentTask) DettleCyan.copy(alpha = 0.6f) else DettleCardBorder,
                RoundedCornerShape(12.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status dot
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                // Priority badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(priorityColor.copy(alpha = 0.12f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(task.priority.name, style = MaterialTheme.typography.labelSmall, color = priorityColor)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TaskTypeBadge(task.type)
                if (task.repoKey.isNotBlank()) {
                    Text(
                        task.repoKey,
                        style = MaterialTheme.typography.labelSmall,
                        color = DettleTextMuted
                    )
                }
            }
            if (task.description.isNotBlank()) {
                Text(
                    task.description.take(80),
                    style = MaterialTheme.typography.bodySmall,
                    color = DettleTextSecondary,
                    maxLines = 2
                )
            }
        }

        if (onRemove != null) {
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Remove", tint = DettleTextMuted)
            }
        }
    }
}

@Composable
fun TaskTypeBadge(type: OvernightTaskType) {
    val (emoji, color) = when (type) {
        OvernightTaskType.CODE_FEATURE -> "✨" to DettleCyan
        OvernightTaskType.CODE_REFACTOR -> "♻️" to DettlePurple
        OvernightTaskType.CODE_FIX -> "🔧" to DettleOrange
        OvernightTaskType.WRITE_TESTS -> "🧪" to DettleGreen
        OvernightTaskType.DEPLOY -> "🚀" to DettleCyan
        OvernightTaskType.DOCS -> "📝" to DettleTextSecondary
        OvernightTaskType.RESEARCH -> "🔎" to DettleGreen
        OvernightTaskType.CUSTOM -> "⚡" to DettleTextMuted
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text("$emoji ${type.name.replace('_', ' ')}", style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
fun EmptyQueueCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DettleSurface)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🌙", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text("Queue is empty", style = MaterialTheme.typography.titleMedium, color = DettleTextSecondary)
            Text("Tap + to add tasks for tonight", style = MaterialTheme.typography.bodySmall, color = DettleTextMuted)
        }
    }
}

// ── Add task sheet ─────────────────────────────────────────────────────────

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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(DettleSurface)
                .clickable(enabled = false) {}  // Prevent dismiss on sheet tap
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Add Task", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = DettleTextMuted)
                }
            }

            DettleOutlinedField(label = "Task title", value = state.title, onValueChange = onTitleChange, singleLine = true)

            DettleOutlinedField(
                label = "Describe what the agent should do",
                value = state.description,
                onValueChange = onDescChange,
                singleLine = false,
                minLines = 3,
                maxLines = 6
            )

            DettleOutlinedField(label = "Repo (owner/repo, optional)", value = state.repoKey, onValueChange = onRepoChange, singleLine = true)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EnumDropdown(
                    label = "Type",
                    selected = state.type.name,
                    options = OvernightTaskType.values().map { it.name },
                    onSelect = { onTypeChange(OvernightTaskType.valueOf(it)) },
                    modifier = Modifier.weight(1f)
                )
                EnumDropdown(
                    label = "Priority",
                    selected = state.priority.name,
                    options = TaskPriority.values().map { it.name },
                    onSelect = { onPriorityChange(TaskPriority.valueOf(it)) },
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = { onAdd(); onDismiss() },
                enabled = state.title.isNotBlank() && state.description.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DettleCyan, contentColor = DettleDark)
            ) {
                Text("Add to Queue", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun DettleOutlinedField(
    label: String, value: String, onValueChange: (String) -> Unit,
    singleLine: Boolean = true, minLines: Int = 1, maxLines: Int = 1
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, color = DettleTextMuted) },
        singleLine = singleLine, minLines = minLines, maxLines = maxLines,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White, unfocusedTextColor = DettleTextSecondary,
            focusedBorderColor = DettleCyan, unfocusedBorderColor = DettleCardBorder,
            cursorColor = DettleCyan, focusedContainerColor = DettleSurfaceVariant,
            unfocusedContainerColor = DettleSurface
        ),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
    )
}

@Composable
fun EnumDropdown(label: String, selected: String, options: List<String>, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(DettleSurfaceVariant)
                .border(1.dp, DettleCardBorder, RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(12.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = DettleTextMuted)
            Text(selected, style = MaterialTheme.typography.bodyMedium, color = Color.White)
        }
        DropdownMenu(
            expanded = expanded, onDismissRequest = { expanded = false },
            modifier = Modifier.background(DettleCard)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = if (option == selected) DettleCyan else Color.White) },
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}

private val DettlePurple = androidx.compose.ui.graphics.Color(0xFF9B59F5)
