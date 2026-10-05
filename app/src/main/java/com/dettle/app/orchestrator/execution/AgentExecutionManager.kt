package com.dettle.app.orchestrator.execution

import android.content.Context
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.dettle.app.data.db.dao.ConversationDao
import com.dettle.app.data.db.entity.ConversationMessageEntity
import com.dettle.app.domain.model.ApiMessage
import com.dettle.app.domain.model.ExecutionTrace
import com.dettle.app.domain.model.MessageType
import com.dettle.app.domain.model.TaskContext
import com.dettle.app.domain.model.TraceStep
import com.dettle.app.domain.model.TraceStatus
import com.dettle.app.orchestrator.LoopEvent
import com.dettle.app.orchestrator.ReActLoop
import com.dettle.app.orchestrator.brain.CognitiveBrain
import com.dettle.app.orchestrator.mode.AgentMode
import com.dettle.app.orchestrator.mode.GateStatus
import com.dettle.app.orchestrator.mode.Goal
import com.dettle.app.orchestrator.mode.GoalRepository
import com.dettle.app.orchestrator.swarm.SwarmOrchestrator
import com.dettle.app.service.AgentForegroundService
import com.dettle.app.service.Command
import com.dettle.app.service.ServiceBridge
import com.dettle.app.ui.chat.SimpleChatMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class ActiveExecutionState(
    val conversationId: String,
    val isRunning: Boolean = true,
    val statusText: String = "Thinking...",
    val modelUsed: String = "Dettle AI"
)

sealed class ExecutionUpdate {
    data class Event(val conversationId: String, val loopEvent: LoopEvent) : ExecutionUpdate()
    data class Completed(val conversationId: String, val finalText: String, val trace: ExecutionTrace) : ExecutionUpdate()
    data class Error(val conversationId: String, val error: String, val trace: ExecutionTrace) : ExecutionUpdate()
}

/**
 * 24/7 Background Execution Daemon Manager.
 *
 * Runs message processing, tool execution, cognitive consolidation, and database
 * persistence in a process-wide [SupervisorJob] scope backed by [AgentForegroundService]
 * and partial wake lock. Even if the user swipes away the application, message processing
 * continues uninterrupted and writes results to the Room database.
 */
@Singleton
class AgentExecutionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reActLoop: ReActLoop,
    private val swarmOrchestrator: SwarmOrchestrator,
    private val cognitiveBrain: CognitiveBrain,
    private val conversationDao: ConversationDao,
    private val goalRepository: GoalRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeJob: Job? = null

    private val _activeExecution = MutableStateFlow<ActiveExecutionState?>(null)
    val activeExecution: StateFlow<ActiveExecutionState?> = _activeExecution.asStateFlow()

    private val _executionUpdates = MutableSharedFlow<ExecutionUpdate>(extraBufferCapacity = 64)
    val executionUpdates: SharedFlow<ExecutionUpdate> = _executionUpdates.asSharedFlow()

    fun isExecuting(conversationId: String?): Boolean {
        return _activeExecution.value?.let { it.isRunning && (conversationId == null || it.conversationId == conversationId) } ?: false
    }

    fun dispatchMessage(
        conversationId: String,
        userMessage: String,
        conversationHistory: List<ApiMessage>,
        taskContext: TaskContext,
        effectiveMode: AgentMode,
        isUncensored: Boolean,
        activeChatMode: SimpleChatMode,
        selectedModel: String,
        goal: Goal? = null,
        startTimeMs: Long = System.currentTimeMillis()
    ) {
        activeJob?.cancel()
        activeJob = scope.launch {
            // 1. Keep CPU and Foreground service alive 24/7
            try {
                ContextCompat.startForegroundService(context, AgentForegroundService.startIntent(context))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start foreground service", e)
            }

            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val wakeLock = try {
                powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "dettle:agent_execution")?.apply {
                    setReferenceCounted(false)
                    acquire(10 * 60 * 1000L) // 10 minutes max wake lock
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to acquire wake lock", e)
                null
            }

            _activeExecution.value = ActiveExecutionState(
                conversationId = conversationId,
                isRunning = true,
                statusText = "Thinking...",
                modelUsed = selectedModel
            )
            ServiceBridge.trySend(Command.UpdateStatus("Thinking..."))

            val traceSteps = mutableListOf<TraceStep>()
            traceSteps.add(
                TraceStep(
                    timestampMs = startTimeMs,
                    offsetMs = 0L,
                    icon = "✉️",
                    title = "User Message Dispatched",
                    description = "Prompt dispatched to background orchestrator (${userMessage.length} chars)",
                    status = TraceStatus.SUCCESS
                )
            )

            var lastStreamingText = ""
            var lastStreamingMessageId: String? = null
            var activeModelName = selectedModel
            var persistedAssistantText = false

            try {
                // Cognitive context preparation
                cognitiveBrain.prepareCognitiveContext(
                    userMessage = userMessage,
                    taskContext = taskContext,
                    isUncensored = isUncensored
                )
                traceSteps.add(
                    TraceStep(
                        timestampMs = System.currentTimeMillis(),
                        offsetMs = System.currentTimeMillis() - startTimeMs,
                        icon = "🧠",
                        title = "Cognitive Recall & Amygdala",
                        description = "Episodic memory & cognitive context prepared",
                        status = TraceStatus.SUCCESS
                    )
                )

                val flow = when (activeChatMode) {
                    SimpleChatMode.SWARM -> {
                        traceSteps.add(
                            TraceStep(
                                timestampMs = System.currentTimeMillis(),
                                offsetMs = System.currentTimeMillis() - startTimeMs,
                                icon = "⚡",
                                title = "Swarm Mode Activated",
                                description = "Launching parallel agents — Sally will judge the winner",
                                status = TraceStatus.SUCCESS
                            )
                        )
                        swarmOrchestrator.executeChatSwarm(
                            userMessage = userMessage,
                            conversationHistory = conversationHistory
                        )
                    }
                    SimpleChatMode.NORMAL, SimpleChatMode.WEB -> {
                        traceSteps.add(
                            TraceStep(
                                timestampMs = System.currentTimeMillis(),
                                offsetMs = System.currentTimeMillis() - startTimeMs,
                                icon = "🧭",
                                title = "Intent Mode Classified",
                                description = "Mode selected: ${effectiveMode.displayName} (${effectiveMode.id.name})",
                                status = TraceStatus.SUCCESS
                            )
                        )
                        reActLoop.run(
                            userMessage = userMessage,
                            conversationHistory = conversationHistory,
                            taskContext = taskContext,
                            mode = effectiveMode,
                            goal = goal,
                            isUncensored = isUncensored,
                            maxSteps = 30,
                            startTimeMs = startTimeMs
                        )
                    }
                }

                flow.collect { event ->
                    _executionUpdates.emit(ExecutionUpdate.Event(conversationId, event))

                    when (event) {
                        is LoopEvent.Thinking -> {
                            ServiceBridge.trySend(Command.UpdateStatus("Step ${event.step}/${event.maxSteps} thinking..."))
                        }
                        is LoopEvent.ExecutingTool -> {
                            ServiceBridge.trySend(Command.UpdateStatus("Running: ${event.toolCall.name}"))
                            // Persist tool call into Room DB for seamless resumption
                            conversationDao.insertMessage(
                                ConversationMessageEntity(
                                    conversationId = conversationId,
                                    role = "assistant",
                                    content = "Executing: `${event.toolCall.name}`",
                                    type = MessageType.TOOL_CALL.name,
                                    toolName = event.toolCall.name,
                                    toolCallId = event.toolCall.id
                                )
                            )
                            conversationDao.incrementMessageCount(conversationId)
                        }
                        is LoopEvent.ToolResultReceived -> {
                            conversationDao.insertMessage(
                                ConversationMessageEntity(
                                    conversationId = conversationId,
                                    role = "tool",
                                    content = event.result.content,
                                    type = MessageType.TOOL_RESULT.name,
                                    toolName = event.result.toolName,
                                    toolCallId = event.result.toolCallId
                                )
                            )
                            conversationDao.incrementMessageCount(conversationId)
                        }
                        is LoopEvent.TokenStreamed -> {
                            lastStreamingText = event.fullText
                            lastStreamingMessageId = event.messageId
                        }
                        is LoopEvent.TraceStepEmitted -> {
                            traceSteps.add(event.step)
                            if (event.step.icon == "⚡" && !event.step.description.isNullOrBlank()) {
                                activeModelName = event.step.description
                            }
                        }
                        is LoopEvent.StreamComplete -> {
                            lastStreamingText = event.fullText
                            if (event.fullText.isNotBlank()) {
                                conversationDao.insertMessage(
                                    ConversationMessageEntity(
                                        conversationId = conversationId,
                                        role = "assistant",
                                        content = event.fullText,
                                        type = MessageType.TEXT.name
                                    )
                                )
                                conversationDao.incrementMessageCount(conversationId)
                                persistedAssistantText = true
                            }
                        }
                        is LoopEvent.FinalAnswer -> {
                            val endMs = System.currentTimeMillis()
                            val finalTrace = ExecutionTrace(
                                startTimeMs = startTimeMs,
                                endTimeMs = endMs,
                                totalDurationMs = (endMs - startTimeMs).coerceAtLeast(0L),
                                modelUsed = activeModelName,
                                steps = traceSteps.toList()
                            )

                            if (!persistedAssistantText && event.text.isNotBlank()) {
                                conversationDao.insertMessage(
                                    ConversationMessageEntity(
                                        conversationId = conversationId,
                                        role = "assistant",
                                        content = event.text,
                                        type = MessageType.TEXT.name
                                    )
                                )
                                conversationDao.incrementMessageCount(conversationId)
                                persistedAssistantText = true
                            }

                            // Autonomous memory consolidation in background
                            val answerToSave = if (lastStreamingText.isNotBlank()) lastStreamingText else event.text
                            cognitiveBrain.consolidateExperience(
                                userMessage = userMessage,
                                assistantResponse = answerToSave,
                                projectId = null
                            )

                            _executionUpdates.emit(ExecutionUpdate.Completed(conversationId, event.text, finalTrace))
                        }
                        is LoopEvent.GoalProgress -> {
                            goal?.let { g ->
                                goalRepository.updateGate(
                                    g.id, event.gateId,
                                    if (event.passed) GateStatus.PASSED else GateStatus.FAILED
                                )
                            }
                        }
                        is LoopEvent.Error -> {
                            val endMs = System.currentTimeMillis()
                            val errorTrace = ExecutionTrace(
                                startTimeMs = startTimeMs,
                                endTimeMs = endMs,
                                totalDurationMs = (endMs - startTimeMs).coerceAtLeast(0L),
                                modelUsed = activeModelName,
                                steps = traceSteps.toList()
                            )
                            conversationDao.insertMessage(
                                ConversationMessageEntity(
                                    conversationId = conversationId,
                                    role = "assistant",
                                    content = "[Error] ${event.message}",
                                    type = MessageType.ERROR.name
                                )
                            )
                            conversationDao.incrementMessageCount(conversationId)
                            _executionUpdates.emit(ExecutionUpdate.Error(conversationId, event.message, errorTrace))
                        }
                        else -> {}
                    }
                }
            } catch (t: Throwable) {
                if (t !is CancellationException) {
                    Log.e(TAG, "Unhandled exception in background daemon execution", t)
                    val endMs = System.currentTimeMillis()
                    val errorTrace = ExecutionTrace(
                        startTimeMs = startTimeMs,
                        endTimeMs = endMs,
                        totalDurationMs = (endMs - startTimeMs).coerceAtLeast(0L),
                        modelUsed = activeModelName,
                        steps = traceSteps.toList()
                    )
                    val errorMsg = t.localizedMessage ?: "Unexpected error occurred during background execution"
                    conversationDao.insertMessage(
                        ConversationMessageEntity(
                            conversationId = conversationId,
                            role = "assistant",
                            content = "System Error: $errorMsg",
                            type = MessageType.ERROR.name
                        )
                    )
                    conversationDao.incrementMessageCount(conversationId)
                    _executionUpdates.emit(ExecutionUpdate.Error(conversationId, errorMsg, errorTrace))
                }
            } finally {
                try {
                    if (wakeLock?.isHeld == true) {
                        wakeLock.release()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to release wake lock", e)
                }
                ServiceBridge.trySend(Command.UpdateStatus("Dettle Agent active"))
                _activeExecution.value = null
            }
        }
    }

    fun stopExecution() {
        activeJob?.cancel()
        activeJob = null
        _activeExecution.value = null
        ServiceBridge.trySend(Command.UpdateStatus("Dettle Agent active"))
    }

    companion object {
        private const val TAG = "AgentExecutionManager"
    }
}
