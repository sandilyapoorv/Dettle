package com.dettle.app.ui.mode

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dettle.app.domain.model.ALL_KNOWN_MODELS
import com.dettle.app.domain.model.AgentTools
import com.dettle.app.orchestrator.mode.AgentMode
import com.dettle.app.orchestrator.mode.ModeConfig
import com.dettle.app.orchestrator.mode.ModeId
import com.dettle.app.ui.theme.DettleGreen
import com.dettle.app.ui.theme.DettleOrange
import com.dettle.app.ui.theme.DettleRed

// ─── Mode Icon Mapper ─────────────────────────────────────────────────────────

fun getModeVectorIcon(id: ModeId): ImageVector = when (id) {
    ModeId.CHAT -> Icons.Outlined.ChatBubbleOutline
    ModeId.RESEARCH -> Icons.Outlined.Search
    ModeId.CODE -> Icons.Outlined.Terminal
    ModeId.PLAN -> Icons.Outlined.Assignment
    ModeId.GOAL -> Icons.Outlined.TrackChanges
    ModeId.WEB -> Icons.Outlined.Public
    ModeId.REVIEW -> Icons.Outlined.RateReview
    ModeId.DEPLOY -> Icons.Outlined.CloudUpload
}

// ─── Mode Pill Bar ────────────────────────────────────────────────────────────

@Composable
fun ModePillBar(
    modes: List<AgentMode>,
    activeModeId: ModeId,
    lockedModeId: ModeId?,
    onModeTap: (ModeId) -> Unit,
    onModeSettings: (AgentMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        modes.forEach { mode ->
            ModePill(
                mode = mode,
                isActive = mode.id == activeModeId,
                isLocked = mode.id == lockedModeId,
                onTap = { onModeTap(mode.id) },
                onSettings = { onModeSettings(mode) }
            )
        }
    }
}

@Composable
fun ModePill(
    mode: AgentMode,
    isActive: Boolean,
    isLocked: Boolean,
    onTap: () -> Unit,
    onSettings: () -> Unit
) {
    val containerColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }

    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onTap,
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        border = if (isActive) {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getModeVectorIcon(mode.id),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                mode.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium
            )
            if (isLocked) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Locked",
                    tint = contentColor,
                    modifier = Modifier.size(12.dp)
                )
            }
            IconButton(onClick = onSettings, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = "Customize ${mode.displayName}",
                    tint = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

// ─── Goal Progress Card ───────────────────────────────────────────────────────

@Composable
fun GoalProgressCard(
    gates: List<String>,
    progress: Map<String, Boolean>,
    modifier: Modifier = Modifier
) {
    if (gates.isEmpty()) return
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.TrackChanges,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Goal Progress",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                val passed = progress.values.count { it }
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        "$passed / ${gates.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            gates.forEach { gate ->
                val status = progress[gate]
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (status) {
                        true -> Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = "Passed",
                            tint = DettleGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        false -> Icon(
                            Icons.Outlined.Cancel,
                            contentDescription = "Failed",
                            tint = DettleRed,
                            modifier = Modifier.size(16.dp)
                        )
                        null -> CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            strokeWidth = 1.5.dp
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        gate,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (status == true) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─── Mode Customization Sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeCustomizationSheet(
    mode: AgentMode,
    onSave: (ModeConfig) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val config = mode.effectiveConfig

    val modelWaterfall = remember { config.modelWaterfall.toMutableStateList() }
    val enabledTools = remember { config.enabledTools.toMutableStateList() }
    var maxSteps by remember { mutableStateOf(config.maxSteps.toFloat()) }
    var customInstructions by remember { mutableStateOf(config.customInstructions) }
    var showModelPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant) }
    ) {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                getModeVectorIcon(mode.id),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Customize ${mode.displayName}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            mode.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        onReset()
                        onDismiss()
                    }) {
                        Icon(Icons.Filled.Refresh, "Reset to defaults", tint = DettleOrange)
                    }
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            // Model Waterfall
            item {
                SectionHeader(Icons.Outlined.SmartToy, "Model Waterfall", "Tried in order — fails over to next")
            }
            items(modelWaterfall, key = { it }) { modelId ->
                val model = ALL_KNOWN_MODELS[modelId]
                ModelWaterfallRow(
                    modelId = modelId,
                    displayName = model?.displayName ?: modelId,
                    bestFor = model?.bestFor ?: "",
                    contextWindow = model?.contextWindow ?: 0,
                    onRemove = { modelWaterfall.remove(modelId) }
                )
            }
            item {
                TextButton(
                    onClick = { showModelPicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                ) {
                    Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Model", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                }
            }

            // Tools
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(4.dp))
                SectionHeader(Icons.Outlined.Build, "Tools", "Toggle which tools this mode can use")
            }
            items(AgentTools.ALL.map { it.name }) { toolName ->
                val enabled = toolName in enabledTools
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (enabled) enabledTools.remove(toolName)
                            else enabledTools.add(toolName)
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        toolName,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = enabled,
                        onCheckedChange = { on ->
                            if (on) enabledTools.add(toolName) else enabledTools.remove(toolName)
                        }
                    )
                }
            }

            // Step Budget
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(4.dp))
                SectionHeader(Icons.Outlined.Speed, "Step Budget", "Max ReAct loop iterations (${maxSteps.toInt()} steps)")
                Slider(
                    value = maxSteps,
                    onValueChange = { maxSteps = it },
                    valueRange = 2f..100f,
                    steps = 49
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("2 (fast)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("100 (deep)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Custom Instructions
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(4.dp))
                SectionHeader(Icons.Outlined.Description, "Custom Instructions", "Appended to system prompt")
                OutlinedTextField(
                    value = customInstructions,
                    onValueChange = { customInstructions = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = MaterialTheme.shapes.small,
                    textStyle = MaterialTheme.typography.bodySmall,
                    placeholder = {
                        Text(
                            "e.g. 'Always write tests first. Never use var.'",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontStyle = FontStyle.Italic
                        )
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    maxLines = 6
                )
            }

            // Save actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(
                        onClick = {
                            onSave(
                                config.copy(
                                    modelWaterfall = modelWaterfall.toList(),
                                    enabledTools = enabledTools.toList(),
                                    maxSteps = maxSteps.toInt(),
                                    customInstructions = customInstructions
                                )
                            )
                            onDismiss()
                        },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .weight(2f)
                            .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                    ) {
                        Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save Changes", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showModelPicker) {
        ModelPickerSheet(
            currentWaterfall = modelWaterfall.toList(),
            onModelSelected = { modelId ->
                if (modelId !in modelWaterfall) modelWaterfall.add(modelId)
                showModelPicker = false
            },
            onDismiss = { showModelPicker = false }
        )
    }
}

// ─── Model Waterfall Row ──────────────────────────────────────────────────────

@Composable
fun ModelWaterfallRow(
    modelId: String,
    displayName: String,
    bestFor: String,
    contextWindow: Int,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.DragHandle, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                if (bestFor.isNotBlank()) {
                    Text(
                        bestFor.take(60),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (contextWindow > 0) {
                Text(
                    "${contextWindow / 1000}k ctx",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Close, "Remove model", tint = DettleRed, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ─── Model Picker Sheet ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelPickerSheet(
    currentWaterfall: List<String>,
    onModelSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant) }
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                "Add Model",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                items(ALL_KNOWN_MODELS.values.toList()) { model ->
                    val alreadyAdded = model.modelId in currentWaterfall
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        color = if (alreadyAdded) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (alreadyAdded) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant
                        ),
                        onClick = { if (!alreadyAdded) onModelSelected(model.modelId) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    model.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (alreadyAdded) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    model.bestFor.take(70),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        "${model.contextWindow / 1000}k ctx",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    if (model.dailyRequestLimit > 0) {
                                        Text(
                                            "${model.dailyRequestLimit} req/day",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                            if (alreadyAdded) {
                                Icon(Icons.Filled.Check, "Already added", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            } else {
                                Icon(Icons.Filled.Add, "Add", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
fun SectionHeader(icon: ImageVector, title: String, subtitle: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
