package com.dettle.app.orchestrator.context

import android.util.Log
import com.dettle.app.domain.model.AIModel
import com.dettle.app.domain.model.ApiMessage
import com.dettle.app.domain.model.ModelSets
import com.dettle.app.data.api.KeyPoolManager
import com.dettle.app.data.api.StreamChunk
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ContextCompressor"

/**
 * Compresses long conversation histories before they overflow a model's context window.
 *
 * Without compression, long GOAL or RESEARCH sessions accumulate thousands of tokens
 * of tool results, intermediate reasoning, and verbose AI responses. Eventually this
 * either silently truncates (losing critical context) or throws an API error.
 *
 * Strategy:
 * 1. Estimate token count (rough: chars / 4)
 * 2. If history > [COMPRESS_THRESHOLD] of the model's context window → compress
 * 3. Summarize the oldest [COMPRESS_RATIO] of messages with a single AI call
 * 4. Replace those messages with a single [SUMMARY_ROLE] message
 * 5. Keep the most recent messages intact (they're the most relevant)
 *
 * The compression itself uses the FAST model set (Groq 8B) — fast and cheap.
 */
@Singleton
class ContextCompressor @Inject constructor(
    private val keyPoolManager: KeyPoolManager
) {
    companion object {
        /** Compress when estimated tokens > this fraction of context window */
        private const val COMPRESS_THRESHOLD = 0.65f

        /** Summarize this fraction of the oldest messages */
        private const val COMPRESS_RATIO = 0.5f

        /** Rough chars-per-token estimate */
        private const val CHARS_PER_TOKEN = 4
    }

    /**
     * Check if compression is needed and apply it.
     * Returns the (possibly compressed) history.
     *
     * @param history Current message history
     * @param model The AI model being used (determines context window size)
     */
    suspend fun compressIfNeeded(
        history: List<ApiMessage>,
        model: AIModel
    ): List<ApiMessage> {
        val estimatedTokens = history.sumOf { (it.content?.length ?: 0) / CHARS_PER_TOKEN }
        val threshold = (model.contextWindow * COMPRESS_THRESHOLD).toInt()

        if (estimatedTokens <= threshold) return history

        Log.w(TAG, "History too long (~$estimatedTokens tokens, threshold=$threshold). Compressing...")
        return compress(history, model)
    }

    private suspend fun compress(history: List<ApiMessage>, model: AIModel): List<ApiMessage> {
        val cutoff = (history.size * COMPRESS_RATIO).toInt().coerceAtLeast(1)
        val toSummarize = history.take(cutoff)
        val toKeep = history.drop(cutoff)

        val summaryPrompt = buildString {
            appendLine("Summarize the following conversation history concisely.")
            appendLine("Preserve: key decisions made, files read, tool results, errors encountered, and current task state.")
            appendLine("Discard: verbose reasoning, repeated information, tool call metadata.")
            appendLine("Output: a dense paragraph of 200-400 words.")
            appendLine()
            appendLine("=== HISTORY TO SUMMARIZE ===")
            toSummarize.forEach { msg ->
                appendLine("[${msg.role.uppercase()}]: ${msg.content?.take(500) ?: ""}")
            }
        }

        return try {
            val summaryMessages = listOf(ApiMessage(role = "user", content = summaryPrompt))
            var summary = ""

            keyPoolManager.chat(
                messages = summaryMessages,
                systemPrompt = "You are a precise context summarizer. Output only the summary, no preamble.",
                maxTokens = 512
            ).collect { chunk ->
                if (chunk is StreamChunk.Token) summary += chunk.text
            }

            val compressionMarker = ApiMessage(
                role = "system",
                content = "[Context compressed: ${toSummarize.size} messages → summary]\n\n$summary"
            )

            Log.d(TAG, "Compressed ${toSummarize.size} messages into ~${summary.length / 4} tokens")
            listOf(compressionMarker) + toKeep

        } catch (e: Exception) {
            Log.e(TAG, "Compression failed: ${e.message}. Keeping tail of history.")
            // Fallback: just keep the most recent half
            toKeep
        }
    }

    /** Estimate tokens in a history list (rough, fast) */
    fun estimateTokens(history: List<ApiMessage>): Int =
        history.sumOf { (it.content?.length ?: 0) / CHARS_PER_TOKEN }
}
