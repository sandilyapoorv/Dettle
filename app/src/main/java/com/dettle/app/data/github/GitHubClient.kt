package com.dettle.app.data.github

import android.util.Log
import com.dettle.app.data.settings.ApiKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
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
     * Returns a list of RepoFile (path, name, type, size, isBinary).
     * Defensively handles JsonNull, non-existent branches, and auto-falls back to repo default branch.
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
                val repoObj = (response["data"] as? JsonObject)?.get("repository") as? JsonObject
                val objectTree = repoObj?.get("object") as? JsonObject

                // If branch object is null, attempt fallback to the repo's actual default branch if different
                if (objectTree == null) {
                    val actualDefaultBranch = (repoObj?.get("defaultBranchRef") as? JsonObject)
                        ?.get("name")?.let { (it as? JsonPrimitive)?.content }
                    if (!actualDefaultBranch.isNullOrBlank() && actualDefaultBranch != branch) {
                        Log.d(TAG, "Branch '$branch' has no tree; retrying with repo default branch '$actualDefaultBranch'")
                        return@withContext getRepoTree(owner, repo, actualDefaultBranch)
                    }
                }

                val entries = objectTree?.get("entries") as? JsonArray
                val files = entries?.flatMap { entry ->
                    (entry as? JsonObject)?.let { parseEntries(it, "") } ?: emptyList()
                } ?: emptyList()

                Result.success(files)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch repo tree", e)
                Result.failure(e)
            }
        }

    private fun parseEntries(entry: JsonObject, parentPath: String): List<RepoFile> {
        val name = (entry["name"] as? JsonPrimitive)?.content ?: return emptyList()
        val path = (entry["path"] as? JsonPrimitive)?.content ?: if (parentPath.isEmpty()) name else "$parentPath/$name"
        val type = (entry["type"] as? JsonPrimitive)?.content ?: "blob"
        val sizeObj = entry["object"] as? JsonObject

        return if (type == "blob") {
            val size = (sizeObj?.get("byteSize") as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
            val isBinary = (sizeObj?.get("isBinary") as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false
            listOf(RepoFile(path, name, FileType.FILE, size, isBinary))
        } else {
            val subEntries = (sizeObj?.get("entries") as? JsonArray)
                ?.flatMap { (it as? JsonObject)?.let { sub -> parseEntries(sub, path) } ?: emptyList() }
                ?: emptyList()
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

                val content = (response["content"] as? JsonPrimitive)?.content
                    ?: return@withContext Result.failure(Exception("No content in response for $path"))

                // GitHub returns base64-encoded content with newlines
                val cleanContent = content.replace("\n", "").replace("\r", "")
                val decoded = Base64.getDecoder().decode(cleanContent)
                Result.success(String(decoded, Charsets.UTF_8))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read file $path", e)
                Result.failure(e)
            }
        }

    // ─── Convenience Wrappers ─────────────────────────────────────────────

    /** Convenience wrapper — throws on failure instead of returning Result. Used by ReposViewModel. */
    suspend fun getRepoFileTree(owner: String, repo: String): List<RepoFile> =
        getRepoTree(owner, repo).getOrThrow()

    /** Convenience wrapper — throws on failure. Used by ReposViewModel. */
    suspend fun readFile(owner: String, repo: String, path: String): String =
        readFile(owner, repo, path, "main").getOrThrow()

    /**
     * Gets default branch of repository (e.g. "main", "master").
     */
    suspend fun getDefaultBranch(owner: String, repo: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val repoObj = rest("GET", "$GITHUB_API/repos/$owner/$repo")
                val branch = (repoObj["default_branch"] as? JsonPrimitive)?.content ?: "main"
                Result.success(branch)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch default branch for $owner/$repo", e)
                Result.failure(e)
            }
        }

    // ─── Create Branch + Commit + PR ─────────────────────────────────────

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
            rest("POST", "$GITHUB_API/repos/$owner/$repo/git/refs", body = createBranchBody)

            // 3. Commit each file change
            for (change in fileChanges) {
                val existingSha = getFileSha(owner, repo, change.path, branchName)
                val encodedContent = Base64.getEncoder().encodeToString(change.content.toByteArray(Charsets.UTF_8))

                val commitBody = buildJsonObject {
                    put("message", commitMessage)
                    put("content", encodedContent)
                    put("branch", branchName)
                    if (existingSha != null) put("sha", existingSha)
                }.toString()

                rest("PUT", "$GITHUB_API/repos/$owner/$repo/contents/${change.path}", body = commitBody)
            }

            // 4. Open the Pull Request
            val prBody2 = buildJsonObject {
                put("title", prTitle)
                put("body", "*Created by Dettle AI Agent*\n\n$prBody")
                put("head", branchName)
                put("base", baseBranch)
                put("draft", false)
            }.toString()

            val prResponse = rest("POST", "$GITHUB_API/repos/$owner/$repo/pulls", body = prBody2)

            val prNumber = (prResponse["number"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
            val prUrl = (prResponse["html_url"] as? JsonPrimitive)?.content ?: ""

            Result.success(
                PullRequest(
                    number = prNumber,
                    title = prTitle,
                    url = prUrl,
                    branch = branchName,
                    filesChanged = fileChanges.size
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create branch/PR", e)
            Result.failure(e)
        }
    }

    // ─── Merge Pull Request ──────────────────────────────────────────────

    /**
     * Merges a Pull Request once CI and review checks succeed.
     */
    suspend fun mergePullRequest(
        owner: String,
        repo: String,
        pullNumber: Int,
        commitTitle: String? = null,
        commitMessage: String? = null,
        mergeMethod: String = "squash"
    ): Result<MergeResult> = withContext(Dispatchers.IO) {
        try {
            val payload = buildJsonObject {
                if (!commitTitle.isNullOrBlank()) put("commit_title", commitTitle)
                if (!commitMessage.isNullOrBlank()) put("commit_message", commitMessage)
                put("merge_method", mergeMethod)
            }.toString()

            val response = rest("PUT", "$GITHUB_API/repos/$owner/$repo/pulls/$pullNumber/merge", body = payload)
            val sha = (response["sha"] as? JsonPrimitive)?.content ?: ""
            val merged = (response["merged"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: true
            val message = (response["message"] as? JsonPrimitive)?.content ?: "PR #$pullNumber merged"

            Result.success(MergeResult(sha = sha, merged = merged, message = message))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to merge PR #$pullNumber for $owner/$repo", e)
            Result.failure(e)
        }
    }

    // ─── Get Pull Request Status ──────────────────────────────────────────

    /**
     * Gets PR details including mergeability, state, and head commit.
     */
    suspend fun getPullRequest(
        owner: String,
        repo: String,
        pullNumber: Int
    ): Result<PullRequestStatus> = withContext(Dispatchers.IO) {
        try {
            val response = rest("GET", "$GITHUB_API/repos/$owner/$repo/pulls/$pullNumber")
            val number = (response["number"] as? JsonPrimitive)?.content?.toIntOrNull() ?: pullNumber
            val state = (response["state"] as? JsonPrimitive)?.content ?: "unknown"
            val merged = (response["merged"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false
            val mergeable = (response["mergeable"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull()
            val mergeableState = (response["mergeable_state"] as? JsonPrimitive)?.content
            val headObj = response["head"] as? JsonObject
            val headSha = (headObj?.get("sha") as? JsonPrimitive)?.content ?: ""
            val headRef = (headObj?.get("ref") as? JsonPrimitive)?.content ?: ""
            val htmlUrl = (response["html_url"] as? JsonPrimitive)?.content ?: ""

            Result.success(
                PullRequestStatus(
                    number = number,
                    state = state,
                    merged = merged,
                    mergeable = mergeable,
                    mergeableState = mergeableState,
                    headSha = headSha,
                    headRef = headRef,
                    htmlUrl = htmlUrl
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get PR #$pullNumber status for $owner/$repo", e)
            Result.failure(e)
        }
    }

    // ─── Create GitHub Release ────────────────────────────────────────────

    /**
     * Tags and creates a GitHub Release.
     */
    suspend fun createRelease(
        owner: String,
        repo: String,
        tagName: String,
        name: String,
        body: String,
        targetCommitish: String = "main",
        draft: Boolean = false,
        prerelease: Boolean = false
    ): Result<GitHubRelease> = withContext(Dispatchers.IO) {
        try {
            val payload = buildJsonObject {
                put("tag_name", tagName)
                put("target_commitish", targetCommitish)
                put("name", name)
                put("body", body)
                put("draft", draft)
                put("prerelease", prerelease)
            }.toString()

            val response = rest("POST", "$GITHUB_API/repos/$owner/$repo/releases", body = payload)
            val id = (response["id"] as? JsonPrimitive)?.content?.toLongOrNull() ?: 0L
            val htmlUrl = (response["html_url"] as? JsonPrimitive)?.content ?: ""
            val returnedTag = (response["tag_name"] as? JsonPrimitive)?.content ?: tagName
            val releaseName = (response["name"] as? JsonPrimitive)?.content ?: name
            val releaseBody = (response["body"] as? JsonPrimitive)?.content ?: body

            Result.success(
                GitHubRelease(
                    id = id,
                    tagName = returnedTag,
                    name = releaseName,
                    htmlUrl = htmlUrl,
                    body = releaseBody
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create release $tagName for $owner/$repo", e)
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
            rest("POST", "$GITHUB_API/repos/$owner/$repo/actions/workflows/$workflowId/dispatches", body = body)
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
            val runsArray = (response["workflow_runs"] as? JsonArray) ?: JsonArray(emptyList())

            val runs = runsArray.mapNotNull { item ->
                val obj = item as? JsonObject ?: return@mapNotNull null
                val id = (obj["id"] as? JsonPrimitive)?.content?.toLongOrNull() ?: return@mapNotNull null
                val status = (obj["status"] as? JsonPrimitive)?.content ?: "unknown"
                val conclusion = (obj["conclusion"] as? JsonPrimitive)?.content
                val url = (obj["html_url"] as? JsonPrimitive)?.content ?: ""
                WorkflowRun(id, status, conclusion, url)
            }
            Result.success(runs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lists recent workflow runs across the entire repository, optionally filtered by branch.
     */
    suspend fun listWorkflowRuns(
        owner: String,
        repo: String,
        branch: String? = null,
        limit: Int = 10
    ): Result<List<WorkflowRun>> = withContext(Dispatchers.IO) {
        try {
            val queryParams = buildString {
                append("?per_page=").append(limit.coerceIn(1, 30))
                if (!branch.isNullOrBlank()) {
                    append("&branch=").append(branch)
                }
            }
            val response = rest("GET", "$GITHUB_API/repos/$owner/$repo/actions/runs$queryParams")
            val runsArray = (response["workflow_runs"] as? JsonArray) ?: JsonArray(emptyList())
            val runs = runsArray.mapNotNull { item ->
                val obj = item as? JsonObject ?: return@mapNotNull null
                val id = (obj["id"] as? JsonPrimitive)?.content?.toLongOrNull() ?: return@mapNotNull null
                val status = (obj["status"] as? JsonPrimitive)?.content ?: "unknown"
                val conclusion = (obj["conclusion"] as? JsonPrimitive)?.content
                val url = (obj["html_url"] as? JsonPrimitive)?.content ?: ""
                WorkflowRun(id, status, conclusion, url)
            }
            Result.success(runs)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list workflow runs for $owner/$repo", e)
            Result.failure(e)
        }
    }

    // ─── Cache & Authenticated User Info ───────────────────────────────────

    private var cachedUser: GitHubUser? = null
    private var cachedRepos: List<GitHubRepoSummary> = emptyList()
    private var cacheTimestampMs: Long = 0L
    private val CACHE_TTL_MS = 60_000L // 60 seconds

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
            val login = (response["login"] as? JsonPrimitive)?.content ?: throw Exception("GitHub API response missing login")
            val name = (response["name"] as? JsonPrimitive)?.content
            val avatarUrl = (response["avatar_url"] as? JsonPrimitive)?.content
            val bio = (response["bio"] as? JsonPrimitive)?.content
            val publicRepos = (response["public_repos"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
            val totalPrivateRepos = (response["total_private_repos"] as? JsonPrimitive)?.content?.toIntOrNull()

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
                val obj = element as? JsonObject ?: return@mapNotNull null
                val name = (obj["name"] as? JsonPrimitive)?.content ?: return@mapNotNull null
                val fullName = (obj["full_name"] as? JsonPrimitive)?.content ?: name
                val ownerObj = obj["owner"] as? JsonObject
                val owner = (ownerObj?.get("login") as? JsonPrimitive)?.content ?: fullName.substringBefore('/', "")
                val description = (obj["description"] as? JsonPrimitive)?.content
                val isPrivate = (obj["private"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false
                val defaultBranch = (obj["default_branch"] as? JsonPrimitive)?.content ?: "main"
                val language = (obj["language"] as? JsonPrimitive)?.content
                val stars = (obj["stargazers_count"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
                val updatedAt = (obj["updated_at"] as? JsonPrimitive)?.content

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

    suspend fun getRepoDetails(owner: String, repo: String): Result<GitHubRepoDetails> = withContext(Dispatchers.IO) {
        try {
            val obj = rest("GET", "$GITHUB_API/repos/$owner/$repo")
            val name = (obj["name"] as? JsonPrimitive)?.content ?: repo
            val fullName = (obj["full_name"] as? JsonPrimitive)?.content ?: "$owner/$repo"
            val description = (obj["description"] as? JsonPrimitive)?.content
            val isPrivate = (obj["private"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false
            val defaultBranch = (obj["default_branch"] as? JsonPrimitive)?.content ?: "main"
            val language = (obj["language"] as? JsonPrimitive)?.content
            val stars = (obj["stargazers_count"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
            val forks = (obj["forks_count"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
            val openIssues = (obj["open_issues_count"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
            val cloneUrl = (obj["clone_url"] as? JsonPrimitive)?.content
            val htmlUrl = (obj["html_url"] as? JsonPrimitive)?.content

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

    suspend fun listRepoBranches(owner: String, repo: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val array = restArray("GET", "$GITHUB_API/repos/$owner/$repo/branches?per_page=30")
            val branches = array.mapNotNull { el ->
                val obj = el as? JsonObject ?: return@mapNotNull null
                (obj["name"] as? JsonPrimitive)?.content
            }
            Result.success(branches)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list branches for $owner/$repo", e)
            Result.failure(e)
        }
    }

    suspend fun listRepoCommits(owner: String, repo: String, branch: String = "main", limit: Int = 10): Result<List<GitHubCommitSummary>> = withContext(Dispatchers.IO) {
        try {
            val array = restArray("GET", "$GITHUB_API/repos/$owner/$repo/commits?sha=$branch&per_page=${limit.coerceIn(1, 30)}")
            val commits = array.mapNotNull { el ->
                val obj = el as? JsonObject ?: return@mapNotNull null
                val sha = (obj["sha"] as? JsonPrimitive)?.content?.take(7) ?: ""
                val commitObj = obj["commit"] as? JsonObject
                val message = (commitObj?.get("message") as? JsonPrimitive)?.content?.lines()?.firstOrNull() ?: ""
                val authorObj = commitObj?.get("author") as? JsonObject
                val author = (authorObj?.get("name") as? JsonPrimitive)?.content ?: "unknown"
                val date = (authorObj?.get("date") as? JsonPrimitive)?.content ?: ""

                GitHubCommitSummary(sha = sha, message = message, author = author, date = date)
            }
            Result.success(commits)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list commits for $owner/$repo", e)
            Result.failure(e)
        }
    }

    suspend fun listRepoIssues(owner: String, repo: String, state: String = "open", limit: Int = 10): Result<List<GitHubIssueSummary>> = withContext(Dispatchers.IO) {
        try {
            val array = restArray("GET", "$GITHUB_API/repos/$owner/$repo/issues?state=$state&per_page=${limit.coerceIn(1, 30)}")
            val issues = array.mapNotNull { el ->
                val obj = el as? JsonObject ?: return@mapNotNull null
                val number = (obj["number"] as? JsonPrimitive)?.content?.toIntOrNull() ?: return@mapNotNull null
                val title = (obj["title"] as? JsonPrimitive)?.content ?: ""
                val issueState = (obj["state"] as? JsonPrimitive)?.content ?: state
                val userObj = obj["user"] as? JsonObject
                val author = (userObj?.get("login") as? JsonPrimitive)?.content ?: "unknown"
                val isPR = obj.containsKey("pull_request")
                val url = (obj["html_url"] as? JsonPrimitive)?.content ?: ""

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
        val rawBody = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            throw Exception("GitHub GraphQL error ${response.code}: $rawBody")
        }
        val element = if (rawBody.isBlank()) JsonObject(emptyMap()) else json.parseToJsonElement(rawBody)
        return (element as? JsonObject) ?: JsonObject(emptyMap())
    }

    private suspend fun restElement(method: String, url: String, body: String? = null): JsonElement {
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
        val rawBody = response.body?.string().orEmpty()
        if (!response.isSuccessful && response.code != 201 && response.code != 204) {
            if (response.code == 401) {
                throw IllegalStateException("GitHub PAT is invalid or expired (401 Unauthorized)")
            } else if (response.code == 403) {
                throw IllegalStateException("GitHub API rate limit exceeded or missing scopes (403 Forbidden)")
            }
            throw Exception("GitHub REST error ${response.code}: $rawBody")
        }
        if (rawBody.isBlank()) {
            return JsonObject(emptyMap())
        }
        return json.parseToJsonElement(rawBody)
    }

    private suspend fun rest(method: String, url: String, body: String? = null): JsonObject {
        val element = restElement(method, url, body)
        return when (element) {
            is JsonObject -> element
            is JsonArray -> (element.firstOrNull() as? JsonObject) ?: JsonObject(emptyMap())
            else -> JsonObject(emptyMap())
        }
    }

    private suspend fun restArray(method: String, url: String, body: String? = null): JsonArray {
        val element = restElement(method, url, body)
        return when (element) {
            is JsonArray -> element
            is JsonObject -> JsonArray(listOf(element))
            else -> JsonArray(emptyList())
        }
    }

    private suspend fun getRefSha(owner: String, repo: String, ref: String): String? {
        val cleanRef = ref.removePrefix("refs/")
        return try {
            val element = restElement("GET", "$GITHUB_API/repos/$owner/$repo/git/ref/$cleanRef")
            extractSha(element)
        } catch (_: Exception) {
            try {
                val element = restElement("GET", "$GITHUB_API/repos/$owner/$repo/git/refs/$cleanRef")
                extractSha(element)
            } catch (_: Exception) { null }
        }
    }

    private fun extractSha(element: JsonElement): String? = when (element) {
        is JsonObject -> (element["object"] as? JsonObject)?.get("sha")?.let { (it as? JsonPrimitive)?.content }
            ?: (element["sha"] as? JsonPrimitive)?.content
        is JsonArray -> element.firstNotNullOfOrNull { item ->
            (item as? JsonObject)?.let { extractSha(it) }
        }
        else -> null
    }

    private suspend fun getFileSha(owner: String, repo: String, path: String, branch: String): String? {
        return try {
            val response = rest("GET", "$GITHUB_API/repos/$owner/$repo/contents/$path?ref=$branch")
            (response["sha"] as? JsonPrimitive)?.content
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

data class MergeResult(
    val sha: String,
    val merged: Boolean,
    val message: String
)

data class PullRequestStatus(
    val number: Int,
    val state: String,
    val merged: Boolean,
    val mergeable: Boolean?,
    val mergeableState: String?,
    val headSha: String,
    val headRef: String,
    val htmlUrl: String
)

data class GitHubRelease(
    val id: Long,
    val tagName: String,
    val name: String?,
    val htmlUrl: String,
    val body: String?
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
