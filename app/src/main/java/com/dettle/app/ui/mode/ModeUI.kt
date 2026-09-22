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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.dettle.app.ui.theme.DettleCyan
import com.dettle.app.ui.theme.DettleDark
import com.dettle.app.ui.theme.DettleGreen
import com.dettle.app.ui.theme.DettleOrange
import com.dettle.app.ui.theme.DettleRed
import com.dettle.app.ui.theme.DettleSurface
import com.dettle.app.ui.theme.DettleTextMuted
import com.dettle.app.ui.theme.DettleTextSecondary

// ─── Mode Pill Bar ────────────────────────────────────────────────────────────

/**
 * Horizontal scrollable row of mode pills.
 *
 * - Tap to lock/unlock a mode (locked = auto-classification disabled)
 * - Settings icon on each pill → opens [ModeCustomizationSheet]
 * - Active mode is highlighted with its accent color
 * - Locked mode shows a 🔒 indicator
 */
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
            .padding(horizontal = 12.dp, vertical = 6.dp),
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
    val accentColor = mode.accentColor
    val bgColor by animateColorAsState(
        targetValue = if (isActive) accentColor.copy(alpha = 0.18f) else DettleSurface.copy(alpha = 0.5f),
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pillBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isActive) accentColor.copy(alpha = 0.7f) else DettleSurface,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pillBorder"
    )
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.04f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pillScale"
    )

    Row(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onTap)
            .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(mode.emoji, fontSize = 14.sp)
        Spacer(Modifier.width(5.dp))
        Text(
            mode.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = if (isActive) accentColor else DettleTextSecondary,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
        )
        if (isLocked) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = accentColor, modifier = Modifier.size(11.dp))
        }
        // Settings tap target (small, unobtrusive)
        IconButton(onClick = onSettings, modifier = Modifier.size(24.dp)) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = "Customize ${mode.displayName}",
                tint = if (isActive) accentColor.copy(alpha = 0.6f) else DettleTextMuted,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

// ─── Goal Progress Card ───────────────────────────────────────────────────────

/**
 * Live checklist shown during GOAL mode execution.
 * Each gate flips from ⏳ → ✅ / ❌ as the agent reports progress.
 */
@Composable
fun GoalProgressCard(
    gates: List<String>,
    progress: Map<String, Boolean>,
    modifier: Modifier = Modifier
) {
    if (gates.isEmpty()) return
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DettleSurface.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, DettleCyan.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎯", fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    "Goal Progress",
                    style = MaterialTheme.typography.labelLarge,
                    color = DettleCyan,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                val passed = progress.values.count { it }
                Text(
                    "$passed / ${gates.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = DettleTextMuted
                )
            }
            Spacer(Modifier.height(8.dp))
            gates.forEach { gate ->
                val status = progress[gate]
                val icon = when (status) {
                    true  -> "✅"
                    false -> "❌"
                    null  -> "⏳"
                }
                val color = when (status) {
                    true  -> DettleGreen
                    false -> DettleRed
                    null  -> DettleTextMuted
                }
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(icon, fontSize = 13.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        gate.replace('_', ' '),
                        style = MaterialTheme.typography.bodySmall,
                        color = color,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// ─── Mode Customization Sheet ─────────────────────────────────────────────────

/**
 * Bottom sheet for customizing a single agent mode.
 *
 * Sections:
 * 1. Header — mode name, description, reset button
 * 2. Model Waterfall — add/remove/reorder models
 * 3. Tools — toggle each tool on/off
 * 4. Step Budget — slider 2-100
 * 5. Custom Instructions — freeform text appended to system prompt
 * 6. Save / Reset buttons
 */
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

    // Editable state
    val modelWaterfall = remember { config.modelWaterfall.toMutableStateList() }
    val enabledTools = remember { config.enabledTools.toMutableStateList() }
    var maxSteps by remember { mutableStateOf(config.maxSteps.toFloat()) }
    var customInstructions by remember { mutableStateOf(config.customInstructions) }
    var showModelPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DettleDark,
        tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle(color = DettleTextMuted) }
    ) {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header ──
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(mode.emoji, fontSize = 24.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Customize ${mode.displayName}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            mode.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = DettleTextMuted
                        )
                    }
                    IconButton(onClick = {
                        onReset()
                        onDismiss()
                    }) {
                        Icon(Icons.Filled.Refresh, "Reset to defaults", tint = DettleOrange)
                    }
                }
                HorizontalDivider(color = DettleSurface, modifier = Modifier.padding(top = 12.dp))
            }

            // ── Model Waterfall ──
            item {
                SectionHeader("🤖 Model Waterfall", "Tried in order — fails over to next")
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
                        .border(1.dp, DettleSurface, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Filled.Add, null, tint = DettleCyan, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Model", color = DettleCyan, style = MaterialTheme.typography.labelMedium)
                }
            }

            // ── Tools ──
            item {
                HorizontalDivider(color = DettleSurface)
                Spacer(Modifier.height(4.dp))
                SectionHeader("🔧 Tools", "Toggle which tools this mode can use")
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
                        color = if (enabled) Color.White else DettleTextMuted,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = enabled,
                        onCheckedChange = { on ->
                            if (on) enabledTools.add(toolName) else enabledTools.remove(toolName)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = mode.accentColor,
                            checkedTrackColor = mode.accentColor.copy(alpha = 0.3f),
                            uncheckedThumbColor = DettleTextMuted,
                            uncheckedTrackColor = DettleSurface
                        )
                    )
                }
            }

            // ── Step Budget ──
            item {
                HorizontalDivider(color = DettleSurface)
                Spacer(Modifier.height(4.dp))
                SectionHeader("⚡ Step Budget", "Max ReAct loop iterations (${maxSteps.toInt()} steps)")
                Slider(
                    value = maxSteps,
                    onValueChange = { maxSteps = it },
                    valueRange = 2f..100f,
                    steps = 49,
                    colors = SliderDefaults.colors(
                        thumbColor = mode.accentColor,
                        activeTrackColor = mode.accentColor
                    )
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("2 (fast)", style = MaterialTheme.typography.labelSmall, color = DettleTextMuted)
                    Text("100 (overnight)", style = MaterialTheme.typography.labelSmall, color = DettleTextMuted)
                }
            }

            // ── Custom Instructions ──
            item {
                HorizontalDivider(color = DettleSurface)
                Spacer(Modifier.height(4.dp))
                SectionHeader("📝 Custom Instructions", "Appended to the system prompt for this mode")
                OutlinedTextField(
                    value = customInstructions,
                    onValueChange = { customInstructions = it },
                    modifier = Modifier.fillMaxWidth().height(130.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                    placeholder = {
                        Text(
                            "e.g. 'Always write tests first. Never use var.'",
                            style = MaterialTheme.typography.bodySmall,
                            color = DettleTextMuted,
                            fontStyle = FontStyle.Italic
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = mode.accentColor,
                        unfocusedBorderColor = DettleSurface,
                        cursorColor = mode.accentColor
                    ),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    maxLines = 6
                )
            }

            // ── Save ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                            .border(1.dp, DettleSurface, RoundedCornerShape(10.dp))
                    ) {
                        Text("Cancel", color = DettleTextMuted)
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
                        modifier = Modifier.weight(2f)
                            .background(mode.accentColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                            .border(1.dp, mode.accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Filled.Check, null, tint = mode.accentColor, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save Changes", color = mode.accentColor, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // ── Model Picker overlay ──
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DettleSurface.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.DragHandle, null, tint = DettleTextMuted, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(displayName, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Medium)
            if (bestFor.isNotBlank()) {
                Text(bestFor.take(60), style = MaterialTheme.typography.labelSmall, color = DettleTextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (contextWindow > 0) {
            Text(
                "${contextWindow / 1000}k ctx",
                style = MaterialTheme.typography.labelSmall,
                color = DettleCyan.copy(alpha = 0.7f),
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.Close, "Remove model", tint = DettleRed.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
        }
    }
}

// ─── Model Picker Sheet ───────────────────────────────────────────────────────

/**
 * Full model catalog picker — shows all known free models the user can add to a waterfall.
 * Already-added models show a checkmark.
 */
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
        containerColor = DettleDark,
        tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle(color = DettleTextMuted) }
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                "Add Model",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                items(ALL_KNOWN_MODELS.values.toList()) { model ->
                    val alreadyAdded = model.modelId in currentWaterfall
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DettleSurface.copy(alpha = 0.6f))
                            .border(
                                1.dp,
                                if (alreadyAdded) DettleCyan.copy(alpha = 0.3f) else DettleSurface,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable(enabled = !alreadyAdded) { onModelSelected(model.modelId) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(model.displayName, style = MaterialTheme.typography.bodySmall, color = if (alreadyAdded) DettleTextMuted else Color.White, fontWeight = FontWeight.Medium)
                            Text(
                                model.bestFor.take(70),
                                style = MaterialTheme.typography.labelSmall,
                                color = DettleTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("${model.contextWindow / 1000}k ctx", style = MaterialTheme.typography.labelSmall, color = DettleCyan.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace)
                                if (model.dailyRequestLimit > 0) {
                                    Text("${model.dailyRequestLimit} req/day", style = MaterialTheme.typography.labelSmall, color = DettleTextMuted, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                        if (alreadyAdded) {
                            Icon(Icons.Filled.Check, "Already added", tint = DettleCyan, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Filled.Add, "Add", tint = DettleTextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = DettleTextMuted)
    }
}
