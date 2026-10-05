package com.dettle.app.ui.chat

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Key
import com.dettle.app.ui.onboarding.OnboardingSheet
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.dettle.app.orchestrator.mode.ALL_SLASH_COMMANDS
import com.dettle.app.orchestrator.mode.EnvironmentMode
import com.dettle.app.orchestrator.mode.ModeId
import com.dettle.app.ui.mode.getModeVectorIcon
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.dettle.app.ui.theme.liquidGlass
import com.dettle.app.ui.theme.LiquidGlassTokens
import com.dettle.app.ui.theme.UserBubble
import com.dettle.app.ui.theme.AiBubble
import com.dettle.app.ui.theme.UserBubbleBorder
import com.dettle.app.ui.theme.AiBubbleBorder
import com.dettle.app.audio.VoiceTypingManager
import com.dettle.app.domain.model.ChatMessage
import com.dettle.app.domain.model.MessageRole
import com.dettle.app.domain.model.MessageType
import com.dettle.app.ui.mode.AppleModePillToggle
import com.dettle.app.ui.mode.GoalProgressCard
import com.dettle.app.ui.mode.ModeCustomizationSheet
import com.dettle.app.ui.mode.ModePillBar
import com.dettle.app.ui.mode.TopEnvironmentSegmentedControl
import androidx.compose.runtime.produceState
import com.dettle.app.domain.model.ExecutionTrace
import com.dettle.app.domain.model.TraceStep
import com.dettle.app.domain.model.TraceStatus
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.dettle.app.ui.theme.DettleGreen
import com.dettle.app.ui.theme.DettleOrange
import com.dettle.app.ui.theme.DettleRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val allModes by viewModel.allModes.collectAsState()
    val recentConversations by viewModel.recentConversations.collectAsState()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var attachments by remember { mutableStateOf<List<ChatAttachment>>(emptyList()) }
    var showOnboardingSheet by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val newAttachments = uris.map { uri ->
                AttachmentHelper.resolveAttachment(context, uri)
            }
            attachments = attachments + newAttachments
        }
    }

    var customizingMode by remember { mutableStateOf<com.dettle.app.orchestrator.mode.AgentMode?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(drawerState.isOpen, drawerState.isAnimationRunning) {
        if (drawerState.isOpen || drawerState.targetValue == DrawerValue.Open) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(Unit) {
        delay(1000)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    // Voice Typing state
    val voiceTypingManager = remember { VoiceTypingManager(context.applicationContext) }
    val isVoiceListening by voiceTypingManager.isListening.collectAsState()
    var baseTextBeforeVoice by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onDispose {
            voiceTypingManager.stopListening()
        }
    }

    val systemSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                inputText = if (inputText.isBlank()) spokenText else "$inputText $spokenText"
            }
        }
    }

    fun startListeningInternal() {
        baseTextBeforeVoice = inputText
        voiceTypingManager.startListening(
            onPartial = { partial ->
                inputText = if (baseTextBeforeVoice.isBlank()) partial else "$baseTextBeforeVoice $partial"
            },
            onFinal = { finalResult ->
                inputText = if (baseTextBeforeVoice.isBlank()) finalResult else "$baseTextBeforeVoice $finalResult"
            },
            onError = { error ->
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            },
            onFallbackToSystemPrompt = {
                try {
                    systemSpeechLauncher.launch(voiceTypingManager.createSystemSpeechIntent())
                } catch (e: Exception) {
                    Toast.makeText(context, "Voice input is not available on this device", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListeningInternal()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice typing", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleMicClick() {
        if (isVoiceListening) {
            voiceTypingManager.stopListening()
        } else {
            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                startListeningInternal()
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val onApprove = remember(viewModel) { { viewModel.approveAction() } }
    val onReject = remember(viewModel) { { viewModel.rejectAction() } }
    val onPromptSelected = remember(viewModel) { { prompt: String -> viewModel.sendMessage(prompt) } }
    val onModeTap = remember(viewModel) { { id: com.dettle.app.orchestrator.mode.ModeId -> viewModel.toggleModelock(id) } }
    val onModeSettings = remember { { mode: com.dettle.app.orchestrator.mode.AgentMode -> customizingMode = mode } }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatHistoryDrawer(
                conversations = recentConversations,
                activeConversationId = uiState.activeConversationId,
                isUnleashed = uiState.isUnleashed,
                onToggleUnleashed = { viewModel.toggleUnleashed() },
                onSelectConversation = { convId ->
                    viewModel.switchConversation(convId)
                    coroutineScope.launch { drawerState.close() }
                },
                onNewChat = {
                    viewModel.startNewChat()
                    coroutineScope.launch { drawerState.close() }
                },
                onDeleteConversation = { convId ->
                    viewModel.deleteConversation(convId)
                },

                onOpenSettings = {
                    coroutineScope.launch { drawerState.close() }
                    onNavigateToSettings()
                },
                onClose = {
                    coroutineScope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    if (inputText.startsWith("/")) {
                        SlashCommandPopup(
                            query = inputText,
                            currentEnvironment = uiState.environmentMode,
                            onSelectCommand = { cmd ->
                                viewModel.selectSlashCommand(cmd)
                                val remainder = inputText.removePrefix(cmd.command).trim()
                                inputText = remainder
                            }
                        )
                    }
                    ChatInputBar(
                        value = inputText,
                        enabled = uiState.inputEnabled,
                        isRunning = uiState.isAgentRunning,
                        isListening = isVoiceListening,
                        voiceRmsFlow = voiceTypingManager.rmsDb,
                        focusRequester = focusRequester,
                        activeChatMode = uiState.activeChatMode,
                        onModeSelect = viewModel::selectChatMode,
                        attachments = attachments,
                        onAddAttachmentClick = { filePickerLauncher.launch("*/*") },
                        onRemoveAttachment = { att -> attachments = attachments.filter { it.id != att.id } },
                        onValueChange = { inputText = it },
                        onSend = {
                            val trimmed = inputText.trim()
                            if (trimmed.isNotBlank() || attachments.isNotEmpty()) {
                                val fullText = AttachmentHelper.formatAttachmentsForPrompt(trimmed, attachments)
                                val matchingCmd = ALL_SLASH_COMMANDS.firstOrNull {
                                    trimmed.startsWith("${it.command} ", ignoreCase = true) || trimmed.equals(it.command, ignoreCase = true)
                                }
                                if (matchingCmd != null) {
                                    viewModel.selectSlashCommand(matchingCmd)
                                    val queryText = trimmed.removePrefix(matchingCmd.command).trim()
                                    val promptToSend = AttachmentHelper.formatAttachmentsForPrompt(queryText, attachments)
                                    if (promptToSend.isNotBlank()) {
                                        viewModel.sendMessage(promptToSend)
                                    }
                                } else {
                                    viewModel.sendMessage(fullText)
                                }
                                inputText = ""
                                attachments = emptyList()
                            }
                        },
                        onMicClick = { handleMicClick() },
                        onStopVoiceClick = { voiceTypingManager.stopListening() },
                        onCancelVoiceClick = {
                            voiceTypingManager.stopListening()
                            inputText = baseTextBeforeVoice
                        }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AnimatedContent(
                    targetState = uiState.environmentMode,
                    transitionSpec = {
                        if (targetState == EnvironmentMode.BUILD) {
                            (slideInHorizontally(
                                initialOffsetX = { (it * 0.35f).toInt() },
                                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                            ) + fadeIn(animationSpec = tween(220)))
                                .togetherWith(
                                    slideOutHorizontally(
                                        targetOffsetX = { -(it * 0.35f).toInt() },
                                        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                                    ) + fadeOut(animationSpec = tween(180))
                                )
                        } else {
                            (slideInHorizontally(
                                initialOffsetX = { -(it * 0.35f).toInt() },
                                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                            ) + fadeIn(animationSpec = tween(220)))
                                .togetherWith(
                                    slideOutHorizontally(
                                        targetOffsetX = { (it * 0.35f).toInt() },
                                        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                                    ) + fadeOut(animationSpec = tween(180))
                                )
                        }
                    },
                    label = "ChatBuildModeTransition"
                ) { mode ->
                    if (mode == EnvironmentMode.CHAT) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (uiState.messages.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .statusBarsPadding()
                                        .padding(top = 56.dp, bottom = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ChatWelcomeHero(
                                        onPromptSelected = onPromptSelected,
                                        hasConfiguredKeys = uiState.hasConfiguredKeys,
                                        onOpenSetup = { showOnboardingSheet = true }
                                    )
                                }
                            } else {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(
                                        items = uiState.messages,
                                        key = { it.id },
                                        contentType = { it.type.name }
                                    ) { message ->
                                    MessageItem(
                                        message = message,
                                        onApprove = onApprove,
                                        onReject = onReject
                                    )
                                }

                                if (uiState.goalGates.isNotEmpty()) {
                                    item(key = "goal_gates_card") {
                                        val activeMode = allModes.firstOrNull { it.id == uiState.activeModeId }
                                        val gates = activeMode?.effectiveConfig?.completionGates ?: emptyList()
                                        GoalProgressCard(
                                            gates = gates,
                                            progress = uiState.goalGates,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                }

                                if (uiState.isAgentRunning) {
                                    item(key = "thinking_indicator") {
                                        ThinkingIndicator(
                                            step = uiState.thinkingStep,
                                            maxSteps = uiState.maxSteps,
                                            cognitiveStatus = uiState.cognitiveStatus,
                                            isUnleashed = uiState.isUnleashed,
                                            activeTrace = uiState.activeExecutionTrace
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                        // BUILD mode
                        val workModes = remember(allModes) {
                            allModes.filter { it.id.environment == EnvironmentMode.BUILD }
                        }
                        BuildWorkspaceView(
                            hasActiveMessages = uiState.messages.isNotEmpty(),
                            workModes = workModes,
                            activeModeId = uiState.activeModeId,
                            lockedModeId = uiState.lockedModeId,
                            onModeTap = onModeTap,
                            onModeSettings = onModeSettings,
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .padding(top = 56.dp),
                            messageStreamContent = {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    if (uiState.messages.isEmpty()) {
                                        item(key = "welcome_card_build") {
                                            BuildWelcomeHero(
                                                onPromptSelected = onPromptSelected,
                                                hasConfiguredKeys = uiState.hasConfiguredKeys,
                                                onOpenSetup = { showOnboardingSheet = true }
                                            )
                                        }
                                    }

                                    items(
                                        items = uiState.messages,
                                        key = { it.id },
                                        contentType = { it.type.name }
                                    ) { message ->
                                        MessageItem(
                                            message = message,
                                            onApprove = onApprove,
                                            onReject = onReject
                                        )
                                    }

                                    if (uiState.goalGates.isNotEmpty()) {
                                        item(key = "goal_gates_card_build") {
                                            val activeMode = allModes.firstOrNull { it.id == uiState.activeModeId }
                                            val gates = activeMode?.effectiveConfig?.completionGates ?: emptyList()
                                            GoalProgressCard(
                                                gates = gates,
                                                progress = uiState.goalGates,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                    }

                                    if (uiState.isAgentRunning) {
                                        item(key = "thinking_indicator_build") {
                                            ThinkingIndicator(
                                                step = uiState.thinkingStep,
                                                maxSteps = uiState.maxSteps,
                                                cognitiveStatus = uiState.cognitiveStatus,
                                                isUnleashed = uiState.isUnleashed,
                                                activeTrace = uiState.activeExecutionTrace
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                // ── Sleek Top App Bar (Centered Segmented Control + Menu & Actions) ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Menu & Agent Indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                coroutineScope.launch { drawerState.open() }
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                Icons.Filled.Menu,
                                contentDescription = "Open Chat History",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        if (uiState.isAgentRunning) {
                            Spacer(Modifier.width(4.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = if (uiState.isUnleashed) DettleGreen else MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    // Center: Clean, horizontal Segmented Mode Control
                    TopEnvironmentSegmentedControl(
                        selected = uiState.environmentMode,
                        onSelect = viewModel::setEnvironmentMode
                    )

                    // Right: New Chat button
                    IconButton(
                        onClick = viewModel::startNewChat,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "New chat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    customizingMode?.let { mode ->
        ModeCustomizationSheet(
            mode = mode,
            onSave = { config -> viewModel.saveModeConfig(mode.id, config) },
            onReset = { viewModel.resetModeToDefault(mode.id) },
            onDismiss = { customizingMode = null }
        )
    }

    if (showOnboardingSheet) {
        OnboardingSheet(
            onSaveKey = { provider, key ->
                viewModel.addQuickApiKey(provider, key)
                showOnboardingSheet = false
            },
            onEnableDeveloperMode = { enabled ->
                if (enabled != uiState.isUnleashed) {
                    viewModel.toggleUnleashed()
                }
            },
            isDeveloperModeEnabled = uiState.isUnleashed,
            onDismiss = { showOnboardingSheet = false }
        )
    }
}

// ─── Welcome Hero Cards ───────────────────────────────────────────────────────

@Composable
fun WelcomeCard(
    onPromptSelected: (String) -> Unit,
    hasConfiguredKeys: Boolean = true,
    onOpenSetup: () -> Unit = {}
) {
    ChatWelcomeHero(
        onPromptSelected = onPromptSelected,
        hasConfiguredKeys = hasConfiguredKeys,
        onOpenSetup = onOpenSetup
    )
}

@Composable
fun ChatWelcomeHero(
    onPromptSelected: (String) -> Unit,
    hasConfiguredKeys: Boolean = true,
    onOpenSetup: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Glowing Apple Liquid Glass Orb
        Box(
            modifier = Modifier
                .size(68.dp)
                .liquidGlass(shape = CircleShape, elevation = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }

        // Apple Typography Display
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Dettle",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.8).sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Autonomous AI Engineering Partner",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!hasConfiguredKeys) {
            Surface(
                onClick = onOpenSetup,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Connect AI Provider",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Add a free Google AI, Groq, or OpenRouter key to start building immediately.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "Setup",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Quick Starter Prompts in Liquid Glass Cards
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionCard(
                icon = Icons.Outlined.Security,
                text = "Audit repo for security vulnerabilities & architecture",
                onClick = { onPromptSelected("Audit repository architecture, dependencies, and security boundaries. Report findings ranked P0 to P3.") }
            )
            QuickActionCard(
                icon = Icons.Outlined.Code,
                text = "Scaffold feature implementation with TDD unit tests",
                onClick = { onPromptSelected("Plan and scaffold the requested feature using strict test-driven development.") }
            )
            QuickActionCard(
                icon = Icons.Outlined.Search,
                text = "Deep-dive research across web documentation & APIs",
                onClick = { onPromptSelected("/research Synthesize latest documentation and official best practices for our stack.") }
            )
        }
    }
}

@Composable
fun BuildWelcomeHero(
    onPromptSelected: (String) -> Unit,
    hasConfiguredKeys: Boolean = true,
    onOpenSetup: () -> Unit = {}
) {
    ChatWelcomeHero(
        onPromptSelected = onPromptSelected,
        hasConfiguredKeys = hasConfiguredKeys,
        onOpenSetup = onOpenSetup
    )
}

@Composable
fun QuickActionCard(icon: ImageVector, text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
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
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Terminal,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
        }

        Column(modifier = Modifier.widthIn(max = 520.dp)) {
            Surface(
                shape = if (isUser) {
                    RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
                } else {
                    RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)
                },
                color = if (isUser) UserBubble else AiBubble,
                border = BorderStroke(
                    1.dp,
                    if (isUser) UserBubbleBorder else AiBubbleBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        ),
                        color = if (isUser) Color(0xFFF4F4F5) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (message.isStreaming) {
                        Spacer(Modifier.width(4.dp))
                        StreamingCursor()
                    }
                }
            }

            if (!isUser && message.executionTrace != null) {
                Spacer(Modifier.height(4.dp))
                ExecutionTimelineCard(trace = message.executionTrace)
            } else if (message.providerName != null) {
                Text(
                    message.providerName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
fun StreamingCursor() {
    val transition = rememberInfiniteTransition(label = "cursor")
    val alpha = transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "blink"
    )
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(16.dp)
            .graphicsLayer { this.alpha = alpha.value }
            .background(MaterialTheme.colorScheme.primary)
    )
}

@Composable
fun ToolCallCard(message: ChatMessage) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Terminal,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Executing Tool",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    message.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                message.toolCall?.arguments?.entries?.forEach { (k, v) ->
                    Text(
                        "$k: ${v.take(60)}${if (v.length > 60) "..." else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 1.5.dp
            )
        }
    }
}

@Composable
fun ToolResultCard(message: ChatMessage) {
    val isError = message.toolResult?.isError == true
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isError) Icons.Outlined.Clear else Icons.Outlined.Code,
                    contentDescription = null,
                    tint = if (isError) MaterialTheme.colorScheme.error else DettleGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (isError) "Tool Error" else "Tool Output: ${message.toolResult?.toolName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isError) MaterialTheme.colorScheme.error else DettleGreen,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                message.content.take(500) + if (message.content.length > 500) "\n...(truncated)" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Approval Required",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                request.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                request.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (request.details.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(8.dp))
                request.details.entries.take(4).forEach { (k, v) ->
                    Row {
                        Text("$k: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(v.take(80), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            if (isPending) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(
                        onClick = onApprove,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Approve")
                    }
                    OutlinedButton(
                        onClick = onReject,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reject")
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                val isApproved = request.status == com.dettle.app.domain.model.ApprovalStatus.APPROVED
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isApproved) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
                        contentDescription = null,
                        tint = if (isApproved) DettleGreen else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isApproved) "Approved" else "Rejected",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SystemMessage(message: ChatMessage) {
    val isError = message.type == MessageType.ERROR
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isError) Alignment.Start else Alignment.CenterHorizontally
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Text(
                message.content,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        if (message.executionTrace != null) {
            Spacer(Modifier.height(4.dp))
            ExecutionTimelineCard(trace = message.executionTrace)
        }
    }
}

@Composable
fun PolicyBlockedCard(message: ChatMessage) {
    var expanded by remember { mutableStateOf(true) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Policy Blocked",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    if (message.policyId != null) {
                        Text(
                            "rule: ${message.policyId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        message.policyReason ?: message.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (message.policyFix != null) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                message.policyFix,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PolicyWarningBadge(message: ChatMessage) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = DettleOrange.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, DettleOrange.copy(alpha = 0.3f)),
            modifier = Modifier.clickable { expanded = !expanded }
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = DettleOrange,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Policy Warning",
                        style = MaterialTheme.typography.labelSmall,
                        color = DettleOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                AnimatedVisibility(visible = expanded) {
                    Text(
                        message.content,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ThinkingIndicator(
    step: Int,
    maxSteps: Int,
    cognitiveStatus: String? = null,
    isUnleashed: Boolean = false,
    activeTrace: ExecutionTrace? = null
) {
    var isExpanded by remember { mutableStateOf(false) }

    val liveElapsedMs by produceState(initialValue = 0L, key1 = activeTrace?.startTimeMs) {
        val start = activeTrace?.startTimeMs ?: System.currentTimeMillis()
        while (true) {
            value = (System.currentTimeMillis() - start).coerceAtLeast(0L)
            delay(80L)
        }
    }

    val latestStep = activeTrace?.steps?.lastOrNull()
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 36.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Surface(
            onClick = {
                if (activeTrace != null && activeTrace.steps.isNotEmpty()) {
                    isExpanded = !isExpanded
                    android.util.Log.d("ThinkingIndicator", "Tapped live trace, isExpanded=$isExpanded")
                }
            },
            shape = RoundedCornerShape(12.dp),
            color = if (isUnleashed) DettleGreen.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(
                1.dp,
                if (isUnleashed) DettleGreen.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            val transition = rememberInfiniteTransition(label = "thinking")
            val alpha = transition.animateFloat(
                1f, 0.3f,
                infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "pulse"
            )

            repeat(3) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(6.dp)
                        .graphicsLayer { this.alpha = alpha.value }
                        .clip(CircleShape)
                        .background(if (isUnleashed) DettleGreen else MaterialTheme.colorScheme.primary)
                )
            }
            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f, fill = false)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (!cognitiveStatus.isNullOrBlank()) {
                            cognitiveStatus
                        } else if (isUnleashed) {
                            "⚡ Unleashed Engine thinking..."
                        } else {
                            "Thinking..."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUnleashed) DettleGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "⏱️ ${liveElapsedMs}ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (latestStep != null) {
                    Text(
                        text = "${latestStep.icon} ${latestStep.title} (+${latestStep.offsetMs}ms)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        maxLines = 1
                    )
                }
            }

            if (activeTrace != null && activeTrace.steps.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    AnimatedVisibility(
            visible = isExpanded && activeTrace != null && activeTrace.steps.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Live Execution Steps (${activeTrace?.steps?.size ?: 0})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    activeTrace?.steps?.forEachIndexed { index, step ->
                        LiveStepRow(
                            step = step,
                            isLast = index == (activeTrace.steps.size - 1),
                            timeFormatter = timeFormatter
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExecutionTimelineCard(
    trace: ExecutionTrace,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

    val durationText = remember(trace.totalDurationMs) {
        val totalMs = trace.totalDurationMs ?: 0L
        if (totalMs >= 1000L) {
            String.format(Locale.getDefault(), "%.2fs (%dms)", totalMs / 1000.0, totalMs)
        } else {
            "${totalMs}ms"
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            onClick = {
                isExpanded = !isExpanded
                android.util.Log.d("TimelineCard", "Tapped execution trace pill, isExpanded=$isExpanded")
            },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(
                1.dp,
                if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "⏱️ $durationText",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " • ${trace.steps.size} steps",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!trace.modelUsed.isNullOrBlank()) {
                    Text(
                        text = " • ${trace.modelUsed}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse timeline" else "Expand timeline",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Expanded Timeline Detail Card
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Header with title, copy button, and collapse button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Execution Trace",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = durationText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Started: ${timeFormatter.format(Date(trace.startTimeMs))}${if (trace.modelUsed != null) " • " + trace.modelUsed else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                val summary = buildString {
                                    appendLine("=== DETTLE EXECUTION TRACE ===")
                                    appendLine("Started: ${timeFormatter.format(Date(trace.startTimeMs))}")
                                    appendLine("Total Duration: ${trace.totalDurationMs}ms")
                                    appendLine("Model: ${trace.modelUsed ?: "Unknown"}")
                                    appendLine("Steps (${trace.steps.size}):")
                                    trace.steps.forEach { step ->
                                        appendLine("  [+${step.offsetMs}ms] ${step.icon} ${step.title} (${timeFormatter.format(Date(step.timestampMs))})")
                                        if (!step.description.isNullOrBlank()) {
                                            appendLine("     -> ${step.description}")
                                        }
                                    }
                                }
                                clipboardManager.setText(AnnotatedString(summary))
                                Toast.makeText(context, "Execution trace copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copy trace",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Vertical Timeline Steps
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        trace.steps.forEachIndexed { index, step ->
                            TimelineStepItem(
                                step = step,
                                isFirst = index == 0,
                                isLast = index == trace.steps.size - 1,
                                timeFormatter = timeFormatter
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineStepItem(
    step: TraceStep,
    isFirst: Boolean,
    isLast: Boolean,
    timeFormatter: SimpleDateFormat
) {
    val statusColor = when (step.status) {
        TraceStatus.SUCCESS -> Color(0xFF2E7D32)
        TraceStatus.ERROR -> MaterialTheme.colorScheme.error
        TraceStatus.WARNING -> Color(0xFFE65100)
        TraceStatus.RUNNING -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        // Vertical line + node indicator
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(if (step.description.isNullOrBlank()) 44.dp else 58.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // Connecting vertical line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .offset(y = 12.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
            }
            // Center node dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.2f))
                    .border(1.5.dp, statusColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
            }
        }

        Spacer(Modifier.width(6.dp))

        // Step info
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = "${step.icon} ",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }

                // Relative offset pill (+0ms, +24ms)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    Text(
                        text = "+${step.offsetMs}ms",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Wall-clock timestamp
            Text(
                text = timeFormatter.format(Date(step.timestampMs)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            // Step description / metadata
            if (!step.description.isNullOrBlank()) {
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun LiveStepRow(
    step: TraceStep,
    isLast: Boolean,
    timeFormatter: SimpleDateFormat
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${step.icon} ",
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = step.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
            color = if (isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        Text(
            text = "+${step.offsetMs}ms",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Input Bar & Dynamic Action Button ─────────────────────────────────────

enum class ActionButtonState {
    MIC,
    HOLD,
    SEND
}

@Composable
fun EffortSlideBar(
    effortLevel: String,
    onEffortChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = remember { listOf("Low", "Medium", "Max") }
    val selectedIndex = remember(effortLevel) {
        when {
            effortLevel.startsWith("Low", ignoreCase = true) -> 0
            effortLevel.startsWith("Max", ignoreCase = true) -> 2
            else -> 1
        }
    }

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "effortSlide"
    )

    BoxWithConstraints(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .border(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                RoundedCornerShape(14.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val segW = size.width / 3f
                    val idx = (offset.x / segW).toInt().coerceIn(0, 2)
                    onEffortChange(options[idx])
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    val segW = size.width / 3f
                    val idx = (change.position.x / segW).toInt().coerceIn(0, 2)
                    onEffortChange(options[idx])
                }
            }
    ) {
        val segmentWidth = maxWidth / 3

        // Sliding Active Capsule
        Box(
            modifier = Modifier
                .padding(vertical = 2.dp, horizontal = 2.dp)
                .offset(x = segmentWidth * animatedIndex)
                .width(segmentWidth - 4.dp)
                .fillMaxHeight()
                .shadow(1.5.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(
                    0.5.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    RoundedCornerShape(12.dp)
                )
        )

        // 3 Components (Low, Medium, Max)
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = selectedIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 10.5.sp
                        ),
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatInputBar(
    value: String,
    enabled: Boolean,
    isRunning: Boolean,
    isListening: Boolean,
    voiceRmsFlow: StateFlow<Float>,
    focusRequester: FocusRequester? = null,
    // ── New streamlined mode selector ───────────────────────────────────
    activeChatMode: SimpleChatMode = SimpleChatMode.NORMAL,
    onModeSelect: (SimpleChatMode) -> Unit = {},
    // ── Legacy params (kept for call-site compatibility) ─────────────────
    selectedChatModes: Set<String> = setOf("Normal"),
    onToggleChatMode: (String) -> Unit = {},
    searchOrResearchMode: String = "Normal",
    onSearchOrResearchChange: (String) -> Unit = {},
    effortLevel: String = "Medium",
    onEffortChange: (String) -> Unit = {},
    // ──────────────────────────────────────────────────────────────────────
    attachments: List<ChatAttachment> = emptyList(),
    onAddAttachmentClick: () -> Unit = {},
    onRemoveAttachment: (ChatAttachment) -> Unit = {},
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    onStopVoiceClick: () -> Unit,
    onCancelVoiceClick: () -> Unit
) {
    val rmsValue by voiceRmsFlow.collectAsState()
    var isModeBarOpen by remember { mutableStateOf(false) }
    val efforts = remember { listOf("Low", "Medium", "Max Effort") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(
                    shape = RoundedCornerShape(20.dp),
                    isDark = true,
                    elevation = 4.dp
                ),
            shape = RoundedCornerShape(20.dp),
            color = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Attached files preview carousel
                if (attachments.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        attachments.forEach { att ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (att.isImage) Icons.Outlined.Edit else Icons.Outlined.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (att.name.length > 15) att.name.take(12) + "..." else att.name,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = AttachmentHelper.formatFileSize(att.sizeBytes),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { onRemoveAttachment(att) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Main Text Input Area or Voice Waveform Mode
                AnimatedContent(
                    targetState = isListening,
                    transitionSpec = {
                        (fadeIn(tween(180)) + scaleIn(initialScale = 0.97f))
                            .togetherWith(fadeOut(tween(140)) + scaleOut(targetScale = 0.97f))
                    },
                    label = "inputModeAnimation"
                ) { listening ->
                    if (listening) {
                        VoiceWaveformBar(
                            rmsFlow = voiceRmsFlow,
                            onCancel = onCancelVoiceClick,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Plus (+) Icon on the LEFT of the text field to upload documents & photos
                            IconButton(
                                onClick = onAddAttachmentClick,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Upload document or photo",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(4.dp))

                            BasicTextField(
                                value = value,
                                onValueChange = onValueChange,
                                enabled = enabled,
                                modifier = Modifier
                                    .weight(1f)
                                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                                    .padding(vertical = 8.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Default
                                ),
                                maxLines = 5,
                                decorationBox = { innerTextField ->
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        if (value.isEmpty() && attachments.isEmpty()) {
                                            Text(
                                                text = if (!enabled && isRunning) "Agent is working..." else "Ask anything...",
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }
                }

                // ── Streamlined 3-pill Mode Selector ────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SimpleChatMode.entries.forEach { mode ->
                        val isSelected = activeChatMode == mode
                        Surface(
                            onClick = { onModeSelect(mode) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(
                                0.5.dp,
                                if (isSelected)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                else
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )
                        ) {
                            Text(
                                text = when (mode) {
                                    SimpleChatMode.NORMAL -> "Normal"
                                    SimpleChatMode.WEB    -> "🌐 Web"
                                    SimpleChatMode.SWARM  -> "⚡ Swarm"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Bottom toolbar — just send/mic button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp, bottom = 8.dp, top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.weight(1f))

                    // Audio wave visualizer overlay when recording
                    if (isListening) {
                        Row(
                            modifier = Modifier.padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            repeat(5) { i ->
                                val barHeight by animateDpAsState(
                                    targetValue = (6 + (rmsValue.coerceIn(0f, 1f) * (14 + i * 3))).dp,
                                    animationSpec = tween(80),
                                    label = "waveBar$i"
                                )
                                Box(
                                    modifier = Modifier
                                        .width(2.5.dp)
                                        .height(barHeight)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }

                    // Circular Action Button (Mic / Stop / Send)
                    val canSend = (value.isNotBlank() || attachments.isNotEmpty()) && enabled
                    val buttonState = when {
                        isListening -> ActionButtonState.HOLD
                        canSend -> ActionButtonState.SEND
                        else -> ActionButtonState.MIC
                    }

                    Surface(
                        onClick = {
                            when (buttonState) {
                                ActionButtonState.HOLD -> onStopVoiceClick()
                                ActionButtonState.SEND -> if (canSend) onSend()
                                ActionButtonState.MIC -> onMicClick()
                            }
                        },
                        shape = CircleShape,
                        color = if (buttonState == ActionButtonState.HOLD) DettleRed else MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = buttonState,
                                transitionSpec = {
                                    (scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) + fadeIn(tween(150)))
                                        .togetherWith(scaleOut(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)) + fadeOut(tween(120)))
                                },
                                label = "circleActionMorph"
                            ) { state ->
                                when (state) {
                                    ActionButtonState.HOLD -> {
                                        Icon(
                                            Icons.Filled.Stop,
                                            contentDescription = "Stop recording",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    ActionButtonState.SEND -> {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Send",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    ActionButtonState.MIC -> {
                                        Icon(
                                            Icons.Filled.Mic,
                                            contentDescription = "Voice input",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceWaveformBar(
    rmsFlow: StateFlow<Float>,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rms by rmsFlow.collectAsState()
    var elapsedSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        while (isActive) {
            elapsedSeconds = ((System.currentTimeMillis() - start) / 1000L).toInt()
            delay(500L)
        }
    }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timeFormatted = "%02d:%02d".format(minutes, seconds)

    val infiniteTransition = rememberInfiniteTransition(label = "recordingPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live pulsing recording dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .graphicsLayer { alpha = pulseAlpha }
                    .clip(CircleShape)
                    .background(DettleRed)
            )
            Spacer(Modifier.width(8.dp))

            // Timer
            Text(
                text = timeFormatted,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.width(12.dp))

            val waveColor = MaterialTheme.colorScheme.primary
            // Live Waveform Visualizer
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(26.dp)
                    .graphicsLayer()
            ) {
                val barCount = 22
                val barWidth = 3.dp.toPx()
                val totalWidth = size.width
                val spacing = (totalWidth - (barCount * barWidth)) / (barCount - 1).coerceAtLeast(1)
                val baseHeight = 4.dp.toPx()
                val maxHeight = size.height

                for (i in 0 until barCount) {
                    val x = i * (barWidth + spacing)
                    val waveFactor = kotlin.math.sin((i.toDouble() / barCount) * Math.PI).toFloat()
                    val modulatedHeight = (baseHeight + (maxHeight - baseHeight) * rms * waveFactor * 1.5f)
                        .coerceIn(baseHeight, maxHeight)
                    val y = (maxHeight - modulatedHeight) / 2f

                    drawRoundRect(
                        color = waveColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, modulatedHeight),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Cancel button
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Cancel voice typing",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ─── Environment Toggle ───────────────────────────────────────────────────────

@Composable
fun EnvironmentSegmentedToggle(
    selected: EnvironmentMode,
    onSelect: (EnvironmentMode) -> Unit,
    modifier: Modifier = Modifier
) {
    AppleModePillToggle(
        selected = selected,
        onSelect = onSelect,
        modifier = modifier
    )
}
