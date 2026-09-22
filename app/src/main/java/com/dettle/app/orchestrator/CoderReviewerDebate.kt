package com.dettle.app.orchestrator

import com.dettle.app.domain.model.ApiMessage
import com.dettle.app.domain.model.TaskContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CODER vs REVIEWER debate protocol.
 *
 * Produces better code than a single-pass generation by:
 * 1. CODER writes initial implementation
 * 2. REVIEWER critiques: bugs, edge cases, style, security
 * 3. CODER revises based on critique
 * 4. Repeat up to MAX_ROUNDS
 *
 * Early termination: if REVIEWER says "LGTM" or critique is < 50 chars, accept.
 *
 * This mimics the peer review process that produces production-quality code.
 */
@Singleton
class CoderReviewerDebate @Inject constructor(
    private val reActLoop: ReActLoop,
    private val agentBus: AgentBus
) {
    companion object {
        const val MAX_ROUNDS = 3
    }

    /**
     * Run the debate and return the final accepted code.
     *
     * @param task          The coding task description
     * @param context       Task context (repo, language, etc.)
     * @param initialCode   Optional starting point (e.g. existing code to refactor)
     */
    suspend fun runDebate(
        task: String,
        context: TaskContext,
        initialCode: String = ""
    ): DebateResult {
        var currentCode = initialCode
        var roundsCompleted = 0
        val history = mutableListOf<DebateRound>()

        for (round in 1..MAX_ROUNDS) {
            roundsCompleted = round

            // ── CODER phase ──────────────────────────────────────────────
            agentBus.setAgentStatus(AgentRole.CODER, AgentStatus.WORKING,
                "Round $round: Writing/revising code...")

            val coderPrompt = buildCoderPrompt(task, currentCode, history, round)
            val coderOutput = callAgent(AgentRole.CODER, coderPrompt, context)

            agentBus.setAgentStatus(AgentRole.CODER, AgentStatus.DONE)

            // ── REVIEWER phase ───────────────────────────────────────────
            agentBus.setAgentStatus(AgentRole.REVIEWER, AgentStatus.WORKING,
                "Round $round: Reviewing code...")

            val reviewerPrompt = buildReviewerPrompt(task, coderOutput, round)
            val critique = callAgent(AgentRole.REVIEWER, reviewerPrompt, context)

            agentBus.setAgentStatus(AgentRole.REVIEWER, AgentStatus.DONE)

            val roundResult = DebateRound(round, coderOutput, critique)
            history.add(roundResult)

            agentBus.postEvent(AgentEvent.DebateRound(round, coderOutput, critique))

            // ── Early termination ────────────────────────────────────────
            val isApproved = critique.length < 60 ||
                critique.contains("LGTM", ignoreCase = true) ||
                critique.contains("looks good", ignoreCase = true) ||
                critique.contains("no issues", ignoreCase = true) ||
                critique.contains("approved", ignoreCase = true)

            currentCode = coderOutput

            if (isApproved) {
                agentBus.postEvent(AgentEvent.DebateResolved(currentCode, roundsCompleted))
                return DebateResult(
                    finalCode = currentCode,
                    rounds = roundsCompleted,
                    rounds_history = history,
                    wasApproved = true
                )
            }
        }

        // Max rounds hit — accept best version
        agentBus.postEvent(AgentEvent.DebateResolved(currentCode, roundsCompleted))
        return DebateResult(
            finalCode = currentCode,
            rounds = roundsCompleted,
            rounds_history = history,
            wasApproved = false
        )
    }

    private suspend fun callAgent(role: AgentRole, prompt: String, context: TaskContext): String {
        val messages = listOf(ApiMessage(role = "user", content = prompt))
        val sb = StringBuilder()

        reActLoop.run(
            userMessage = prompt,
            conversationHistory = messages,
            taskContext = context
        ).collect { event ->
            when (event) {
                is LoopEvent.StreamComplete -> sb.append(event.fullText)
                is LoopEvent.FinalAnswer -> { /* done */ }
                else -> {}
            }
        }
        return sb.toString().trim()
    }

    private fun buildCoderPrompt(
        task: String,
        previousCode: String,
        history: List<DebateRound>,
        round: Int
    ): String = buildString {
        appendLine("You are the CODER agent. Your job: write clean, correct, production-ready code.")
        appendLine()
        appendLine("TASK: $task")
        appendLine()

        if (round == 1 && previousCode.isBlank()) {
            appendLine("Write the complete implementation. Think step by step before writing code.")
        } else {
            if (previousCode.isNotBlank() && round == 1) {
                appendLine("EXISTING CODE TO IMPROVE:")
                appendLine("```")
                appendLine(previousCode)
                appendLine("```")
                appendLine()
            }

            if (history.isNotEmpty()) {
                val lastReview = history.last()
                appendLine("REVIEWER CRITIQUE (Round ${lastReview.round}):")
                appendLine(lastReview.critique)
                appendLine()
                appendLine("Revise the code to address ALL points in the critique.")
            }
        }

        appendLine()
        appendLine("Output ONLY the code. No explanations unless inside code comments.")
    }

    private fun buildReviewerPrompt(task: String, code: String, round: Int): String = buildString {
        appendLine("You are the REVIEWER agent. Your job: find bugs, issues, and improvements.")
        appendLine()
        appendLine("ORIGINAL TASK: $task")
        appendLine()
        appendLine("CODE TO REVIEW (Round $round):")
        appendLine("```")
        appendLine(code)
        appendLine("```")
        appendLine()
        appendLine("""Review checklist:
□ Correctness — does it actually do what the task requires?
□ Edge cases — null inputs, empty lists, network failures, race conditions
□ Security — no injection, no hardcoded secrets, no unvalidated input  
□ Performance — any O(n²) where O(n) works, any memory leaks
□ Kotlin idioms — idiomatic use of language features
□ Error handling — all errors handled, not silently swallowed

If the code looks good with only minor issues, respond with: "LGTM - [brief note]"
If there are real issues, list them numbered and concise. No praise, just problems.""")
    }
}

data class DebateRound(val round: Int, val coderOutput: String, val critique: String)

data class DebateResult(
    val finalCode: String,
    val rounds: Int,
    val rounds_history: List<DebateRound>,
    val wasApproved: Boolean
)
