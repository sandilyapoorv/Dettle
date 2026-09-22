package com.dettle.app.orchestrator.swarm

import android.util.Log
import com.dettle.app.data.api.KeyPoolManager
import com.dettle.app.data.api.StreamChunk
import com.dettle.app.domain.model.AIModel
import com.dettle.app.domain.model.ApiMessage
import com.dettle.app.domain.model.ModelSets
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SwarmOrchestrator"

/**
 * The Swarm Orchestrator manages a hierarchy of specialized agents.
 * It enforces strictly different perspectives to ensure robust code generation.
 */
@Singleton
class SwarmOrchestrator @Inject constructor(
    private val keyPoolManager: KeyPoolManager,
    private val json: Json,
    private val agentLogger: com.dettle.app.orchestrator.telemetry.AgentLogger
) {
    /**
     * Executes a complex user request using a Multi-Agent Swarm.
     * 1. Planner generates Architecture & Task List
     * 2. Workers execute tasks in parallel (Isolated perspective)
     * 3. Critic reviews each output (Adversarial perspective)
     */
    suspend fun executeSwarmTask(userGoal: String): String {
        Log.d(TAG, "Starting Swarm Execution for goal: ${userGoal.take(50)}...")

        // Step 1: The Planner Architect (Deep Reasoning Model)
        val plan = generateArchitecturePlan(userGoal)
        if (plan == null || plan.subTasks.isEmpty()) {
            return "Swarm Failed: Chief Architect could not generate a plan."
        }
        
        Log.d(TAG, "Architect generated ${plan.subTasks.size} tasks. Spawning workers in parallel...")
        agentLogger.logSwarmEvent("PLANNER", "Generated ${plan.subTasks.size} parallel tasks for architecture: ${plan.overallArchitecture}")

        // Step 2 & 3: Workers and Critics in parallel
        val finalResults = coroutineScope {
            plan.subTasks.map { task ->
                async {
                    processTaskWithWorkerAndCritic(task, plan.overallArchitecture)
                }
            }.awaitAll()
        }

        // Aggregate final result
        return buildString {
            appendLine("## Swarm Execution Complete")
            appendLine("Architect Plan: ${plan.overallArchitecture}")
            appendLine()
            finalResults.forEach { result ->
                appendLine("### File: ${result.subTask.targetFile} [${result.status}]")
                if (result.status == ResultStatus.REJECTED) {
                    appendLine("> Critic Rejection: ${result.criticFeedback}")
                }
                appendLine("```kotlin\n${result.generatedCode}\n```\n")
            }
        }
    }

    private suspend fun generateArchitecturePlan(userGoal: String): SwarmArchitecturePlan? {
        val prompt = buildString {
            appendLine(AgentRole.PLANNER.perspective)
            appendLine()
            appendLine("USER GOAL: $userGoal")
            appendLine()
            appendLine("OUTPUT FORMAT: You MUST return raw JSON matching this schema:")
            appendLine("""{
              "overallArchitecture": "Brief string describing the system design",
              "subTasks": [
                {
                  "id": "T1",
                  "description": "Exact implementation details for this file",
                  "targetFile": "Path/To/File.kt",
                  "requiredDependencies": ["T2"]
                }
              ]
            }""")
        }

        var jsonResponse = ""
        // Use a Deep Reasoning model for the Planner
        keyPoolManager.chat(
            messages = listOf(ApiMessage("user", prompt)),
            systemPrompt = AgentRole.PLANNER.perspective,
            preferModel = ModelSets.DEEP_REASONING.first()
        ).collect { chunk ->
            if (chunk is StreamChunk.Token) {
                jsonResponse += chunk.text
            }
        }

        return try {
            // Extract JSON from markdown blocks if present
            val cleanedJson = jsonResponse.substringAfter("```json").substringBefore("```").trim()
            val finalJson = if (cleanedJson.isEmpty()) jsonResponse else cleanedJson
            
            json.decodeFromString<SwarmArchitecturePlan>(finalJson)
        } catch (e: Exception) {
            Log.e(TAG, "Planner failed to output valid JSON", e)
            null
        }
    }

    private suspend fun processTaskWithWorkerAndCritic(
        task: SwarmSubTask,
        architectureContext: String
    ): WorkerResult {
        
        var currentCode = runWorker(task, architectureContext)
        var result = WorkerResult(task, currentCode)

        // Give the critic up to 2 chances to bounce it back to the worker
        var retries = 0
        while (retries < 2) {
            val critique = runCritic(task, currentCode)
            if (critique.contains("APPROVED", ignoreCase = true)) {
                result.status = ResultStatus.APPROVED
                result.criticFeedback = "Approved by Security Critic."
                break
            } else {
                Log.w(TAG, "Critic REJECTED ${task.targetFile}. Retrying... ($retries/2)")
                result.status = ResultStatus.REJECTED
                result.criticFeedback = critique
                
                // Worker tries again with the critic's brutal feedback
                currentCode = runWorker(task, architectureContext, critique)
                result = WorkerResult(task, currentCode)
                retries++
            }
        }
        
        return result
    }

    private suspend fun runWorker(
        task: SwarmSubTask, 
        architectureContext: String, 
        criticFeedback: String? = null
    ): String {
        val prompt = buildString {
            appendLine(AgentRole.WORKER.perspective)
            appendLine("You are assigned to build: ${task.targetFile}")
            appendLine("Task: ${task.description}")
            if (criticFeedback != null) {
                appendLine("WARNING! YOUR PREVIOUS CODE WAS REJECTED BY THE CRITIC:")
                appendLine(criticFeedback)
                appendLine("FIX THE ISSUES IDENTIFIED BY THE CRITIC.")
            }
        }

        var code = ""
        // Use a Fast/Cheap model for the Isolated Worker
        keyPoolManager.chat(
            messages = listOf(ApiMessage("user", prompt)),
            systemPrompt = AgentRole.WORKER.perspective,
            preferModel = ModelSets.FAST.first() 
        ).collect { chunk ->
            if (chunk is StreamChunk.Token) code += chunk.text
        }
        return code
    }

    private suspend fun runCritic(task: SwarmSubTask, code: String): String {
        val prompt = buildString {
            appendLine(AgentRole.CRITIC.perspective)
            appendLine("Review this implementation for ${task.targetFile}:")
            appendLine("```kotlin")
            appendLine(code)
            appendLine("```")
            appendLine("Reply with exactly 'APPROVED' if perfect, or list all REJECTIONS.")
        }

        var feedback = ""
        // Use a high-quality model for the Critic to catch subtle bugs
        keyPoolManager.chat(
            messages = listOf(ApiMessage("user", prompt)),
            systemPrompt = AgentRole.CRITIC.perspective,
            preferModel = ModelSets.CODE_REVIEW.first()
        ).collect { chunk ->
            if (chunk is StreamChunk.Token) feedback += chunk.text
        }
        return feedback
    }
}
