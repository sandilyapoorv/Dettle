package com.dettle.app.orchestrator.alignment

import android.util.Log
import com.dettle.app.data.api.KeyPoolManager
import com.dettle.app.data.api.StreamChunk
import com.dettle.app.domain.model.ApiMessage
import com.dettle.app.domain.model.ModelSets
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "IntentAligner"

@Serializable
data class AlignmentResult(
    val isClear: Boolean,
    val assumptionsMade: List<String>,
    val clarifyingQuestions: List<String>
)

/**
 * The Intent Aligner sits in front of the Swarm.
 * Its entire purpose is to prevent the AI from guessing.
 * If the user's prompt is underspecified, this agent blocks execution 
 * and interviews the user until the exact intent is locked in.
 */
@Singleton
class IntentAligner @Inject constructor(
    private val keyPoolManager: KeyPoolManager,
    private val json: Json
) {
    /**
     * Evaluates a user request to determine if it is fully specified.
     * Returns an AlignmentResult. If isClear == false, the UI should prompt the user 
     * with the clarifyingQuestions before allowing the Swarm to start.
     */
    suspend fun evaluateIntent(userGoal: String, projectContext: String): AlignmentResult {
        Log.d(TAG, "Running Pre-Flight Intent Alignment...")

        val systemPrompt = """
            You are the Pre-Flight Intent Aligner.
            Your job is to read the user's request and aggressively identify AMBIGUITY.
            Do not solve the problem. Do not write code.
            
            Look for missing constraints:
            - Did they specify the UI framework?
            - Did they specify how to handle edge cases or errors?
            - Are they asking for a database change without specifying the schema?
            
            If the prompt is highly specific and ready to code, set "isClear" to true.
            If the prompt is vague (e.g., "build a login screen"), set "isClear" to false 
            and output 1-3 highly specific questions to ask the user.
            
            OUTPUT FORMAT: JSON matching this schema:
            {
              "isClear": false,
              "assumptionsMade": ["Assuming user wants Firebase auth"],
              "clarifyingQuestions": ["Do you want Google OAuth or Email/Password?"]
            }
        """.trimIndent()

        var jsonResponse = ""
        keyPoolManager.chat(
            messages = listOf(
                ApiMessage("system", "Project Context:\n$projectContext"),
                ApiMessage("user", userGoal)
            ),
            systemPrompt = systemPrompt,
            preferModel = ModelSets.FAST.first() // Use a fast model for instant UI feedback
        ).collect { chunk ->
            if (chunk is StreamChunk.Token) jsonResponse += chunk.text
        }

        return try {
            val cleaned = jsonResponse.substringAfter("```json").substringBefore("```").trim()
            json.decodeFromString<AlignmentResult>(if (cleaned.isEmpty()) jsonResponse else cleaned)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse alignment check. Defaulting to clear.", e)
            AlignmentResult(isClear = true, emptyList(), emptyList())
        }
    }
}
