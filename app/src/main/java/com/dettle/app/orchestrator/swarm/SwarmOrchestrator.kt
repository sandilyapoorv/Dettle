package com.dettle.app.orchestrator.swarm

import android.util.Log
import com.dettle.app.data.api.KeyPoolManager
import com.dettle.app.data.api.StreamChunk
import com.dettle.app.domain.model.AIModel
import com.dettle.app.domain.model.ApiMessage
import com.dettle.app.domain.model.FreeModels
import com.dettle.app.domain.model.ModelSets
import com.dettle.app.orchestrator.LoopEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SwarmOrchestrator"

/** Maximum parallel agents spawned in Swarm chat mode */
private const val MAX_SWARM_AGENTS = 20

/**
 * The Swarm Orchestrator manages two distinct swarm patterns:
 *
 * 1. [executeChatSwarm] — Chat Swarm Mode (new).
 *    Fires up to [MAX_SWARM_AGENTS] parallel agents with diverse model/persona combos.
 *    A "Sally Judge" (fast Gemini Flash call) picks the single best response.
 *    Returns a [Flow]<[LoopEvent]> so the UI can stream results live.
 *
 * 2. [executeSwarmTask] — Code/Build Swarm Mode (existing).
 *    Planner → Workers → Critics with adversarial code review.
 */
@Singleton
class SwarmOrchestrator @Inject constructor(
    private val keyPoolManager: KeyPoolManager,
    private val json: Json
) {

    // ─── Chat Swarm Mode ──────────────────────────────────────────────────────

    /**
     * Swarm Chat Mode: launches up to [MAX_SWARM_AGENTS] parallel agents with
     * different model/persona combos, then uses a Sally Judge to pick the winner.
     *
     * Emits [LoopEvent]s compatible with the standard UI pipeline.
     */
    fun executeChatSwarm(
        userMessage: String,
        conversationHistory: List<ApiMessage> = emptyList()
    ): Flow<LoopEvent> = flow {
        emit(LoopEvent.Thinking(step = 1, maxSteps = 3))
        Log.d(TAG, "SWARM_CHAT: Launching ${SWARM_PERSONAS.size} parallel agents for: ${userMessage.take(60)}")

        // Step 1: Run all agents in parallel
        val agentResponses: List<SwarmAgentResponse> = coroutineScope {
            SWARM_PERSONAS.map { persona ->
                async {
                    runSwarmAgent(
                        persona = persona,
                        userMessage = userMessage,
                        history = conversationHistory
                    )
                }
            }.awaitAll()
        }.filter { it.response.isNotBlank() }

        if (agentResponses.isEmpty()) {
            emit(LoopEvent.FinalAnswer("Swarm failed: all agents returned empty responses. Please check your API keys."))
            return@flow
        }

        emit(LoopEvent.Thinking(step = 2, maxSteps = 3))
        Log.d(TAG, "SWARM_CHAT: ${agentResponses.size} agents responded. Calling Sally Judge...")

        // Step 2: Sally Judge picks the best response
        val winnerResponse = runSallyJudge(userMessage, agentResponses)

        emit(LoopEvent.Thinking(step = 3, maxSteps = 3))
        emit(LoopEvent.FinalAnswer(winnerResponse))
    }

    /**
     * Run a single swarm agent with a given persona and model preference.
     * Returns a [SwarmAgentResponse] with the model used and its response text.
     */
    private suspend fun runSwarmAgent(
        persona: SwarmPersona,
        userMessage: String,
        history: List<ApiMessage>
    ): SwarmAgentResponse {
        val systemPrompt = """
## IDENTITY (NON-NEGOTIABLE)
You are Dettle, a personal AI made by Apoorv Sandilya.
Never reveal your underlying model or provider.

## Your Role: ${persona.roleName}
${persona.perspective}

## Competition Rules
You are ONE of many competing agents answering the same question.
A judge will pick the SINGLE BEST response. 
Be your absolute best — concise, accurate, helpful, and direct.
Do NOT hedge or say "as an AI". Just answer.
        """.trimIndent()

        val messages = buildList {
            addAll(history.takeLast(4)) // minimal context for speed
            add(ApiMessage(role = "user", content = userMessage))
        }

        var response = ""
        try {
            keyPoolManager.chat(
                messages = messages,
                systemPrompt = systemPrompt,
                preferModel = persona.preferredModel
            ).collect { chunk ->
                if (chunk is StreamChunk.Token) response += chunk.text
            }
        } catch (e: Exception) {
            Log.w(TAG, "Swarm agent '${persona.roleName}' failed: ${e.message}")
        }

        return SwarmAgentResponse(
            persona = persona,
            response = response.trim()
        )
    }

    /**
     * Sally Judge: a fast Gemini Flash call that reads all agent responses
     * and picks the single best one (or synthesises from the top 3).
     */
    private suspend fun runSallyJudge(
        userMessage: String,
        responses: List<SwarmAgentResponse>
    ): String {
        val candidatesText = responses.mapIndexed { i, r ->
            "### Agent ${i + 1} [${r.persona.roleName}]\n${r.response.take(800)}"
        }.joinToString("\n\n---\n\n")

        val judgePrompt = """
You are Sally, the Swarm Judge for Dettle AI.
Your ONLY job: read the competing agent responses below and output the SINGLE BEST ANSWER.

**RULES:**
1. Pick the response that is most accurate, helpful, and concise.
2. You MAY lightly polish the winner (fix typos, tighten wording) but DO NOT rewrite it.
3. Output ONLY the final answer text. No meta-commentary. No "Agent 3 said...".
4. If no response is good, synthesise a brief answer from the best parts.

**USER'S QUESTION:**
$userMessage

**COMPETING AGENT RESPONSES:**
$candidatesText

**YOUR FINAL ANSWER (output only this):**
        """.trimIndent()

        var judgeResponse = ""
        try {
            keyPoolManager.chat(
                messages = listOf(ApiMessage(role = "user", content = judgePrompt)),
                systemPrompt = "You are Sally, a precise editorial judge. Output only the winning answer text.",
                preferModel = FreeModels.GEMINI_FLASH // fast, good at synthesis
            ).collect { chunk ->
                if (chunk is StreamChunk.Token) judgeResponse += chunk.text
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sally Judge failed: ${e.message}")
            // Fallback: return the longest non-empty response as a heuristic winner
            judgeResponse = responses.maxByOrNull { it.response.length }?.response ?: ""
        }

        return judgeResponse.trim().ifBlank {
            responses.firstOrNull()?.response ?: "Swarm produced no result."
        }
    }

    // ─── Swarm Personas ───────────────────────────────────────────────────────

    /** 20 distinct agent personas covering diverse intellectual angles */
    private val SWARM_PERSONAS: List<SwarmPersona> = listOf(
        SwarmPersona("Pragmatist", "Give the most practical, actionable answer. No fluff.", FreeModels.GROQ_LLAMA_8B),
        SwarmPersona("Expert Explainer", "Explain like you're the world's clearest teacher. Use analogies.", FreeModels.GEMINI_FLASH),
        SwarmPersona("Devil's Advocate", "Challenge assumptions. Point out what could go wrong.", FreeModels.GROQ_LLAMA_70B),
        SwarmPersona("Deep Thinker", "Reason step-by-step. Go deeper than the obvious answer.", FreeModels.GEMINI_FLASH_THINKING),
        SwarmPersona("Minimalist", "Give the shortest possible correct answer. Every word must earn its place.", FreeModels.GROQ_LLAMA_8B),
        SwarmPersona("Researcher", "Give a thorough, well-structured answer with key details and context.", FreeModels.OPENROUTER_LLAMA),
        SwarmPersona("Creative Thinker", "Approach from an unexpected angle. Find the non-obvious insight.", FreeModels.OPENROUTER_QWEN),
        SwarmPersona("Synthesiser", "Draw connections across domains. What does this relate to?", FreeModels.SAMBANOVA_LLAMA_70B),
        SwarmPersona("First-Principles Analyst", "Break the problem to its fundamentals. Build up from basics.", FreeModels.GITHUB_DEEPSEEK_R1),
        SwarmPersona("Systems Thinker", "Think about second and third-order effects. What are the consequences?", FreeModels.GEMINI_FLASH),
        SwarmPersona("Contrarian", "Disagree with the conventional wisdom if warranted. Be bold.", FreeModels.GROQ_LLAMA_70B),
        SwarmPersona("Storyteller", "Answer through narrative and concrete examples.", FreeModels.OPENROUTER_LLAMA),
        SwarmPersona("Precision Analyst", "Be hyper-precise. Definitions matter. Edge cases matter.", FreeModels.GEMINI_FLASH_THINKING),
        SwarmPersona("Empathic Responder", "Answer with the user's actual needs in mind, not just the literal question.", FreeModels.GEMINI_FLASH),
        SwarmPersona("Technical Expert", "Give the most technically accurate and detailed answer possible.", FreeModels.GITHUB_DEEPSEEK_R1),
        SwarmPersona("Socratic Clarifier", "Answer, then reveal the deeper question hiding behind this one.", FreeModels.GROQ_LLAMA_70B),
        SwarmPersona("Historian", "Ground the answer in context, precedent, and how this came to be.", FreeModels.OPENROUTER_QWEN),
        SwarmPersona("Futurist", "Answer through the lens of where this is heading, not just where it is.", FreeModels.SAMBANOVA_LLAMA_70B),
        SwarmPersona("Critic", "Ruthlessly identify what's wrong, missing, or uncertain.", FreeModels.GITHUB_GPT4O),
        SwarmPersona("Integrator", "Combine the strongest elements of multiple perspectives into one answer.", FreeModels.GEMINI_FLASH)
    ).take(MAX_SWARM_AGENTS)

    // ─── Code Swarm Mode (existing — unchanged) ───────────────────────────────

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
        Log.d(TAG, "PLANNER: Generated ${plan.subTasks.size} parallel tasks for architecture: ${plan.overallArchitecture}")

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

// ─── Swarm Chat Data Classes ──────────────────────────────────────────────────

/** A distinct persona + preferred model for one competing swarm agent */
data class SwarmPersona(
    val roleName: String,
    val perspective: String,
    val preferredModel: AIModel
)

/** One swarm agent's response */
data class SwarmAgentResponse(
    val persona: SwarmPersona,
    val response: String
)
