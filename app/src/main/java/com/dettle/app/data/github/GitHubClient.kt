package com.dettle.app.data.github

import android.util.Log
import com.dettle.app.data.settings.ApiKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "GitHubClient"
private const val GITHUB_API = "https://api.github.com"
private const val GITHUB_GRAPHQL = "https://api.github.com/graphql"

@Singleton
class GitHubClient @Inject constructor(
    private val keyStore: ApiKeyStore,
    private val client: OkHttpClient,
    private val json: Json
) {
    private val pat get() = keyStore.githubPat

    // ─── Repo Mapping (GraphQL — 1 request for entire tree) ───────────────

    /**
     * Fetches the complete file tree of a repository in a single GraphQL call.
     * Returns a map of path -> file type ("blob" or "tree")
     */
    suspend fun getRepoTree(owner: String, repo: String, branch: String = "main"): Result<List<RepoFile>> =
        withContext(Dispatchers.IO) {
            try {
                val query = """
                    query RepoTree(${'$'}owner: String!, ${'$'}repo: String!, ${'$'}expr: String!) {
                      repository(owner: ${'$'}owner, name: ${'$'}repo) {
                        defaultBranchRef { name }
                        object(expression: ${'$'}expr) {
                          ... on Tree {
                            entries {
                              name
                              path
                              type
                              object {
                                ... on Blob {
                                  byteSize
                                  isBinary
                                }
                                ... on Tree {
                                  entries { name path type }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                """.trimIndent()

                val variables = buildJsonObject {
                    put("owner", owner)
                    put("repo", repo)
                    put("expr", "$branch:")
                }

                val response = graphql(query, variables)
                val entries = response["data"]?.jsonObject
                    ?.get("repository")?.jsonObject
                    ?.get("object")?.jsonObject
                    ?.get("entries")?.jsonArray

                val files = entries?.flatMap { entry ->
                    parseEntries(entry.jsonObject, "")
                } ?: emptyList()

                Result.success(files)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch repo tree", e)
                Result.failure(e)
            }
        }

    private fun parseEntries(entry: JsonObject, parentPath: String): List<RepoFile> {
        val name = entry["name"]?.jsonPrimitive?.content ?: return emptyList()
        val path = entry["path"]?.jsonPrimitive?.content ?: "$parentPath/$name"
        val type = entry["type"]?.jsonPrimitive?.content ?: "blob"
        val sizeObj = entry["object"]?.jsonObject

        return if (type == "blob") {
            val size = sizeObj?.get("byteSize")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val isBinary = sizeObj?.get("isBinary")?.jsonPrimitive?.content?.toBoolean() ?: false
            listOf(RepoFile(path, name, FileType.FILE, size, isBinary))
        } else {
            val subEntries = sizeObj?.get("entries")?.jsonArray
                ?.flatMap { parseEntries(it.jsonObject, path) } ?: emptyList()
            listOf(RepoFile(path, name, FileType.DIRECTORY, 0, false)) + subEntries
        }
    }

    // ─── Read File (REST) ─────────────────────────────────────────────────

    /**
     * Reads file content from GitHub. Returns decoded text content.
     * Handles base64 decoding of GitHub's API response.
     */
    suspend fun readFile(owner: String, repo: String, path: String, branch: String = "main"): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val url = "$GITHUB_API/repos/$owner/$repo/contents/$path?ref=$branch"
                val response = rest("GET", url)

                val content = response["content"]?.jsonPrimitive?.content
                    ?: return@withContext Result.failure(Exception("No content in response"))

                // GitHub returns base64-encoded content with newlines
                val decoded = Base64.getDecoder().decode(content.replace("\n", ""))
                Result.success(String(decoded))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read file $path", e)
                Result.failure(e)
            }
        }

    // ─── Create Branch + Commit + PR ─────────────────────────────────────

    /** Convenience wrapper — throws on failure instead of returning Result. Used by ReposViewModel. */
    suspend fun getRepoFileTree(owner: String, repo: String): List<RepoFile> =
        getRepoTree(owner, repo).getOrThrow()

    /** Convenience wrapper — throws on failure. Used by ReposViewModel. */
    suspend fun readFile(owner: String, repo: String, path: String): String =
        readFile(owner, repo, path, "main").getOrThrow()

    /**
     * Creates a new branch, commits the provided file changes, and opens a Pull Request.
     * The AI agent ALWAYS writes code through this method — never directly to main.
     */
    suspend fun createBranchAndPR(
        owner: String,
        repo: String,
        branchName: String,
        fileChanges: List<FileChange>,
        commitMessage: String,
        prTitle: String,
        prBody: String,
        baseBranch: String = "main"
    ): Result<PullRequest> = withContext(Dispatchers.IO) {
        try {
            // 1. Get the SHA of the base branch's HEAD
            val baseSha = getRefSha(owner, repo, "heads/$baseBranch")
                ?: return@withContext Result.failure(Exception("Cannot get SHA of $baseBranch"))

            // 2. Create the new branch
            val createBranchBody = buildJsonObject {
                put("ref", "refs/heads/$branchName")
                put("sha", baseSha)
            }.toString()
            rest("POST", "$GITHUB_API/repos/$owner/$repo/git/refs",
                body = createBranchBody)

            // 3. Commit each file change
            for (change in fileChanges) {
                val existingSha = getFileSha(owner, repo, change.path, branchName)
                val encodedContent = Base64.getEncoder().encodeToString(change.content.toByteArray())

                val commitBody = buildJsonObject {
                    put("message", commitMessage)
                    put("content", encodedContent)
                    put("branch", branchName)
                    if (existingSha != null) put("sha", existingSha)
                }.toString()

                rest("PUT", "$GITHUB_API/repos/$owner/$repo/contents/${change.path}",
                    body = commitBody)
            }

            // 4. Open the Pull Request
            val prBody2 = buildJsonObject {
                put("title", prTitle)
                put("body", "*Created by Dettle AI Agent*\n\n$prBody")
                put("head", branchName)
                put("base", baseBranch)
                put("draft", false)
            }.toString()

            val prResponse = rest("POST", "$GITHUB_API/repos/$owner/$repo/pulls",
                body = prBody2)

            val prNumber = prResponse["number"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val prUrl = prResponse["html_url"]?.jsonPrimitive?.content ?: ""

            Result.success(PullRequest(
                number = prNumber,
                title = prTitle,
                url = prUrl,
                branch = branchName,
                filesChanged = fileChanges.size
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create branch/PR", e)
            Result.failure(e)
        }
    }

    // ─── Trigger GitHub Action ────────────────────────────────────────────

    suspend fun triggerWorkflow(
        owner: String,
        repo: String,
        workflowId: String,
        ref: String,
        inputs: Map<String, String> = emptyMap()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val body = buildJsonObject {
                put("ref", ref)
                if (inputs.isNotEmpty()) {
                    put("inputs", buildJsonObject {
                        inputs.forEach { (k, v) -> put(k, v) }
                    })
                }
            }.toString()
            rest("POST", "$GITHUB_API/repos/$owner/$repo/actions/workflows/$workflowId/dispatches",
                body = body)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches recent workflow runs for a specific workflow file.
     */
    suspend fun getWorkflowRuns(
        owner: String,
        repo: String,
        workflowId: String
    ): Result<List<WorkflowRun>> = withContext(Dispatchers.IO) {
        try {
            val response = rest("GET", "$GITHUB_API/repos/$owner/$repo/actions/workflows/$workflowId/runs?per_page=5")
            val runsArray = response["workflow_runs"]?.let {
                runCatching { it.jsonArray }.getOrNull()
            } ?: emptyList()

            val runs = runsArray.mapNotNull { item ->
                val obj = runCatching { item.jsonObject }.getOrNull() ?: return@mapNotNull null
                val id = obj["id"]?.jsonPrimitive?.content?.toLongOrNull() ?: return@mapNotNull null
                val status = obj["status"]?.jsonPrimitive?.content ?: "unknown"
                val conclusion = obj["conclusion"]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() }
                val url = obj["html_url"]?.jsonPrimitive?.content ?: ""
                WorkflowRun(id, status, conclusion, url)
            }
            Result.success(runs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Cache & Authenticated User Info ───────────────────────────────────

    private var cachedUser: GitHubUser? = null
    private var cachedRepos: List<GitHubRepoSummary> = emptyList()
    private var cacheTimestampMs: Long = 0L
    private val CACHE_TTL_MS = 60_000L // 60 seconds

    /**
     * Fetches details of the authenticated GitHub user.
     * Failsafe: Returns cached user if network fails.
     */
    suspend fun getAuthenticatedUser(forceRefresh: Boolean = false): Result<GitHubUser> = withContext(Dispatchers.IO) {
        val token = pat
        if (token.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("GitHub Access Token is not configured. Please add your GitHub Personal Access Token in Settings or Onboarding."))
        }

        if (!forceRefresh && cachedUser != null && System.currentTimeMillis() - cacheTimestampMs < CACHE_TTL_MS) {
            return@withContext Result.success(cachedUser!!)
        }

        try {
            val response = rest("GET", "$GITHUB_API/user")
            val login = response["login"]?.jsonPrimitive?.content ?: throw Exception("GitHub API response missing login")
            val name = response["name"]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() }
            val avatarUrl = response["avatar_url"]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() }
            val bio = response["bio"]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() }
            val publicRepos = response["public_repos"]?.let { runCatching { it.jsonPrimitive.content.toInt() }.getOrNull() } ?: 0
            val totalPrivateRepos = response["total_private_repos"]?.let { runCatching { it.jsonPrimitive.content.toInt() }.getOrNull() }

            val user = GitHubUser(
                login = login,
                name = name,
                avatarUrl = avatarUrl,
                bio = bio,
                publicRepos = publicRepos,
                totalPrivateRepos = totalPrivateRepos
            )
            cachedUser = user
            cacheTimestampMs = System.currentTimeMillis()

            if (keyStore.githubOwner.isNullOrBlank()) {
                keyStore.githubOwner = login
            }

            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch authenticated user", e)
            cachedUser?.let { return@withContext Result.success(it) }
            Result.failure(e)
        }
    }

    /**
     * Lists repositories belonging to or accessible by the authenticated user.
     * Sorted by recently updated, up to 100 repositories.
     * Failsafe: Falls back to cached repository list if network or rate limit fails.
     */
    suspend fun getUserRepositories(forceRefresh: Boolean = false): Result<List<GitHubRepoSummary>> = withContext(Dispatchers.IO) {
        val token = pat
        if (token.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("GitHub Access Token is not configured. Please add your GitHub Personal Access Token in Settings or Onboarding."))
        }

        if (!forceRefresh && cachedRepos.isNotEmpty() && System.currentTimeMillis() - cacheTimestampMs < CACHE_TTL_MS) {
            return@withContext Result.success(cachedRepos)
        }

        try {
            val array = restArray("GET", "$GITHUB_API/user/repos?sort=updated&per_page=100&affiliation=owner,collaborator,organization_member")
            val repos = array.mapNotNull { element ->
                val obj = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
                val name = obj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val fullName = obj["full_name"]?.jsonPrimitive?.content ?: name
                val ownerObj = obj["owner"]?.let { runCatching { it.jsonObject }.getOrNull() }
                val owner = ownerObj?.get("login")?.jsonPrimitive?.content ?: fullName.substringBefore('/', "")
                val description = obj["description"]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() }
                val isPrivate = obj["private"]?.let { runCatching { it.jsonPrimitive.content.toBoolean() }.getOrNull() } ?: false
                val defaultBranch = obj["default_branch"]?.jsonPrimitive?.content ?: "main"
                val language = obj["language"]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() }
                val stars = obj["stargazers_count"]?.let { runCatching { it.jsonPrimitive.content.toInt() }.getOrNull() } ?: 0
                val updatedAt = obj["updated_at"]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() }

                GitHubRepoSummary(
                    name = name,
                    fullName = fullName,
                    owner = owner,
                    description = description,
                    isPrivate = isPrivate,
                    defaultBranch = defaultBranch,
                    language = language,
                    stars = stars,
                    updatedAt = updatedAt
                )
            }
            cachedRepos = repos
            cacheTimestampMs = System.currentTimeMillis()
            Result.success(repos)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch repositories", e)
            if (cachedRepos.isNotEmpty()) {
                return@withContext Result.success(cachedRepos)
            }
            Result.failure(e)
        }
    }

    /**
     * Get detailed information for a specific repository.
     */
    suspend fun getRepoDetails(owner: String, repo: String): Result<GitHubRepoDetails> = withContext(Dispatchers.IO) {
        try {
            val obj = rest("GET", "$GITHUB_API/repos/$owner/$repo")
            val name = obj["name"]?.jsonPrimitive?.content ?: repo
            val fullName = obj["full_name"]?.jsonPrimitive?.content ?: "$owner/$repo"
            val description = obj["description"]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() }
            val isPrivate = obj["private"]?.let { runCatching { it.jsonPrimitive.content.toBoolean() }.getOrNull() } ?: false
            val defaultBranch = obj["default_branch"]?.jsonPrimitive?.content ?: "main"
            val language = obj["language"]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() }
            val stars = obj["stargazers_count"]?.let { runCatching { it.jsonPrimitive.content.toInt() }.getOrNull() } ?: 0
            val forks = obj["forks_count"]?.let { runCatching { it.jsonPrimitive.content.toInt() }.getOrNull() } ?: 0
            val openIssues = obj["open_issues_count"]?.let { runCatching { it.jsonPrimitive.content.toInt() }.getOrNull() } ?: 0
            val cloneUrl = obj["clone_url"]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() }
            val htmlUrl = obj["html_url"]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() }

            Result.success(
                GitHubRepoDetails(
                    name = name,
                    fullName = fullName,
                    owner = owner,
                    description = description,
                    isPrivate = isPrivate,
                    defaultBranch = defaultBranch,
                    language = language,
                    stars = stars,
                    forks = forks,
                    openIssuesCount = openIssues,
                    cloneUrl = cloneUrl,
                    htmlUrl = htmlUrl
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get repo details for $owner/$repo", e)
            Result.failure(e)
        }
    }

    /**
     * List all branches in a repository.
     */
    suspend fun listRepoBranches(owner: String, repo: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val array = restArray("GET", "$GITHUB_API/repos/$owner/$repo/branches?per_page=30")
            val branches = array.mapNotNull { el ->
                runCatching { el.jsonObject["name"]?.jsonPrimitive?.content }.getOrNull()
            }
            Result.success(branches)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list branches for $owner/$repo", e)
            Result.failure(e)
        }
    }

    /**
     * List recent commits on a repository branch.
     */
    suspend fun listRepoCommits(owner: String, repo: String, branch: String = "main", limit: Int = 10): Result<List<GitHubCommitSummary>> = withContext(Dispatchers.IO) {
        try {
            val array = restArray("GET", "$GITHUB_API/repos/$owner/$repo/commits?sha=$branch&per_page=${limit.coerceIn(1, 30)}")
            val commits = array.mapNotNull { el ->
                val obj = runCatching { el.jsonObject }.getOrNull() ?: return@mapNotNull null
                val sha = obj["sha"]?.jsonPrimitive?.content?.take(7) ?: ""
                val commitObj = obj["commit"]?.let { runCatching { it.jsonObject }.getOrNull() }
                val message = commitObj?.get("message")?.jsonPrimitive?.content?.lines()?.firstOrNull() ?: ""
                val authorObj = commitObj?.get("author")?.let { runCatching { it.jsonObject }.getOrNull() }
                val author = authorObj?.get("name")?.jsonPrimitive?.content ?: "unknown"
                val date = authorObj?.get("date")?.jsonPrimitive?.content ?: ""

                GitHubCommitSummary(sha = sha, message = message, author = author, date = date)
            }
            Result.success(commits)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list commits for $owner/$repo", e)
            Result.failure(e)
        }
    }

    /**
     * List open issues and pull requests on a repository.
     */
    suspend fun listRepoIssues(owner: String, repo: String, state: String = "open", limit: Int = 10): Result<List<GitHubIssueSummary>> = withContext(Dispatchers.IO) {
        try {
            val array = restArray("GET", "$GITHUB_API/repos/$owner/$repo/issues?state=$state&per_page=${limit.coerceIn(1, 30)}")
            val issues = array.mapNotNull { el ->
                val obj = runCatching { el.jsonObject }.getOrNull() ?: return@mapNotNull null
                val number = obj["number"]?.jsonPrimitive?.content?.toIntOrNull() ?: return@mapNotNull null
                val title = obj["title"]?.jsonPrimitive?.content ?: ""
                val issueState = obj["state"]?.jsonPrimitive?.content ?: state
                val userObj = obj["user"]?.let { runCatching { it.jsonObject }.getOrNull() }
                val author = userObj?.get("login")?.jsonPrimitive?.content ?: "unknown"
                val isPR = obj.containsKey("pull_request")
                val url = obj["html_url"]?.jsonPrimitive?.content ?: ""

                GitHubIssueSummary(
                    number = number,
                    title = title,
                    state = issueState,
                    author = author,
                    isPullRequest = isPR,
                    url = url
                )
            }
            Result.success(issues)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list issues for $owner/$repo", e)
            Result.failure(e)
        }
    }

    // ─── Internal Helpers ─────────────────────────────────────────────────

    private suspend fun graphql(query: String, variables: JsonObject): JsonObject {
        val body = buildJsonObject {
            put("query", query)
            put("variables", variables)
        }.toString()

        val token = pat
        if (token.isNullOrBlank()) {
            throw IllegalStateException("GitHub Personal Access Token is not configured.")
        }

        val request = Request.Builder()
            .url(GITHUB_GRAPHQL)
            .post(body.toRequestBody("application/json".toMediaType()))
            .header("Authorization", "Bearer $token")
            .header("User-Agent", "Dettle-App/1.0")
            .header("Accept", "application/vnd.github+json")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("GitHub GraphQL error ${response.code}: ${response.body?.string()}")
        }
        return json.parseToJsonElement(response.body?.string() ?: "{}").jsonObject
    }

    private suspend fun rest(method: String, url: String, body: String? = null): JsonObject {
        val requestBody = body?.toRequestBody("application/json".toMediaType())

        val token = pat
        if (token.isNullOrBlank()) {
            throw IllegalStateException("GitHub Personal Access Token is not configured. Please add it in Settings or Onboarding.")
        }

        val request = Request.Builder()
            .url(url)
            .method(method, requestBody)
            .header("Authorization", "Bearer $token")
            .header("User-Agent", "Dettle-App/1.0")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful && response.code != 201) {
            val errBody = response.body?.string().orEmpty()
            if (response.code == 401) {
                throw IllegalStateException("GitHub PAT is invalid or expired (401 Unauthorized)")
            } else if (response.code == 403) {
                throw IllegalStateException("GitHub API rate limit exceeded or missing scopes (403 Forbidden)")
            }
            throw Exception("GitHub REST error ${response.code}: $errBody")
        }
        return json.parseToJsonElement(response.body?.string() ?: "{}").jsonObject
    }

    private suspend fun restArray(method: String, url: String, body: String? = null): JsonArray {
        val requestBody = body?.toRequestBody("application/json".toMediaType())

        val token = pat
        if (token.isNullOrBlank()) {
            throw IllegalStateException("GitHub Personal Access Token is not configured. Please add it in Settings or Onboarding.")
        }

        val request = Request.Builder()
            .url(url)
            .method(method, requestBody)
            .header("Authorization", "Bearer $token")
            .header("User-Agent", "Dettle-App/1.0")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful && response.code != 201) {
            val errBody = response.body?.string().orEmpty()
            if (response.code == 401) {
                throw IllegalStateException("GitHub PAT is invalid or expired (401 Unauthorized)")
            } else if (response.code == 403) {
                throw IllegalStateException("GitHub API rate limit exceeded or missing scopes (403 Forbidden)")
            }
            throw Exception("GitHub REST error ${response.code}: $errBody")
        }
        val raw = response.body?.string() ?: "[]"
        return json.parseToJsonElement(raw).jsonArray
    }

    private suspend fun getRefSha(owner: String, repo: String, ref: String): String? {
        return try {
            val response = rest("GET", "$GITHUB_API/repos/$owner/$repo/git/refs/$ref")
            response["object"]?.jsonObject?.get("sha")?.jsonPrimitive?.content
        } catch (_: Exception) { null }
    }

    private suspend fun getFileSha(owner: String, repo: String, path: String, branch: String): String? {
        return try {
            val response = rest("GET", "$GITHUB_API/repos/$owner/$repo/contents/$path?ref=$branch")
            response["sha"]?.jsonPrimitive?.content
        } catch (_: Exception) { null }
    }
}

// ─── Data classes ──────────────────────────────────────────────────────────

data class RepoFile(
    val path: String,
    val name: String,
    val type: FileType,
    val size: Int,
    val isBinary: Boolean
) {
    val extension: String get() = name.substringAfterLast('.', "")
    val isCode: Boolean get() = !isBinary && extension in CODE_EXTENSIONS

    companion object {
        val CODE_EXTENSIONS = setOf(
            "kt", "java", "py", "js", "ts", "jsx", "tsx", "go", "rs", "c", "cpp", "h",
            "cs", "rb", "php", "swift", "dart", "html", "css", "scss", "json", "yaml",
            "yml", "toml", "gradle", "xml", "md", "sh", "sql"
        )
    }
}

enum class FileType { FILE, DIRECTORY }

data class FileChange(
    val path: String,
    val content: String
)

data class PullRequest(
    val number: Int,
    val title: String,
    val url: String,
    val branch: String,
    val filesChanged: Int
)

data class WorkflowRun(
    val id: Long,
    val status: String,
    val conclusion: String?,
    val url: String
)

data class GitHubUser(
    val login: String,
    val name: String?,
    val avatarUrl: String?,
    val bio: String?,
    val publicRepos: Int,
    val totalPrivateRepos: Int?
)

data class GitHubRepoSummary(
    val name: String,
    val fullName: String,
    val owner: String,
    val description: String?,
    val isPrivate: Boolean,
    val defaultBranch: String,
    val language: String?,
    val stars: Int,
    val updatedAt: String?
)

data class GitHubRepoDetails(
    val name: String,
    val fullName: String,
    val owner: String,
    val description: String?,
    val isPrivate: Boolean,
    val defaultBranch: String,
    val language: String?,
    val stars: Int,
    val forks: Int,
    val openIssuesCount: Int,
    val cloneUrl: String?,
    val htmlUrl: String?
)

data class GitHubCommitSummary(
    val sha: String,
    val message: String,
    val author: String,
    val date: String
)

data class GitHubIssueSummary(
    val number: Int,
    val title: String,
    val state: String,
    val author: String,
    val isPullRequest: Boolean,
    val url: String
)
