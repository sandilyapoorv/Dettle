package com.dettle.app.orchestrator.policy

import com.dettle.app.domain.model.ApiMessage
import com.dettle.app.domain.model.ToolCall
import com.dettle.app.orchestrator.config.DettleAgentConfig

/**
 * A single enforceable rule that runs against a tool call before it executes.
 *
 * Every rule that AGENTS.legacy.md described in prose is implemented as a
 * concrete [ToolPolicy] that [PolicyEngine] evaluates automatically.
 */
interface ToolPolicy {
    val id: String
    val description: String

    /**
     * Evaluate whether [call] is safe to execute.
     * @param call The tool call the agent wants to make
     * @param config The project configuration
     * @param history The full message history so far (read-only, for context)
     */
    fun evaluate(
        call: ToolCall,
        config: DettleAgentConfig,
        history: List<ApiMessage>
    ): PolicyResult
}

// ─── Policy Result ────────────────────────────────────────────────────────────

sealed class PolicyResult {
    /** Tool call may proceed */
    object Approved : PolicyResult()

    /**
     * Tool call is blocked — the agent MUST NOT execute it.
     * [reason] explains what rule was violated.
     * [fix] tells the agent exactly what to do instead.
     */
    data class Blocked(
        val reason: String,
        val fix: String,
        val policyId: String
    ) : PolicyResult()

    /**
     * Tool call may proceed, but the agent should know about this.
     * Used for non-critical advisory warnings (e.g. free-tier approaching limit).
     */
    data class Warning(
        val message: String,
        val policyId: String
    ) : PolicyResult()
}

// ─── Policies ─────────────────────────────────────────────────────────────────

/**
 * P0 — Blocks any commit that contains secret/credential patterns.
 *
 * Replaces AGENTS.md Protocol A §8 + §11 and Protocol B prose:
 * "Never commit passwords, keystores, signing secrets or privileged tokens."
 */
object ForbiddenSecretPolicy : ToolPolicy {
    override val id = "forbidden-secret"
    override val description = "Prevents committing secrets, keys, or credentials to code"

    override fun evaluate(call: ToolCall, config: DettleAgentConfig, history: List<ApiMessage>): PolicyResult {
        if (call.name != "github_create_branch_pr") return PolicyResult.Approved

        val fileChanges = call.arguments["file_changes"] ?: return PolicyResult.Approved
        val patterns = config.security.compiledForbiddenPatterns

        val match = patterns.firstOrNull { it.containsMatchIn(fileChanges) }
            ?: return PolicyResult.Approved

        return PolicyResult.Blocked(
            reason = "Secret pattern detected in file_changes: '${match.pattern}'",
            fix = "Move credentials to GitHub Actions Secrets: ${config.security.githubActionsSecrets.joinToString(", ")}. " +
                    "Reference them in code as `BuildConfig.FIELD` populated by CI, never hardcoded.",
            policyId = id
        )
    }
}

/**
 * P0 — Prevents pushing directly to the default branch.
 *
 * Replaces AGENTS.md Protocol A §3 and Protocol B §2:
 * "GitHub is source of truth. Never push directly to main."
 */
object BranchProtectionPolicy : ToolPolicy {
    override val id = "branch-protection"
    override val description = "Prevents direct commits to the main branch — all changes must go through a PR"

    override fun evaluate(call: ToolCall, config: DettleAgentConfig, history: List<ApiMessage>): PolicyResult {
        if (call.name != "github_create_branch_pr") return PolicyResult.Approved

        val branchName = call.arguments["branch_name"] ?: return PolicyResult.Approved
        val defaultBranch = config.delivery.defaultBranch

        if (branchName.trim() == defaultBranch) {
            return PolicyResult.Blocked(
                reason = "Attempted to create a branch named '$branchName' — this is the default branch.",
                fix = "Use a feature branch with the prefix '${config.delivery.branchPrefix}', " +
                        "e.g. '${config.delivery.branchPrefix}your-feature-name'.",
                policyId = id
            )
        }

        // Warn if branch doesn't follow prefix convention
        if (!branchName.startsWith(config.delivery.branchPrefix)) {
            return PolicyResult.Warning(
                message = "Branch '$branchName' doesn't follow the '${config.delivery.branchPrefix}' prefix convention. " +
                        "This is allowed but non-standard for this project.",
                policyId = id
            )
        }

        return PolicyResult.Approved
    }
}

/**
 * P1 — Guards the Cloudflare free-tier CPU limit per Worker invocation.
 *
 * Replaces AGENTS.md Protocol B §6:
 * "Workers Free: 100,000 req/day, 10ms CPU per invocation."
 */
object CloudflareFreeTierPolicy : ToolPolicy {
    override val id = "cloudflare-free-tier"
    override val description = "Warns when a Worker script likely exceeds the free-tier CPU limit"

    private val EXPENSIVE_PATTERNS = listOf(
        Regex("""while\s*\(true\)"""),
        Regex("""for\s*\(.*\bfetch\b"""),    // fetch inside a loop
        Regex("""\.json\(\)\s*\n.*\.json\(\)"""), // multiple serial awaits
        Regex("""new\s+CompressionStream"""),
        Regex("""crypto\.subtle\.digest"""),
    )

    override fun evaluate(call: ToolCall, config: DettleAgentConfig, history: List<ApiMessage>): PolicyResult {
        if (call.name != "cloudflare_publish_worker") return PolicyResult.Approved

        val script = call.arguments["script_content"] ?: return PolicyResult.Approved
        val cpuLimit = config.freeTier.cloudflare.workerCpuMsPerInvocation

        val match = EXPENSIVE_PATTERNS.firstOrNull { it.containsMatchIn(script) }
        if (match != null) {
            return PolicyResult.Warning(
                message = "Worker script may exceed the ${cpuLimit}ms CPU free-tier limit. " +
                        "Pattern detected: '${match.pattern}'. " +
                        "Consider offloading heavy work to Firestore or splitting into separate requests.",
                policyId = id
            )
        }

        return PolicyResult.Approved
    }
}

/**
 * P1 — Prevents the agent from claiming it's done before running Production Critic.
 *
 * Replaces AGENTS.md Protocol A §15 and Protocol B §9:
 * "Before completion, skeptically review..."
 */
class CompletionGatePolicy : ToolPolicy {
    override val id = "completion-gate"
    override val description = "Requires Production Critic review before final_answer on substantial tasks"

    /** Keywords that indicate a Production Critic memory recall was performed */
    private val CRITIC_EVIDENCE_PATTERNS = listOf(
        "production critic",
        "P0", "P1", "P2", "P3",
        "critic review",
        "review: completed",
        "production_critic"
    )

    override fun evaluate(call: ToolCall, config: DettleAgentConfig, history: List<ApiMessage>): PolicyResult {
        if (call.name != "final_answer") return PolicyResult.Approved
        if (!config.delivery.requireProductionCriticBeforeCompletion) return PolicyResult.Approved

        // Check if the conversation history contains evidence that Production Critic ran
        val allText = history.joinToString("\n") { it.content ?: "" }
        val criticRan = CRITIC_EVIDENCE_PATTERNS.any { pattern ->
            allText.contains(pattern, ignoreCase = true)
        }

        if (!criticRan) {
            return PolicyResult.Blocked(
                reason = "Production Critic review has not been performed before completion.",
                fix = "Before calling final_answer on substantial work, perform a Production Critic review: " +
                        "check requirements, trust boundaries, auth/authz, secrets, races, free-tier limits, " +
                        "CI, signing, and APK generation. Rank issues P0–P3. Fix P0/P1/P2.",
                policyId = id
            )
        }

        return PolicyResult.Approved
    }
}

/**
 * P0 — Prevents changing the Android applicationId without explicit user instruction.
 *
 * Replaces AGENTS.md Protocol A §5:
 * "Do not casually change app ID or signing identity."
 */
object ApplicationIdProtectionPolicy : ToolPolicy {
    override val id = "app-id-protection"
    override val description = "Prevents accidental applicationId changes that would break Play Store updates"

    override fun evaluate(call: ToolCall, config: DettleAgentConfig, history: List<ApiMessage>): PolicyResult {
        if (call.name != "github_create_branch_pr") return PolicyResult.Approved

        val fileChanges = call.arguments["file_changes"] ?: return PolicyResult.Approved

        // Check if build.gradle.kts is being modified with a different applicationId
        if (!fileChanges.contains("build.gradle.kts") && !fileChanges.contains("applicationId")) {
            return PolicyResult.Approved
        }

        val expectedId = config.android.applicationId
        // If a different applicationId is being written, block it
        val idPattern = Regex("""applicationId\s*=\s*["']([^"']+)["']""")
        val match = idPattern.find(fileChanges) ?: return PolicyResult.Approved
        val newId = match.groupValues[1]

        if (newId != expectedId) {
            return PolicyResult.Blocked(
                reason = "Attempted to change applicationId from '$expectedId' to '$newId'. " +
                        "Changing the applicationId breaks Play Store update delivery for existing users.",
                fix = "Keep applicationId = \"$expectedId\" unless the user has explicitly requested an ID change. " +
                        "Confirm with the user before proceeding.",
                policyId = id
            )
        }

        return PolicyResult.Approved
    }
}

/**
 * P1 — Ensures CI workflow files exist before triggering them.
 *
 * Replaces AGENTS.md Protocol A §6:
 * "Android repos must include CI."
 */
object CiRequiredPolicy : ToolPolicy {
    override val id = "ci-required"
    override val description = "Warns when attempting to trigger a CI workflow that may not exist"

    override fun evaluate(call: ToolCall, config: DettleAgentConfig, history: List<ApiMessage>): PolicyResult {
        if (call.name != "github_trigger_action") return PolicyResult.Approved
        if (!config.delivery.requireCI) return PolicyResult.Approved

        val workflowId = call.arguments["workflow_id"] ?: return PolicyResult.Approved

        // Check history for github_map_repo result mentioning .github/workflows/
        val historyText = history.joinToString("\n") { it.content ?: "" }
        val workflowMentioned = historyText.contains(workflowId, ignoreCase = true)
                || historyText.contains(".github/workflows", ignoreCase = true)

        if (!workflowMentioned) {
            return PolicyResult.Warning(
                message = "Triggering workflow '$workflowId' but no prior github_map_repo result confirms it exists. " +
                        "Call github_map_repo first to verify .github/workflows/$workflowId exists in the repo.",
                policyId = id
            )
        }

        return PolicyResult.Approved
    }
}
