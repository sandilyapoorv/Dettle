package com.dettle.app.data.cloudflare

import android.util.Log
import com.dettle.app.data.settings.ApiKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CloudflareClient"
private const val CF_API = "https://api.cloudflare.com/client/v4"

/**
 * Cloudflare API client for Pages + Workers + CDN Cache.
 *
 * All operations use the Cloudflare API token stored in ApiKeyStore.
 * Free tier limits enforced in architecture:
 *  - Pages: unlimited static deployments via Direct Upload (not Git-triggered)
 *  - Workers: 100K req/day, 10ms CPU per invocation
 *  - R2: 10GB free storage, 1M Class B ops/month
 *
 * Primary use-cases for Dettle:
 *  - Deploy generated websites/apps directly to Cloudflare Pages
 *  - Create Workers for thin API endpoints / webhooks
 *  - Check live deployment status & verify URLs
 *  - Purge CDN cache
 */
@Singleton
class CloudflareClient @Inject constructor(
    private val keyStore: ApiKeyStore,
    private val client: OkHttpClient,
    private val json: Json
) {
    private val accountId get() = keyStore.cloudflareAccountId ?: ""
    private val token get() = keyStore.cloudflareApiToken ?: ""

    private fun authHeaders(request: Request.Builder) = request
        .header("Authorization", "Bearer $token")
        .header("Content-Type", "application/json")

    // ── Pages ─────────────────────────────────────────────────────────────

    /**
     * Creates a new Cloudflare Pages project (one-time per site).
     * Returns the project name and production URL.
     */
    suspend fun createPagesProject(projectName: String): Result<PagesProject> =
        withContext(Dispatchers.IO) {
            if (token.isBlank() || accountId.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Cloudflare API Token and Account ID must be configured in Settings."))
            }
            try {
                val body = """{"name":"$projectName","production_branch":"main"}"""
                val request = authHeaders(
                    Request.Builder()
                        .url("$CF_API/accounts/$accountId/pages/projects")
                        .post(body.toRequestBody("application/json".toMediaType()))
                ).build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string().orEmpty()
                val parsed = json.decodeFromString<CfResponse<PagesProjectResult>>(responseBody)

                if (parsed.success && parsed.result != null) {
                    Result.success(
                        PagesProject(
                            name = parsed.result.name,
                            subdomain = parsed.result.subdomain,
                            productionUrl = "https://${parsed.result.subdomain}.pages.dev"
                        )
                    )
                } else {
                    Result.failure(Exception(parsed.errors.joinToString { it.message }.ifBlank { "Failed to create Pages project $projectName" }))
                }
            } catch (e: Exception) {
                Log.e(TAG, "createPagesProject failed", e)
                Result.failure(e)
            }
        }

    /**
     * Deploys a set of files to Cloudflare Pages via Direct Upload.
     * This is FREE and UNLIMITED (unlike Git-triggered builds).
     *
     * @param projectName  Existing Pages project name
     * @param files        Map of filePath -> fileContent (e.g. "index.html" -> "<html>...")
     */
    suspend fun deployToPages(
        projectName: String,
        files: Map<String, String>
    ): Result<PagesDeployment> = withContext(Dispatchers.IO) {
        if (token.isBlank() || accountId.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Cloudflare API Token and Account ID must be configured in Settings."))
        }
        try {
            // Step 1: Create deployment
            val createRequest = authHeaders(
                Request.Builder()
                    .url("$CF_API/accounts/$accountId/pages/projects/$projectName/deployments")
                    .post("{}".toRequestBody("application/json".toMediaType()))
            ).build()
            val createResponse = client.newCall(createRequest).execute()
            val createBody = createResponse.body?.string().orEmpty()
            val createParsed = json.decodeFromString<CfResponse<PagesDeploymentResult>>(createBody)

            if (!createParsed.success || createParsed.result == null) {
                return@withContext Result.failure(Exception("Failed to create deployment: ${createParsed.errors.joinToString { it.message }}"))
            }

            val deploymentId = createParsed.result.id
            val uploadUrl = "$CF_API/accounts/$accountId/pages/projects/$projectName/deployments/$deploymentId/files"

            // Step 2: Upload each file via multipart
            files.forEach { (path, content) ->
                val multipart = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file",
                        path,
                        content.toRequestBody(guessMimeType(path).toMediaType())
                    )
                    .build()

                val uploadRequest = authHeaders(
                    Request.Builder().url(uploadUrl).post(multipart)
                ).removeHeader("Content-Type").build()  // Let OkHttp set multipart Content-Type

                client.newCall(uploadRequest).execute()
            }

            // Step 3: Finalize deployment
            val finalizeRequest = authHeaders(
                Request.Builder()
                    .url("$CF_API/accounts/$accountId/pages/projects/$projectName/deployments/$deploymentId")
                    .patch("{}".toRequestBody("application/json".toMediaType()))
            ).build()
            client.newCall(finalizeRequest).execute()

            Log.d(TAG, "Deployed $projectName: ${files.size} files")
            Result.success(
                PagesDeployment(
                    id = deploymentId,
                    projectName = projectName,
                    url = "https://$deploymentId.$projectName.pages.dev",
                    productionUrl = "https://$projectName.pages.dev",
                    fileCount = files.size
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "deployToPages failed", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches details of a specific Pages deployment.
     */
    suspend fun getDeployment(
        projectName: String,
        deploymentId: String
    ): Result<PagesDeployment> = withContext(Dispatchers.IO) {
        if (token.isBlank() || accountId.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Cloudflare API Token and Account ID must be configured in Settings."))
        }
        try {
            val request = authHeaders(
                Request.Builder()
                    .url("$CF_API/accounts/$accountId/pages/projects/$projectName/deployments/$deploymentId")
                    .get()
            ).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            val parsed = json.decodeFromString<CfResponse<PagesDeploymentResult>>(body)

            if (parsed.success && parsed.result != null) {
                val r = parsed.result
                Result.success(
                    PagesDeployment(
                        id = r.id.ifBlank { deploymentId },
                        projectName = projectName,
                        url = r.url ?: "https://$deploymentId.$projectName.pages.dev",
                        productionUrl = "https://$projectName.pages.dev",
                        fileCount = 0,
                        createdAt = r.created_on ?: "",
                        stage = r.latest_stage?.name ?: "active"
                    )
                )
            } else {
                Result.failure(Exception(parsed.errors.joinToString { it.message }.ifBlank { "Deployment $deploymentId not found" }))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getDeployment failed for $projectName/$deploymentId", e)
            Result.failure(e)
        }
    }

    /**
     * Lists recent deployments for a Pages project.
     */
    suspend fun listDeployments(projectName: String): Result<List<PagesDeployment>> =
        withContext(Dispatchers.IO) {
            if (token.isBlank() || accountId.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Cloudflare API Token and Account ID must be configured in Settings."))
            }
            try {
                val request = authHeaders(
                    Request.Builder()
                        .url("$CF_API/accounts/$accountId/pages/projects/$projectName/deployments")
                        .get()
                ).build()
                val body = client.newCall(request).execute().body?.string().orEmpty()
                val parsed = json.decodeFromString<CfResponse<List<PagesDeploymentResult>>>(body)

                if (parsed.success) {
                    Result.success(
                        parsed.result?.map { r ->
                            PagesDeployment(
                                id = r.id,
                                projectName = projectName,
                                url = r.url ?: "",
                                productionUrl = "https://$projectName.pages.dev",
                                fileCount = 0,
                                createdAt = r.created_on ?: "",
                                stage = r.latest_stage?.name ?: "unknown"
                            )
                        } ?: emptyList()
                    )
                } else {
                    Result.failure(Exception(parsed.errors.joinToString { it.message }))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Lists all Pages projects in the account.
     */
    suspend fun listPagesProjects(): Result<List<PagesProject>> = withContext(Dispatchers.IO) {
        if (token.isBlank() || accountId.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Cloudflare API Token and Account ID must be configured in Settings."))
        }
        try {
            val request = authHeaders(
                Request.Builder()
                    .url("$CF_API/accounts/$accountId/pages/projects")
                    .get()
            ).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            val parsed = json.decodeFromString<CfResponse<List<PagesProjectResult>>>(body)

            if (parsed.success) {
                val projects = parsed.result?.map { r ->
                    PagesProject(
                        name = r.name,
                        subdomain = r.subdomain,
                        productionUrl = "https://${r.subdomain}.pages.dev"
                    )
                } ?: emptyList()
                Result.success(projects)
            } else {
                Result.failure(Exception(parsed.errors.joinToString { it.message }))
            }
        } catch (e: Exception) {
            Log.e(TAG, "listPagesProjects failed", e)
            Result.failure(e)
        }
    }

    // ── Workers ───────────────────────────────────────────────────────────

    /**
     * Creates or updates a Cloudflare Worker script.
     * @param workerName  Unique name for the worker (becomes the subdomain)
     * @param scriptBody  JavaScript/TypeScript worker code
     */
    suspend fun deployWorker(
        workerName: String,
        scriptBody: String
    ): Result<WorkerDeployment> = withContext(Dispatchers.IO) {
        if (token.isBlank() || accountId.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Cloudflare API Token and Account ID must be configured in Settings."))
        }
        try {
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "script",
                    "worker.js",
                    scriptBody.toRequestBody("application/javascript".toMediaType())
                )
                .addFormDataPart(
                    "metadata",
                    "metadata.json",
                    """{"main_module":"worker.js","compatibility_date":"2024-01-01"}"""
                        .toRequestBody("application/json".toMediaType())
                )
                .build()

            val request = authHeaders(
                Request.Builder()
                    .url("$CF_API/accounts/$accountId/workers/scripts/$workerName")
                    .put(multipart)
            ).removeHeader("Content-Type").build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                Log.d(TAG, "Worker deployed: $workerName")
                Result.success(
                    WorkerDeployment(
                        name = workerName,
                        url = "https://$workerName.$accountId.workers.dev",
                        scriptSize = scriptBody.length
                    )
                )
            } else {
                Result.failure(Exception("Worker deploy failed (${response.code}): $body"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "deployWorker failed", e)
            Result.failure(e)
        }
    }

    /**
     * Lists all Workers in the account.
     */
    suspend fun listWorkers(): Result<List<String>> = withContext(Dispatchers.IO) {
        if (token.isBlank() || accountId.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Cloudflare API Token and Account ID must be configured in Settings."))
        }
        try {
            val request = authHeaders(
                Request.Builder()
                    .url("$CF_API/accounts/$accountId/workers/scripts")
                    .get()
            ).build()
            val body = client.newCall(request).execute().body?.string().orEmpty()
            val parsed = json.decodeFromString<CfResponse<List<WorkerScript>>>(body)
            Result.success(parsed.result?.map { it.id } ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── CDN Cache ─────────────────────────────────────────────────────────

    /**
     * Purges all cached files from Cloudflare edge nodes for a specific zone.
     */
    suspend fun purgeCache(zoneId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Cloudflare API Token must be configured in Settings."))
        }
        try {
            val body = """{"purge_everything":true}"""
            val request = authHeaders(
                Request.Builder()
                    .url("$CF_API/zones/$zoneId/purge_cache")
                    .post(body.toRequestBody("application/json".toMediaType()))
            ).build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()
            val parsed = json.decodeFromString<CfResponse<PurgeResult>>(responseBody)
            if (parsed.success) {
                Result.success(true)
            } else {
                Result.failure(Exception(parsed.errors.joinToString { it.message }.ifBlank { "Cache purge failed" }))
            }
        } catch (e: Exception) {
            Log.e(TAG, "purgeCache failed for zone $zoneId", e)
            Result.failure(e)
        }
    }

    // ── Account info ──────────────────────────────────────────────────────

    suspend fun verifyToken(): Result<String> = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Cloudflare API Token is not configured."))
        }
        try {
            val request = authHeaders(
                Request.Builder().url("$CF_API/user/tokens/verify").get()
            ).build()
            val body = client.newCall(request).execute().body?.string().orEmpty()
            val parsed = json.decodeFromString<CfResponse<TokenVerifyResult>>(body)
            if (parsed.success) Result.success(parsed.result?.status ?: "active")
            else Result.failure(Exception("Token invalid"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun guessMimeType(path: String) = when {
        path.endsWith(".html") -> "text/html"
        path.endsWith(".css") -> "text/css"
        path.endsWith(".js") || path.endsWith(".mjs") -> "application/javascript"
        path.endsWith(".json") -> "application/json"
        path.endsWith(".png") -> "image/png"
        path.endsWith(".svg") -> "image/svg+xml"
        path.endsWith(".woff2") -> "font/woff2"
        else -> "application/octet-stream"
    }
}

// ── Cloudflare API response wrappers ──────────────────────────────────────

@Serializable data class CfResponse<T>(
    val success: Boolean = false,
    val result: T? = null,
    val errors: List<CfError> = emptyList()
)
@Serializable data class CfError(val code: Int = 0, val message: String = "")
@Serializable data class PagesProjectResult(val name: String = "", val subdomain: String = "")
@Serializable data class PagesDeploymentResult(
    val id: String = "",
    val url: String? = null,
    val created_on: String? = null,
    val latest_stage: PagesStage? = null
)
@Serializable data class PagesStage(val name: String = "")
@Serializable data class WorkerScript(val id: String = "")
@Serializable data class PurgeResult(val id: String = "")
@Serializable data class TokenVerifyResult(val status: String = "")

// ── Domain models ─────────────────────────────────────────────────────────

data class PagesProject(val name: String, val subdomain: String, val productionUrl: String)

data class PagesDeployment(
    val id: String,
    val projectName: String,
    val url: String,
    val productionUrl: String,
    val fileCount: Int,
    val createdAt: String = "",
    val stage: String = "active"
)

data class WorkerDeployment(val name: String, val url: String, val scriptSize: Int)
