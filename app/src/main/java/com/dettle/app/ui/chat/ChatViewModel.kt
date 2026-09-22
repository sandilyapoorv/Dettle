package com.dettle.app.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import com.dettle.app.domain.model.ApiMessage
import com.dettle.app.domain.model.ApprovalRequest
import com.dettle.app.domain.model.ApprovalStatus
import com.dettle.app.domain.model.ChatMessage
import com.dettle.app.domain.model.MessageRole
import com.dettle.app.domain.model.MessageType
import com.dettle.app.domain.model.TaskContext
import com.dettle.app.domain.model.TaskType
import com.dettle.app.domain.model.ToolCall
import com.dettle.app.domain.model.ToolResult
import com.dettle.app.domain.model.toApiMessage
import com.dettle.app.orchestrator.LoopEvent
import com.dettle.app.orchestrator.ReActLoop
import com.dettle.app.orchestrator.mode.AgentMode
import com.dettle.app.orchestrator.mode.AgentModes
import com.dettle.app.orchestrator.mode.GateStatus
import com.dettle.app.orchestrator.mode.Goal
import com.dettle.app.orchestrator.mode.GoalRepository
import com.dettle.app.orchestrator.mode.ModeId
import com.dettle.app.orchestrator.mode.ModeRepository
import com.dettle.app.orchestrator.mode.ModeRouter
import com.dettle.app.orchestrator.learning.LearningEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val reActLoop: ReActLoop,
    private val modeRouter: ModeRouter,
    private val modeRepository: ModeRepository,
    private val goalRepository: GoalRepository,
    private val learningEngine: LearningEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /** All modes with their effective (default + user) configs — live-updating from DataStore */
    val allModes: StateFlow<List<AgentMode>> = modeRepository.observeAllModes()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AgentModes.ALL)

    private val conversationHistory = mutableListOf<ApiMessage>()
    private var pendingApproval: Pair<ApprovalRequest, ToolCall>? = null
    private var lockedModeId: ModeId? = null  // null = auto-classify; set = user-locked
    private var activeGoalId: String? = null

    // ── Mode control ────────────────────────────────────────────────────────

    /** User taps a mode pill to lock it. Tap again to unlock (return to auto) */
    fun toggleModelock(id: ModeId) {
        lockedModeId = if (lockedModeId == id) null else id
        _uiState.update { it.copy(lockedModeId = lockedModeId) }
    }

    /** ViewModel for the mode customization sheet - save a full ModeConfig */
    fun saveModeConfig(id: ModeId, config: com.dettle.app.orchestrator.mode.ModeConfig) {
        viewModelScope.launch { modeRepository.saveConfig(id, config) }
    }

    fun resetModeToDefault(id: ModeId) {
        viewModelScope.launch { modeRepository.resetToDefault(id) }
    }

    // ── Message sending ─────────────────────────────────────────────────────

    fun sendMessage(text: String) {
        if (text.isBlank() || _uiState.value.isAgentRunning) return

        val userMessage = ChatMessage(role = MessageRole.USER, content = text, type = MessageType.TEXT)
        addMessage(userMessage)
        _uiState.update { it.copy(isAgentRunning = true, inputEnabled = false) }

        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e("ChatViewModel", "Unhandled exception in sendMessage coroutine", throwable)
            _uiState.update { it.copy(isAgentRunning = false, inputEnabled = true, thinkingStep = 0) }
            addMessage(
                ChatMessage(
                    role = MessageRole.ASSISTANT,
                    type = MessageType.ERROR,
                    content = "System Error: ${throwable.localizedMessage ?: "Unexpected error occurred. Please verify your API keys and network."}"
                )
            )
        }

        viewModelScope.launch(exceptionHandler) {
            try {
                // 1. Classify intent (or use locked mode)
                val effectiveMode = resolveMode(text)
                _uiState.update { it.copy(activeModeId = effectiveMode.id) }

                // 2. For GOAL mode: create or resume a persistent goal
                val goal = if (effectiveMode.id == ModeId.GOAL) {
                    resolveGoal(text, effectiveMode)
                } else null

                // 3. Run the loop
                reActLoop.run(
                    userMessage = text,
                    conversationHistory = conversationHistory.toList(),
                    mode = effectiveMode,
                    goal = goal
                ).collect { event -> handleLoopEvent(event) }
            } catch (t: Throwable) {
                Log.e("ChatViewModel", "Caught throwable in chat execution loop", t)
                addMessage(
                    ChatMessage(
                        role = MessageRole.ASSISTANT,
                        type = MessageType.ERROR,
                        content = "Execution failed: ${t.localizedMessage ?: "Check API settings and connectivity."}"
                    )
                )
            } finally {
                _uiState.update { it.copy(isAgentRunning = false, inputEnabled = true, thinkingStep = 0) }
            }
        }
    }

    private suspend fun resolveMode(text: String): AgentMode {
        // If user locked a mode, use that
        lockedModeId?.let { locked ->
            return allModes.value.firstOrNull { it.id == locked } ?: AgentModes.forId(locked)
        }
        // Otherwise auto-classify
        val result = modeRouter.classify(text, conversationHistory.takeLast(6))
        val effectiveMode = allModes.value.firstOrNull { it.id == result.modeId }
            ?: AgentModes.forId(result.modeId)

        // If ambiguous, show a subtle indicator (not a full block) but proceed anyway
        if (result.isAmbiguous) {
            _uiState.update { it.copy(modeClassificationHint = "Auto → ${effectiveMode.displayName}?") }
        }
        return effectiveMode
    }

    private suspend fun resolveGoal(text: String, mode: AgentMode): Goal? {
        // If there's an active goal, resume it
        val existing = goalRepository.getActiveGoal()
        if (existing != null) return existing
        // Otherwise create one from the message + mode's completion gates
        return goalRepository.createGoal(
            description = text,
            gates = mode.effectiveConfig.completionGates
        ).also { activeGoalId = it.id }
    }

    // ── Event handling ──────────────────────────────────────────────────────

    private fun handleLoopEvent(event: LoopEvent) {
        when (event) {
            is LoopEvent.Thinking -> {
                _uiState.update { it.copy(thinkingStep = event.step, maxSteps = event.maxSteps) }
            }

            is LoopEvent.TokenStreamed -> {
                val existing = _uiState.value.messages.find { it.id == event.messageId }
                if (existing == null) {
                    addMessage(ChatMessage(
                        id = event.messageId, role = MessageRole.ASSISTANT,
                        type = MessageType.TEXT, content = event.fullText, isStreaming = true
                    ))
                } else {
                    updateMessage(event.messageId) { it.copy(content = event.fullText) }
                }
            }

            is LoopEvent.StreamComplete -> {
                updateMessage(event.messageId) { it.copy(isStreaming = false) }
                conversationHistory.add(ApiMessage(role = "assistant", content = event.fullText))
            }

            is LoopEvent.ExecutingTool -> {
                addMessage(ChatMessage(
                    role = MessageRole.ASSISTANT, type = MessageType.TOOL_CALL,
                    content = "Executing: `${event.toolCall.name}`", toolCall = event.toolCall
                ))
            }

            is LoopEvent.ToolResultReceived -> {
                addMessage(ChatMessage(
                    role = MessageRole.TOOL, type = MessageType.TOOL_RESULT,
                    content = event.result.content, toolResult = event.result
                ))
                conversationHistory.add(ApiMessage(
                    role = "tool", content = event.result.content,
                    tool_call_id = event.result.toolCallId, name = event.result.toolName
                ))
            }

            is LoopEvent.NeedsApproval -> {
                pendingApproval = Pair(event.request, event.pendingToolCall)
                addMessage(ChatMessage(
                    role = MessageRole.SYSTEM, type = MessageType.APPROVAL,
                    content = event.request.description, approvalRequest = event.request
                ))
                _uiState.update { it.copy(isAgentRunning = false, inputEnabled = false) }
            }

            is LoopEvent.FinalAnswer -> {
                val lastAiMsg = _uiState.value.messages.lastOrNull { it.isStreaming }
                updateMessage(lastAiMsg?.id ?: "") {
                    it.copy(isStreaming = false)
                }
                
                // --- Background Intelligence ---
                // Post snapshot for LearningEngine to extract facts/corrections
                val lastUserMsg = _uiState.value.messages.lastOrNull { it.role == MessageRole.USER }
                if (lastUserMsg != null && lastAiMsg != null) {
                    val snapshot = com.dettle.app.orchestrator.learning.LearningEngine.ConversationSnapshot(
                        userMessage = lastUserMsg.content,
                        aiResponse = lastAiMsg.content,
                        projectId = null // TODO: pass active project ID when fully wired
                    )
                    learningEngine.post(snapshot)
                }

                _uiState.update { it.copy(isAgentRunning = false, inputEnabled = true, thinkingStep = 0) }
            }

            is LoopEvent.StepLimitReached -> {
                addMessage(ChatMessage(
                    role = MessageRole.SYSTEM, type = MessageType.SYSTEM,
                    content = "[Step Limit] ${event.message}"
                ))
                _uiState.update { it.copy(isAgentRunning = false, inputEnabled = true, thinkingStep = 0) }
            }

            is LoopEvent.Error -> {
                addMessage(ChatMessage(
                    role = MessageRole.SYSTEM, type = MessageType.ERROR, content = "[Error] ${event.message}"
                ))
                _uiState.update { it.copy(isAgentRunning = false, inputEnabled = true, thinkingStep = 0) }
            }

            is LoopEvent.PolicyBlocked -> {
                addMessage(ChatMessage(
                    role = MessageRole.SYSTEM, type = MessageType.POLICY_BLOCKED,
                    content = event.reason, policyId = event.policyId,
                    policyReason = event.reason, policyFix = event.fix
                ))
            }

            is LoopEvent.PolicyWarning -> {
                addMessage(ChatMessage(
                    role = MessageRole.SYSTEM, type = MessageType.POLICY_WARNING,
                    content = event.warnings, policyId = "warning:${event.toolName}"
                ))
            }

            is LoopEvent.GoalProgress -> {
                // Update the live gate checklist in UI state
                _uiState.update { state ->
                    val updated = state.goalGates.toMutableMap()
                    updated[event.gateId] = event.passed
                    state.copy(goalGates = updated)
                }
                // Persist to Room
                activeGoalId?.let { goalId ->
                    viewModelScope.launch {
                        goalRepository.updateGate(
                            goalId, event.gateId,
                            if (event.passed) GateStatus.PASSED else GateStatus.FAILED
                        )
                    }
                }
            }
        }
    }

    // ── Approval ────────────────────────────────────────────────────────────

    fun approveAction() {
        val (approval, _) = pendingApproval ?: return
        approval.status = ApprovalStatus.APPROVED
        updateApprovalStatus(approval.id, ApprovalStatus.APPROVED)
        pendingApproval = null
        addMessage(ChatMessage(role = MessageRole.SYSTEM, type = MessageType.SYSTEM, content = "Action approved. Executing..."))
        _uiState.update { it.copy(isAgentRunning = true, inputEnabled = false) }
    }

    fun rejectAction() {
        val (approval, _) = pendingApproval ?: return
        approval.status = ApprovalStatus.REJECTED
        updateApprovalStatus(approval.id, ApprovalStatus.REJECTED)
        pendingApproval = null
        addMessage(ChatMessage(role = MessageRole.SYSTEM, type = MessageType.SYSTEM, content = "Action rejected."))
        _uiState.update { it.copy(isAgentRunning = false, inputEnabled = true) }
    }

    fun clearChat() {
        _uiState.update { it.copy(messages = emptyList(), goalGates = emptyMap()) }
        conversationHistory.clear()
        activeGoalId = null
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun addMessage(message: ChatMessage) {
        _uiState.update { state -> state.copy(messages = state.messages + message) }
    }

    private fun updateMessage(id: String, update: (ChatMessage) -> ChatMessage) {
        if (id.isBlank()) return
        _uiState.update { state ->
            state.copy(messages = state.messages.map { if (it.id == id) update(it) else it })
        }
    }

    private fun updateApprovalStatus(approvalId: String, status: ApprovalStatus) {
        _uiState.update { state ->
            state.copy(messages = state.messages.map { msg ->
                if (msg.approvalRequest?.id == approvalId)
                    msg.copy(approvalRequest = msg.approvalRequest?.copy(status = status))
                else msg
            })
        }
    }
}

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isAgentRunning: Boolean = false,
    val inputEnabled: Boolean = true,
    val thinkingStep: Int = 0,
    val maxSteps: Int = 8,
    val activeModeId: ModeId = ModeId.CHAT,
    val lockedModeId: ModeId? = null,                    // null = auto
    val modeClassificationHint: String? = null,          // "Auto → Research?" shown briefly
    val goalGates: Map<String, Boolean> = emptyMap()     // gateId → passed?
)
