package com.dettle.app.orchestrator.mode

import androidx.compose.ui.graphics.Color
import com.dettle.app.domain.model.AIModel
import com.dettle.app.domain.model.ModelSets
import com.dettle.app.domain.model.FreeModels
import com.dettle.app.domain.model.Tool
import com.dettle.app.domain.model.AgentTools
import kotlinx.serialization.Serializable

// ─── Mode Identifier ──────────────────────────────────────────────────────────

enum class ModeId(val displayName: String, val emoji: String, val description: String) {
    CHAT(
        "Chat",       "",
        "Quick answers and light tasks. Auto-routes for simplicity."
    ),
    RESEARCH(
        "Research",   "",
        "Deep-dive into topics, codebases, or docs. Reads everything. No writes."
    ),
    CODE(
        "Code",       "",
        "Write, fix, and ship code. TDD enforced. Always branches, never touches main."
    ),
    PLAN(
        "Plan",       "",
        "Think before acting. Produces a structured implementation plan, not code."
    ),
    GOAL(
        "Goal",       "",
        "Long-running autonomous objective. Tracks progress across sessions. Stops when done."
    ),
    WEB(
        "Web",        "",
        "Aggressive multi-query web research. Triangulates from 3+ sources."
    ),
    REVIEW(
        "Review",     "",
        "Code review with P0–P3 ranked findings. Production-critic standard."
    ),
    DEPLOY(
        "Deploy",     "",
        "Build → test → release. CI trigger, APK upload, GitHub Release."
    )
}

// ─── Customizable config ──────────────────────────────────────────────────────

/**
 * The user-editable parts of an [AgentMode].
 *
 * This is what gets serialized to DataStore when the user customizes a mode.
 * [AgentMode] merges these on top of its defaults.
 */
@Serializable
data class ModeConfig(
    /** Tool names enabled in this mode */
    val enabledTools: List<String>,
    /** Model IDs in preferred order (waterfall) */
    val modelWaterfall: List<String>,
    /** Max ReAct loop iterations */
    val maxSteps: Int,
    /** Completion gates — strings that must appear in history before final_answer */
    val completionGates: List<String> = emptyList(),
    /** Tool names that require explicit user approval in this mode */
    val requireApprovalFor: Set<String> = emptySet(),
    /** Extra system prompt instructions appended to the standard skill blocks */
    val customInstructions: String = ""
)

// ─── Mode definition ──────────────────────────────────────────────────────────

/**
 * A complete agent execution profile.
 *
 * Immutable at runtime. [ModeRepository] computes the *effective* config by
 * layering user customizations from DataStore on top of [defaultConfig].
 */
data class AgentMode(
    val id: ModeId,
    val accentHex: Long,              // stored as Long for Serializable compat
    val defaultConfig: ModeConfig,
    /** Effective config = defaultConfig merged with user overrides */
    val effectiveConfig: ModeConfig = defaultConfig
) {
    val displayName: String get() = id.displayName
    val emoji: String        get() = id.emoji
    val description: String  get() = id.description
    val accentColor: Color   get() = Color(accentHex)
    val tools: List<String>  get() = effectiveConfig.enabledTools
    val modelIds: List<String> get() = effectiveConfig.modelWaterfall
    val maxSteps: Int        get() = effectiveConfig.maxSteps
}

// ─── Default mode registry ────────────────────────────────────────────────────

object AgentModes {

    val CHAT = AgentMode(
        id = ModeId.CHAT,
        accentHex = 0xFF9E9E9E,
        defaultConfig = ModeConfig(
            enabledTools = listOf("web_search", "memory_recall", "final_answer"),
            modelWaterfall = ModelSets.FAST.ids(),
            maxSteps = 4
        )
    )

    val RESEARCH = AgentMode(
        id = ModeId.RESEARCH,
        accentHex = 0xFF00BCD4,
        defaultConfig = ModeConfig(
            enabledTools = listOf(
                "web_search", "read_url",
                "github_map_repo", "github_read_file",
                "memory_recall", "final_answer"
            ),
            modelWaterfall = ModelSets.LARGE_CONTEXT.ids(),
            maxSteps = 20,
            customInstructions = "Cite every claim. Never invent API signatures. " +
                "Use read_url to verify before citing. Run 3+ searches before concluding."
        )
    )

    val CODE = AgentMode(
        id = ModeId.CODE,
        accentHex = 0xFF4CAF50,
        defaultConfig = ModeConfig(
            enabledTools = AgentTools.ALL.map { it.name },
            modelWaterfall = listOf(
                FreeModels.GROQ_LLAMA_8B.modelId,   // fast inner loop
                FreeModels.GEMINI_FLASH.modelId,     // heavy code writing
                FreeModels.GITHUB_GPT4O.modelId      // hard problems
            ),
            maxSteps = 12,
            completionGates = listOf("build_passed", "tests_passed", "pr_created"),
            requireApprovalFor = setOf("github_create_branch_pr", "github_trigger_action"),
            customInstructions = "Write tests FIRST. Never push to main — always branch. " +
                "One change at a time. Verify each step before the next."
        )
    )

    val PLAN = AgentMode(
        id = ModeId.PLAN,
        accentHex = 0xFFFF9800,
        defaultConfig = ModeConfig(
            enabledTools = listOf(
                "github_map_repo", "github_read_file",
                "web_search", "read_url",
                "memory_recall", "final_answer"
            ),
            modelWaterfall = ModelSets.DEEP_REASONING.ids(),
            maxSteps = 8,
            customInstructions = "Output a structured plan: phases → files → risks → test plan. " +
                "No implementation code. Decompose into a depth tree first."
        )
    )

    val GOAL = AgentMode(
        id = ModeId.GOAL,
        accentHex = 0xFFE91E63,
        defaultConfig = ModeConfig(
            enabledTools = AgentTools.ALL.map { it.name },
            modelWaterfall = ModelSets.ALL.ids(),
            maxSteps = 50,
            requireApprovalFor = setOf("github_create_branch_pr", "github_trigger_action"),
            customInstructions = "This is a persistent goal — track progress against each gate. " +
                "Commit progress after every passing gate. Report '[GATE: id] PASSED' or " +
                "'[GATE: id] FAILED: reason' for each gate exactly. " +
                "Do NOT stop until ALL gates are passed or explicitly blocked."
        )
    )

    val WEB = AgentMode(
        id = ModeId.WEB,
        accentHex = 0xFF2196F3,
        defaultConfig = ModeConfig(
            enabledTools = listOf("web_search", "read_url", "memory_recall", "final_answer"),
            modelWaterfall = ModelSets.LARGE_CONTEXT.ids(),
            maxSteps = 15,
            completionGates = listOf("three_sources_cited"),
            customInstructions = "Run 3-5 different search queries with different angles FIRST. " +
                "Read at least 3 distinct pages before concluding. " +
                "Triangulate contradictions between sources. " +
                "Cite every claim with a URL."
        )
    )

    val REVIEW = AgentMode(
        id = ModeId.REVIEW,
        accentHex = 0xFF9C27B0,
        defaultConfig = ModeConfig(
            enabledTools = listOf(
                "github_map_repo", "github_read_file",
                "read_url", "memory_recall", "final_answer"
            ),
            modelWaterfall = ModelSets.CODE_REVIEW.ids(),
            maxSteps = 10,
            completionGates = listOf("p0_p3_rankings_produced"),
            customInstructions = "Produce a structured review. " +
                "Rank ALL findings P0 (critical/security), P1 (bug), P2 (improvement), P3 (nit). " +
                "End with a Verdict: APPROVE / REQUEST_CHANGES / NEEDS_DISCUSSION."
        )
    )

    val DEPLOY = AgentMode(
        id = ModeId.DEPLOY,
        accentHex = 0xFFFF5722,
        defaultConfig = ModeConfig(
            enabledTools = listOf(
                "github_map_repo", "github_read_file",
                "github_create_branch_pr", "github_trigger_action",
                "cloudflare_deploy_preview", "cloudflare_publish_worker",
                "final_answer"
            ),
            modelWaterfall = ModelSets.FAST.ids(),
            maxSteps = 8,
            completionGates = listOf("build_passed", "apk_uploaded", "release_tagged", "release_created"),
            requireApprovalFor = setOf("github_trigger_action", "cloudflare_publish_worker"),
            customInstructions = "Deploy checklist: build → test → tag → release → APK artifact. " +
                "Production Critic review required before final_answer. " +
                "Never skip a gate — report each one explicitly."
        )
    )

    /** All modes in pill-bar display order */
    val ALL: List<AgentMode> = listOf(CHAT, RESEARCH, CODE, PLAN, GOAL, WEB, REVIEW, DEPLOY)

    fun forId(id: ModeId): AgentMode = ALL.first { it.id == id }
    fun forIdOrNull(id: ModeId): AgentMode? = ALL.firstOrNull { it.id == id }
}

// ─── Helper ───────────────────────────────────────────────────────────────────

private fun List<AIModel>.ids(): List<String> = map { it.modelId }

/** All known models by ID — used for resolving waterfall model IDs back to AIModel objects */
val ALL_KNOWN_MODELS: Map<String, AIModel> = listOf(
    FreeModels.GROQ_LLAMA_8B,
    FreeModels.GROQ_LLAMA_70B,
    FreeModels.GEMINI_FLASH,
    FreeModels.GEMINI_FLASH_THINKING,
    FreeModels.OPENROUTER_LLAMA,
    FreeModels.OPENROUTER_QWEN,
    FreeModels.SAMBANOVA_LLAMA_70B,
    FreeModels.GITHUB_GPT4O,
    FreeModels.GITHUB_DEEPSEEK_R1
).associateBy { it.modelId }
