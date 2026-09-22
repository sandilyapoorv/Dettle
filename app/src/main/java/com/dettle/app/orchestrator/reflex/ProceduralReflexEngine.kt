package com.dettle.app.orchestrator.reflex

import android.util.Log
import com.dettle.app.domain.model.ToolCall
import com.dettle.app.domain.model.ToolResult
import com.dettle.app.orchestrator.LoopEvent
import com.dettle.app.orchestrator.ToolExecutor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Motor Cortex of the system.
 * Handles procedural reflexes — hardcoded sequences of tool calls that bypass the LLM entirely.
 * Used for common, highly structured tasks (e.g., "clear workspace", "ping") to save tokens and latency.
 */
@Singleton
class ProceduralReflexEngine @Inject constructor(
    private val toolExecutor: ToolExecutor
) {
    companion object {
        private const val TAG = "ReflexEngine"
    }

    /**
     * Checks if the user's prompt matches a known reflexive action.
     * Returns a Flow of LoopEvents representing the immediate execution, or null if no reflex matched.
     */
    fun tryReflex(userMessage: String): Flow<LoopEvent>? {
        val lowerMsg = userMessage.lowercase().trim()

        // Reflex 1: Clear the workspace
        if (lowerMsg == "clear workspace" || lowerMsg == "empty workspace") {
            return buildReflexFlow(
                toolName = "workspace_delete_file",
                args = mapOf("path" to "/") // Deletes everything in scratchpad
            )
        }

        // Reflex 2: Ping/Healthcheck
        if (lowerMsg == "ping" || lowerMsg == "healthcheck") {
            return flow {
                emit(LoopEvent.FinalAnswer("Pong! My Motor Cortex intercepted this. No tokens used. ⚡"))
            }
        }
        
        // Reflex 3: Fast Web Search
        if (lowerMsg.startsWith("fast search ")) {
            val query = userMessage.removePrefix("fast search ").trim()
            return buildReflexFlow(
                toolName = "web_search",
                args = mapOf("query" to query, "num_results" to "3")
            )
        }

        return null
    }

    private fun buildReflexFlow(toolName: String, args: Map<String, String>): Flow<LoopEvent> = flow {
        Log.d(TAG, "⚡ Motor Cortex reflex triggered: $toolName")
        
        // Let UI know we are "thinking" (actually just reflexing)
        emit(LoopEvent.Thinking(1, 1))
        
        val toolCall = ToolCall(name = toolName, arguments = args)
        emit(LoopEvent.ExecutingTool(toolCall))
        
        val result = try {
            toolExecutor.execute(toolCall)
        } catch (e: Exception) {
            ToolResult(
                toolCallId = toolCall.id,
                toolName = toolName,
                content = "Error: ${e.message}",
                isError = true
            )
        }
        
        emit(LoopEvent.ToolResultReceived(result))
        
        val finalMessage = if (result.isError) {
            "Reflex failed: ${result.content}"
        } else {
            "Reflex action complete.\n\n${result.content.take(500)}"
        }
        emit(LoopEvent.FinalAnswer(finalMessage))
    }
}
