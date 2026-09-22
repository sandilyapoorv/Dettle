package com.dettle.app.orchestrator.overnight

import android.util.Log
import com.dettle.app.orchestrator.MemoryInjector
import com.dettle.app.orchestrator.SkillInjector
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SkillUpdater"

/**
 * Self-improving skill updater — the agent gets better over time.
 *
 * After every overnight run, this class:
 * 1. Records failures as corrections in long-term memory
 *    → future runs avoid the same mistakes
 * 2. Records successful patterns as learned skills
 *    → future runs reuse what worked
 * 3. Analyzes common failure categories and adds them to the system prompt
 *    → the agent gets globally better at those task types
 *
 * Inspired by: rebelytics/one-skill-to-rule-them-all meta-skill concept
 *
 * This is not fine-tuning — it's prompt engineering backed by persistent memory.
 * No model weights change; just the context gets smarter over time.
 */
@Singleton
class SelfImprovingSkillUpdater @Inject constructor(
    private val memoryInjector: MemoryInjector
) {
    private val recentFailures = mutableListOf<FailureRecord>()

    data class FailureRecord(
        val taskTitle: String,
        val taskType: OvernightTaskType,
        val errorReason: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    /** Called immediately when a task fails during overnight run */
    suspend fun recordFailure(task: OvernightTask, errorReason: String) {
        recentFailures.add(FailureRecord(task.title, task.type, errorReason))

        // Store as correction so it's included in future prompts
        val correctionText = buildCorrectionFromFailure(task, errorReason)
        memoryInjector.storeCorrection(correctionText, task.repoKey)

        Log.d(TAG, "Recorded failure for task: ${task.title}")
    }

    /** Called at end of overnight run — synthesizes learnings */
    suspend fun applyLearnings(results: List<OvernightTaskResult>) {
        val succeeded = results.filter { it.outcome == TaskOutcome.SUCCESS }
        val failed = results.filter { it.outcome == TaskOutcome.FAILED }

        // Learn from successful patterns
        succeeded.forEach { result ->
            if (result.durationMs < 30_000 && result.tokensUsed < 2000) {
                // Fast + efficient task — note the approach
                memoryInjector.storeSkillLearned(
                    "Efficient approach for ${result.task.type.name}: completed in ${result.durationMs / 1000}s. " +
                    "Provider used: ${result.providerUsed}."
                )
            }
        }

        // Analyze failure patterns by task type
        val failuresByType = failed.groupBy { it.task.type }
        failuresByType.forEach { (type, failures) ->
            if (failures.size >= 2) {
                // Repeated failures of same type → add avoidance rule
                val commonErrors = failures.mapNotNull { it.errorReason }.take(3).joinToString("; ")
                memoryInjector.storeCorrection(
                    "[Warning] ${type.name} tasks have been failing repeatedly. Common errors: $commonErrors. " +
                    "Be extra careful with these tasks and verify preconditions first.",
                    ""
                )
            }
        }

        // Clear session failures after applying
        recentFailures.clear()

        Log.d(TAG, "Applied learnings: ${succeeded.size} successes, ${failed.size} failures analyzed")
    }

    private fun buildCorrectionFromFailure(task: OvernightTask, errorReason: String): String {
        val typeAdvice = when (task.type) {
            OvernightTaskType.CODE_FEATURE -> "Check that all required files exist and imports are correct before implementing."
            OvernightTaskType.CODE_FIX -> "Read the failing test output carefully before making changes."
            OvernightTaskType.DEPLOY -> "Verify Cloudflare token and account ID are valid. Check file paths."
            OvernightTaskType.WRITE_TESTS -> "Ensure the class under test is importable and its dependencies are injectable."
            OvernightTaskType.RESEARCH -> "Use multiple search queries. Cross-reference results."
            else -> "Verify prerequisites before starting. Check tool availability."
        }

        return "Task '${task.title}' (${task.type.name}) failed: $errorReason. " +
               "$typeAdvice"
    }
}
