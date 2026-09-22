package com.dettle.app.ui.overnight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dettle.app.data.settings.ApiKeyStore
import com.dettle.app.orchestrator.overnight.OvernightLoop
import com.dettle.app.orchestrator.overnight.OvernightPhase
import com.dettle.app.orchestrator.overnight.OvernightState
import com.dettle.app.orchestrator.overnight.OvernightTask
import com.dettle.app.orchestrator.overnight.OvernightTaskType
import com.dettle.app.orchestrator.overnight.TaskOutcome
import com.dettle.app.orchestrator.overnight.TaskPriority
import com.dettle.app.orchestrator.overnight.TaskQueue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OvernightViewModel @Inject constructor(
    private val overnightLoop: OvernightLoop,
    private val taskQueue: TaskQueue,
    private val keyStore: ApiKeyStore
) : ViewModel() {

    private val _addTaskState = MutableStateFlow(AddTaskState())
    val addTaskState: StateFlow<AddTaskState> = _addTaskState

    val uiState: StateFlow<OvernightUiState> = combine(
        overnightLoop.state,
        taskQueue.tasks
    ) { loopState, tasks ->
        OvernightUiState(
            loopState = loopState,
            tasks = tasks,
            pendingCount = tasks.count { it.status.name == "PENDING" },
            isRunning = overnightLoop.isRunning,
            maxHours = keyStore.overnightMaxHours,
            scheduledTime = keyStore.overnightScheduledTime,
            autoStart = keyStore.overnightAutoStart
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        OvernightUiState()
    )

    // ── Queue management ───────────────────────────────────────────────────

    fun addTask(title: String, description: String, type: OvernightTaskType,
                priority: TaskPriority, repoKey: String) {
        if (title.isBlank() || description.isBlank()) return
        taskQueue.enqueue(OvernightTask(
            title = title.trim(),
            description = description.trim(),
            type = type,
            priority = priority,
            repoKey = repoKey.trim()
        ))
        _addTaskState.value = AddTaskState()  // Reset form
    }

    fun removeTask(taskId: String) = taskQueue.remove(taskId)
    fun retryFailed() = taskQueue.resetFailed()
    fun clearDone() = taskQueue.clearCompleted()

    // ── Loop control ───────────────────────────────────────────────────────

    fun startOvernight() {
        if (!taskQueue.hasWork) return
        overnightLoop.start()
    }

    fun stopOvernight() = overnightLoop.stop()
    fun interruptOvernight() = overnightLoop.interrupt()

    // ── Settings ───────────────────────────────────────────────────────────

    fun setMaxHours(hours: Long) { keyStore.overnightMaxHours = hours }
    fun setAutoStart(enabled: Boolean) { keyStore.overnightAutoStart = enabled }
    fun setScheduledTime(time: String) { keyStore.overnightScheduledTime = time }

    // ── Add task form ──────────────────────────────────────────────────────

    fun updateAddTaskTitle(v: String) = _addTaskState.update { it.copy(title = v) }
    fun updateAddTaskDesc(v: String) = _addTaskState.update { it.copy(description = v) }
    fun updateAddTaskType(v: OvernightTaskType) = _addTaskState.update { it.copy(type = v) }
    fun updateAddTaskPriority(v: TaskPriority) = _addTaskState.update { it.copy(priority = v) }
    fun updateAddTaskRepo(v: String) = _addTaskState.update { it.copy(repoKey = v) }
    fun showAddTask() = _addTaskState.update { it.copy(showing = true) }
    fun hideAddTask() = _addTaskState.value = AddTaskState()
}

data class OvernightUiState(
    val loopState: OvernightState = OvernightState(),
    val tasks: List<OvernightTask> = emptyList(),
    val pendingCount: Int = 0,
    val isRunning: Boolean = false,
    val maxHours: Long = 8,
    val scheduledTime: String = "23:00",
    val autoStart: Boolean = false
)

data class AddTaskState(
    val showing: Boolean = false,
    val title: String = "",
    val description: String = "",
    val type: OvernightTaskType = OvernightTaskType.CUSTOM,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val repoKey: String = ""
)
