package com.dettle.app.orchestrator.overnight

import android.util.Log
import com.dettle.app.data.db.dao.TaskLogDao
import com.dettle.app.data.db.entity.TaskLogEntity
import com.dettle.app.data.settings.ApiKeyStore
import com.dettle.app.domain.model.ApiMessage
import com.dettle.app.orchestrator.AgentBus
import com.dettle.app.orchestrator.AgentRole
import com.dettle.app.orchestrator.AgentStatus
import com.dettle.app.orchestrator.LoopEvent
import com.dettle.app.orchestrator.MemoryInjector
import com.dettle.app.orchestrator.ReActLoop
import com.dettle.app.orchestrator.TaskContext
import com.dettle.app.orchestrator.TaskType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "OvernightLoop"

/**
 * The gnhf-style overnight autonomous agent loop.
 *
 * Concept: you queue tasks before bed → Dettle works through them overnight →
 * commits code, opens PRs, deploys to Cloudflare, uploads summaries to Drive.
 * You wake up to done work.
 *
 * Architecture:
 * - TaskQueue provides ordered list of OvernightTask items
 * - Loop executes each task via ReActLoop (full agent with tools)
 * - After each task: stores outcome in Room memory, writes to task log
 * - After all tasks: generates Markdown summary → pushes to GitHub + Drive
 * - Self-improving: if a task FAILED, stores the failure reason in memory
 *   so the agent avoids the same mistake on next run
 *
 * Safety:
 * - All GitHub changes go to branches + PRs (never direct to main)
 * - Human-in-loop for REQUIRE-safety tools enforced in ReActLoop
 * - Configurable max runtime (default 8 hours)
 * - WakeLock held via foreground service — no battery abuse
 * - Immediately stops if user sends a message
 *
 * Inspired by: kunchenguid/gnhf overnight loop concept
 */
@Singleton
class OvernightLoop @Inject constructor(
    private val taskQueue: TaskQueue,
    private val reActLoop: ReActLoop,
    private val agentBus: AgentBus,
    private val memoryInjector: MemoryInjector,
    private val taskLogDao: TaskLogDao,
    private val summaryWriter: OvernightSummaryWriter,
    private val skillUpdater: SelfImprovingSkillUpdater,
    private val keyStore: ApiKeyStore,
    private val experienceConsolidator: ExperienceConsolidator
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loopJob: Job? = null

    private val _state = MutableStateFlow(OvernightState())
    val state: StateFlow<OvernightState> = _state.asStateFlow()

    val isRunning: Boolean get() = loopJob?.isActive == true

    // ── Start/stop ─────────────────────────────────────────────────────────

    fun start() {
        if (isRunning) return
        _state.update { OvernightState(phase = OvernightPhase.STARTING) }
        
        // Listen for amygdala hijacks
        scope.launch {
            agentBus.events.collect { events ->
                events.lastOrNull()?.let { event ->
                    if (event is com.dettle.app.orchestrator.AgentEvent.Interrupt) {
                        Log.w(TAG, "Overnight loop interrupted by Amygdala: ${event.reason}")
                        interrupt()
                    }
                }
            }
        }
        
        loopJob = scope.launch { runLoop() }
        Log.d(TAG, "Overnight loop started")
    }

    fun stop() {
        loopJob?.cancel()
        loopJob = null
        _state.update { it.copy(phase = OvernightPhase.IDLE, currentTask = null) }
        Log.d(TAG, "Overnight loop stopped")
    }

    fun interrupt() {
        if (!isRunning) return
        _state.update { it.copy(phase = OvernightPhase.INTERRUPTED) }
        loopJob?.cancel()
        loopJob = null
        Log.d(TAG, "Overnight loop interrupted by user")
    }

    // ── Main loop ──────────────────────────────────────────────────────────

    private suspend fun runLoop() {
        val startTime = System.currentTimeMillis()
        val maxRuntimeMs = keyStore.overnightMaxHours * 3_600_000L

        _state.update { it.copy(phase = OvernightPhase.RUNNING, startedAt = startTime) }
        agentBus.setAgentStatus(AgentRole.ORCHESTRATOR, AgentStatus.WORKING, "Overnight run started")

        val completedTasks = mutableListOf<OvernightTaskResult>()

        while (coroutineContext.isActive) {
            // Max runtime guard
            if (System.currentTimeMillis() - startTime > maxRuntimeMs) {
                log("Max runtime (${keyStore.overnightMaxHours}h) reached — stopping")
                break
            }

            val task = taskQueue.dequeue() ?: break  // No more tasks

            _state.update { it.copy(currentTask = task, completedCount = completedTasks.size) }
            log("▶ Starting task [${task.id}]: ${task.title}")

            val result = executeTask(task)
            completedTasks.add(result)
            taskQueue.markDone(task.id, result.outcome)

            // Store outcome in memory so future runs learn from it
            when (result.outcome) {
                TaskOutcome.SUCCESS -> {
                    memoryInjector.storeTaskOutcome(
                        "Task '${task.title}' succeeded: ${result.summary}",
                        task.repoKey
                    )
                }
                TaskOutcome.FAILED -> {
                    memoryInjector.storeCorrection(
                        "Task '${task.title}' failed: ${result.errorReason}. Avoid this approach.",
                        task.repoKey
                    )
                    skillUpdater.recordFailure(task, result.errorReason ?: "Unknown error")
                }
                TaskOutcome.PARTIAL -> {
                    memoryInjector.storeTaskOutcome(
                        "Task '${task.title}' partially completed: ${result.summary}",
                        task.repoKey
                    )
                }
            }

            // Persist to task log
            taskLogDao.insert(TaskLogEntity(
                userRequest = task.description,
                taskType = task.type.name,
                repoKey = task.repoKey,
                outcome = result.outcome.name,
                summary = result.summary,
                tokensUsed = result.tokensUsed,
                providerUsed = result.providerUsed,
                durationMs = result.durationMs
            ))

            log("${result.outcome.emoji} Task done: ${result.summary.take(80)}")
            delay(2000) // Brief pause between tasks
        }

        // ── Wrap up ───────────────────────────────────────────────────────
        _state.update { it.copy(phase = OvernightPhase.WRAPPING_UP) }
        agentBus.setAgentStatus(AgentRole.ORCHESTRATOR, AgentStatus.WORKING, "Writing overnight summary...")

        try {
            summaryWriter.writeSummary(
                tasks = completedTasks,
                totalDurationMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            log("⚠️ Summary write failed: ${e.message}")
        }

        agentBus.setAgentStatus(AgentRole.ORCHESTRATOR, AgentStatus.WORKING, "Dreaming (Consolidating Memory)...")
        try {
            experienceConsolidator.dream()
            log("🧠 Dream phase complete")
        } catch (e: Exception) {
            log("⚠️ Dream phase failed: ${e.message}")
        }

        // Update skills based on what happened tonight
        skillUpdater.applyLearnings(completedTasks)

        val donePhase = if (coroutineContext.isActive) OvernightPhase.DONE else OvernightPhase.INTERRUPTED
        _state.update {
            it.copy(
                phase = donePhase,
                currentTask = null,
                completedCount = completedTasks.size,
                successCount = completedTasks.count { r -> r.outcome == TaskOutcome.SUCCESS },
                failedCount = completedTasks.count { r -> r.outcome == TaskOutcome.FAILED }
            )
        }

        agentBus.setAgentStatus(AgentRole.ORCHESTRATOR, AgentStatus.DONE,
            "${completedTasks.size} tasks completed")
        log("🌅 Overnight run complete — ${completedTasks.size} tasks in " +
            "${(System.currentTimeMillis() - startTime) / 60_000}min")
    }

    // ── Execute one task ───────────────────────────────────────────────────

    private suspend fun executeTask(task: OvernightTask): OvernightTaskResult {
        val start = System.currentTimeMillis()
        val sb = StringBuilder()
        var errorReason: String? = null

        return try {
            val taskContext = TaskContext(
                repoOwner = task.repoKey.substringBefore("/").takeIf { task.repoKey.contains("/") },
                repoName = task.repoKey.substringAfter("/").takeIf { task.repoKey.contains("/") },
                language = task.language,
                isOvernightRun = true,
                isLongRunning = true,
                taskType = when (task.type) {
                    OvernightTaskType.CODE_FEATURE,
                    OvernightTaskType.CODE_REFACTOR,
                    OvernightTaskType.CODE_FIX,
                    OvernightTaskType.WRITE_TESTS -> TaskType.CODE_WRITE
                    OvernightTaskType.DEPLOY -> TaskType.DEPLOY
                    OvernightTaskType.RESEARCH -> TaskType.RESEARCH
                    else -> TaskType.CHAT
                }
            )

            reActLoop.run(
                userMessage = task.description,
                conversationHistory = emptyList(),
                taskContext = taskContext
            ).collect { event ->
                when (event) {
                    is LoopEvent.StreamComplete -> sb.append(event.fullText)
                    is LoopEvent.FinalAnswer -> sb.append(event.text)
                    is LoopEvent.Error -> errorReason = event.message
                    is LoopEvent.StepLimitReached -> sb.append(event.message)
                    else -> {}
                }
            }

            val outcome = if (errorReason != null) TaskOutcome.FAILED
                          else if (sb.isBlank()) TaskOutcome.PARTIAL
                          else TaskOutcome.SUCCESS

            OvernightTaskResult(
                task = task,
                outcome = outcome,
                summary = sb.take(500).toString(),
                errorReason = errorReason,
                tokensUsed = 0,
                providerUsed = "waterfall",
                durationMs = System.currentTimeMillis() - start
            )
        } catch (e: Exception) {
            OvernightTaskResult(
                task = task,
                outcome = TaskOutcome.FAILED,
                summary = "Exception: ${e.message}",
                errorReason = e.message,
                tokensUsed = 0,
                providerUsed = "none",
                durationMs = System.currentTimeMillis() - start
            )
        }
    }

    // ── Log ───────────────────────────────────────────────────────────────

    private fun log(message: String) {
        Log.d(TAG, message)
        _state.update { it.copy(log = (it.log + LogEntry(message)).takeLast(200)) }
    }
}

// ── State types ───────────────────────────────────────────────────────────

enum class OvernightPhase {
    IDLE, STARTING, RUNNING, WRAPPING_UP, DONE, INTERRUPTED
}

data class OvernightState(
    val phase: OvernightPhase = OvernightPhase.IDLE,
    val currentTask: OvernightTask? = null,
    val completedCount: Int = 0,
    val successCount: Int = 0,
    val failedCount: Int = 0,
    val startedAt: Long = 0L,
    val log: List<LogEntry> = emptyList()
)

data class LogEntry(
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class TaskOutcome(val emoji: String) {
    SUCCESS("✅"), FAILED("❌"), PARTIAL("⚠️")
}

data class OvernightTaskResult(
    val task: OvernightTask,
    val outcome: TaskOutcome,
    val summary: String,
    val errorReason: String?,
    val tokensUsed: Int,
    val providerUsed: String,
    val durationMs: Long
)
