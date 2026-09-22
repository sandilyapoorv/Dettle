package com.dettle.app.orchestrator

import com.dettle.app.data.github.GitHubClient
import com.dettle.app.data.workspace.LocalWorkspaceManager
import com.dettle.app.domain.model.ToolCall
import com.dettle.app.domain.model.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executes tool calls from the AI agent by routing to the correct service.
 */
@Singleton
class ToolExecutor @Inject constructor(
    private val githubClient: GitHubClient,
    private val repoMapper: RepoMapper,
    private val webSearchClient: WebSearchClient,
    private val httpClient: OkHttpClient,
    private val json: Json,
    private val workspaceManager: LocalWorkspaceManager
) {
    suspend fun execute(toolCall: ToolCall): ToolResult {
        val args = toolCall.arguments
        val content = when (toolCall.name) {

            "github_map_repo" -> {
                val owner = args["owner"] ?: return errResult(toolCall, "Missing owner")
                val repo = args["repo"] ?: return errResult(toolCall, "Missing repo")
                val branch = args["branch"] ?: "main"
                repoMapper.buildRepoMap(owner, repo, branch)
                    .getOrElse { return errResult(toolCall, it.message ?: "Failed to map repo") }
            }

            "github_read_file" -> {
                val owner = args["owner"] ?: return errResult(toolCall, "Missing owner")
                val repo = args["repo"] ?: return errResult(toolCall, "Missing repo")
                val path = args["path"] ?: return errResult(toolCall, "Missing path")
                val branch = args["branch"] ?: "main"
                githubClient.readFile(owner, repo, path, branch)
                    .getOrElse { return errResult(toolCall, it.message ?: "Failed to read file") }
            }

            "github_create_branch_pr" -> {
                val owner = args["owner"] ?: return errResult(toolCall, "Missing owner")
                val repo = args["repo"] ?: return errResult(toolCall, "Missing repo")
                val branch = args["branch_name"] ?: return errResult(toolCall, "Missing branch_name")
                val fileChangesJson = args["file_changes"] ?: "[]"
                val commitMsg = args["commit_message"] ?: "Update by Dettle agent"
                val prTitle = args["pr_title"] ?: commitMsg
                val prBody = args["pr_body"] ?: ""

                val fileChanges = try {
                    json.parseToJsonElement(fileChangesJson).jsonArray.map { el ->
                        val obj = el.jsonObject
                        com.dettle.app.data.github.FileChange(
                            path = obj["path"]?.jsonPrimitive?.content ?: "",
                            content = obj["content"]?.jsonPrimitive?.content ?: ""
                        )
                    }
                } catch (e: Exception) {
                    return errResult(toolCall, "Invalid file_changes JSON: ${e.message}")
                }

                val pr = githubClient.createBranchAndPR(
                    owner, repo, branch, fileChanges, commitMsg, prTitle, prBody
                ).getOrElse { return errResult(toolCall, it.message ?: "Failed to create PR") }

                "Pull Request created successfully!\n" +
                        "**PR #${pr.number}**: ${pr.title}\n" +
                        "**Branch**: `${pr.branch}`\n" +
                        "**Files changed**: ${pr.filesChanged}\n" +
                        "**URL**: ${pr.url}"
            }

            "github_trigger_action" -> {
                val owner = args["owner"] ?: return errResult(toolCall, "Missing owner")
                val repo = args["repo"] ?: return errResult(toolCall, "Missing repo")
                val workflowId = args["workflow_id"] ?: return errResult(toolCall, "Missing workflow_id")
                val ref = args["ref"] ?: "main"
                val inputsJson = args["inputs"] ?: "{}"
                val inputs = try {
                    json.parseToJsonElement(inputsJson).jsonObject
                        .entries.associate { it.key to it.value.jsonPrimitive.content }
                } catch (_: Exception) { emptyMap() }

                githubClient.triggerWorkflow(owner, repo, workflowId, ref, inputs)
                    .getOrElse { return errResult(toolCall, it.message ?: "Failed to trigger workflow") }
                "Workflow `$workflowId` triggered on `$ref`"
            }

            "web_search" -> {
                val query = args["query"] ?: return errResult(toolCall, "Missing query")
                val numResults = args["num_results"]?.toIntOrNull() ?: 5
                webSearchClient.search(query, numResults)
            }

            "read_url" -> {
                val url = args["url"] ?: return errResult(toolCall, "Missing url")
                fetchUrl(url)
            }

            "memory_recall" -> {
                // Phase 3: implemented with Room database
                "Memory recall not yet implemented. This will be available in Phase 3."
            }

            "cloudflare_deploy_preview" -> {
                // Phase 3: Cloudflare client
                "Cloudflare deployment will be available in Phase 3."
            }

            "cloudflare_publish_worker" -> {
                "Cloudflare Worker deployment will be available in Phase 3."
            }

            "workspace_list_files" -> {
                val dir = args["dir"] ?: ""
                val files = workspaceManager.listFiles(dir)
                if (files.isEmpty()) "No files found in workspace."
                else files.joinToString("\n")
            }

            "workspace_read_file" -> {
                val path = args["path"] ?: return errResult(toolCall, "Missing path")
                try {
                    workspaceManager.readFile(path)
                } catch (e: Exception) {
                    errResult(toolCall, e.message ?: "Failed to read file").content
                }
            }

            "workspace_write_file" -> {
                val path = args["path"] ?: return errResult(toolCall, "Missing path")
                val writeContent = args["content"] ?: return errResult(toolCall, "Missing content")
                try {
                    workspaceManager.writeFile(path, writeContent)
                    "Successfully wrote $path"
                } catch (e: Exception) {
                    errResult(toolCall, e.message ?: "Failed to write file").content
                }
            }

            "workspace_delete_file" -> {
                val path = args["path"] ?: return errResult(toolCall, "Missing path")
                try {
                    if (workspaceManager.deleteFile(path)) "Deleted $path" else "Failed to delete $path"
                } catch (e: Exception) {
                    errResult(toolCall, e.message ?: "Failed to delete file").content
                }
            }

            "final_answer" -> {
                args["summary"] ?: args["details"] ?: "Task complete."
            }

            else -> "Unknown tool: ${toolCall.name}"
        }

        return ToolResult(
            toolCallId = toolCall.id,
            toolName = toolCall.name,
            content = content
        )
    }

    private fun errResult(toolCall: ToolCall, message: String) = ToolResult(
        toolCallId = toolCall.id,
        toolName = toolCall.name,
        content = "Error: $message",
        isError = true
    )

    private suspend fun fetchUrl(url: String): String = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Dettle-Agent/1.0")
                .build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext "Empty response"
            // Strip HTML tags for readability
            body.replace(Regex("<[^>]+>"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(8000)  // Limit to avoid token explosion
        } catch (e: Exception) {
            "Error fetching URL: ${e.message}"
        }
    }
}
