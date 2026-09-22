package com.dettle.app.orchestrator.overnight

import android.util.Log
import com.dettle.app.data.settings.ApiKeyStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TaskQueue"

/**
 * Persistent queue of tasks for the overnight loop.
 *
 * Tasks survive app restarts — serialized to EncryptedSharedPreferences.
 * Ordering: tasks run in priority order (HIGH → NORMAL → LOW), then FIFO.
 *
 * Task types:
 *  CODE_FEATURE    — implement a new feature in a GitHub repo
 *  CODE_REFACTOR   — refactor existing code
 *  CODE_FIX        — fix a known bug
 *  WRITE_TESTS     — write unit tests for an existing class
 *  DEPLOY          — build and deploy a website to Cloudflare Pages
 *  DOCS            — write/update documentation
 *  RESEARCH        — research a topic and write a report
 *  CUSTOM          — freeform agent task
 */
@Singleton
class TaskQueue @Inject constructor(
    private val keyStore: ApiKeyStore,
    private val json: Json
) {
    private val _tasks = MutableStateFlow<List<OvernightTask>>(emptyList())
    val tasks: StateFlow<List<OvernightTask>> = _tasks.asStateFlow()

    init {
        load()
    }

    // ── Queue operations ───────────────────────────────────────────────────

    fun enqueue(task: OvernightTask) {
        _tasks.update { current ->
            (current + task).sortedWith(
                compareByDescending<OvernightTask> { it.priority.ordinal }
                    .thenBy { it.createdAt }
            )
        }
        persist()
        Log.d(TAG, "Queued task [${task.id}]: ${task.title}")
    }

    fun dequeue(): OvernightTask? {
        val pending = _tasks.value.filter { it.status == OvernightTaskStatus.PENDING }
        val next = pending.firstOrNull() ?: return null
        _tasks.update { tasks ->
            tasks.map { if (it.id == next.id) it.copy(status = OvernightTaskStatus.RUNNING) else it }
        }
        persist()
        return next
    }

    fun markDone(taskId: String, outcome: TaskOutcome) {
        val status = when (outcome) {
            TaskOutcome.SUCCESS -> OvernightTaskStatus.DONE
            TaskOutcome.FAILED -> OvernightTaskStatus.FAILED
            TaskOutcome.PARTIAL -> OvernightTaskStatus.PARTIAL
        }
        _tasks.update { tasks ->
            tasks.map { if (it.id == taskId) it.copy(status = status) else it }
        }
        persist()
    }

    fun remove(taskId: String) {
        _tasks.update { it.filter { t -> t.id != taskId } }
        persist()
    }

    fun reorder(taskId: String, newIndex: Int) {
        val current = _tasks.value.toMutableList()
        val idx = current.indexOfFirst { it.id == taskId }
        if (idx == -1) return
        val task = current.removeAt(idx)
        current.add(newIndex.coerceIn(0, current.size), task)
        _tasks.value = current
        persist()
    }

    fun clearCompleted() {
        _tasks.update { tasks ->
            tasks.filter { it.status == OvernightTaskStatus.PENDING || it.status == OvernightTaskStatus.RUNNING }
        }
        persist()
    }

    fun resetFailed() {
        _tasks.update { tasks ->
            tasks.map { if (it.status == OvernightTaskStatus.FAILED) it.copy(status = OvernightTaskStatus.PENDING) else it }
        }
        persist()
    }

    val pendingCount: Int get() = _tasks.value.count { it.status == OvernightTaskStatus.PENDING }
    val hasWork: Boolean get() = pendingCount > 0

    // ── Persistence ───────────────────────────────────────────────────────

    private fun persist() {
        try {
            keyStore.taskQueueJson = json.encodeToString(_tasks.value)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist task queue", e)
        }
    }

    private fun load() {
        try {
            val saved = keyStore.taskQueueJson
            if (!saved.isNullOrBlank()) {
                _tasks.value = json.decodeFromString(saved)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load task queue, starting fresh", e)
            _tasks.value = emptyList()
        }
    }
}

// ── Task model ─────────────────────────────────────────────────────────────

@Serializable
data class OvernightTask(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String,          // Full prompt sent to agent
    val type: OvernightTaskType = OvernightTaskType.CUSTOM,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val repoKey: String = "",         // "owner/repo" if task touches a repo
    val language: String = "kotlin",
    val status: OvernightTaskStatus = OvernightTaskStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList()
)

enum class OvernightTaskType {
    CODE_FEATURE, CODE_REFACTOR, CODE_FIX, WRITE_TESTS, DEPLOY, DOCS, RESEARCH, CUSTOM
}

enum class TaskPriority { LOW, NORMAL, HIGH, CRITICAL }

enum class OvernightTaskStatus {
    PENDING, RUNNING, DONE, FAILED, PARTIAL, SKIPPED
}
