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

            "github_get_default_branch" -> {
                val owner = args["owner"] ?: activeProj?.owner ?: return errResult(toolCall, "Missing owner and no active project anchored")
                val repo = args["repo"] ?: activeProj?.repo ?: return errResult(toolCall, "Missing repo and no active project anchored")
                val defaultBranch = githubClient.getDefaultBranch(owner, repo)
                    .getOrElse { return errResult(toolCall, it.message ?: "Failed to get default branch") }
                "Default branch for **$owner/$repo** is `$defaultBranch`"
            }

            "github_map_repo" -> {
                val owner = args["owner"] ?: activeProj?.owner ?: return errResult(toolCall, "Missing owner and no active project anchored")
                val repo = args["repo"] ?: activeProj?.repo ?: return errResult(toolCall, "Missing repo and no active project anchored")
                val branch = args["branch"] ?: activeProj?.branch ?: runCatching {
                    githubClient.getDefaultBranch(owner, repo).getOrNull()
                }.getOrNull() ?: "main"
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
                val rawChanges = (args["file_changes"] ?: "[]").trim()
                val cleanedJson = if (rawChanges.startsWith("```")) {
                    rawChanges.substringAfter("\n").substringBeforeLast("```").trim()
                } else rawChanges

                val commitMsg = args["commit_message"] ?: "Update by Dettle agent"
                val prTitle = args["pr_title"] ?: commitMsg
                val prBody = args["pr_body"] ?: ""

                val fileChanges = try {
                    val element = json.parseToJsonElement(cleanedJson)
                    when (element) {
                        is kotlinx.serialization.json.JsonArray -> {
                            element.mapNotNull { el ->
                                val obj = el as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
                                val path = (obj["path"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.trim()
                                val content = (obj["content"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                                if (!path.isNullOrBlank() && content != null) {
                                    com.dettle.app.data.github.FileChange(path = path, content = content)
                                } else null
                            }
                        }
                        is kotlinx.serialization.json.JsonObject -> {
                            val path = (element["path"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.trim()
                            val content = (element["content"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                            if (!path.isNullOrBlank() && content != null) {
                                listOf(com.dettle.app.data.github.FileChange(path = path, content = content))
                            } else emptyList()
                        }
                        else -> emptyList()
                    }
                } catch (e: Exception) {
                    return errResult(toolCall, "Invalid file_changes JSON: ${e.message}")
                }

                if (fileChanges.isEmpty()) {
                    return errResult(toolCall, "No valid file changes found in file_changes parameter. Expected JSON array of {path, content} objects.")
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

            "github_merge_pr" -> {
                val owner = args["owner"] ?: activeProj?.owner ?: return errResult(toolCall, "Missing owner and no active project anchored")
                val repo = args["repo"] ?: activeProj?.repo ?: return errResult(toolCall, "Missing repo and no active project anchored")
                val pullNumber = args["pull_number"]?.toIntOrNull() ?: return errResult(toolCall, "Missing or invalid pull_number")
                val commitTitle = args["commit_title"]
                val commitMessage = args["commit_message"]
                val mergeMethod = args["merge_method"] ?: "squash"

                val result = githubClient.mergePullRequest(
                    owner = owner,
                    repo = repo,
                    pullNumber = pullNumber,
                    commitTitle = commitTitle,
                    commitMessage = commitMessage,
                    mergeMethod = mergeMethod
                ).getOrElse { return errResult(toolCall, it.message ?: "Failed to merge PR") }

                if (result.merged) {
                    "Pull Request #$pullNumber merged successfully!\n" +
                            "**Merge Commit SHA**: `${result.sha.take(7)}`\n" +
                            "**Message**: ${result.message}"
                } else {
                    "PR #$pullNumber could not be merged: ${result.message}"
                }
            }

            "github_get_pr_status" -> {
                val owner = args["owner"] ?: activeProj?.owner ?: return errResult(toolCall, "Missing owner and no active project anchored")
                val repo = args["repo"] ?: activeProj?.repo ?: return errResult(toolCall, "Missing repo and no active project anchored")
                val pullNumber = args["pull_number"]?.toIntOrNull() ?: return errResult(toolCall, "Missing or invalid pull_number")

                val status = githubClient.getPullRequest(owner, repo, pullNumber)
                    .getOrElse { return errResult(toolCall, it.message ?: "Failed to get PR status") }

                buildString {
                    appendLine("### PR #$pullNumber Status:")
                    appendLine("- **State**: `${status.state}`")
                    appendLine("- **Merged**: ${status.merged}")
                    appendLine("- **Mergeable**: ${status.mergeable ?: "calculating..."}")
                    status.mergeableState?.let { appendLine("- **Mergeable State**: `$it`") }
                    appendLine("- **Head Branch**: `${status.headRef}` (`${status.headSha.take(7)}`)")
                    if (status.htmlUrl.isNotBlank()) appendLine("- **URL**: ${status.htmlUrl}")
                }
            }

            "github_create_release" -> {
                val owner = args["owner"] ?: activeProj?.owner ?: return errResult(toolCall, "Missing owner and no active project anchored")
                val repo = args["repo"] ?: activeProj?.repo ?: return errResult(toolCall, "Missing repo and no active project anchored")
                val tagName = args["tag_name"] ?: return errResult(toolCall, "Missing tag_name")
                val name = args["name"] ?: tagName
                val body = args["body"] ?: "Release $tagName created by Dettle AI Agent"
                val targetBranch = args["target_branch"] ?: activeProj?.branch ?: "main"
                val draft = args["draft"]?.toBooleanStrictOrNull() ?: false
                val prerelease = args["prerelease"]?.toBooleanStrictOrNull() ?: false

                val release = githubClient.createRelease(
                    owner = owner,
                    repo = repo,
                    tagName = tagName,
                    name = name,
                    body = body,
                    targetCommitish = targetBranch,
                    draft = draft,
                    prerelease = prerelease
                ).getOrElse { return errResult(toolCall, it.message ?: "Failed to create release") }

                "GitHub Release Created Successfully!\n" +
                        "**Tag**: `${release.tagName}`\n" +
                        "**Name**: ${release.name ?: tagName}\n" +
                        "**Release URL**: ${release.htmlUrl}"
            }

            "github_trigger_action" -> {
                val owner = args["owner"] ?: activeProj?.owner ?: return errResult(toolCall, "Missing owner and no active project anchored")
                val repo = args["repo"] ?: activeProj?.repo ?: return errResult(toolCall, "Missing repo and no active project anchored")
                val workflowId = args["workflow_id"] ?: return errResult(toolCall, "Missing workflow_id")
                val ref = args["ref"] ?: activeProj?.branch ?: "main"
                val inputsJson = args["inputs"] ?: "{}"
                val inputs = try {
                    val el = json.parseToJsonElement(inputsJson)
                    (el as? kotlinx.serialization.json.JsonObject)?.entries?.associate {
                        it.key to ((it.value as? kotlinx.serialization.json.JsonPrimitive)?.content ?: it.value.toString())
                    } ?: emptyMap()
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

            "github_list_workflow_runs" -> {
                val owner = args["owner"] ?: activeProj?.owner ?: return errResult(toolCall, "Missing owner and no active project anchored")
                val repo = args["repo"] ?: activeProj?.repo ?: return errResult(toolCall, "Missing repo and no active project anchored")
                val branch = args["branch"] ?: activeProj?.branch
                val limit = args["limit"]?.toIntOrNull() ?: 10

                val runs = githubClient.listWorkflowRuns(owner, repo, branch, limit)
                    .getOrElse { return errResult(toolCall, it.message ?: "Failed to list workflow runs") }

                if (runs.isEmpty()) {
                    "No workflow runs found in **$owner/$repo**" + (if (branch != null) " on branch `$branch`" else "")
                } else {
                    buildString {
                        appendLine("### Recent Workflow Runs in **$owner/$repo**:")
                        runs.forEach { r ->
                            val conclusion = r.conclusion ?: "in progress"
                            val icon = when (conclusion) {
                                "success" -> "✅"
                                "failure" -> "❌"
                                "cancelled" -> "⏹️"
                                else -> "⏳"
                            }
                            appendLine("- $icon Run #${r.id}: **${r.status}** (${conclusion}) - [View Run](${r.url})")
                        }
                    }
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

            "cloudflare_get_deploy_status" -> {
                val projectName = args["project_name"]?.ifBlank { null }
                    ?: activeProj?.cloudflareProjectName
                    ?: "dettle-site"
                val deploymentId = args["deployment_id"]?.ifBlank { null }

                if (deploymentId != null) {
                    val deploy = cloudflareClient.getDeployment(projectName, deploymentId)
                        .getOrElse { return errResult(toolCall, it.message ?: "Failed to get deployment status") }
                    buildString {
                        appendLine("### Cloudflare Pages Deployment `$deploymentId`")
                        appendLine("- **Project**: ${deploy.projectName}")
                        appendLine("- **Stage**: `${deploy.stage}`")
                        appendLine("- **Preview URL**: ${deploy.url}")
                        appendLine("- **Production URL**: ${deploy.productionUrl}")
                        if (deploy.createdAt.isNotBlank()) appendLine("- **Created**: ${deploy.createdAt}")
                    }
                } else {
                    val deployments = cloudflareClient.listDeployments(projectName)
                        .getOrElse { return errResult(toolCall, it.message ?: "Failed to list deployments") }
                    if (deployments.isEmpty()) {
                        "No deployments found for Cloudflare Pages project `$projectName`."
                    } else {
                        val latest = deployments.first()
                        buildString {
                            appendLine("### Latest Deployment for `$projectName`")
                            appendLine("- **ID**: `${latest.id}`")
                            appendLine("- **Stage**: `${latest.stage}`")
                            appendLine("- **Preview URL**: ${latest.url}")
                            appendLine("- **Production URL**: ${latest.productionUrl}")
                            if (latest.createdAt.isNotBlank()) appendLine("- **Created**: ${latest.createdAt}")
                            appendLine("\nTotal recent deployments: ${deployments.size}")
                        }
                    }
                }
            }

            "cloudflare_list_projects" -> {
                val pages = cloudflareClient.listPagesProjects().getOrNull() ?: emptyList()
                val workers = cloudflareClient.listWorkers().getOrNull() ?: emptyList()
                buildString {
                    appendLine("### Cloudflare Resources")
                    appendLine("#### Pages Projects (${pages.size}):")
                    if (pages.isEmpty()) appendLine("_No Pages projects found._")
                    else pages.forEach { p ->
                        appendLine("- **${p.name}** → [${p.productionUrl}](${p.productionUrl})")
                    }
                    appendLine()
                    appendLine("#### Workers Scripts (${workers.size}):")
                    if (workers.isEmpty()) appendLine("_No Workers scripts found._")
                    else workers.forEach { w ->
                        appendLine("- **$w**")
                    }
                }
            }

            "cloudflare_purge_cache" -> {
                val zoneId = args["zone_id"] ?: return errResult(toolCall, "Missing zone_id")
                val success = cloudflareClient.purgeCache(zoneId)
                    .getOrElse { return errResult(toolCall, it.message ?: "Failed to purge cache") }
                if (success) "Successfully purged Cloudflare cache for zone `$zoneId`."
                else "Failed to purge cache for zone `$zoneId`."
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
