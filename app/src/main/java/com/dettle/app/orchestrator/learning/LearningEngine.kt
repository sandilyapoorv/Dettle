package com.dettle.app.orchestrator.learning

import android.util.Log
import com.dettle.app.data.api.KeyPoolManager
import com.dettle.app.data.api.StreamChunk
import com.dettle.app.data.db.dao.UserProfileDao
import com.dettle.app.data.db.entity.UserProfileEntity
import com.dettle.app.orchestrator.MemoryInjector
import com.dettle.app.domain.model.ApiMessage
import com.dettle.app.domain.model.AIModel
import com.dettle.app.domain.model.ALL_KNOWN_MODELS
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "LearningEngine"

/**
 * Processes conversation snapshots in the background and extracts learnings.
 *
 * After every conversation turn, ChatViewModel posts a ConversationSnapshot.
 * LearningEngine drains this channel in the background (on the service coroutine scope)
 * and makes a fast Groq 8B call (~100-200ms) to extract 0-3 things worth remembering.
 *
 * What it extracts:
 * - CORRECTION: user corrected the AI's behavior ("don't use X", "that's wrong")
 * - PREFERENCE: expressed preference ("I prefer concise answers")
 * - FACT: new fact about the user or their project ("I'm building a fitness app")
 * - PROFILE_UPDATE: updates to UserProfile fields (name, focus, stack, etc.)
 *
 * Design: non-blocking. If the AI call fails, we log and move on. No retry.
 * Volume: at most 1 Groq call per conversation turn — negligible cost.
 */
@Singleton
class LearningEngine @Inject constructor(
    private val keyPoolManager: KeyPoolManager,
    private val memoryInjector: MemoryInjector,
    private val userProfileDao: UserProfileDao
) {
    data class ConversationSnapshot(
        val userMessage: String,
        val aiResponse: String,
        val projectId: String? = null
    )

    // Memory & queue backpressure protection: Bounded channel drops oldest snapshots if the queue backs up
    private val queue = Channel<ConversationSnapshot>(capacity = 20, onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)

    /** Post a snapshot for background processing. Non-blocking. */
    fun post(snapshot: ConversationSnapshot) {
        queue.trySend(snapshot).also {
            Log.d(TAG, "Snapshot queued. Success=${it.isSuccess}")
        }
    }

    /** Start draining the queue. Call from the service's coroutine scope. */
    fun startProcessing(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            for (snapshot in queue) {
                try {
                    processSnapshot(snapshot)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing snapshot: ${e.message}")
                }
            }
        }
    }

    private suspend fun processSnapshot(snapshot: ConversationSnapshot) {
        val systemPrompt = """
You are a memory extraction system for an AI coding assistant.
Analyze this conversation exchange and extract 0-3 items worth remembering for future interactions.
Be VERY selective — only extract things that meaningfully affect future behavior.

Extract in this exact format (one per line):
CORRECTION: <what the user wants corrected, specific and actionable>
PREFERENCE: <user preference, specific>
FACT: <key:value> (e.g. "name:Ritesh" or "framework:Jetpack Compose")
PROFILE_FOCUS: <what the user is currently working on>

If nothing is worth extracting, output: NOTHING
Do NOT output anything else. No preamble, no explanation.
        """.trimIndent()

        val userContent = """
User said: ${snapshot.userMessage.take(500)}
AI responded: ${snapshot.aiResponse.take(500)}
        """.trimIndent()

        var response = ""
        keyPoolManager.chat(
            messages = listOf(ApiMessage(role = "user", content = userContent)),
            systemPrompt = systemPrompt,
            maxTokens = 150,
            preferModel = ALL_KNOWN_MODELS.values.firstOrNull()
        ).collect { chunk ->
            if (chunk is StreamChunk.Token) response += chunk.text
        }

        if (response.trim() == "NOTHING" || response.isBlank()) return

        response.lines().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("CORRECTION:") -> {
                    val content = trimmed.removePrefix("CORRECTION:").trim()
                    if (content.isNotBlank()) memoryInjector.storeCorrection(content)
                    Log.d(TAG, "Stored correction: $content")
                }
                trimmed.startsWith("PREFERENCE:") -> {
                    val content = trimmed.removePrefix("PREFERENCE:").trim()
                    if (content.isNotBlank()) {
                        memoryInjector.storeSkillLearned("[PREFERENCE] $content")
                    }
                }
                trimmed.startsWith("FACT:") -> {
                    val content = trimmed.removePrefix("FACT:").trim()
                    if (content.isNotBlank()) {
                        memoryInjector.storeSkillLearned("[FACT] $content")
                    }
                }
                trimmed.startsWith("PROFILE_FOCUS:") -> {
                    val focus = trimmed.removePrefix("PROFILE_FOCUS:").trim()
                    if (focus.isNotBlank()) {
                        val existing = userProfileDao.get()
                        if (existing != null) {
                            userProfileDao.updateFocus(focus)
                        } else {
                            userProfileDao.insert(UserProfileEntity(currentFocus = focus))
                        }
                        Log.d(TAG, "Updated user focus: $focus")
                    }
                }
            }
        }
    }
}
