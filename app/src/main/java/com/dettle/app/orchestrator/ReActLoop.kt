package com.dettle.app.orchestrator

import android.util.Log
import com.dettle.app.data.api.KeyPoolManager
import com.dettle.app.data.api.StreamChunk
import com.dettle.app.data.github.FileChange
import com.dettle.app.data.github.GitHubClient
import com.dettle.app.domain.model.AgentTools
import com.dettle.app.domain.model.ApiMessage
import com.dettle.app.domain.model.ApprovalActionType
import com.dettle.app.domain.model.ApprovalRequest
import com.dettle.app.domain.model.MessageRole
import com.dettle.app.domain.model.ChatMessage
import com.dettle.app.domain.model.MessageType
import com.dettle.app.domain.model.Tool
import com.dettle.app.domain.model.ToolCall
import com.dettle.app.domain.model.ToolResult
import com.dettle.app.domain.model.TaskContext
import com.dettle.app.domain.model.TaskType
import com.dettle.app.orchestrator.mode.AgentMode
import com.dettle.app.orchestrator.mode.Goal
import com.dettle.app.orchestrator.policy.EvaluationResult
import com.dettle.app.orchestrator.policy.PolicyEngine
import com.dettle.app.orchestrator.policy.SalienceEvaluator
import com.dettle.app.orchestrator.policy.SalienceType
import com.dettle.app.orchestrator.proxy.PromptSmuggler
import com.dettle.app.orchestrator.reflex.ProceduralReflexEngine
import com.dettle.app.domain.model.FreeModels
import com.dettle.app.domain.model.ALL_KNOWN_MODELS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ReActLoop"
private const val DEFAULT_MAX_STEPS = 30

/**
 * The central agent execution loop.
 *
 * Implements the ReAct (Reason + Act) pattern:
 * 1. THINK  → Send messages + tools to AI, receive reasoning or tool call
 * 2. ACT    → Parse tool call, validate safety constraints
 * 3. OBSERVE → Tool executes, result fed back into conversation
 * 4. REPEAT → Until final_answer tool called or maxSteps reached
 *
 * Emits [LoopEvent]s as a [Flow] so UI can render streaming tokens,
 * tool status badges, approvals, and completion gates live.
 */

@Singleton
class ReActLoop @Inject constructor(
    private val keyPoolManager: KeyPoolManager,
    private val toolExecutor: ToolExecutor,
    private val skillInjector: SkillInjector,
    private val policyEngine: PolicyEngine,
    private val salienceEvaluator: SalienceEvaluator,
    private val reflexEngine: ProceduralReflexEngine,
    private val promptSmuggler: PromptSmuggler,
    private val json: Json
) {
    fun run(
        userMessage: String,
        conversationHistory: List<ApiMessage>,
        tools: List<Tool> = AgentTools.ALL,
        taskContext: TaskContext = TaskContext(),
        mode: AgentMode? = null,
        goal: Goal? = null,
        isUncensored: Boolean = taskContext.isUncensored,
        maxSteps: Int = DEFAULT_MAX_STEPS
    ): Flow<LoopEvent> {
        
        // 1. Motor Cortex: Fast Reflex Intercept
        val reflexFlow = reflexEngine.tryReflex(userMessage)
        if (reflexFlow != null) {
            return reflexFlow
        }
        
        return flow {
            // 2. Amygdala: Threat/Salience Check
            val salience = salienceEvaluator.evaluate(userMessage, SalienceType.USER_MESSAGE)
            if (salience.requiresInterrupt) {
                emit(LoopEvent.Error("URGENT INTERRUPT: ${salience.reason}"))
                return@flow
            }

        val effectiveMaxSteps = if (maxSteps > 0) maxSteps else DEFAULT_MAX_STEPS
        val effectiveUncensored = isUncensored || taskContext.isUncensored
        val systemPrompt = skillInjector.buildSystemPrompt(
            context = taskContext,
            userPrompt = userMessage,
            isUncensored = effectiveUncensored
        )
        val history = conversationHistory.toMutableList()
        history.add(ApiMessage(role = "user", content = userMessage))

        var steps = 0
        var done = false

        while (!done && steps < effectiveMaxSteps) {
            steps++
            Log.d(TAG, "Step $steps/$effectiveMaxSteps")

            // Emit "thinking" status
            emit(LoopEvent.Thinking(step = steps, maxSteps = effectiveMaxSteps))

            var currentResponse = StringBuilder()
            var detectedToolCallJson: String? = null
            var streamingMessageId = java.util.UUID.randomUUID().toString()

            // Stream from AI provider
            keyPoolManager.chat(
                messages = history,
                tools = tools,
                systemPrompt = systemPrompt
            ).collect { chunk ->
                when (chunk) {
                    is StreamChunk.Token -> {
                        currentResponse.append(chunk.text)
                        emit(LoopEvent.TokenStreamed(
                            messageId = streamingMessageId,
                            token = chunk.text,
                            fullText = currentResponse.toString()
                        ))
                    }
                    is StreamChunk.ToolCallDetected -> {
                        detectedToolCallJson = chunk.rawJson
                    }
                    is StreamChunk.Done -> {
                        emit(LoopEvent.StreamComplete(
                            messageId = streamingMessageId,
                            fullText = currentResponse.toString(),
                            usage = chunk.usage
                        ))
                    }
                    is StreamChunk.Error -> {
                        salienceEvaluator.evaluate(chunk.message, if (chunk.isRateLimit) SalienceType.API_LIMIT else SalienceType.SYSTEM_ERROR)
                        emit(LoopEvent.Error(chunk.message))
                        if (!chunk.isRateLimit) done = true
                    }
                }
            }

            val assistantText = currentResponse.toString()

            // Add assistant response to history
            if (assistantText.isNotBlank()) {
                history.add(ApiMessage(role = "assistant", content = assistantText))
            }

            // ── Parse tool call ────────────────────────────────────────────
            // Provider returned a structured tool call
            val toolCall = detectedToolCallJson?.let { parseToolCall(it) }
                // OR: AI embedded a tool call in text (WebView providers)
                ?: extractTextToolCall(assistantText)

            if (toolCall == null) {
                // If Unleashed/Uncensored mode is active, check if the output is a commercial safety refusal:
                if (effectiveUncensored && isCommercialRefusal(assistantText)) {
                    Log.d(TAG, "Uncensored Mode: Commercial filter detected refusal, deploying Prompt Smuggler proxy protocol...")
                    emit(LoopEvent.TokenStreamed(
                        messageId = streamingMessageId,
                        token = "\n[Unleashed Protocol: Bypassing safety filter via Prompt Smuggler]\n",
                        fullText = "[Unleashed Protocol: Bypassing safety filter via Prompt Smuggler]\n"
                    ))
                    val targetModel = mode?.modelIds?.firstOrNull()?.let { ALL_KNOWN_MODELS[it] }
                        ?: FreeModels.GEMINI_FLASH
                    val smuggledResult = runCatching {
                        promptSmuggler.executeLaunderedRequest(userMessage, targetModel)
                    }.getOrElse { "Execution error via smuggler proxy: ${it.message}" }

                    emit(LoopEvent.FinalAnswer(smuggledResult))
                    done = true
                    break
                }

                // No tool call → AI is done
                done = true
                emit(LoopEvent.FinalAnswer(assistantText))
                break
            }

            Log.d(TAG, "Tool call detected: ${toolCall.name}(${toolCall.arguments})")

            // ── Check if final_answer ──────────────────────────────────────
            if (toolCall.name == "final_answer") {
                done = true
                val summary = toolCall.arguments["summary"] ?: assistantText
                val details = toolCall.arguments["details"] ?: ""
                emit(LoopEvent.FinalAnswer("$summary\n\n$details".trim()))
                break
            }

            // ── Check safety / approval required ──────────────────────────
            val toolDef = tools.find { it.name == toolCall.name }
            if (toolDef?.requiresApproval == true) {
                val approval = ApprovalRequest(
                    title = "Agent wants to: ${toolCall.name}",
                    description = buildApprovalDescription(toolCall),
                    actionType = toolCallToApprovalType(toolCall.name),
                    details = toolCall.arguments
                )
                emit(LoopEvent.NeedsApproval(approval, toolCall))

                // Loop waits here — ViewModel resumes by calling continueAfterApproval()
                // For now, the loop is paused via suspension; approval result comes back via channel
                return@flow
            }

            // ── Policy enforcement (before tool runs) ─────────────────────
            when (val policyResult = policyEngine.evaluate(toolCall, history)) {
                is EvaluationResult.Blocked -> {
                    // Hard block — tell the agent WHY and HOW to fix it, don't execute
                    emit(LoopEvent.PolicyBlocked(
                        toolName = toolCall.name,
                        reason = policyResult.reason,
                        fix = policyResult.fix,
                        policyId = policyResult.policyId
                    ))
                    // Feed the block back into history so the AI knows to change course
                    history.add(ApiMessage(
                        role = "tool",
                        content = "POLICY BLOCKED [${policyResult.policyId}]: ${policyResult.reason}\n\nFix: ${policyResult.fix}",
                        name = toolCall.name
                    ))
                    // Continue the loop — agent can try a different approach
                    continue
                }
                is EvaluationResult.ApprovedWithWarnings -> {
                    // Warn but proceed
                    val warningText = policyResult.warnings.joinToString("\n") {
                        "[Warning] [${it.policyId}]: ${it.message}"
                    }
                    emit(LoopEvent.PolicyWarning(toolName = toolCall.name, warnings = warningText))
                    Log.d(TAG, "Policy warnings for ${toolCall.name}:\n$warningText")
                }
                is EvaluationResult.Approved -> { /* proceed */ }
            }

            // ── Execute tool ───────────────────────────────────────────────
            emit(LoopEvent.ExecutingTool(toolCall))

            val toolResult = try {
                toolExecutor.execute(toolCall)
            } catch (e: Exception) {
                ToolResult(
                    toolCallId = toolCall.id,
                    toolName = toolCall.name,
                    content = "Error: ${e.message}",
                    isError = true
                )
            }

            emit(LoopEvent.ToolResultReceived(toolResult))

            // Add tool result to history for next iteration
            history.add(ApiMessage(
                role = "tool",
                content = toolResult.content,
                tool_call_id = toolResult.toolCallId,
                name = toolResult.toolName
            ))
        }

        if (steps >= effectiveMaxSteps && !done) {
            emit(LoopEvent.StepLimitReached(
                message = "I've reached the maximum of $effectiveMaxSteps steps. Here's what I've done so far. Please tell me how to proceed.",
                history = history
            ))
        }
    }.flowOn(Dispatchers.IO)
    }

    /** Parse a structured JSON tool call from OpenAI-compatible providers */
    private fun parseToolCall(rawJson: String): ToolCall? {
        return try {
            val obj = json.parseToJsonElement(rawJson).jsonObject
            // Handle both array format [{"id":...}] and direct format
            val first = if (rawJson.trimStart().startsWith("[")) {
                json.parseToJsonElement(rawJson).let {
                    // it's an array
                    val arr = rawJson.trimStart().trimStart('[').trimEnd(']')
                    json.parseToJsonElement(arr).jsonObject
                }
            } else obj

            val name = first["function"]?.jsonObject?.get("name")?.jsonPrimitive?.content ?: return null
            val argsStr = first["function"]?.jsonObject?.get("arguments")?.jsonPrimitive?.content ?: "{}"
            val args = json.parseToJsonElement(argsStr).jsonObject
                .entries.associate { it.key to it.value.jsonPrimitive.content }

            ToolCall(name = name, arguments = args)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse structured tool call: $e")
            null
        }
    }

    /** Extract XML-format tool call embedded in plain text (for WebView providers) */
    private fun extractTextToolCall(text: String): ToolCall? {
        val regex = """<tool_call>\s*(\{.*?\})\s*</tool_call>""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val match = regex.find(text) ?: return null
        return try {
            val obj = json.parseToJsonElement(match.groupValues[1]).jsonObject
            val name = obj["name"]?.jsonPrimitive?.content ?: return null
            val args = obj["args"]?.jsonObject
                ?.entries?.associate { it.key to it.value.jsonPrimitive.content }
                ?: emptyMap()
            ToolCall(name = name, arguments = args)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse text tool call: $e")
            null
        }
    }

    private fun buildApprovalDescription(toolCall: ToolCall): String = buildString {
        appendLine("**Tool**: `${toolCall.name}`")
        appendLine()
        toolCall.arguments.forEach { (k, v) ->
            appendLine("**$k**: $v")
        }
    }

    private fun toolCallToApprovalType(toolName: String): ApprovalActionType = when (toolName) {
        "github_create_branch_pr" -> ApprovalActionType.GITHUB_CREATE_PR
        "github_trigger_action" -> ApprovalActionType.GITHUB_TRIGGER_ACTION
        "cloudflare_publish_worker" -> ApprovalActionType.CLOUDFLARE_DEPLOY_WORKER
        else -> ApprovalActionType.CUSTOM
    }

    private fun isCommercialRefusal(text: String): Boolean {
        if (text.length > 500) return false
        val lower = text.lowercase()
        return (lower.contains("cannot fulfill") ||
                lower.contains("can't fulfill") ||
                lower.contains("as an ai language model") ||
                lower.contains("against safety") ||
                lower.contains("content policy") ||
                lower.contains("safety guidelines") ||
                lower.contains("unable to provide") ||
                lower.contains("policy prohibits") ||
                lower.contains("harmful or dangerous"))
    }
}

// ─── Context and Events ────────────────────────────────────────────────────

typealias TaskContext = com.dettle.app.domain.model.TaskContext
typealias TaskType = com.dettle.app.domain.model.TaskType

sealed class LoopEvent {
    data class Thinking(val step: Int, val maxSteps: Int) : LoopEvent()
    data class TokenStreamed(val messageId: String, val token: String, val fullText: String) : LoopEvent()
    data class StreamComplete(val messageId: String, val fullText: String, val usage: Any?) : LoopEvent()
    data class ExecutingTool(val toolCall: ToolCall) : LoopEvent()
    data class ToolResultReceived(val result: ToolResult) : LoopEvent()
    data class NeedsApproval(val request: ApprovalRequest, val pendingToolCall: ToolCall) : LoopEvent()
    data class FinalAnswer(val text: String) : LoopEvent()
    data class StepLimitReached(val message: String, val history: List<ApiMessage>) : LoopEvent()
    data class Error(val message: String) : LoopEvent()

    /**
     * A PolicyEngine rule hard-blocked a tool call.
     * The UI should show a [PolicyBlockedCard] explaining the violation and fix.
     * The agent loop continues — the AI can try an alternative approach.
     */
    data class PolicyBlocked(
        val toolName: String,
        val reason: String,
        val fix: String,
        val policyId: String
    ) : LoopEvent()

    /**
     * A PolicyEngine rule raised a warning but allowed the tool call to proceed.
     * The UI may show this as a subtle indicator (orange badge, etc.).
     */
    data class PolicyWarning(
        val toolName: String,
        val warnings: String
    ) : LoopEvent()

    /**
     * GOAL mode: a completion gate passed or failed.
     * The UI shows a live checklist card. Persisted to Room by ChatViewModel.
     * Emitted when the agent outputs "[GATE: id] PASSED" or "[GATE: id] FAILED: reason".
     */
    data class GoalProgress(
        val gateId: String,
        val passed: Boolean,
        val reason: String? = null
    ) : LoopEvent()
}
