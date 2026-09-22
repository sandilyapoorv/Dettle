package com.dettle.app.orchestrator.config

import android.util.Log
import com.dettle.app.data.github.GitHubClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AgentConfigLoader"
private const val CONFIG_PATH = ".agents/config.json"

/**
 * Loads [DettleAgentConfig] from the project's `.agents/config.json` on GitHub.
 *
 * This makes `.agents/config.json` the single source of truth for project rules.
 * The GitHub repo is the authoritative config store — not a local file, not a
 * hardcoded string in Kotlin.
 *
 * Falls back to [DettleAgentConfig.DEFAULT] if:
 * - No GitHub PAT is configured
 * - The remote file doesn't exist yet
 * - Network is unavailable
 * - JSON parsing fails
 *
 * Cached for the session — call [invalidate] to force a reload.
 */
@Singleton
class AgentConfigLoader @Inject constructor(
    private val githubClient: GitHubClient,
    private val json: Json
) {
    private var cached: DettleAgentConfig? = null
    private var cacheOwner: String? = null
    private var cacheRepo: String? = null

    /**
     * Load config for a specific repo. Returns cached value if repo matches.
     * @param owner GitHub repo owner
     * @param repo GitHub repo name
     * @param branch Branch to read config from (default: "main")
     */
    suspend fun load(
        owner: String,
        repo: String,
        branch: String = "main"
    ): DettleAgentConfig = withContext(Dispatchers.IO) {
        // Return cache if same repo
        if (cached != null && cacheOwner == owner && cacheRepo == repo) {
            Log.d(TAG, "Using cached config for $owner/$repo")
            return@withContext cached!!
        }

        Log.d(TAG, "Loading .agents/config.json from $owner/$repo@$branch")

        val result = githubClient.readFile(owner, repo, CONFIG_PATH, branch)
        val configJson = result.getOrElse {
            Log.w(TAG, "Could not read .agents/config.json: ${it.message}. Using defaults.")
            return@withContext DettleAgentConfig.DEFAULT
        }

        return@withContext try {
            parseConfig(configJson).also { config ->
                cached = config
                cacheOwner = owner
                cacheRepo = repo
                Log.d(TAG, "Loaded config: project=${config.project}, version=${config.version}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse .agents/config.json: ${e.message}. Using defaults.")
            DettleAgentConfig.DEFAULT
        }
    }

    /** Force a re-fetch on the next [load] call */
    fun invalidate() {
        cached = null
        cacheOwner = null
        cacheRepo = null
    }

    /**
     * Parse the raw JSON string into a [DettleAgentConfig].
     *
     * Uses manual JSON parsing rather than @Serializable deserialization
     * because the config file is written by humans and may have missing fields,
     * comments or extra fields. Manual parsing gives graceful defaults per field.
     */
    private fun parseConfig(raw: String): DettleAgentConfig {
        val root = json.parseToJsonElement(raw).jsonObject

        val android = root["android"]?.jsonObject
        val delivery = root["delivery"]?.jsonObject
        val backend = root["backend"]?.jsonObject
        val security = root["security"]?.jsonObject
        val freeTierObj = root["freeTier"]?.jsonObject
        val cloudflare = freeTierObj?.get("cloudflare")?.jsonObject
        val firebase = freeTierObj?.get("firebase")?.jsonObject
        val github = freeTierObj?.get("github")?.jsonObject
        val imagekit = freeTierObj?.get("imagekit")?.jsonObject

        return DettleAgentConfig(
            project = root["project"]?.jsonPrimitive?.content ?: "Dettle",
            version = root["version"]?.jsonPrimitive?.content ?: "1.0",
            android = AndroidStackConfig(
                minSdk = android?.get("minSdk")?.jsonPrimitive?.int ?: 26,
                targetSdk = android?.get("targetSdk")?.jsonPrimitive?.int ?: 35,
                compileSdk = android?.get("compileSdk")?.jsonPrimitive?.int ?: 35,
                agp = android?.get("agp")?.jsonPrimitive?.content ?: "8.5",
                kotlin = android?.get("kotlin")?.jsonPrimitive?.content ?: "2.0",
                jdk = android?.get("jdk")?.jsonPrimitive?.content ?: "17",
                compose = android?.get("compose")?.jsonPrimitive?.boolean ?: true,
                material3 = android?.get("material3")?.jsonPrimitive?.boolean ?: true,
                applicationId = android?.get("applicationId")?.jsonPrimitive?.content ?: "com.dettle.app"
            ),
            delivery = DeliveryPolicy(
                requireCI = delivery?.get("requireCI")?.jsonPrimitive?.boolean ?: true,
                requireAPK = delivery?.get("requireAPK")?.jsonPrimitive?.boolean ?: true,
                requireTests = delivery?.get("requireTests")?.jsonPrimitive?.boolean ?: true,
                requireLint = delivery?.get("requireLint")?.jsonPrimitive?.boolean ?: true,
                defaultBranch = delivery?.get("defaultBranch")?.jsonPrimitive?.content ?: "main",
                branchPrefix = delivery?.get("branchPrefix")?.jsonPrimitive?.content ?: "dettle/",
                requirePR = delivery?.get("requirePR")?.jsonPrimitive?.boolean ?: true,
                neverPushDirectlyToMain = delivery?.get("neverPushDirectlyToMain")?.jsonPrimitive?.boolean ?: true,
                requireProductionCriticBeforeCompletion =
                    delivery?.get("requireProductionCriticBeforeCompletion")?.jsonPrimitive?.boolean ?: true,
                releaseWorkflow = delivery?.get("releaseWorkflow")?.jsonPrimitive?.content ?: "release.yml",
                debugWorkflow = delivery?.get("debugWorkflow")?.jsonPrimitive?.content ?: "build.yml"
            ),
            backend = BackendStackConfig(
                auth = backend?.get("auth")?.jsonPrimitive?.content ?: "firebase-auth",
                database = backend?.get("database")?.jsonPrimitive?.content ?: "firestore",
                realtimeSync = backend?.get("realtimeSync")?.jsonPrimitive?.content ?: "firebase-rtdb",
                media = backend?.get("media")?.jsonPrimitive?.content ?: "imagekit",
                edgeFunctions = backend?.get("edgeFunctions")?.jsonPrimitive?.content ?: "cloudflare-workers",
                staticHosting = backend?.get("staticHosting")?.jsonPrimitive?.content ?: "cloudflare-pages",
                requireSecurityRules = backend?.get("requireSecurityRules")?.jsonPrimitive?.boolean ?: true,
                neverClientOnlyAuthz = backend?.get("neverClientOnlyAuthz")?.jsonPrimitive?.boolean ?: true
            ),
            security = SecurityPolicy(
                forbiddenPatterns = security?.get("forbiddenPatterns")
                    ?.jsonArray?.map { it.jsonPrimitive.content }
                    ?: SecurityPolicy().forbiddenPatterns,
                signingSecretsStorage = security?.get("signingSecretsStorage")?.jsonPrimitive?.content
                    ?: "github_actions_secrets",
                githubActionsSecrets = security?.get("githubActionsSecrets")
                    ?.jsonArray?.map { it.jsonPrimitive.content }
                    ?: SecurityPolicy().githubActionsSecrets
            ),
            freeTier = FreeTierBudget(
                cloudflare = CloudflareQuota(
                    workersRequestsPerDay = cloudflare?.get("workersRequestsPerDay")?.jsonPrimitive?.int ?: 100_000,
                    workerCpuMsPerInvocation = cloudflare?.get("workerCpuMsPerInvocation")?.jsonPrimitive?.int ?: 10
                ),
                firebase = FirebaseQuota(
                    firestoreReadsPerDay = firebase?.get("firestoreReadsPerDay")?.jsonPrimitive?.int ?: 50_000,
                    firestoreWritesPerDay = firebase?.get("firestoreWritesPerDay")?.jsonPrimitive?.int ?: 20_000,
                    firestoreDeletesPerDay = firebase?.get("firestoreDeletesPerDay")?.jsonPrimitive?.int ?: 20_000
                ),
                github = GitHubQuota(
                    actionsMinutesPerMonth = github?.get("actionsMinutesPerMonth")?.jsonPrimitive?.int ?: 2_000
                )
            )
        )
    }
}
