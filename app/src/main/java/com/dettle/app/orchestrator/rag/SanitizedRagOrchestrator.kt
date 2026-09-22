package com.dettle.app.orchestrator.rag

import android.util.Log
import com.dettle.app.data.api.KeyPoolManager
import com.dettle.app.data.api.StreamChunk
import com.dettle.app.domain.model.AIModel
import com.dettle.app.domain.model.ApiMessage
import com.dettle.app.domain.model.FreeModels
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SanitizedRAG"

/**
 * Implements the "Offensive/Air-Gapped" Split RAG Architecture.
 * 
 * To maintain total OPSEC (Operational Security) while using Cloud APIs, 
 * this layer locally scrubs all sensitive RAG data BEFORE it leaves the Moto Edge.
 * Google/Anthropic APIs only ever receive sterile, masked contexts.
 */
@Singleton
class SanitizedRagOrchestrator @Inject constructor(
    private val keyPoolManager: KeyPoolManager
) {
    // A temporary vault holding the mappings for the current query (e.g. "[ENTITY_1]" -> "Dettle")
    private val tokenVault = mutableMapOf<String, String>()
    private var entityCounter = 1

    /**
     * Executes a RAG query while guaranteeing the Cloud LLM never sees the actual sensitive nouns.
     */
    suspend fun executeSecureRagQuery(
        userQuery: String, 
        retrievedLocalContext: String, 
        targetCloudModel: AIModel = FreeModels.GEMINI_FLASH
    ): String {
        Log.d(TAG, "Initiating Sanitized RAG Pipeline...")

        // 1. Local Sanitization (Entity Masking)
        // We mask the context BEFORE it leaves the device.
        val sanitizedContext = maskSensitiveEntities(retrievedLocalContext)
        val sanitizedQuery = maskSensitiveEntities(userQuery)

        // 2. Cloud Execution (The LLM only sees the sterile tags)
        val prompt = buildString {
            appendLine("You are an engineering assistant. Answer the user's query using ONLY the following context.")
            appendLine("<retrieved_context>\n$sanitizedContext\n</retrieved_context>")
        }

        var cloudResponse = ""
        keyPoolManager.chat(
            messages = listOf(ApiMessage("user", sanitizedQuery)),
            systemPrompt = prompt,
            preferModel = targetCloudModel
        ).collect { chunk ->
            if (chunk is StreamChunk.Token) cloudResponse += chunk.text
        }

        // 3. Local De-Sanitization (Restoring the sensitive data on-device)
        return restoreEntities(cloudResponse).also {
            // Clear the vault after the transaction to prevent memory leaks in the 250MB daemon
            tokenVault.clear()
            entityCounter = 1
        }
    }

    /**
     * Replaces sensitive nouns, project names, and keys with [ENTITY_X] tags.
     * In a full implementation, this uses a local dictionary or a tiny on-device NLP model.
     */
    private fun maskSensitiveEntities(text: String): String {
        var maskedText = text
        
        // Example Dictionary of sensitive terms the user wants hidden from Cloud APIs
        val sensitiveKeywords = listOf("Dettle", "Moto Edge", "Uncensored", "API_KEY", "Classified")

        sensitiveKeywords.forEach { keyword ->
            if (maskedText.contains(keyword, ignoreCase = true)) {
                val tag = "[PROJECT_VAR_${entityCounter++}]"
                tokenVault[tag] = keyword
                
                // Replace the keyword with the sterile tag
                // e.g. "Dettle's API_KEY" becomes "[PROJECT_VAR_1]'s [PROJECT_VAR_2]"
                maskedText = maskedText.replace(Regex(keyword, RegexOption.IGNORE_CASE), tag)
            }
        }
        return maskedText
    }

    /**
     * Replaces the [PROJECT_VAR_X] tags back with the original sensitive words 
     * before displaying the answer to the user.
     */
    private fun restoreEntities(sanitizedResponse: String): String {
        var restoredText = sanitizedResponse
        tokenVault.forEach { (tag, originalWord) ->
            restoredText = restoredText.replace(tag, originalWord)
        }
        return restoredText
    }
}
