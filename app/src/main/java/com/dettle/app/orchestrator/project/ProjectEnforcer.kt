package com.dettle.app.orchestrator.project

import android.util.Log
import com.dettle.app.data.api.KeyPoolManager
import com.dettle.app.data.api.StreamChunk
import com.dettle.app.data.db.dao.ProjectDao
import com.dettle.app.domain.model.ApiMessage
import com.dettle.app.domain.model.ModelSets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ProjectEnforcer"

/**
 * Parses user-provided raw "Custom Instructions" into a strict JSON Enforced Protocol.
 * The AI will then read this JSON on every prompt to strictly adhere to the rules.
 */
@Singleton
class ProjectEnforcer @Inject constructor(
    private val keyPoolManager: KeyPoolManager,
    private val projectDao: ProjectDao
) {

    /**
     * Triggered when the user taps "Enforce Rules" in the Project Settings UI.
     * Takes the raw instructions and asks the AI to structure them.
     */
    suspend fun enforceInstructions(projectId: String) = withContext(Dispatchers.IO) {
        val project = projectDao.getProjectById(projectId) ?: return@withContext
        val rawInstructions = project.systemInstructions

        if (rawInstructions.isBlank()) {
            projectDao.updateProject(project.copy(enforcedProtocolJson = null))
            return@withContext
        }

        Log.d(TAG, "Compiling raw instructions into a strict Enforced Protocol for $projectId...")

        val systemPrompt = """
            You are a strict protocol compiler. The user has provided custom instructions for how the AI must behave in this project.
            Convert their raw text into a strict JSON configuration file.
            
            OUTPUT FORMAT: JSON with these keys:
            {
                "tone": "Brief description of personality/tone",
                "forbidden_actions": ["List of things the AI must NEVER do"],
                "mandatory_actions": ["List of things the AI MUST ALWAYS do"],
                "code_style": "Instructions related to coding standards (if any)"
            }
            Do not output anything except the JSON.
        """.trimIndent()

        var jsonResponse = ""
        keyPoolManager.chat(
            messages = listOf(ApiMessage("user", rawInstructions)),
            systemPrompt = systemPrompt,
            preferModel = ModelSets.FAST.first() // Fast structured JSON output
        ).collect { chunk ->
            if (chunk is StreamChunk.Token) jsonResponse += chunk.text
        }

        try {
            val cleanedJson = jsonResponse.substringAfter("```json").substringBefore("```").trim()
            projectDao.updateProject(project.copy(enforcedProtocolJson = cleanedJson))
            Log.d(TAG, "Successfully enforced rules for Project: ${project.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compile custom instructions into JSON", e)
        }
    }
}
