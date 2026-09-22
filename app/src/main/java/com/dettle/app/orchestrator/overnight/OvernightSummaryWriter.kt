package com.dettle.app.orchestrator.overnight

import android.util.Log
import com.dettle.app.data.drive.GoogleDriveConnector
import com.dettle.app.data.github.GitHubClient
import com.dettle.app.data.settings.ApiKeyStore
import com.dettle.app.data.workspace.LocalWorkspaceManager
import javax.inject.Inject
import javax.inject.Singleton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "SummaryWriter"

/**
 * Writes the overnight run summary to the local workspace, GitHub, and Google Drive.
 */
@Singleton
class OvernightSummaryWriter @Inject constructor(
    private val gitHubClient: GitHubClient,
    private val driveConnector: GoogleDriveConnector,
    private val keyStore: ApiKeyStore,
    private val workspaceManager: LocalWorkspaceManager
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    suspend fun writeSummary(
        tasks: List<OvernightTaskResult>,
        totalDurationMs: Long
    ) {
        val markdown = buildSummaryMarkdown(tasks, totalDurationMs)
        val date = dateFormat.format(Date())
        val fileName = "overnight-summary-$date.md"

        // ── Save Locally to Workspace ─────────────────────────────────────
        try {
            workspaceManager.writeFile("summaries/$fileName", markdown)
            Log.d(TAG, "Summary saved to local workspace: summaries/$fileName")
        } catch (e: Exception) {
            Log.w(TAG, "Local workspace write failed: ${e.message}")
        }

        // ── Upload to Google Drive ─────────────────────────────────────
        if (driveConnector.isConnected()) {
            try {
                val result = driveConnector.saveOvernightSummary(markdown)
                if (result.isSuccess) {
                    Log.d(TAG, "Summary uploaded to Drive: ${result.getOrNull()?.webViewLink}")
                } else {
                    Log.w(TAG, "Drive upload failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Drive upload exception", e)
            }
        }

        // ── Push to GitHub ─────────────────────────────────────────────
        val owner = keyStore.githubOwner ?: return

        try {
            val branchName = "dettle-summary-$date"
            gitHubClient.createBranchAndPR(
                owner = owner,
                repo = "dettle-workspace",
                branchName = branchName,
                fileChanges = listOf(
                    com.dettle.app.data.github.FileChange(
                        path = "summaries/$fileName",
                        content = markdown
                    )
                ),
                commitMessage = "🤖 Overnight run summary — $date",
                prTitle = "🤖 Overnight Run Summary — $date",
                prBody = buildPrDescription(tasks, totalDurationMs)
            )
            Log.d(TAG, "Summary pushed to GitHub: $owner/dettle-workspace")
        } catch (e: Exception) {
            Log.w(TAG, "GitHub push failed: ${e.message}")
        }
    }

    private fun buildSummaryMarkdown(
        tasks: List<OvernightTaskResult>,
        totalDurationMs: Long
    ): String = buildString {
        val successCount = tasks.count { it.outcome == TaskOutcome.SUCCESS }
        val failedCount = tasks.count { it.outcome == TaskOutcome.FAILED }
        val partialCount = tasks.count { it.outcome == TaskOutcome.PARTIAL }
        val totalTokens = tasks.sumOf { it.tokensUsed }
        val durationMin = totalDurationMs / 60_000

        appendLine("# 🤖 Dettle Overnight Run")
        appendLine("**Date:** ${timeFormat.format(Date())}")
        appendLine()
        appendLine("## Summary")
        appendLine("| Metric | Value |")
        appendLine("|--------|-------|")
        appendLine("| Total tasks | ${tasks.size} |")
        appendLine("| ✅ Succeeded | $successCount |")
        appendLine("| ⚠️ Partial | $partialCount |")
        appendLine("| ❌ Failed | $failedCount |")
        appendLine("| Duration | ${durationMin}m |")
        appendLine("| Tokens used | ${"%,d".format(totalTokens)} |")
        appendLine("| Providers | ${tasks.map { it.providerUsed }.distinct().joinToString(", ")} |")
        appendLine()

        appendLine("## Task Results")
        appendLine()
        tasks.forEachIndexed { i, result ->
            appendLine("### ${i + 1}. ${result.outcome.emoji} ${result.task.title}")
            appendLine("**Type:** `${result.task.type.name}` | **Repo:** `${result.task.repoKey.ifBlank { "N/A" }}` | **Duration:** ${result.durationMs / 1000}s")
            appendLine()
            appendLine(result.summary)
            if (result.errorReason != null) {
                appendLine()
                appendLine("> ❌ **Error:** ${result.errorReason}")
            }
            appendLine()
            appendLine("---")
            appendLine()
        }

        if (failedCount > 0) {
            appendLine("## Failed Tasks — Error Analysis")
            tasks.filter { it.outcome == TaskOutcome.FAILED }.forEach { result ->
                appendLine("- **${result.task.title}**: ${result.errorReason ?: "Unknown"}")
            }
            appendLine()
        }

        appendLine("---")
        appendLine("*Generated by Dettle Agent — [github.com/dettle](https://github.com/dettle)*")
    }

    private fun buildPrDescription(
        tasks: List<OvernightTaskResult>,
        totalDurationMs: Long
    ): String {
        val success = tasks.count { it.outcome == TaskOutcome.SUCCESS }
        return buildString {
            appendLine("## 🤖 Overnight Run Summary")
            appendLine()
            appendLine("Dettle ran **${tasks.size} tasks** overnight. $success/${tasks.size} succeeded.")
            appendLine()
            appendLine("### Tasks:")
            tasks.forEach { r ->
                appendLine("- ${r.outcome.emoji} **${r.task.title}** (${r.durationMs / 1000}s)")
            }
            appendLine()
            appendLine("Total runtime: ${totalDurationMs / 60_000}min")
        }
    }
}
