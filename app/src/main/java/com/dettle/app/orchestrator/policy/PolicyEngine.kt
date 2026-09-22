package com.dettle.app.orchestrator.policy

import android.util.Log
import com.dettle.app.domain.model.ApiMessage
import com.dettle.app.domain.model.ToolCall
import com.dettle.app.orchestrator.config.DettleAgentConfig
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PolicyEngine"

/**
 * Evaluates all registered [ToolPolicy] rules against every tool call
 * BEFORE it is executed by [ToolExecutor].
 *
 * This is where AGENTS.legacy.md rules become real code.
 *
 * Usage in [ReActLoop]:
 * ```kotlin
 * val result = policyEngine.evaluate(toolCall, history)
 * when (result) {
 *     is EvaluationResult.Approved -> toolExecutor.execute(toolCall)
 *     is EvaluationResult.Blocked  -> emit(LoopEvent.PolicyBlocked(result.reason, result.fix))
 *     is EvaluationResult.Warnings -> { /* log warnings, then proceed */ toolExecutor.execute(toolCall) }
 * }
 * ```
 */
@Singleton
class PolicyEngine @Inject constructor(
    private val config: DettleAgentConfig
) {
    /**
     * All active policies, evaluated in order.
     * A single [PolicyResult.Blocked] from any policy blocks the tool call.
     * All [PolicyResult.Warning]s are accumulated and surfaced together.
     */
    private val policies: List<ToolPolicy> = listOf(
        ForbiddenSecretPolicy,          // P0 — no secrets in code
        BranchProtectionPolicy,         // P0 — no direct main push
        ApplicationIdProtectionPolicy,  // P0 — no accidental ID change
        CompletionGatePolicy(),         // P1 — critic before done
        CiRequiredPolicy,               // P1 — CI must exist
        CloudflareFreeTierPolicy        // P1 — worker CPU guard
    )

    /**
     * Evaluate a tool call against all active policies.
     *
     * @param call The tool call the agent wants to execute
     * @param history The full conversation history (for context-aware policies)
     * @return [EvaluationResult] — proceed, block, or proceed-with-warnings
     */
    fun evaluate(call: ToolCall, history: List<ApiMessage>): EvaluationResult {
        val warnings = mutableListOf<PolicyResult.Warning>()

        for (policy in policies) {
            when (val result = policy.evaluate(call, config, history)) {
                is PolicyResult.Approved -> {
                    // continue to next policy
                }
                is PolicyResult.Blocked -> {
                    Log.w(TAG, "BLOCKED by '${result.policyId}': ${result.reason}")
                    return EvaluationResult.Blocked(
                        reason = result.reason,
                        fix = result.fix,
                        policyId = result.policyId
                    )
                }
                is PolicyResult.Warning -> {
                    Log.d(TAG, "WARNING from '${result.policyId}': ${result.message}")
                    warnings.add(result)
                }
            }
        }

        return if (warnings.isEmpty()) {
            EvaluationResult.Approved
        } else {
            EvaluationResult.ApprovedWithWarnings(warnings)
        }
    }

    /**
     * Get a summary of all active policies for display in the Settings screen.
     */
    fun getActivePolicies(): List<PolicySummary> = policies.map { policy ->
        PolicySummary(id = policy.id, description = policy.description)
    }
}

// ─── Evaluation Result ────────────────────────────────────────────────────────

sealed class EvaluationResult {
    /** All policies approved — proceed with tool execution */
    object Approved : EvaluationResult()

    /** One or more warnings — proceed but inform the agent */
    data class ApprovedWithWarnings(val warnings: List<PolicyResult.Warning>) : EvaluationResult()

    /** A policy hard-blocked the call — do NOT execute the tool */
    data class Blocked(
        val reason: String,
        val fix: String,
        val policyId: String
    ) : EvaluationResult()
}

data class PolicySummary(
    val id: String,
    val description: String
)
