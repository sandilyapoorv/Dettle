package com.dettle.app.orchestrator.telemetry

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AgentLogger"

@Serializable
data class AgentLogEntry(
    val timestamp: Long,
    val formattedTime: String,
    val type: String,
    val modelId: String? = null,
    val content: String,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * A "Glass Box" telemetry system.
 * It records literally everything the AI models do (raw prompts, raw completions, 
 * swarm delegations, and self-healing loops) into a persistent JSON Lines (.jsonl) file.
 * 
 * This runs on a background channel so it never blocks the AI execution loop.
 */
@Singleton
class AgentLogger @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext context: Context,
    private val json: Json
) {
    private val logDir = File(context.filesDir, "agent_telemetry").apply { mkdirs() }
    
    // Create a new log file per day to prevent massive files
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    
    private fun getCurrentLogFile(): File {
        val dateString = dateFormat.format(Date())
        return File(logDir, "dettle_ai_log_$dateString.jsonl")
    }

    // Protects the 250MB RAM limit: Bounded channel drops oldest logs if disk I/O gets stuck
    private val writeChannel = Channel<AgentLogEntry>(capacity = 1000, onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)

    init {
        // Background worker to flush logs to disk sequentially
        CoroutineScope(Dispatchers.IO).launch {
            for (entry in writeChannel) {
                try {
                    pruneOldLogsIfNeeded() // Protects the 10GB storage limit
                    
                    val file = getCurrentLogFile()
                    val jsonLine = json.encodeToString(entry) + "\n"
                    FileOutputStream(file, true).bufferedWriter().use { writer ->
                        writer.append(jsonLine)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to write agent log to disk", e)
                }
            }
        }
    }

    /** 
     * Ensures we never bloat the Moto Edge's 256GB storage.
     * Deletes telemetry files older than 7 days, keeping footprint < 500MB.
     */
    private fun pruneOldLogsIfNeeded() {
        val cutoff = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        logDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".jsonl") && file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }

    fun logPrompt(modelId: String, systemPrompt: String?, userMessages: String) {
        val content = buildString {
            if (systemPrompt != null) appendLine("=== SYSTEM PROMPT ===\n$systemPrompt\n")
            appendLine("=== MESSAGES ===\n$userMessages")
        }
        enqueue("PROMPT_OUTBOUND", content, modelId)
    }

    fun logResponse(modelId: String, rawResponse: String, durationMs: Long, tokens: Int) {
        enqueue(
            type = "MODEL_RESPONSE",
            content = rawResponse,
            modelId = modelId,
            metadata = mapOf("durationMs" to durationMs.toString(), "tokens" to tokens.toString())
        )
    }

    fun logSwarmEvent(agentRole: String, action: String, targetFile: String? = null) {
        val meta = mutableMapOf("role" to agentRole)
        targetFile?.let { meta["targetFile"] = it }
        
        enqueue("SWARM_EVENT", action, metadata = meta)
    }

    fun logHealingEvent(targetFile: String, exitCode: Int, trace: String) {
        enqueue(
            type = "HEALING_LOOP",
            content = trace,
            metadata = mapOf("targetFile" to targetFile, "exitCode" to exitCode.toString())
        )
    }

    fun logToolExecution(toolName: String, args: String, result: String) {
        val content = "ARGS: $args\n\nRESULT:\n$result"
        enqueue("TOOL_EXECUTION", content, metadata = mapOf("toolName" to toolName))
    }

    private fun enqueue(type: String, content: String, modelId: String? = null, metadata: Map<String, String> = emptyMap()) {
        val now = System.currentTimeMillis()
        val entry = AgentLogEntry(
            timestamp = now,
            formattedTime = timeFormat.format(Date(now)),
            type = type,
            modelId = modelId,
            content = content,
            metadata = metadata
        )
        writeChannel.trySend(entry)
    }
}
