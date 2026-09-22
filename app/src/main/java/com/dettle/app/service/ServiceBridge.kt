package com.dettle.app.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Zero-AIDL IPC bridge between [AgentForegroundService] and the Activity/ViewModel layer.
 *
 * Both service and activity live in the same process, so we can use shared coroutine flows.
 * No serialization, no binders, no intent extras for high-frequency updates.
 *
 * Pattern:
 *   Service emits -> DaemonEvent -> ViewModel observes
 *   ViewModel emits -> Command -> Service handles
 *
 * This object is process-global (singleton by Kotlin object semantics).
 */
object ServiceBridge {

    // ── Daemon → Activity ──────────────────────────────────────────────────

    private val _events = MutableSharedFlow<DaemonEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<DaemonEvent> = _events.asSharedFlow()

    suspend fun emit(event: DaemonEvent) = _events.emit(event)
    fun tryEmit(event: DaemonEvent) = _events.tryEmit(event)

    // ── Activity → Daemon ──────────────────────────────────────────────────

    private val _commands = MutableSharedFlow<Command>(extraBufferCapacity = 16)
    val commands: SharedFlow<Command> = _commands.asSharedFlow()

    suspend fun send(command: Command) = _commands.emit(command)
    fun trySend(command: Command) = _commands.tryEmit(command)
}

// ─── Events (daemon → activity) ───────────────────────────────────────────────

sealed class DaemonEvent {
    data class StatusUpdate(val status: String, val icon: String = "🤖") : DaemonEvent()
    data class GoalProgressUpdate(val goalId: String, val gateId: String, val passed: Boolean) : DaemonEvent()
    data class GoalCompleted(val goalId: String, val summary: String) : DaemonEvent()
    data class HeartbeatAlert(val source: String, val title: String, val body: String) : DaemonEvent()
    data class ProactiveSuggestion(val message: String) : DaemonEvent()
    data class LearningComplete(val extracted: Int) : DaemonEvent()  // how many items extracted
    object ServiceStarted : DaemonEvent()
    object ServiceStopped : DaemonEvent()
}

// ─── Commands (activity → daemon) ─────────────────────────────────────────────

sealed class Command {
    data class StartGoal(val goalId: String) : Command()
    data class StopGoal(val goalId: String) : Command()
    object StartOvernightLoop : Command()
    object StopOvernightLoop : Command()
    data class UpdateStatus(val status: String) : Command()
}
