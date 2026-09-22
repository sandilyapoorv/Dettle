package com.dettle.app.orchestrator.mode

import android.util.Log
import com.dettle.app.data.api.KeyPoolManager
import com.dettle.app.data.api.StreamChunk
import com.dettle.app.domain.model.ApiMessage
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ModeRouter"

/**
 * Classifies user intent into a [ModeId] using a fast AI call.
 *
 * Uses Groq Llama 8B — typically < 500ms. Falls back to [ModeId.CHAT] on any failure.
 *
 * The classification is one-shot and structured: the model is forced to output
 * exactly one of the valid mode IDs as its first token, so parsing is trivial.
 *
 * When confidence is low (ambiguous message), returns [ModeId.CHAT] and sets
 * [ClassificationResult.isAmbiguous] = true, letting the ViewModel decide whether
 * to show a mode-picker bottom sheet.
 */
@Singleton
class ModeRouter @Inject constructor(
    private val keyPoolManager: KeyPoolManager
) {
    data class ClassificationResult(
        val modeId: ModeId,
        val isAmbiguous: Boolean = false,
        val confidence: String = "HIGH"   // HIGH / MEDIUM / LOW
    )

    suspend fun classify(
        userMessage: String,
        history: List<ApiMessage> = emptyList()
    ): ClassificationResult {
        return try {
            val response = callClassifier(userMessage, history)
            parseResponse(response)
        } catch (e: Exception) {
            Log.w(TAG, "Classification failed: ${e.message}. Defaulting to CHAT.")
            ClassificationResult(modeId = ModeId.CHAT, isAmbiguous = true, confidence = "LOW")
        }
    }

    private suspend fun callClassifier(message: String, history: List<ApiMessage>): String {
        val recentHistory = history.takeLast(4)
            .joinToString("\n") { "[${it.role}]: ${it.content?.take(200) ?: ""}" }

        val systemPrompt = """
You are an intent classifier for an AI coding agent. Classify the user's message into exactly ONE mode.

MODES:
- CHAT: Casual question, general conversation, no specific task
- RESEARCH: Investigate a codebase, library, concept, or technical topic in depth  
- CODE: Write code, fix bugs, implement features, refactor — anything that produces commits
- PLAN: Create an implementation plan, architecture design, or technical spec — no code
- GOAL: Long multi-step autonomous task the agent should complete without interruption
- WEB: Search the web, find information, research news or current events
- REVIEW: Review code, a PR, or a diff — produce ranked findings
- DEPLOY: Build, test, release, trigger CI/CD, publish to Cloudflare

Reply with EXACTLY this format on one line:
MODE: <mode_id> CONFIDENCE: <HIGH|MEDIUM|LOW>

Examples:
"fix the login bug and open a PR" → MODE: CODE CONFIDENCE: HIGH
"what are the best practices for Kotlin coroutines?" → MODE: RESEARCH CONFIDENCE: HIGH
"search for the latest Android 15 features" → MODE: WEB CONFIDENCE: HIGH
"plan how to implement offline mode" → MODE: PLAN CONFIDENCE: HIGH
"build, sign and release the APK" → MODE: DEPLOY CONFIDENCE: HIGH
"hello" → MODE: CHAT CONFIDENCE: HIGH
        """.trimIndent()

        val messages = buildList {
            if (recentHistory.isNotBlank()) {
                add(ApiMessage(role = "user", content = "Recent context:\n$recentHistory"))
                add(ApiMessage(role = "assistant", content = "Understood. I'll use that context."))
            }
            add(ApiMessage(role = "user", content = "Classify: \"$message\""))
        }

        var result = ""
        keyPoolManager.chat(
            messages = messages,
            systemPrompt = systemPrompt,
            maxTokens = 20,
            preferredModelId = com.dettle.app.domain.model.FreeModels.GROQ_LLAMA_8B.modelId
        ).collect { chunk ->
            if (chunk is StreamChunk.Token) result += chunk.text
        }
        return result.trim()
    }

    private fun parseResponse(response: String): ClassificationResult {
        // Expected: "MODE: CODE CONFIDENCE: HIGH"
        val modeMatch = Regex("""MODE:\s*(\w+)""").find(response)
        val confidenceMatch = Regex("""CONFIDENCE:\s*(\w+)""").find(response)

        val modeStr = modeMatch?.groupValues?.getOrNull(1)?.uppercase()
        val confidence = confidenceMatch?.groupValues?.getOrNull(1)?.uppercase() ?: "HIGH"

        val modeId = modeStr?.let {
            runCatching { ModeId.valueOf(it) }.getOrNull()
        } ?: ModeId.CHAT

        val isAmbiguous = confidence == "LOW" || modeId == ModeId.CHAT && modeStr != "CHAT"

        Log.d(TAG, "Classified: $modeId (confidence=$confidence) from response: $response")
        return ClassificationResult(modeId, isAmbiguous, confidence)
    }
}
