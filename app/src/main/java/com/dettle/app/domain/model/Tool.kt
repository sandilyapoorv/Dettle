package com.dettle.app.domain.model

import kotlinx.serialization.Serializable

/** A tool definition sent to AI APIs so they know what they can call */
@Serializable
data class Tool(
    val name: String,
    val description: String,
    val parameters: ToolParameters,
    val requiresApproval: Boolean = false,
    val safetyLevel: SafetyLevel = SafetyLevel.AUTO
)

@Serializable
data class ToolParameters(
    val type: String = "object",
    val properties: Map<String, ToolProperty>,
    val required: List<String> = emptyList()
)

@Serializable
data class ToolProperty(
    val type: String,
    val description: String,
    val enum: List<String>? = null
)

enum class SafetyLevel {
    AUTO,       // Execute automatically
    CONFIRM,    // Show approval card, auto-approve after 10s
    REQUIRE     // Must be manually tapped before execution
}

/** Registry of all tools available to the agent */
object AgentTools {

    val GITHUB_MAP_REPO = Tool(
        name = "github_map_repo",
        description = "Get the complete directory tree and file signatures of a GitHub repository. Use this first before reading any files — it gives you the architecture without wasting tokens on full file contents.",
        parameters = ToolParameters(
            properties = mapOf(
                "owner" to ToolProperty("string", "GitHub repository owner/org name"),
                "repo" to ToolProperty("string", "Repository name"),
                "branch" to ToolProperty("string", "Branch name, defaults to main")
            ),
            required = listOf("owner", "repo")
        ),
        safetyLevel = SafetyLevel.AUTO
    )

    val GITHUB_READ_FILE = Tool(
        name = "github_read_file",
        description = "Read the full content of a specific file from a GitHub repository. Only call this after you've used github_map_repo to know which file you need.",
        parameters = ToolParameters(
            properties = mapOf(
                "owner" to ToolProperty("string", "GitHub repository owner/org name"),
                "repo" to ToolProperty("string", "Repository name"),
                "path" to ToolProperty("string", "File path within the repository"),
                "branch" to ToolProperty("string", "Branch name, defaults to main")
            ),
            required = listOf("owner", "repo", "path")
        ),
        safetyLevel = SafetyLevel.AUTO
    )

    val GITHUB_CREATE_BRANCH_PR = Tool(
        name = "github_create_branch_pr",
        description = "Create a new branch, commit code changes to it, and open a Pull Request. NEVER push directly to main. Always use this tool when writing code.",
        parameters = ToolParameters(
            properties = mapOf(
                "owner" to ToolProperty("string", "GitHub repository owner/org name"),
                "repo" to ToolProperty("string", "Repository name"),
                "branch_name" to ToolProperty("string", "New branch name, e.g. dettle/fix-jwt-bug"),
                "base_branch" to ToolProperty("string", "Base branch to branch off of and target PR to (defaults to repository default branch)"),
                "file_changes" to ToolProperty("string", "JSON array of {path, content} objects for files to create/update"),
                "commit_message" to ToolProperty("string", "Commit message"),
                "pr_title" to ToolProperty("string", "Pull Request title"),
                "pr_body" to ToolProperty("string", "Pull Request description in Markdown")
            ),
            required = listOf("owner", "repo", "branch_name", "file_changes", "commit_message", "pr_title", "pr_body")
        ),
        requiresApproval = true,
        safetyLevel = SafetyLevel.REQUIRE
    )

    val GITHUB_TRIGGER_ACTION = Tool(
        name = "github_trigger_action",
        description = "Trigger a GitHub Actions workflow manually via workflow_dispatch event.",
        parameters = ToolParameters(
            properties = mapOf(
                "owner" to ToolProperty("string", "GitHub repository owner/org name"),
                "repo" to ToolProperty("string", "Repository name"),
                "workflow_id" to ToolProperty("string", "Workflow file name, e.g. deploy.yml"),
                "ref" to ToolProperty("string", "Branch or tag to run the workflow on"),
                "inputs" to ToolProperty("string", "JSON object of workflow inputs (optional)")
            ),
            required = listOf("owner", "repo", "workflow_id", "ref")
        ),
        requiresApproval = true,
        safetyLevel = SafetyLevel.REQUIRE
    )

    val GITHUB_POLL_RUN = Tool(
        name = "github_poll_run",
        description = "Check the status and conclusion (e.g. success, failure, in_progress) of recent GitHub Actions workflow runs.",
        parameters = ToolParameters(
            properties = mapOf(
                "owner" to ToolProperty("string", "GitHub repository owner/org name"),
                "repo" to ToolProperty("string", "Repository name"),
                "workflow_id" to ToolProperty("string", "Workflow file name, e.g. android-build.yml")
            ),
            required = listOf("owner", "repo", "workflow_id")
        ),
        safetyLevel = SafetyLevel.AUTO
    )

    val GITHUB_LIST_REPOS = Tool(
        name = "github_list_repos",
        description = "List all GitHub repositories belonging to or accessible by the connected GitHub account. Returns repository names, full names, visibility, stars, language, description, and last updated timestamps.",
        parameters = ToolParameters(
            properties = mapOf(
                "force_refresh" to ToolProperty("boolean", "Whether to bypass in-memory cache and fetch fresh list from GitHub")
            ),
            required = emptyList()
        ),
        safetyLevel = SafetyLevel.AUTO
    )

    val GITHUB_GET_REPO = Tool(
        name = "github_get_repo",
        description = "Get detailed information about a GitHub repository, including stars, forks, default branch, language, description, and open issues.",
        parameters = ToolParameters(
            properties = mapOf(
                "owner" to ToolProperty("string", "GitHub repository owner/org name (defaults to active project owner)"),
                "repo" to ToolProperty("string", "Repository name (defaults to active project repo)")
            ),
            required = emptyList()
        ),
        safetyLevel = SafetyLevel.AUTO
    )

    val GITHUB_LIST_BRANCHES = Tool(
        name = "github_list_branches",
        description = "List all branches on a GitHub repository.",
        parameters = ToolParameters(
            properties = mapOf(
                "owner" to ToolProperty("string", "GitHub repository owner/org name (defaults to active project owner)"),
                "repo" to ToolProperty("string", "Repository name (defaults to active project repo)")
            ),
            required = emptyList()
        ),
        safetyLevel = SafetyLevel.AUTO
    )

    val GITHUB_LIST_COMMITS = Tool(
        name = "github_list_commits",
        description = "View recent commit history and commit messages on a repository branch.",
        parameters = ToolParameters(
            properties = mapOf(
                "owner" to ToolProperty("string", "GitHub repository owner/org name (defaults to active project owner)"),
                "repo" to ToolProperty("string", "Repository name (defaults to active project repo)"),
                "branch" to ToolProperty("string", "Branch name, defaults to main"),
                "limit" to ToolProperty("integer", "Max number of commits to return (default 10)")
            ),
            required = emptyList()
        ),
        safetyLevel = SafetyLevel.AUTO
    )

    val GITHUB_LIST_ISSUES = Tool(
        name = "github_list_issues",
        description = "List open or closed issues and pull requests on a GitHub repository.",
        parameters = ToolParameters(
            properties = mapOf(
                "owner" to ToolProperty("string", "GitHub repository owner/org name (defaults to active project owner)"),
                "repo" to ToolProperty("string", "Repository name (defaults to active project repo)"),
                "state" to ToolProperty("string", "Issue state: 'open', 'closed', or 'all' (default 'open')"),
                "limit" to ToolProperty("integer", "Max issues to return (default 10)")
            ),
            required = emptyList()
        ),
        safetyLevel = SafetyLevel.AUTO
    )

    val CODEBASE_SEARCH = Tool(
        name = "codebase_search",
        description = "Semantically and lexically search the indexed codebase for functions, types, schemas, and implementations. Returns exact snippets with file paths and line ranges without wasting tokens on full files.",
        parameters = ToolParameters(
            properties = mapOf(
                "query" to ToolProperty("string", "Search query or symbol name (e.g. 'auth tokens', 'UserRepository', 'deployWorker')"),
                "filter_type" to ToolProperty("string", "Optional symbol filter: 'all', 'function', 'class', 'interface'")
            ),
            required = listOf("query")
        ),
        safetyLevel = SafetyLevel.AUTO
    )

    val CLOUDFLARE_DEPLOY_PREVIEW = Tool(
        name = "cloudflare_deploy_preview",
        description = "Deploy files from local agent workspace (or optional files JSON) directly to Cloudflare Pages as a live preview deployment.",
        parameters = ToolParameters(
            properties = mapOf(
                "project_name" to ToolProperty("string", "Cloudflare Pages project name (defaults to active project)"),
                "files" to ToolProperty("string", "Optional JSON map of relative path to file content. If omitted, deploys workspace files.")
            ),
            required = emptyList()
        ),
        safetyLevel = SafetyLevel.AUTO
    )

    val CLOUDFLARE_PUBLISH_WORKER = Tool(
        name = "cloudflare_publish_worker",
        description = "Deploy or update a Cloudflare Worker script.",
        parameters = ToolParameters(
            properties = mapOf(
                "worker_name" to ToolProperty("string", "Cloudflare Worker name"),
                "script_content" to ToolProperty("string", "JavaScript Worker script content")
            ),
            required = listOf("worker_name", "script_content")
        ),
        requiresApproval = true,
        safetyLevel = SafetyLevel.REQUIRE
    )

    val WEB_SEARCH = Tool(
        name = "web_search",
        description = "Search the web for current information, documentation, error messages, or research. Use when you need up-to-date information beyond your training data.",
        parameters = ToolParameters(
            properties = mapOf(
                "query" to ToolProperty("string", "Search query"),
                "num_results" to ToolProperty("string", "Number of results to return (1-10, default 5)")
            ),
            required = listOf("query")
        ),
        safetyLevel = SafetyLevel.AUTO
    )

    val READ_URL = Tool(
        name = "read_url",
        description = "Fetch and extract readable text content from any URL. Use to read documentation, articles, GitHub READMEs, or error pages.",
        parameters = ToolParameters(
            properties = mapOf(
                "url" to ToolProperty("string", "URL to fetch and read")
            ),
            required = listOf("url")
        ),
        safetyLevel = SafetyLevel.AUTO
    )

    val MEMORY_RECALL = Tool(
        name = "memory_recall",
        description = "Search past tasks, errors, and corrections stored in memory. Use at the start of a task to check if you've encountered this problem before.",
        parameters = ToolParameters(
            properties = mapOf(
                "query" to ToolProperty("string", "What to search for in memory"),
                "repo" to ToolProperty("string", "Optional: filter memories for a specific repository")
            ),
            required = listOf("query")
        ),
        safetyLevel = SafetyLevel.AUTO
    )

    val FINAL_ANSWER = Tool(
        name = "final_answer",
        description = "Call this when you have completed the task. Provide a clear summary of what you did, what changed, and any relevant URLs.",
        parameters = ToolParameters(
            properties = mapOf(
                "summary" to ToolProperty("string", "Summary of what was accomplished"),
                "details" to ToolProperty("string", "Detailed breakdown in Markdown"),
                "links" to ToolProperty("string", "JSON array of relevant URLs (PRs, deployments, docs)")
            ),
            required = listOf("summary")
        ),
        safetyLevel = SafetyLevel.AUTO
    )

    val WORKSPACE_LIST_FILES = Tool(
        name = "workspace_list_files",
        description = "List all files in the local agent workspace. Returns paths relative to the workspace root.",
        parameters = ToolParameters(
            properties = mapOf(
                "dir" to ToolProperty("string", "Optional directory path to list, defaults to root")
            ),
            required = emptyList()
        ),
        safetyLevel = SafetyLevel.AUTO
    )

    val WORKSPACE_READ_FILE = Tool(
        name = "workspace_read_file",
        description = "Read the contents of a file from the local agent workspace.",
        parameters = ToolParameters(
            properties = mapOf(
                "path" to ToolProperty("string", "Path to the file relative to the workspace root")
            ),
            required = listOf("path")
        ),
        safetyLevel = SafetyLevel.AUTO
    )

    val WORKSPACE_WRITE_FILE = Tool(
        name = "workspace_write_file",
        description = "Write or overwrite a file in the local agent workspace.",
        parameters = ToolParameters(
            properties = mapOf(
                "path" to ToolProperty("string", "Path to the file relative to the workspace root"),
                "content" to ToolProperty("string", "The complete text content to write to the file")
            ),
            required = listOf("path", "content")
        ),
        safetyLevel = SafetyLevel.AUTO // Auto because it's a local sandbox
    )

    val WORKSPACE_DELETE_FILE = Tool(
        name = "workspace_delete_file",
        description = "Delete a file from the local agent workspace.",
        parameters = ToolParameters(
            properties = mapOf(
                "path" to ToolProperty("string", "Path to the file to delete")
            ),
            required = listOf("path")
        ),
        safetyLevel = SafetyLevel.AUTO
    )


    val GITHUB_GET_DEFAULT_BRANCH = Tool(
        name = "github_get_default_branch",
        description = "Get the default branch name of a GitHub repository (e.g. 'main' or 'master') to avoid assuming 'main'.",
        parameters = ToolParameters(
            properties = mapOf(
                "owner" to ToolProperty("string", "GitHub repository owner/org name (defaults to active project owner)"),
                "repo" to ToolProperty("string", "Repository name (defaults to active project repo)")
            ),
            required = emptyList()
        ),
        safetyLevel = SafetyLevel.AUTO
    )

    val GITHUB_CREATE_RELEASE = Tool(
        name = "github_create_release",
        description = "Create and publish a new GitHub Release with a Git tag and release notes.",
        parameters = ToolParameters(
            properties = mapOf(
                "owner" to ToolProperty("string", "GitHub repository owner/org name (defaults to active project owner)"),
                "repo" to ToolProperty("string", "Repository name (defaults to active project repo)"),
                "tag_name" to ToolProperty("string", "Git tag name, e.g. v1.12.0"),
                "name" to ToolProperty("string", "Release title, e.g. 'v1.12.0 - Dark Mode Toggle'"),
                "body" to ToolProperty("string", "Release notes description in Markdown"),
                "target_branch" to ToolProperty("string", "Target branch or commit SHA (defaults to default branch)"),
                "draft" to ToolProperty("boolean", "Whether to create as draft (default false)"),
                "prerelease" to ToolProperty("boolean", "Whether this is a prerelease (default false)")
            ),
            required = listOf("tag_name")
        ),
        requiresApproval = true,
        safetyLevel = SafetyLevel.REQUIRE
    )

    val GITHUB_MERGE_PR = Tool(
        name = "github_merge_pr",
        description = "Merge an approved pull request into the base branch once CI tests pass.",
        parameters = ToolParameters(
            properties = mapOf(
                "owner" to ToolProperty("string", "GitHub repository owner/org name (defaults to active project owner)"),
                "repo" to ToolProperty("string", "Repository name (defaults to active project repo)"),
                "pull_number" to ToolProperty("integer", "Pull request number to merge"),
                "commit_title" to ToolProperty("string", "Optional commit title for the merge commit"),
                "commit_message" to ToolProperty("string", "Optional commit message"),
                "merge_method" to ToolProperty("string", "Merge method: 'squash', 'merge', or 'rebase' (default 'squash')")
            ),
            required = listOf("pull_number")
        ),
        requiresApproval = true,
        safetyLevel = SafetyLevel.REQUIRE
    )

    val GITHUB_GET_PR_STATUS = Tool(
        name = "github_get_pr_status",
        description = "Check the status, mergeability, and head branch of an open Pull Request before merging.",
        parameters = ToolParameters(
            properties = mapOf(
                "owner" to ToolProperty("string", "GitHub repository owner/org name (defaults to active project owner)"),
                "repo" to ToolProperty("string", "Repository name (defaults to active project repo)"),
                "pull_number" to ToolProperty("integer", "Pull request number")
            ),
            required = listOf("pull_number")
        ),
        safetyLevel = SafetyLevel.AUTO
    )

    val GITHUB_LIST_WORKFLOW_RUNS = Tool(
        name = "github_list_workflow_runs",
        description = "List recent GitHub Actions workflow runs across the repository, optionally filtered by branch.",
        parameters = ToolParameters(
            properties = mapOf(
                "owner" to ToolProperty("string", "GitHub repository owner/org name (defaults to active project owner)"),
                "repo" to ToolProperty("string", "Repository name (defaults to active project repo)"),
                "branch" to ToolProperty("string", "Optional branch name filter"),
                "limit" to ToolProperty("integer", "Max runs to return (default 10)")
            ),
            required = emptyList()
        ),
        safetyLevel = SafetyLevel.AUTO
    )

    val CLOUDFLARE_GET_DEPLOY_STATUS = Tool(
        name = "cloudflare_get_deploy_status",
        description = "Check the deployment status, stage, and live URL of a Cloudflare Pages project deployment.",
        parameters = ToolParameters(
            properties = mapOf(
                "project_name" to ToolProperty("string", "Cloudflare Pages project name (defaults to active project)"),
                "deployment_id" to ToolProperty("string", "Optional specific deployment ID (defaults to latest deployment)")
            ),
            required = emptyList()
        ),
        safetyLevel = SafetyLevel.AUTO
    )

    val CLOUDFLARE_LIST_PROJECTS = Tool(
        name = "cloudflare_list_projects",
        description = "List all Cloudflare Pages projects and Workers scripts linked to the configured Cloudflare account.",
        parameters = ToolParameters(
            properties = emptyMap(),
            required = emptyList()
        ),
        safetyLevel = SafetyLevel.AUTO
    )

    val CLOUDFLARE_PURGE_CACHE = Tool(
        name = "cloudflare_purge_cache",
        description = "Purge Cloudflare CDN edge cache for a zone after deployment.",
        parameters = ToolParameters(
            properties = mapOf(
                "zone_id" to ToolProperty("string", "Cloudflare Zone ID to purge")
            ),
            required = listOf("zone_id")
        ),
        safetyLevel = SafetyLevel.AUTO
    )

    /** All tools available to agents by default */
    val ALL: List<Tool> = listOf(
        GITHUB_LIST_REPOS,
        GITHUB_GET_REPO,
        GITHUB_GET_DEFAULT_BRANCH,
        GITHUB_MAP_REPO,
        GITHUB_READ_FILE,
        GITHUB_LIST_BRANCHES,
        GITHUB_LIST_COMMITS,
        GITHUB_LIST_ISSUES,
        GITHUB_CREATE_BRANCH_PR,
        GITHUB_TRIGGER_ACTION,
        GITHUB_POLL_RUN,
        GITHUB_LIST_WORKFLOW_RUNS,
        GITHUB_GET_PR_STATUS,
        GITHUB_MERGE_PR,
        GITHUB_CREATE_RELEASE,
        CLOUDFLARE_DEPLOY_PREVIEW,
        CLOUDFLARE_PUBLISH_WORKER,
        CLOUDFLARE_GET_DEPLOY_STATUS,
        CLOUDFLARE_LIST_PROJECTS,
        CLOUDFLARE_PURGE_CACHE,
        WORKSPACE_LIST_FILES,
        WORKSPACE_READ_FILE,
        WORKSPACE_WRITE_FILE,
        WORKSPACE_DELETE_FILE,
        CODEBASE_SEARCH,
        WEB_SEARCH,
        READ_URL,
        MEMORY_RECALL,
        FINAL_ANSWER
    )

    /** Read-only tools (safe for Chat mode & READER agent - 100% read access) */
    val READ_ONLY: List<Tool> = listOf(
        GITHUB_LIST_REPOS,
        GITHUB_GET_REPO,
        GITHUB_GET_DEFAULT_BRANCH,
        GITHUB_MAP_REPO,
        GITHUB_READ_FILE,
        GITHUB_LIST_BRANCHES,
        GITHUB_LIST_COMMITS,
        GITHUB_LIST_ISSUES,
        GITHUB_POLL_RUN,
        GITHUB_LIST_WORKFLOW_RUNS,
        GITHUB_GET_PR_STATUS,
        CLOUDFLARE_GET_DEPLOY_STATUS,
        CLOUDFLARE_LIST_PROJECTS,
        CODEBASE_SEARCH,
        WORKSPACE_LIST_FILES,
        WORKSPACE_READ_FILE,
        MEMORY_RECALL,
        WEB_SEARCH,
        READ_URL,
        FINAL_ANSWER
    )

    /** Tools for the DEPLOYER agent */
    val DEPLOY: List<Tool> = listOf(
        GITHUB_TRIGGER_ACTION,
        GITHUB_POLL_RUN,
        GITHUB_LIST_WORKFLOW_RUNS,
        GITHUB_GET_PR_STATUS,
        GITHUB_MERGE_PR,
        GITHUB_CREATE_RELEASE,
        CLOUDFLARE_DEPLOY_PREVIEW,
        CLOUDFLARE_PUBLISH_WORKER,
        CLOUDFLARE_GET_DEPLOY_STATUS,
        CLOUDFLARE_PURGE_CACHE,
        WORKSPACE_READ_FILE,
        FINAL_ANSWER
    )
}
