package com.dettle.app.orchestrator

import com.dettle.app.data.cloudflare.CloudflareClient
import com.dettle.app.data.db.dao.MemoryDao
import com.dettle.app.data.github.GitHubClient
import com.dettle.app.data.workspace.LocalWorkspaceManager
import com.dettle.app.domain.model.ToolCall
import com.dettle.app.domain.model.ToolResult
import com.dettle.app.orchestrator.project.ProjectContextManager
import com.dettle.app.orchestrator.rag.CodebaseRagService
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
 * Bare-metal tool executor routing AI function calls directly to services:
 * GitHub API, Cloudflare API, Local Workspace, Room Vector/FTS RAG, and Web Search.
 */
@Singleton
class ToolExecutor @Inject constructor(
    private val githubClient: GitHubClient,
    private val cloudflareClient: CloudflareClient,
    private val repoMapper: RepoMapper,
    private val webSearchClient: WebSearchClient,
    private val httpClient: OkHttpClient,
    private val json: Json,
    private val workspaceManager: LocalWorkspaceManager,
    private val codebaseRagService: CodebaseRagService,
    private val projectContextManager: ProjectContextManager,
    private val memoryDao: MemoryDao
) {
    suspend fun execute(toolCall: ToolCall): ToolResult {
        val args = toolCall.arguments
        val activeProj = projectContextManager.activeProject.value

        val content = when (toolCall.name) {

            "github_list_repos" -> {
                val forceRefresh = args["force_refresh"]?.toBooleanStrictOrNull() ?: false
                val repos = githubClient.getUserRepositories(forceRefresh)
                    .getOrElse { return errResult(toolCall, it.message ?: "Failed to list repositories") }
                if (repos.isEmpty()) {
                    "No repositories found for the authenticated GitHub user."
                } else {
                    buildString {
                        appendLine("### Connected GitHub Repositories (${repos.size} total):")
                        repos.take(25).forEach { r ->
                            val priv = if (r.isPrivate) "🔒 Private" else "🌐 Public"
                            val lang = r.language?.let { " • $it" }.orEmpty()
                            val stars = if (r.stars > 0) " • ⭐ ${r.stars}" else ""
                            appendLine("- **[${r.fullName}](https://github.com/${r.fullName})** ($priv$lang$stars)")
                            if (!r.description.isNullOrBlank()) {
                                appendLine("  _${r.description}_")
                            }
                        }
                        if (repos.size > 25) {
                            appendLine("\n_...and ${repos.size - 25} more repositories._")
                        }
                    }
                }
            }

            "github_get_repo" -> {
                val owner = args["owner"] ?: activeProj?.owner ?: return errResult(toolCall, "Missing owner and no active project anchored")
                val repo = args["repo"] ?: activeProj?.repo ?: return errResult(toolCall, "Missing repo and no active project anchored")
                val details = githubClient.getRepoDetails(owner, repo)
                    .getOrElse { return errResult(toolCall, it.message ?: "Failed to get repository details") }
                buildString {
                    appendLine("### Repository: ${details.fullName}")
                    appendLine("- **Visibility**: ${if (details.isPrivate) "Private" else "Public"}")
                    appendLine("- **Default Branch**: `${details.defaultBranch}`")
                    appendLine("- **Primary Language**: ${details.language ?: "None"}")
                    appendLine("- **Stars**: ⭐ ${details.stars} | **Forks**: 🍴 ${details.forks}")
                    appendLine("- **Open Issues/PRs**: ${details.openIssuesCount}")
                    if (!details.description.isNullOrBlank()) {
                        appendLine("- **Description**: ${details.description}")
                    }
                    details.htmlUrl?.let { appendLine("- **URL**: $it") }
                }
            }

            "github_list_branches" -> {
                val owner = args["owner"] ?: activeProj?.owner ?: return errResult(toolCall, "Missing owner and no active project anchored")
                val repo = args["repo"] ?: activeProj?.repo ?: return errResult(toolCall, "Missing repo and no active project anchored")
                val branches = githubClient.listRepoBranches(owner, repo)
                    .getOrElse { return errResult(toolCall, it.message ?: "Failed to list branches") }
                if (branches.isEmpty()) {
                    "No branches found for $owner/$repo"
                } else {
                    "Branches in **$owner/$repo**:\n" + branches.joinToString("\n") { "- `$it`" }
                }
            }

            "github_list_commits" -> {
                val owner = args["owner"] ?: activeProj?.owner ?: return errResult(toolCall, "Missing owner and no active project anchored")
                val repo = args["repo"] ?: activeProj?.repo ?: return errResult(toolCall, "Missing repo and no active project anchored")
                val branch = args["branch"] ?: activeProj?.branch ?: "main"
                val limit = args["limit"]?.toIntOrNull() ?: 10
                val commits = githubClient.listRepoCommits(owner, repo, branch, limit)
                    .getOrElse { return errResult(toolCall, it.message ?: "Failed to list commits") }
                if (commits.isEmpty()) {
                    "No commits found on branch `$branch` for $owner/$repo"
                } else {
                    buildString {
                        appendLine("### Recent commits on `$branch` in **$owner/$repo**:")
                        commits.forEach { c ->
                            appendLine("- [`${c.sha}`] **${c.message}** (by ${c.author} on ${c.date.take(10)})")
                        }
                    }
                }
            }

            "github_list_issues" -> {
                val owner = args["owner"] ?: activeProj?.owner ?: return errResult(toolCall, "Missing owner and no active project anchored")
                val repo = args["repo"] ?: activeProj?.repo ?: return errResult(toolCall, "Missing repo and no active project anchored")
                val state = args["state"] ?: "open"
                val limit = args["limit"]?.toIntOrNull() ?: 10
                val issues = githubClient.listRepoIssues(owner, repo, state, limit)
                    .getOrElse { return errResult(toolCall, it.message ?: "Failed to list issues") }
                if (issues.isEmpty()) {
                    "No $state issues or pull requests found in $owner/$repo"
                } else {
                    buildString {
                        appendLine("### Issues & PRs ($state) in **$owner/$repo**:")
                        issues.forEach { issue ->
                            val type = if (issue.isPullRequest) "PR" else "Issue"
                            appendLine("- **#${issue.number}** [$type]: ${issue.title} (by @${issue.author})")
                        }
                    }
                }
            }

            "github_map_repo" -> {
                val owner = args["owner"] ?: activeProj?.owner ?: return errResult(toolCall, "Missing owner and no active project anchored")
                val repo = args["repo"] ?: activeProj?.repo ?: return errResult(toolCall, "Missing repo and no active project anchored")
                val branch = args["branch"] ?: activeProj?.branch ?: "main"
                repoMapper.buildRepoMap(owner, repo, branch)
                    .getOrElse { return errResult(toolCall, it.message ?: "Failed to map repo") }
            }

            "github_read_file" -> {
                val owner = args["owner"] ?: activeProj?.owner ?: return errResult(toolCall, "Missing owner and no active project anchored")
                val repo = args["repo"] ?: activeProj?.repo ?: return errResult(toolCall, "Missing repo and no active project anchored")
                val path = args["path"] ?: return errResult(toolCall, "Missing path")
                val branch = args["branch"] ?: activeProj?.branch ?: "main"
                githubClient.readFile(owner, repo, path, branch)
                    .getOrElse { return errResult(toolCall, it.message ?: "Failed to read file") }
            }

            "github_create_branch_pr" -> {
                val owner = args["owner"] ?: activeProj?.owner ?: return errResult(toolCall, "Missing owner and no active project anchored")
                val repo = args["repo"] ?: activeProj?.repo ?: return errResult(toolCall, "Missing repo and no active project anchored")
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
                val owner = args["owner"] ?: activeProj?.owner ?: return errResult(toolCall, "Missing owner and no active project anchored")
                val repo = args["repo"] ?: activeProj?.repo ?: return errResult(toolCall, "Missing repo and no active project anchored")
                val workflowId = args["workflow_id"] ?: return errResult(toolCall, "Missing workflow_id")
                val ref = args["ref"] ?: activeProj?.branch ?: "main"
                val inputsJson = args["inputs"] ?: "{}"
                val inputs = try {
                    json.parseToJsonElement(inputsJson).jsonObject
                        .entries.associate { it.key to it.value.jsonPrimitive.content }
                } catch (_: Exception) { emptyMap() }

                githubClient.triggerWorkflow(owner, repo, workflowId, ref, inputs)
                    .getOrElse { return errResult(toolCall, it.message ?: "Failed to trigger workflow") }
                "Workflow `$workflowId` triggered on `$ref` in $owner/$repo"
            }

            "github_poll_run" -> {
                val owner = args["owner"] ?: activeProj?.owner ?: return errResult(toolCall, "Missing owner and no active project anchored")
                val repo = args["repo"] ?: activeProj?.repo ?: return errResult(toolCall, "Missing repo and no active project anchored")
                val workflowId = args["workflow_id"] ?: return errResult(toolCall, "Missing workflow_id")
                val runs = githubClient.getWorkflowRuns(owner, repo, workflowId)
                    .getOrElse { return errResult(toolCall, it.message ?: "Failed to fetch workflow runs") }
                if (runs.isEmpty()) {
                    "No workflow runs found for `$workflowId`"
                } else {
                    val latest = runs.first()
                    "Latest Workflow Run #${latest.id}:\n" +
                            "**Status**: `${latest.status}`\n" +
                            "**Conclusion**: `${latest.conclusion ?: "running"}`\n" +
                            "**URL**: ${latest.url}"
                }
            }

            "cloudflare_deploy_preview" -> {
                val projectName = args["project_name"]?.ifBlank { null }
                    ?: activeProj?.cloudflareProjectName
                    ?: "dettle-site"
                val files = workspaceManager.readAllFiles()
                if (files.isEmpty()) {
                    return errResult(toolCall, "Workspace is empty. Write HTML/JS files first with workspace_write_file before deploying.")
                }
                val deploy = cloudflareClient.deployToPages(projectName, files)
                    .getOrElse { return errResult(toolCall, it.message ?: "Failed to deploy to Cloudflare Pages") }
                "Cloudflare Pages Preview Deployed Successfully!\n" +
                        "**Project**: ${deploy.projectName}\n" +
                        "**Preview URL**: ${deploy.url}\n" +
                        "**Production URL**: ${deploy.productionUrl}\n" +
                        "**Files Deployed**: ${deploy.fileCount}"
            }

            "cloudflare_publish_worker" -> {
                val workerName = args["worker_name"] ?: args["script_name"]
                    ?: "${activeProj?.cloudflareProjectName ?: "dettle"}-worker"
                val scriptBody = args["script"] ?: args["code"] ?: runCatching { workspaceManager.readFile("worker.js") }.getOrNull()
                    ?: return errResult(toolCall, "Missing script body and worker.js not found in workspace")
                val deploy = cloudflareClient.deployWorker(workerName, scriptBody)
                    .getOrElse { return errResult(toolCall, it.message ?: "Failed to deploy Cloudflare Worker") }
                "Cloudflare Worker Deployed Successfully!\n" +
                        "**Worker**: ${deploy.name}\n" +
                        "**Live URL**: ${deploy.url}\n" +
                        "**Script Size**: ${deploy.scriptSize} bytes"
            }

            "codebase_search" -> {
                val query = args["query"] ?: return errResult(toolCall, "Missing query")
                val projectId = activeProj?.id ?: "default"
                codebaseRagService.search(projectId, query)
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
                val query = args["query"] ?: ""
                val memories = memoryDao.getAllMemories()
                if (memories.isEmpty()) {
                    "No memories stored yet."
                } else {
                    val matching = if (query.isNotBlank()) {
                        memories.filter { it.content.contains(query, ignoreCase = true) || it.tags.any { tag -> tag.contains(query, ignoreCase = true) } }
                    } else memories
                    matching.take(5).joinToString("\n") { "• [${it.type}] ${it.content}" }
                        .ifBlank { "No memories matching '$query'" }
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
            body.replace(Regex("<[^>]+>"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(8000)
        } catch (e: Exception) {
            "Error fetching URL: ${e.message}"
        }
    }
}
