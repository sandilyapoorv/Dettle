package com.dettle.app.ui.agents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dettle.app.orchestrator.AgentBus
import com.dettle.app.orchestrator.AgentEvent
import com.dettle.app.orchestrator.AgentRole
import com.dettle.app.orchestrator.AgentState
import com.dettle.app.orchestrator.AgentStatus
import com.dettle.app.orchestrator.mcp.McpClient
import com.dettle.app.orchestrator.mcp.McpStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AgentsViewModel @Inject constructor(
    private val agentBus: AgentBus,
    private val mcpClient: McpClient
) : ViewModel() {

    val uiState: StateFlow<AgentsUiState> = combine(
        agentBus.agentStates,
        agentBus.events
    ) { states, events ->
        AgentsUiState(
            agentStates = states,
            recentEvents = events.takeLast(20).reversed(),
            mcpStatus = mcpClient.getStatus(),
            busyCount = states.values.count { it.status == AgentStatus.WORKING }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AgentsUiState()
    )
}

data class AgentsUiState(
    val agentStates: Map<AgentRole, AgentState> = emptyMap(),
    val recentEvents: List<AgentEvent> = emptyList(),
    val mcpStatus: McpStatus = McpStatus(false, false, "Not configured", false),
    val busyCount: Int = 0
)
