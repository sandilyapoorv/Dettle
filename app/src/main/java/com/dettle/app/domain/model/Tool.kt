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

    val CLOUDFLARE_DEPLOY_PREVIEW = Tool(
        name = "cloudflare_deploy_preview",
        description = "Trigger a Cloudflare Pages preview deployment for a specific branch or PR. Use this to verify your code changes deploy successfully before merging.",
        parameters = ToolParameters(
            properties = mapOf(
                "project_name" to ToolProperty("string", "Cloudflare Pages project name"),
                "branch" to ToolProperty("string", "Branch to deploy a preview for")
            ),
            required = listOf("project_name", "branch")
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


    /** All tools available to agents by default */
    val ALL: List<Tool> = listOf(
        GITHUB_MAP_REPO,
        GITHUB_READ_FILE,
        GITHUB_CREATE_BRANCH_PR,
        GITHUB_TRIGGER_ACTION,
        CLOUDFLARE_DEPLOY_PREVIEW,
        CLOUDFLARE_PUBLISH_WORKER,
        WORKSPACE_LIST_FILES,
        WORKSPACE_READ_FILE,
        WORKSPACE_WRITE_FILE,
        WORKSPACE_DELETE_FILE,
        WEB_SEARCH,
        READ_URL,
        MEMORY_RECALL,
        FINAL_ANSWER
    )

    /** Read-only tools (safe for READER agent) */
    val READ_ONLY: List<Tool> = listOf(
        GITHUB_MAP_REPO, GITHUB_READ_FILE, WORKSPACE_LIST_FILES, WORKSPACE_READ_FILE, MEMORY_RECALL, WEB_SEARCH, READ_URL, FINAL_ANSWER
    )

    /** Tools for the DEPLOYER agent */
    val DEPLOY: List<Tool> = listOf(
        GITHUB_TRIGGER_ACTION, CLOUDFLARE_DEPLOY_PREVIEW, CLOUDFLARE_PUBLISH_WORKER, WORKSPACE_READ_FILE, FINAL_ANSWER
    )
}
