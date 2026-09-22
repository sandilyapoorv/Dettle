package com.dettle.app.orchestrator

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Multi-agent bus — coordinates specialized agent roles working in parallel.
 *
 * Roles:
 *  ORCHESTRATOR — Breaks tasks into sub-tasks, assigns roles, synthesizes results
 *  READER       — Reads and summarizes repo files, finds relevant code sections
 *  CODER        — Writes the actual code changes
 *  REVIEWER     — Reviews CODER's output, finds bugs/issues, triggers debate rounds
 *  DEPLOYER     — Handles GitHub PR creation and Cloudflare deployment
 *  RESEARCHER   — Web search, documentation lookup, API exploration
 *
 * Debate protocol (CODER vs REVIEWER):
 *  - CODER produces code
 *  - REVIEWER critiques it
 *  - CODER refines based on critique
 *  - Max 3 rounds, then ORCHESTRATOR accepts best version
 *
 * The bus is event-driven — agents post AgentEvents and subscribe to others.
 */
@Singleton
class AgentBus @Inject constructor() {

    private val _agentStates = MutableStateFlow<Map<AgentRole, AgentState>>(
        AgentRole.values().associateWith { AgentState(role = it) }
    )
    val agentStates: StateFlow<Map<AgentRole, AgentState>> = _agentStates.asStateFlow()

    private val _events = MutableStateFlow<List<AgentEvent>>(emptyList())
    val events: StateFlow<List<AgentEvent>> = _events.asStateFlow()

    // ── State management ──────────────────────────────────────────────────

    fun setAgentStatus(role: AgentRole, status: AgentStatus, task: String = "") {
        _agentStates.value = _agentStates.value.toMutableMap().apply {
            this[role] = AgentState(
                role = role,
                status = status,
                currentTask = task,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    fun postEvent(event: AgentEvent) {
        _events.value = (_events.value + event).takeLast(100) // Keep last 100 events
    }

    fun resetAll() {
        _agentStates.value = AgentRole.values().associateWith { AgentState(role = it) }
        _events.value = emptyList()
    }

    // ── Convenience methods ───────────────────────────────────────────────

    fun isAnyAgentBusy(): Boolean =
        _agentStates.value.values.any { it.status == AgentStatus.WORKING }

    fun getBusyAgents(): List<AgentRole> =
        _agentStates.value.entries
            .filter { it.value.status == AgentStatus.WORKING }
            .map { it.key }
}

// ── Agent roles ───────────────────────────────────────────────────────────

enum class AgentRole(val displayName: String, val emoji: String, val description: String) {
    ORCHESTRATOR(
        "Orchestrator",
        "",
        "Breaks tasks into sub-tasks and synthesizes results"
    ),
    READER(
        "Reader",
        "",
        "Reads repos, finds relevant code, builds context"
    ),
    CODER(
        "Coder",
        "",
        "Writes code changes, implements features"
    ),
    REVIEWER(
        "Reviewer",
        "",
        "Reviews code for bugs, style issues, security"
    ),
    DEPLOYER(
        "Deployer",
        "",
        "Creates PRs, triggers Actions, deploys to Cloudflare"
    ),
    RESEARCHER(
        "Researcher",
        "",
        "Searches the web, reads docs, explores APIs"
    )
}

enum class AgentStatus { IDLE, WORKING, WAITING, DONE, FAILED }

data class AgentState(
    val role: AgentRole,
    val status: AgentStatus = AgentStatus.IDLE,
    val currentTask: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)

// ── Agent events ──────────────────────────────────────────────────────────

sealed class AgentEvent {
    data class TaskAssigned(val role: AgentRole, val task: String) : AgentEvent()
    data class TaskCompleted(val role: AgentRole, val result: String) : AgentEvent()
    data class TaskFailed(val role: AgentRole, val error: String) : AgentEvent()
    data class DebateRound(
        val round: Int,
        val coderOutput: String,
        val reviewerCritique: String
    ) : AgentEvent()
    data class DebateResolved(val finalCode: String, val rounds: Int) : AgentEvent()
    data class DeploymentStarted(val target: String) : AgentEvent()
    data class DeploymentComplete(val url: String) : AgentEvent()
    data class Message(val from: AgentRole, val to: AgentRole, val content: String) : AgentEvent()
    data class Interrupt(val reason: String, val score: Int) : AgentEvent()
}
