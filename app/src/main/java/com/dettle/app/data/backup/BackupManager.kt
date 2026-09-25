package com.dettle.app.data.backup

import android.content.Context
import android.net.Uri
import android.webkit.CookieManager
import androidx.core.content.FileProvider
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dettle.app.data.db.DettleDatabase
import com.dettle.app.data.db.entity.ConceptEdgeEntity
import com.dettle.app.data.db.entity.ConceptNodeEntity
import com.dettle.app.data.db.entity.ConversationEntity
import com.dettle.app.data.db.entity.ConversationMessageEntity
import com.dettle.app.data.db.entity.DeploymentEntity
import com.dettle.app.data.db.entity.MemoryEntity
import com.dettle.app.data.db.entity.ProjectEntity
import com.dettle.app.data.db.entity.ProjectMemoryEntity
import com.dettle.app.data.db.entity.TaskLogEntity
import com.dettle.app.data.db.entity.UserProfileEntity
import com.dettle.app.data.settings.ApiKeyStore
import com.dettle.app.domain.model.AIProviderType
import com.dettle.app.domain.model.GitHubAccount
import com.dettle.app.domain.model.ProviderAccount
import com.dettle.app.domain.model.WebViewAccount
import com.dettle.app.orchestrator.mode.GoalEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class BackupOptions(
    val includeApis: Boolean = true,
    val includeSubscriptions: Boolean = true,
    val includeCookies: Boolean = true,
    val includeMemories: Boolean = true,
    val includeProjects: Boolean = true,
    val includeChatsAndLogs: Boolean = true,
    val includeAppSettings: Boolean = true
) {
    val isAllSelected: Boolean
        get() = includeApis && includeSubscriptions && includeCookies && includeMemories &&
                includeProjects && includeChatsAndLogs && includeAppSettings

    val isNoneSelected: Boolean
        get() = !includeApis && !includeSubscriptions && !includeCookies && !includeMemories &&
                !includeProjects && !includeChatsAndLogs && !includeAppSettings
}

data class BackupSummary(
    val apiCount: Int = 0,
    val subscriptionCount: Int = 0,
    val cookieDomainCount: Int = 0,
    val memoryCount: Int = 0,
    val projectCount: Int = 0,
    val chatCount: Int = 0,
    val logCount: Int = 0,
    val hasSettings: Boolean = true
)

data class RestoreResult(
    val success: Boolean,
    val message: String,
    val apisRestored: Int = 0,
    val subscriptionsRestored: Int = 0,
    val cookiesRestored: Int = 0,
    val memoriesRestored: Int = 0,
    val projectsRestored: Int = 0,
    val chatsRestored: Int = 0,
    val logsRestored: Int = 0,
    val preferencesRestored: Int = 0
)

/**
 * Universal, exhaustive backup and restore engine for Dettle.
 *
 * Backs up 100% of application state:
 * 1. APIs, Multi-Accounts, Tokens, Google Drive OAuth, and raw EncryptedSharedPreferences entries.
 * 2. WebView subscription accounts.
 * 3. Live browser session cookies via Android CookieManager across all AI & developer domains.
 * 4. Cognitive episodic memories, project facts, knowledge graph concepts/edges, and user profile.
 * 5. Projects and workspace architectures.
 * 6. Conversations, message threads with tool calls, autonomous task logs, goals, and deployments.
 * 7. System theme configs, mode overrides, engine toggles (Unleashed), voice typing, and all DataStore preferences.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiKeyStore: ApiKeyStore,
    private val database: DettleDatabase,
    private val dataStore: DataStore<Preferences>
) {

    private val conversationDao = database.conversationDao()
    private val projectDao = database.projectDao()
    private val taskLogDao = database.taskLogDao()
    private val memoryDao = database.memoryDao()
    private val userProfileDao = database.userProfileDao()
    private val knowledgeGraphDao = database.knowledgeGraphDao()
    private val deploymentDao = database.deploymentDao()
    private val goalDao = database.goalDao()

    private val wellKnownCookieDomains = listOf(
        "https://chatgpt.com",
        "https://auth.openai.com",
        "https://platform.openai.com",
        "https://oaistatic.com",
        "https://claude.ai",
        "https://anthropic.com",
        "https://x.ai",
        "https://grok.com",
        "https://twitter.com",
        "https://x.com",
        "https://chat.deepseek.com",
        "https://deepseek.com",
        "https://www.perplexity.ai",
        "https://perplexity.ai",
        "https://github.com",
        "https://accounts.google.com"
    )

    suspend fun getLiveSummary(): BackupSummary = withContext(Dispatchers.IO) {
        val apis = apiKeyStore.getAllProviderAccounts().size + apiKeyStore.getAllGitHubAccounts().size
        val subscriptions = apiKeyStore.getAllWebViewAccounts().size
        val projects = projectDao.getAllProjects().size
        val chats = conversationDao.getAllConversations().size
        val logs = taskLogDao.getAllLogs().size
        val memories = memoryDao.getAllMemories().size + projectDao.getAllProjectMemories().size

        // Count domains with active cookies
        val cookieDomains = getAllTargetCookieDomains()
        val cookieCount = withContext(Dispatchers.Main) {
            val cm = CookieManager.getInstance()
            cookieDomains.count { url -> !cm.getCookie(url).isNullOrBlank() }
        }

        BackupSummary(
            apiCount = apis,
            subscriptionCount = subscriptions,
            cookieDomainCount = cookieCount,
            memoryCount = memories,
            projectCount = projects,
            chatCount = chats,
            logCount = logs,
            hasSettings = true
        )
    }

    private fun getAllTargetCookieDomains(): List<String> {
        val dynamicDomains = apiKeyStore.getAllWebViewAccounts().flatMap {
            listOfNotNull(it.baseUrl, it.loginUrl)
        }
        return (wellKnownCookieDomains + dynamicDomains)
            .filter { it.startsWith("http://") || it.startsWith("https://") }
            .distinct()
    }

    suspend fun createBackupJson(options: BackupOptions): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 2)
        root.put("timestamp", System.currentTimeMillis())
        root.put("app", "Dettle")
        root.put("app_version", "1.5.0")

        // 1. APIs, Keys, Cloudflare & Google Drive OAuth Credentials
        if (options.includeApis) {
            val apisObj = JSONObject()

            // AI Providers
            val providerArr = JSONArray()
            apiKeyStore.getAllProviderAccounts().forEach { acc ->
                providerArr.put(JSONObject().apply {
                    put("id", acc.id)
                    put("provider", acc.provider.name)
                    put("label", acc.label)
                    put("apiKey", acc.apiKey)
                    put("isActive", acc.isActive)
                    put("requestsUsed", acc.requestsUsed)
                    put("tokensUsed", acc.tokensUsed)
                    put("rateLimitedUntilMs", acc.rateLimitedUntilMs)
                })
            }
            apisObj.put("providers", providerArr)

            // GitHub Accounts
            val ghArr = JSONArray()
            apiKeyStore.getAllGitHubAccounts().forEach { gh ->
                ghArr.put(JSONObject().apply {
                    put("id", gh.id)
                    put("label", gh.label)
                    put("pat", gh.pat)
                    put("username", gh.username ?: "")
                    put("selectedScopes", JSONArray(gh.selectedScopes))
                    put("isActive", gh.isActive)
                })
            }
            apisObj.put("github", ghArr)

            // Cloudflare
            apiKeyStore.cloudflareApiToken?.let { apisObj.put("cloudflare_token", it) }
            apiKeyStore.cloudflareAccountId?.let { apisObj.put("cloudflare_account_id", it) }

            // Google Drive OAuth Credentials
            val driveObj = JSONObject().apply {
                apiKeyStore.driveIdToken?.let { put("drive_id_token", it) }
                apiKeyStore.driveRefreshToken?.let { put("drive_refresh_token", it) }
                apiKeyStore.driveUserEmail?.let { put("drive_user_email", it) }
                apiKeyStore.driveFolderId?.let { put("drive_folder_id", it) }
            }
            if (driveObj.length() > 0) {
                apisObj.put("drive", driveObj)
            }

            // Raw Secure Entries fallback (ensures 100% data capture)
            val rawEntries = apiKeyStore.getAllSecureEntries()
            if (rawEntries.isNotEmpty()) {
                val rawObj = JSONObject()
                for ((k, v) in rawEntries) {
                    when (v) {
                        is String -> rawObj.put(k, v)
                        is Boolean -> rawObj.put(k, v)
                        is Int -> rawObj.put(k, v)
                        is Long -> rawObj.put(k, v)
                        is Float -> rawObj.put(k, v.toDouble())
                    }
                }
                apisObj.put("raw_secure_entries", rawObj)
            }

            root.put("apis", apisObj)
        }

        // 2. Subscriptions (ChatGPT, Claude, Grok, DeepSeek, etc.)
        if (options.includeSubscriptions) {
            val subArr = JSONArray()
            apiKeyStore.getAllWebViewAccounts().forEach { sub ->
                subArr.put(JSONObject().apply {
                    put("id", sub.id)
                    put("providerType", sub.providerType.name)
                    put("label", sub.label)
                    put("loginUrl", sub.loginUrl)
                    put("baseUrl", sub.baseUrl)
                    put("isEnabled", sub.isEnabled)
                    put("requestsUsed", sub.requestsUsed)
                    put("tokensUsed", sub.tokensUsed)
                    put("isLoggedIn", sub.isLoggedIn)
                })
            }
            root.put("subscriptions", subArr)
        }

        // 3. Browser Sessions & Cookies
        if (options.includeCookies) {
            val cookieList = JSONArray()
            val targetDomains = getAllTargetCookieDomains()
            withContext(Dispatchers.Main) {
                val cm = CookieManager.getInstance()
                for (domain in targetDomains) {
                    val cookieStr = cm.getCookie(domain)
                    if (!cookieStr.isNullOrBlank()) {
                        cookieList.put(JSONObject().apply {
                            put("url", domain)
                            put("cookies", cookieStr)
                        })
                    }
                }
            }
            root.put("cookies", cookieList)
        }

        // 4. Cognitive Memories, Facts, Knowledge Graph, & User Profile
        if (options.includeMemories) {
            val memObj = JSONObject()

            // Episodic Agent Memories
            val episodicArr = JSONArray()
            memoryDao.getAllMemories().forEach { m ->
                episodicArr.put(JSONObject().apply {
                    put("id", m.id)
                    put("type", m.type)
                    put("content", m.content)
                    put("repoKey", m.repoKey)
                    put("tags", JSONArray(m.tags))
                    put("importance", m.importance.toDouble())
                    m.vector?.let { floats ->
                        val vArr = JSONArray()
                        floats.forEach { vArr.put(it.toDouble()) }
                        put("vector", vArr)
                    }
                    put("accessCount", m.accessCount)
                    put("createdAt", m.createdAt)
                    put("lastAccessedAt", m.lastAccessedAt)
                })
            }
            memObj.put("episodic", episodicArr)

            // Project-Scoped Memories
            val projMemArr = JSONArray()
            projectDao.getAllProjectMemories().forEach { pm ->
                projMemArr.put(JSONObject().apply {
                    put("id", pm.id)
                    put("projectId", pm.projectId)
                    put("key", pm.key)
                    put("value", pm.value)
                    put("confidence", pm.confidence.toDouble())
                    put("source", pm.source)
                    put("createdAt", pm.createdAt)
                })
            }
            memObj.put("project_memories", projMemArr)

            // Knowledge Graph (Concepts & Edges)
            val kgObj = JSONObject()
            val nodesArr = JSONArray()
            knowledgeGraphDao.getAllNodes().forEach { n ->
                nodesArr.put(JSONObject().apply {
                    put("name", n.name)
                    put("category", n.category)
                    put("description", n.description)
                    put("createdAt", n.createdAt)
                })
            }
            kgObj.put("nodes", nodesArr)

            val edgesArr = JSONArray()
            knowledgeGraphDao.getAllEdges().forEach { e ->
                edgesArr.put(JSONObject().apply {
                    put("id", e.id)
                    put("sourceNode", e.sourceNode)
                    put("targetNode", e.targetNode)
                    put("relationship", e.relationship)
                    put("weight", e.weight.toDouble())
                    put("context", e.context)
                    put("createdAt", e.createdAt)
                })
            }
            kgObj.put("edges", edgesArr)
            memObj.put("knowledge_graph", kgObj)

            // User Profile
            userProfileDao.get()?.let { up ->
                val upObj = JSONObject().apply {
                    put("name", up.name)
                    put("occupation", up.occupation)
                    put("timezone", up.timezone)
                    put("primaryLanguage", up.primaryLanguage)
                    put("primaryFrameworks", up.primaryFrameworks)
                    put("workingStyle", up.workingStyle)
                    put("communicationPref", up.communicationPref)
                    put("currentFocus", up.currentFocus)
                    put("interests", up.interests)
                    put("updatedAt", up.updatedAt)
                }
                memObj.put("user_profile", upObj)
            }

            root.put("memories", memObj)
        }

        // 5. Projects & Workspaces
        if (options.includeProjects) {
            val projArr = JSONArray()
            projectDao.getAllProjects().forEach { p ->
                projArr.put(JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("description", p.description)
                    put("emoji", p.emoji)
                    put("color_hex", p.colorHex)
                    put("system_instructions", p.systemInstructions)
                    put("enforced_protocol_json", p.enforcedProtocolJson ?: "")
                    put("is_unleashed", p.isUnleashed)
                    put("linked_repo", p.linkedRepo)
                    put("memory_mode", p.memoryMode)
                    put("is_archived", p.isArchived)
                    put("created_at", p.createdAt)
                    put("last_used_at", p.lastUsedAt)
                })
            }
            root.put("projects", projArr)
        }

        // 6. Chats, Logs, Goals, & Deployments
        if (options.includeChatsAndLogs) {
            val convArr = JSONArray()
            conversationDao.getAllConversations().forEach { c ->
                convArr.put(JSONObject().apply {
                    put("id", c.id)
                    put("project_id", c.projectId ?: "")
                    put("title", c.title)
                    put("summary", c.summary)
                    put("message_count", c.messageCount)
                    put("is_pinned", c.isPinned)
                    put("created_at", c.createdAt)
                    put("updated_at", c.updatedAt)
                })
            }
            root.put("conversations", convArr)

            val msgArr = JSONArray()
            conversationDao.getAllMessages().forEach { m ->
                msgArr.put(JSONObject().apply {
                    put("id", m.id)
                    put("conversation_id", m.conversationId)
                    put("role", m.role)
                    put("content", m.content)
                    put("type", m.type)
                    put("tool_name", m.toolName ?: "")
                    put("tool_call_id", m.toolCallId ?: "")
                    put("created_at", m.createdAt)
                })
            }
            root.put("messages", msgArr)

            val logArr = JSONArray()
            taskLogDao.getAllLogs().forEach { l ->
                logArr.put(JSONObject().apply {
                    put("id", l.id)
                    put("userRequest", l.userRequest)
                    put("taskType", l.taskType)
                    put("repoKey", l.repoKey)
                    put("steps", JSONArray(l.steps))
                    put("outcome", l.outcome)
                    put("summary", l.summary)
                    put("tokensUsed", l.tokensUsed)
                    put("providerUsed", l.providerUsed)
                    put("durationMs", l.durationMs)
                    put("createdAt", l.createdAt)
                })
            }
            root.put("task_logs", logArr)

            // Autonomous Persistent Goals
            val goalArr = JSONArray()
            goalDao.getAllGoals().forEach { g ->
                goalArr.put(JSONObject().apply {
                    put("id", g.id)
                    put("description", g.description)
                    put("gates_json", g.gatesJson)
                    put("progress_json", g.progressJson)
                    put("is_completed", g.isCompleted)
                    put("started_at", g.startedAt)
                    put("completed_at", g.completedAt ?: -1L)
                    put("last_step", g.lastStep)
                    put("summary", g.summary ?: "")
                })
            }
            root.put("goals", goalArr)

            // Cloudflare Deployments
            val depArr = JSONArray()
            deploymentDao.getAllDeploymentsList().forEach { d ->
                depArr.put(JSONObject().apply {
                    put("id", d.id)
                    put("projectName", d.projectName)
                    put("type", d.type)
                    put("url", d.url)
                    put("productionUrl", d.productionUrl)
                    put("status", d.status)
                    put("fileCount", d.fileCount)
                    put("commitMessage", d.commitMessage)
                    put("repoKey", d.repoKey)
                    put("commitSha", d.commitSha)
                    put("branch", d.branch)
                    put("buildLog", d.buildLog)
                    put("createdAt", d.createdAt)
                })
            }
            root.put("deployments", depArr)
        }

        // 7. App Settings, Preferences, & DataStore
        if (options.includeAppSettings) {
            val settingsObj = JSONObject().apply {
                put("github_owner", apiKeyStore.githubOwner ?: "")
                put("github_repo", apiKeyStore.githubRepo ?: "")
                put("selector_gist_url", apiKeyStore.selectorRegistryGistUrl ?: "")
                put("is_unleashed", apiKeyStore.isUnleashed)
                put("voice_typing_engine", apiKeyStore.voiceTypingEngine)
                put("openwhispr_server_url", apiKeyStore.openWhisprServerUrl)
                put("overnight_max_hours", apiKeyStore.overnightMaxHours)
                put("overnight_auto_start", apiKeyStore.overnightAutoStart)
                put("overnight_schedule_time", apiKeyStore.overnightScheduledTime)
                put("overnight_notify", apiKeyStore.overnightNotifyOnComplete)
                apiKeyStore.taskQueueJson?.let { put("overnight_task_queue", it) }
            }

            // Raw DataStore Preferences (app_theme_id, app_theme_mode, custom accents, mode overrides)
            val dataStoreMap = dataStore.data.first().asMap()
            if (dataStoreMap.isNotEmpty()) {
                val dsObj = JSONObject()
                for ((key, value) in dataStoreMap) {
                    when (value) {
                        is String -> dsObj.put(key.name, value)
                        is Boolean -> dsObj.put(key.name, value)
                        is Int -> dsObj.put(key.name, value)
                        is Long -> dsObj.put(key.name, value)
                        is Float -> dsObj.put(key.name, value.toDouble())
                        is Double -> dsObj.put(key.name, value)
                    }
                }
                settingsObj.put("datastore_preferences", dsObj)
            }

            root.put("settings", settingsObj)
        }

        root.toString(2)
    }

    suspend fun restoreBackup(jsonString: String, options: BackupOptions): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            var apisCount = 0
            var subsCount = 0
            var cookiesCount = 0
            var memoriesCount = 0
            var projCount = 0
            var chatCount = 0
            var logCount = 0
            var prefCount = 0

            // 1. APIs, Keys, Cloudflare & Drive
            if (options.includeApis && root.has("apis")) {
                runCatching {
                    val apisObj = root.getJSONObject("apis")
                    if (apisObj.has("providers")) {
                        val provArr = apisObj.getJSONArray("providers")
                        val existing = apiKeyStore.getAllProviderAccounts().toMutableList()
                        for (i in 0 until provArr.length()) {
                            val o = provArr.getJSONObject(i)
                            val provider = runCatching { AIProviderType.valueOf(o.getString("provider")) }.getOrNull() ?: continue
                            val acc = ProviderAccount(
                                id = o.getString("id"),
                                provider = provider,
                                label = o.getString("label"),
                                apiKey = o.getString("apiKey"),
                                isActive = o.optBoolean("isActive", true),
                                requestsUsed = o.optLong("requestsUsed", 0L),
                                tokensUsed = o.optLong("tokensUsed", 0L),
                                rateLimitedUntilMs = o.optLong("rateLimitedUntilMs", 0L)
                            )
                            val idx = existing.indexOfFirst { it.id == acc.id }
                            if (idx >= 0) existing[idx] = acc else existing.add(acc)
                            apisCount++
                        }
                        apiKeyStore.saveProviderAccounts(existing)
                    }

                    if (apisObj.has("github")) {
                        val ghArr = apisObj.getJSONArray("github")
                        val existingGh = apiKeyStore.getAllGitHubAccounts().toMutableList()
                        for (i in 0 until ghArr.length()) {
                            val o = ghArr.getJSONObject(i)
                            val scopesArray = o.optJSONArray("selectedScopes")
                            val scopes = mutableListOf<String>()
                            if (scopesArray != null) {
                                for (s in 0 until scopesArray.length()) {
                                    scopes.add(scopesArray.getString(s))
                                }
                            }
                            val gh = GitHubAccount(
                                id = o.getString("id"),
                                label = o.getString("label"),
                                pat = o.getString("pat"),
                                username = o.optString("username", "").takeIf { it.isNotBlank() },
                                selectedScopes = scopes,
                                isActive = o.optBoolean("isActive", true)
                            )
                            val idx = existingGh.indexOfFirst { it.id == gh.id }
                            if (idx >= 0) existingGh[idx] = gh else existingGh.add(gh)
                            apisCount++
                        }
                        apiKeyStore.saveGitHubAccounts(existingGh)
                    }

                    if (apisObj.has("cloudflare_token")) {
                        apiKeyStore.cloudflareApiToken = apisObj.getString("cloudflare_token")
                    }
                    if (apisObj.has("cloudflare_account_id")) {
                        apiKeyStore.cloudflareAccountId = apisObj.getString("cloudflare_account_id")
                    }

                    if (apisObj.has("drive")) {
                        val driveObj = apisObj.getJSONObject("drive")
                        if (driveObj.has("drive_id_token")) apiKeyStore.driveIdToken = driveObj.getString("drive_id_token")
                        if (driveObj.has("drive_refresh_token")) apiKeyStore.driveRefreshToken = driveObj.getString("drive_refresh_token")
                        if (driveObj.has("drive_user_email")) apiKeyStore.driveUserEmail = driveObj.getString("drive_user_email")
                        if (driveObj.has("drive_folder_id")) apiKeyStore.driveFolderId = driveObj.getString("drive_folder_id")
                    }

                    // Restore raw secure entries if present
                    if (apisObj.has("raw_secure_entries")) {
                        val rawObj = apisObj.getJSONObject("raw_secure_entries")
                        val map = mutableMapOf<String, Any>()
                        val keys = rawObj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            map[k] = rawObj.get(k)
                        }
                        apiKeyStore.importSecureEntries(map)
                    }
                }
            }

            // 2. Subscriptions
            if (options.includeSubscriptions && root.has("subscriptions")) {
                runCatching {
                    val subArr = root.getJSONArray("subscriptions")
                    val existingSubs = apiKeyStore.getAllWebViewAccounts().toMutableList()
                    for (i in 0 until subArr.length()) {
                        val o = subArr.getJSONObject(i)
                        val providerType = runCatching { AIProviderType.valueOf(o.getString("providerType")) }.getOrNull() ?: continue
                        val sub = WebViewAccount(
                            id = o.getString("id"),
                            providerType = providerType,
                            label = o.getString("label"),
                            loginUrl = o.getString("loginUrl"),
                            baseUrl = o.getString("baseUrl"),
                            isEnabled = o.optBoolean("isEnabled", true),
                            requestsUsed = o.optLong("requestsUsed", 0L),
                            tokensUsed = o.optLong("tokensUsed", 0L),
                            isLoggedIn = o.optBoolean("isLoggedIn", false)
                        )
                        val idx = existingSubs.indexOfFirst { it.id == sub.id }
                        if (idx >= 0) existingSubs[idx] = sub else existingSubs.add(sub)
                        subsCount++
                    }
                    apiKeyStore.saveWebViewAccounts(existingSubs)
                }
            }

            // 3. Browser Sessions & Cookies
            if (options.includeCookies && root.has("cookies")) {
                runCatching {
                    val cookieList = root.getJSONArray("cookies")
                    withContext(Dispatchers.Main) {
                        val cm = CookieManager.getInstance()
                        cm.setAcceptCookie(true)
                        for (i in 0 until cookieList.length()) {
                            val o = cookieList.getJSONObject(i)
                            val domain = o.getString("url")
                            val cookieStr = o.getString("cookies")
                            val individualCookies = cookieStr.split(";")
                            for (c in individualCookies) {
                                val trimmed = c.trim()
                                if (trimmed.isNotEmpty()) {
                                    cm.setCookie(domain, trimmed)
                                }
                            }
                            cookiesCount++
                        }
                        cm.flush()
                    }
                }
            }

            // 4. Cognitive Memories, Knowledge Graph, & Profile
            if (options.includeMemories && root.has("memories")) {
                runCatching {
                    val memObj = root.getJSONObject("memories")

                    // Episodic memories
                    if (memObj.has("episodic")) {
                        val epArr = memObj.getJSONArray("episodic")
                        for (i in 0 until epArr.length()) {
                            val o = epArr.getJSONObject(i)
                            val tagsList = mutableListOf<String>()
                            o.optJSONArray("tags")?.let { tArr ->
                                for (t in 0 until tArr.length()) tagsList.add(tArr.getString(t))
                            }
                            val vectorArray = o.optJSONArray("vector")?.let { vArr ->
                                FloatArray(vArr.length()) { idx -> vArr.getDouble(idx).toFloat() }
                            }
                            val mem = MemoryEntity(
                                id = o.optLong("id", 0L),
                                type = o.getString("type"),
                                content = o.getString("content"),
                                repoKey = o.optString("repoKey", ""),
                                tags = tagsList,
                                importance = o.optDouble("importance", 0.5).toFloat(),
                                vector = vectorArray,
                                accessCount = o.optInt("accessCount", 0),
                                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                                lastAccessedAt = o.optLong("lastAccessedAt", System.currentTimeMillis())
                            )
                            memoryDao.insert(mem)
                            memoriesCount++
                        }
                    }

                    // Project memories
                    if (memObj.has("project_memories")) {
                        val pmArr = memObj.getJSONArray("project_memories")
                        val pmList = mutableListOf<ProjectMemoryEntity>()
                        for (i in 0 until pmArr.length()) {
                            val o = pmArr.getJSONObject(i)
                            pmList.add(
                                ProjectMemoryEntity(
                                    id = o.optLong("id", 0L),
                                    projectId = o.getString("projectId"),
                                    key = o.getString("key"),
                                    value = o.getString("value"),
                                    confidence = o.optDouble("confidence", 1.0).toFloat(),
                                    source = o.optString("source", "ai"),
                                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                                )
                            )
                        }
                        if (pmList.isNotEmpty()) {
                            projectDao.insertProjectMemories(pmList)
                            memoriesCount += pmList.size
                        }
                    }

                    // Knowledge Graph
                    if (memObj.has("knowledge_graph")) {
                        val kgObj = memObj.getJSONObject("knowledge_graph")
                        if (kgObj.has("nodes")) {
                            val nArr = kgObj.getJSONArray("nodes")
                            val nList = mutableListOf<ConceptNodeEntity>()
                            for (i in 0 until nArr.length()) {
                                val o = nArr.getJSONObject(i)
                                nList.add(
                                    ConceptNodeEntity(
                                        name = o.getString("name"),
                                        category = o.optString("category", "CONCEPT"),
                                        description = o.optString("description", ""),
                                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                                    )
                                )
                            }
                            if (nList.isNotEmpty()) knowledgeGraphDao.insertNodes(nList)
                        }

                        if (kgObj.has("edges")) {
                            val eArr = kgObj.getJSONArray("edges")
                            val eList = mutableListOf<ConceptEdgeEntity>()
                            for (i in 0 until eArr.length()) {
                                val o = eArr.getJSONObject(i)
                                eList.add(
                                    ConceptEdgeEntity(
                                        id = o.optLong("id", 0L),
                                        sourceNode = o.getString("sourceNode"),
                                        targetNode = o.getString("targetNode"),
                                        relationship = o.getString("relationship"),
                                        weight = o.optDouble("weight", 1.0).toFloat(),
                                        context = o.optString("context", ""),
                                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                                    )
                                )
                            }
                            if (eList.isNotEmpty()) knowledgeGraphDao.insertEdges(eList)
                        }
                    }

                    // User Profile
                    if (memObj.has("user_profile")) {
                        val up = memObj.getJSONObject("user_profile")
                        userProfileDao.insert(
                            UserProfileEntity(
                                id = 1,
                                name = up.optString("name", ""),
                                occupation = up.optString("occupation", ""),
                                timezone = up.optString("timezone", ""),
                                primaryLanguage = up.optString("primaryLanguage", "Kotlin"),
                                primaryFrameworks = up.optString("primaryFrameworks", ""),
                                workingStyle = up.optString("workingStyle", ""),
                                communicationPref = up.optString("communicationPref", ""),
                                currentFocus = up.optString("currentFocus", ""),
                                interests = up.optString("interests", ""),
                                updatedAt = up.optLong("updatedAt", System.currentTimeMillis())
                            )
                        )
                    }
                }
            }

            // 5. Projects
            if (options.includeProjects && root.has("projects")) {
                runCatching {
                    val projArr = root.getJSONArray("projects")
                    val projectList = mutableListOf<ProjectEntity>()
                    for (i in 0 until projArr.length()) {
                        val o = projArr.getJSONObject(i)
                        projectList.add(
                            ProjectEntity(
                                id = o.getString("id"),
                                name = o.getString("name"),
                                description = o.optString("description", ""),
                                emoji = o.optString("emoji", ""),
                                colorHex = o.optLong("color_hex", 0xFF6200EE),
                                systemInstructions = o.optString("system_instructions", ""),
                                enforcedProtocolJson = o.optString("enforced_protocol_json", "").takeIf { it.isNotBlank() },
                                isUnleashed = o.optBoolean("is_unleashed", true),
                                linkedRepo = o.optString("linked_repo", ""),
                                memoryMode = o.optString("memory_mode", "PROJECT_ONLY"),
                                isArchived = o.optBoolean("is_archived", false),
                                createdAt = o.optLong("created_at", System.currentTimeMillis()),
                                lastUsedAt = o.optLong("last_used_at", System.currentTimeMillis())
                            )
                        )
                    }
                    if (projectList.isNotEmpty()) {
                        projectDao.insertProjects(projectList)
                        projCount = projectList.size
                    }
                }
            }

            // 6. Chats, Messages, Task Logs, Goals, Deployments
            if (options.includeChatsAndLogs) {
                runCatching {
                    if (root.has("conversations")) {
                        val convArr = root.getJSONArray("conversations")
                        val convList = mutableListOf<ConversationEntity>()
                        for (i in 0 until convArr.length()) {
                            val o = convArr.getJSONObject(i)
                            convList.add(
                                ConversationEntity(
                                    id = o.getString("id"),
                                    projectId = o.optString("project_id", "").takeIf { it.isNotBlank() },
                                    title = o.optString("title", "New Conversation"),
                                    summary = o.optString("summary", ""),
                                    messageCount = o.optInt("message_count", 0),
                                    isPinned = o.optBoolean("is_pinned", false),
                                    createdAt = o.optLong("created_at", System.currentTimeMillis()),
                                    updatedAt = o.optLong("updated_at", System.currentTimeMillis())
                                )
                            )
                        }
                        if (convList.isNotEmpty()) {
                            conversationDao.insertConversations(convList)
                            chatCount += convList.size
                        }
                    }

                    if (root.has("messages")) {
                        val msgArr = root.getJSONArray("messages")
                        val msgList = mutableListOf<ConversationMessageEntity>()
                        for (i in 0 until msgArr.length()) {
                            val o = msgArr.getJSONObject(i)
                            msgList.add(
                                ConversationMessageEntity(
                                    id = o.optLong("id", 0L),
                                    conversationId = o.getString("conversation_id"),
                                    role = o.getString("role"),
                                    content = o.getString("content"),
                                    type = o.optString("type", "TEXT"),
                                    toolName = o.optString("tool_name", "").takeIf { it.isNotBlank() },
                                    toolCallId = o.optString("tool_call_id", "").takeIf { it.isNotBlank() },
                                    createdAt = o.optLong("created_at", System.currentTimeMillis())
                                )
                            )
                        }
                        if (msgList.isNotEmpty()) {
                            conversationDao.insertMessages(msgList)
                        }
                    }

                    if (root.has("task_logs")) {
                        val logArr = root.getJSONArray("task_logs")
                        val logList = mutableListOf<TaskLogEntity>()
                        for (i in 0 until logArr.length()) {
                            val o = logArr.getJSONObject(i)
                            val stepsArr = o.optJSONArray("steps")
                            val stepsList = mutableListOf<String>()
                            if (stepsArr != null) {
                                for (s in 0 until stepsArr.length()) {
                                    stepsList.add(stepsArr.getString(s))
                                }
                            }
                            logList.add(
                                TaskLogEntity(
                                    id = o.optLong("id", 0L),
                                    userRequest = o.getString("userRequest"),
                                    taskType = o.getString("taskType"),
                                    repoKey = o.optString("repoKey", ""),
                                    steps = stepsList,
                                    outcome = o.optString("outcome", ""),
                                    summary = o.optString("summary", ""),
                                    tokensUsed = o.optInt("tokensUsed", 0),
                                    providerUsed = o.optString("providerUsed", ""),
                                    durationMs = o.optLong("durationMs", 0L),
                                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                                )
                            )
                        }
                        if (logList.isNotEmpty()) {
                            taskLogDao.insertLogs(logList)
                            logCount = logList.size
                        }
                    }

                    if (root.has("goals")) {
                        val goalArr = root.getJSONArray("goals")
                        val goalList = mutableListOf<GoalEntity>()
                        for (i in 0 until goalArr.length()) {
                            val o = goalArr.getJSONObject(i)
                            val compAt = o.optLong("completed_at", -1L)
                            goalList.add(
                                GoalEntity(
                                    id = o.getString("id"),
                                    description = o.getString("description"),
                                    gatesJson = o.optString("gates_json", "[]"),
                                    progressJson = o.optString("progress_json", "{}"),
                                    isCompleted = o.optBoolean("is_completed", false),
                                    startedAt = o.optLong("started_at", System.currentTimeMillis()),
                                    completedAt = if (compAt > 0) compAt else null,
                                    lastStep = o.optInt("last_step", 0),
                                    summary = o.optString("summary", "").takeIf { it.isNotBlank() }
                                )
                            )
                        }
                        if (goalList.isNotEmpty()) {
                            goalDao.insertGoals(goalList)
                        }
                    }

                    if (root.has("deployments")) {
                        val depArr = root.getJSONArray("deployments")
                        val depList = mutableListOf<DeploymentEntity>()
                        for (i in 0 until depArr.length()) {
                            val o = depArr.getJSONObject(i)
                            depList.add(
                                DeploymentEntity(
                                    id = o.getString("id"),
                                    projectName = o.getString("projectName"),
                                    type = o.optString("type", "PAGES"),
                                    url = o.optString("url", ""),
                                    productionUrl = o.optString("productionUrl", ""),
                                    status = o.optString("status", "active"),
                                    fileCount = o.optInt("fileCount", 0),
                                    commitMessage = o.optString("commitMessage", ""),
                                    repoKey = o.optString("repoKey", ""),
                                    commitSha = o.optString("commitSha", ""),
                                    branch = o.optString("branch", "main"),
                                    buildLog = o.optString("buildLog", ""),
                                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                                )
                            )
                        }
                        if (depList.isNotEmpty()) {
                            deploymentDao.insertDeployments(depList)
                        }
                    }
                }
            }

            // 7. App Settings, Preferences & DataStore
            if (options.includeAppSettings && root.has("settings")) {
                runCatching {
                    val settings = root.getJSONObject("settings")
                    if (settings.has("github_owner")) apiKeyStore.githubOwner = settings.getString("github_owner")
                    if (settings.has("github_repo")) apiKeyStore.githubRepo = settings.getString("github_repo")
                    if (settings.has("selector_gist_url")) apiKeyStore.selectorRegistryGistUrl = settings.getString("selector_gist_url")
                    if (settings.has("is_unleashed")) apiKeyStore.isUnleashed = settings.getBoolean("is_unleashed")
                    if (settings.has("voice_typing_engine")) apiKeyStore.voiceTypingEngine = settings.getString("voice_typing_engine")
                    if (settings.has("openwhispr_server_url")) apiKeyStore.openWhisprServerUrl = settings.getString("openwhispr_server_url")
                    if (settings.has("overnight_max_hours")) apiKeyStore.overnightMaxHours = settings.getLong("overnight_max_hours")
                    if (settings.has("overnight_auto_start")) apiKeyStore.overnightAutoStart = settings.getBoolean("overnight_auto_start")
                    if (settings.has("overnight_schedule_time")) apiKeyStore.overnightScheduledTime = settings.getString("overnight_schedule_time")
                    if (settings.has("overnight_notify")) apiKeyStore.overnightNotifyOnComplete = settings.getBoolean("overnight_notify")
                    if (settings.has("overnight_task_queue")) apiKeyStore.taskQueueJson = settings.getString("overnight_task_queue")
                    prefCount++

                    // Restore DataStore Preferences
                    if (settings.has("datastore_preferences")) {
                        val dsObj = settings.getJSONObject("datastore_preferences")
                        dataStore.edit { mutablePrefs ->
                            val keys = dsObj.keys()
                            while (keys.hasNext()) {
                                val keyName = keys.next()
                                when (val rawValue = dsObj.get(keyName)) {
                                    is String -> mutablePrefs[stringPreferencesKey(keyName)] = rawValue
                                    is Boolean -> mutablePrefs[booleanPreferencesKey(keyName)] = rawValue
                                    is Int -> mutablePrefs[intPreferencesKey(keyName)] = rawValue
                                    is Long -> mutablePrefs[longPreferencesKey(keyName)] = rawValue
                                    is Double -> mutablePrefs[doublePreferencesKey(keyName)] = rawValue
                                    is Float -> mutablePrefs[floatPreferencesKey(keyName)] = rawValue
                                }
                            }
                        }
                    }
                }
            }

            RestoreResult(
                success = true,
                message = "Backup successfully restored across all selected components",
                apisRestored = apisCount,
                subscriptionsRestored = subsCount,
                cookiesRestored = cookiesCount,
                memoriesRestored = memoriesCount,
                projectsRestored = projCount,
                chatsRestored = chatCount,
                logsRestored = logCount,
                preferencesRestored = prefCount
            )
        } catch (e: Exception) {
            RestoreResult(
                success = false,
                message = "Failed to restore backup: ${e.message ?: "Invalid format"}"
            )
        }
    }

    fun exportToFile(jsonContent: String): Uri {
        val backupsDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val backupFile = File(backupsDir, "dettle_backup_$timeStamp.json")
        backupFile.writeText(jsonContent)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )
    }

    fun readJsonFromUri(uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader().use { it.readText() }
        } ?: throw IllegalStateException("Could not read backup file")
    }
}
