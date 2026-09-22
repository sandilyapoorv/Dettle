package com.dettle.app.data.github

import android.util.Log
import com.dettle.app.data.settings.ApiKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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
                put("body", "🤖 *Created by Dettle AI Agent*\n\n$prBody")
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

    // ─── Internal Helpers ─────────────────────────────────────────────────

    private suspend fun graphql(query: String, variables: JsonObject): JsonObject {
        val body = buildJsonObject {
            put("query", query)
            put("variables", variables)
        }.toString()

        val request = Request.Builder()
            .url(GITHUB_GRAPHQL)
            .post(body.toRequestBody("application/json".toMediaType()))
            .header("Authorization", "Bearer ${pat ?: throw Exception("GitHub PAT not configured")}")
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

        val request = Request.Builder()
            .url(url)
            .method(method, requestBody)
            .header("Authorization", "Bearer ${pat ?: throw Exception("GitHub PAT not configured")}")
            .header("User-Agent", "Dettle-App/1.0")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful && response.code != 201) {
            throw Exception("GitHub REST error ${response.code}: ${response.body?.string()}")
        }
        return json.parseToJsonElement(response.body?.string() ?: "{}").jsonObject
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
