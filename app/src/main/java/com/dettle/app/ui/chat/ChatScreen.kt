package com.dettle.app.ui.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dettle.app.domain.model.ChatMessage
import com.dettle.app.domain.model.MessageRole
import com.dettle.app.domain.model.MessageType
import com.dettle.app.ui.theme.AiBubble
import com.dettle.app.ui.theme.AiBubbleBorder
import com.dettle.app.ui.theme.ApprovalBg
import com.dettle.app.ui.theme.ApprovalBorder
import com.dettle.app.ui.theme.DettleCyan
import com.dettle.app.ui.theme.DettleDark
import com.dettle.app.ui.theme.DettleGreen
import com.dettle.app.ui.theme.DettleOrange
import com.dettle.app.ui.theme.DettlePurple
import com.dettle.app.ui.theme.DettleRed
import com.dettle.app.ui.theme.DettleSurface
import com.dettle.app.ui.theme.DettleTextMuted
import com.dettle.app.ui.theme.DettleTextSecondary
import com.dettle.app.ui.theme.ToolCallBg
import com.dettle.app.ui.theme.ToolCallBorder
import com.dettle.app.ui.theme.UserBubble
import com.dettle.app.ui.theme.UserBubbleBorder

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val allModes by viewModel.allModes.collectAsState()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var customizingMode by remember { mutableStateOf<com.dettle.app.orchestrator.mode.AgentMode?>(null) }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DettleDark)
    ) {
        // ── Top Bar ──────────────────────────────────────────────────────
        ChatTopBar(
            isAgentRunning = uiState.isAgentRunning,
            step = uiState.thinkingStep,
            maxSteps = uiState.maxSteps,
            onClear = viewModel::clearChat
        )

        // ── Mode Pill Bar ─────────────────────────────────────────────────
        com.dettle.app.ui.mode.ModePillBar(
            modes = allModes,
            activeModeId = uiState.activeModeId,
            lockedModeId = uiState.lockedModeId,
            onModeTap = viewModel::toggleModelock,
            onModeSettings = { mode -> customizingMode = mode }
        )

        // ── Message List ─────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.messages.isEmpty()) {
                item { WelcomeCard() }
            }

            items(uiState.messages, key = { it.id }) { message ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { it / 2 }
                ) {
                    MessageItem(
                        message = message,
                        onApprove = viewModel::approveAction,
                        onReject = viewModel::rejectAction
                    )
                }
            }

            // GOAL mode: live gate checklist
            if (uiState.goalGates.isNotEmpty()) {
                item {
                    val activeMode = allModes.firstOrNull { it.id == uiState.activeModeId }
                    val gates = activeMode?.effectiveConfig?.completionGates ?: emptyList()
                    com.dettle.app.ui.mode.GoalProgressCard(
                        gates = gates,
                        progress = uiState.goalGates,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // Thinking indicator
            if (uiState.isAgentRunning && uiState.thinkingStep > 0) {
                item { ThinkingIndicator(step = uiState.thinkingStep, maxSteps = uiState.maxSteps) }
            }
        }

        // ── Input Bar ────────────────────────────────────────────────────
        ChatInputBar(
            value = inputText,
            enabled = uiState.inputEnabled,
            isRunning = uiState.isAgentRunning,
            onValueChange = { inputText = it },
            onSend = {
                if (inputText.isNotBlank()) {
                    viewModel.sendMessage(inputText.trim())
                    inputText = ""
                }
            }
        )
    }

    // ── Mode customization bottom sheet ───────────────────────────────────
    customizingMode?.let { mode ->
        com.dettle.app.ui.mode.ModeCustomizationSheet(
            mode = mode,
            onSave = { config -> viewModel.saveModeConfig(mode.id, config) },
            onReset = { viewModel.resetModeToDefault(mode.id) },
            onDismiss = { customizingMode = null }
        )
    }
}


// ─── Top Bar ───────────────────────────────────────────────────────────────

@Composable
fun ChatTopBar(
    isAgentRunning: Boolean,
    step: Int,
    maxSteps: Int,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DettleSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo / name
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(DettleCyan, DettlePurple))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Psychology,
                contentDescription = null,
                tint = DettleDark,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Dettle",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (isAgentRunning && step > 0) {
                Text(
                    "Step $step/$maxSteps",
                    style = MaterialTheme.typography.labelSmall,
                    color = DettleCyan
                )
            } else {
                Text(
                    "Autonomous AI Agent",
                    style = MaterialTheme.typography.labelSmall,
                    color = DettleTextMuted
                )
            }
        }

        if (isAgentRunning) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = DettleCyan,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(8.dp))
        }

        IconButton(onClick = onClear) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Options", tint = DettleTextSecondary)
        }
    }
    HorizontalDivider(color = DettleSurface, thickness = 1.dp)
}

// ─── Welcome Card ──────────────────────────────────────────────────────────

@Composable
fun WelcomeCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(DettleCyan.copy(alpha = 0.3f), DettlePurple.copy(alpha = 0.1f)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Memory, contentDescription = null, tint = DettleCyan, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("Dettle is ready", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Your autonomous AI developer agent.\nAsk me to read repos, write code, create PRs, or deploy to Cloudflare.",
            style = MaterialTheme.typography.bodyMedium,
            color = DettleTextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        // Quick prompts
        listOf(
            "🔍 Map my GitHub repo",
            "🐛 Fix a bug and open a PR",
            "🚀 Deploy to Cloudflare",
            "📋 Review my latest PR"
        ).forEach { prompt ->
            QuickPromptChip(prompt)
        }
    }
}

@Composable
fun QuickPromptChip(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, DettleSurface.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
            .background(DettleSurface.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = DettleTextSecondary)
    }
}

// ─── Message Items ─────────────────────────────────────────────────────────

@Composable
fun MessageItem(
    message: ChatMessage,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    when (message.type) {
        MessageType.TEXT -> TextMessageBubble(message)
        MessageType.TOOL_CALL -> ToolCallCard(message)
        MessageType.TOOL_RESULT -> ToolResultCard(message)
        MessageType.APPROVAL -> ApprovalCard(message, onApprove, onReject)
        MessageType.SYSTEM, MessageType.ERROR -> SystemMessage(message)
        MessageType.POLICY_BLOCKED -> PolicyBlockedCard(message)
        MessageType.POLICY_WARNING -> PolicyWarningBadge(message)
    }
}

@Composable
fun TextMessageBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // AI avatar
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(DettleCyan, DettlePurple))),
                contentAlignment = Alignment.Center
            ) {
                Text("D", fontSize = 12.sp, color = DettleDark, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(modifier = Modifier.widthIn(max = 320.dp)) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = if (isUser) 16.dp else 4.dp,
                            topEnd = if (isUser) 4.dp else 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        )
                    )
                    .background(if (isUser) UserBubble else AiBubble)
                    .border(
                        1.dp,
                        if (isUser) UserBubbleBorder else AiBubbleBorder,
                        RoundedCornerShape(
                            topStart = if (isUser) 16.dp else 4.dp,
                            topEnd = if (isUser) 4.dp else 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        )
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                if (message.isStreaming) {
                    StreamingCursor()
                }
            }

            // Provider tag
            if (message.providerName != null) {
                Text(
                    message.providerName,
                    style = MaterialTheme.typography.labelSmall,
                    color = DettleTextMuted,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun StreamingCursor() {
    val transition = rememberInfiniteTransition(label = "cursor")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "blink"
    )
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(16.dp)
            .alpha(alpha)
            .background(DettleCyan)
    )
}

@Composable
fun ToolCallCard(message: ChatMessage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ToolCallBg),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .border(1.dp, ToolCallBorder, RoundedCornerShape(10.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Terminal, contentDescription = null, tint = DettleCyan, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    "Tool Call",
                    style = MaterialTheme.typography.labelSmall,
                    color = DettleCyan,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    message.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = DettleTextSecondary,
                    fontFamily = FontFamily.Monospace
                )
                // Show arguments
                message.toolCall?.arguments?.entries?.forEach { (k, v) ->
                    Text(
                        "$k: ${v.take(60)}${if (v.length > 60) "..." else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = DettleTextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = DettleCyan, strokeWidth = 1.5.dp)
        }
    }
}

@Composable
fun ToolResultCard(message: ChatMessage) {
    val isError = message.toolResult?.isError == true
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ToolCallBg),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, if (isError) DettleRed.copy(alpha = 0.4f) else DettleGreen.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isError) Icons.Filled.Clear else Icons.Filled.Code,
                    contentDescription = null,
                    tint = if (isError) DettleRed else DettleGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (isError) "Tool Error" else "Tool Result: ${message.toolResult?.toolName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isError) DettleRed else DettleGreen,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                message.content.take(500) + if (message.content.length > 500) "\n...(truncated)" else "",
                style = MaterialTheme.typography.bodySmall,
                color = DettleTextSecondary,
                fontFamily = if (message.content.startsWith("{") || message.content.startsWith("[")) FontFamily.Monospace else FontFamily.Default
            )
        }
    }
}

@Composable
fun ApprovalCard(
    message: ChatMessage,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val request = message.approvalRequest ?: return
    val isPending = request.status == com.dettle.app.domain.model.ApprovalStatus.PENDING

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ApprovalBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, ApprovalBorder, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⚡", fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Approval Required",
                    style = MaterialTheme.typography.titleMedium,
                    color = DettleOrange,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(request.title, style = MaterialTheme.typography.bodyMedium, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(request.description, style = MaterialTheme.typography.bodySmall, color = DettleTextSecondary)

            if (request.details.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = ApprovalBorder)
                Spacer(Modifier.height(8.dp))
                request.details.entries.take(4).forEach { (k, v) ->
                    Row {
                        Text("$k: ", style = MaterialTheme.typography.labelSmall, color = DettleTextMuted, fontFamily = FontFamily.Monospace)
                        Text(v.take(80), style = MaterialTheme.typography.labelSmall, color = DettleTextSecondary, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            if (isPending) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledIconButton(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = DettleGreen.copy(alpha = 0.2f))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = DettleGreen, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Approve", color = DettleGreen, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    FilledIconButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = DettleRed.copy(alpha = 0.2f))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Filled.Clear, contentDescription = null, tint = DettleRed, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reject", color = DettleRed, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                val statusText = if (request.status == com.dettle.app.domain.model.ApprovalStatus.APPROVED)
                    "✅ Approved" else "🚫 Rejected"
                Text(statusText, style = MaterialTheme.typography.labelMedium, color = DettleTextSecondary)
            }
        }
    }
}

@Composable
fun SystemMessage(message: ChatMessage) {
    val isError = message.type == MessageType.ERROR
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background((if (isError) DettleRed else DettleTextMuted).copy(alpha = 0.1f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                message.content,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) DettleRed else DettleTextMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// ─── Policy Cards ──────────────────────────────────────────────────────────

/**
 * Hard-block card — shown when PolicyEngine stops a tool call cold.
 *
 * Tells the user (and the AI, via conversation history) exactly WHAT rule
 * was violated and HOW to fix it. The agent loop continues so the AI can
 * try a different approach.
 */
@Composable
fun PolicyBlockedCard(message: ChatMessage) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = DettleRed.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, DettleRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🛡️", fontSize = 15.sp)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Policy Blocked",
                        style = MaterialTheme.typography.labelLarge,
                        color = DettleRed,
                        fontWeight = FontWeight.Bold
                    )
                    if (message.policyId != null) {
                        Text(
                            "rule: ${message.policyId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = DettleRed.copy(alpha = 0.6f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Text(
                    if (expanded) "▾" else "▸",
                    color = DettleRed.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = DettleRed.copy(alpha = 0.2f))
                    Spacer(Modifier.height(10.dp))

                    // Reason
                    Text(
                        "WHY",
                        style = MaterialTheme.typography.labelSmall,
                        color = DettleTextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        message.policyReason ?: message.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    // Fix
                    if (message.policyFix != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "FIX",
                            style = MaterialTheme.typography.labelSmall,
                            color = DettleTextMuted,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(DettleGreen.copy(alpha = 0.07f))
                                .padding(10.dp)
                        ) {
                            Text(
                                message.policyFix,
                                style = MaterialTheme.typography.bodySmall,
                                color = DettleGreen.copy(alpha = 0.9f)
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Text(
                        "The agent will automatically try an alternative approach.",
                        style = MaterialTheme.typography.labelSmall,
                        color = DettleTextMuted
                    )
                }
            }
        }
    }
}

/**
 * Warning badge — shown when PolicyEngine flagged something advisory
 * but the tool still ran. Subtle, collapsible.
 */
@Composable
fun PolicyWarningBadge(message: ChatMessage) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(DettleOrange.copy(alpha = 0.1f))
                .border(1.dp, DettleOrange.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚠️", fontSize = 12.sp)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "Policy Warning",
                        style = MaterialTheme.typography.labelSmall,
                        color = DettleOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        if (expanded) "▾" else "▸",
                        color = DettleOrange.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                }
                AnimatedVisibility(visible = expanded) {
                    Text(
                        message.content,
                        style = MaterialTheme.typography.labelSmall,
                        color = DettleOrange.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun ThinkingIndicator(step: Int, maxSteps: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val transition = rememberInfiniteTransition(label = "thinking")
        val alpha by transition.animateFloat(1f, 0.3f,
            infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "pulse")


        repeat(3) { i ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(6.dp)
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(DettleCyan)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "Thinking... Step $step/$maxSteps",
            style = MaterialTheme.typography.labelSmall,
            color = DettleTextMuted
        )
    }
}

// ─── Input Bar ─────────────────────────────────────────────────────────────

@Composable
fun ChatInputBar(
    value: String,
    enabled: Boolean,
    isRunning: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DettleSurface)
            .imePadding()
            .navigationBarsPadding()
    ) {
        HorizontalDivider(color = DettleSurface, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        if (!enabled && isRunning) "Agent is working..." else "Ask anything...",
                        color = DettleTextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DettleCyan.copy(alpha = 0.6f),
                    unfocusedBorderColor = DettleSurface,
                    focusedContainerColor = DettleSurface,
                    unfocusedContainerColor = DettleSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    disabledTextColor = DettleTextMuted,
                    disabledBorderColor = DettleSurface
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                maxLines = 6,
                textStyle = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.width(8.dp))

            // Send button
            FilledIconButton(
                onClick = onSend,
                enabled = enabled && value.isNotBlank(),
                modifier = Modifier.size(46.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = DettleCyan,
                    contentColor = DettleDark,
                    disabledContainerColor = DettleSurface
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
