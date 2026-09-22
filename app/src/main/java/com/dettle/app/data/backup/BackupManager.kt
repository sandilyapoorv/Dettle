package com.dettle.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.dettle.app.data.db.dao.ConversationDao
import com.dettle.app.data.db.dao.ProjectDao
import com.dettle.app.data.db.dao.TaskLogDao
import com.dettle.app.data.db.entity.ConversationEntity
import com.dettle.app.data.db.entity.ConversationMessageEntity
import com.dettle.app.data.db.entity.ProjectEntity
import com.dettle.app.data.db.entity.TaskLogEntity
import com.dettle.app.data.settings.ApiKeyStore
import com.dettle.app.domain.model.AIProviderType
import com.dettle.app.domain.model.GitHubAccount
import com.dettle.app.domain.model.ProviderAccount
import com.dettle.app.domain.model.WebViewAccount
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
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
    val includeProjects: Boolean = true,
    val includeChatsAndLogs: Boolean = true,
    val includeAppSettings: Boolean = true
) {
    val isAllSelected: Boolean
        get() = includeApis && includeSubscriptions && includeProjects && includeChatsAndLogs && includeAppSettings

    val isNoneSelected: Boolean
        get() = !includeApis && !includeSubscriptions && !includeProjects && !includeChatsAndLogs && !includeAppSettings
}

data class BackupSummary(
    val apiCount: Int = 0,
    val subscriptionCount: Int = 0,
    val projectCount: Int = 0,
    val chatCount: Int = 0,
    val logCount: Int = 0,
    val hasSettings: Boolean = false
)

data class RestoreResult(
    val success: Boolean,
    val message: String,
    val apisRestored: Int = 0,
    val subscriptionsRestored: Int = 0,
    val projectsRestored: Int = 0,
    val chatsRestored: Int = 0,
    val logsRestored: Int = 0
)

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiKeyStore: ApiKeyStore,
    private val conversationDao: ConversationDao,
    private val projectDao: ProjectDao,
    private val taskLogDao: TaskLogDao
) {

    suspend fun getLiveSummary(): BackupSummary = withContext(Dispatchers.IO) {
        val apis = apiKeyStore.getAllProviderAccounts().size + apiKeyStore.getAllGitHubAccounts().size
        val subscriptions = apiKeyStore.getAllWebViewAccounts().size
        val projects = projectDao.getAllProjects().size
        val chats = conversationDao.getAllConversations().size
        val logs = taskLogDao.getAllLogs().size
        BackupSummary(
            apiCount = apis,
            subscriptionCount = subscriptions,
            projectCount = projects,
            chatCount = chats,
            logCount = logs,
            hasSettings = true
        )
    }

    suspend fun createBackupJson(options: BackupOptions): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())
        root.put("app", "Dettle")
        root.put("app_version", "1.0.6")

        // 1. APIs & Keys
        if (options.includeApis) {
            val apisObj = JSONObject()
            val providerArr = JSONArray()
            apiKeyStore.getAllProviderAccounts().forEach { acc ->
                val o = JSONObject().apply {
                    put("id", acc.id)
                    put("provider", acc.provider.name)
                    put("label", acc.label)
                    put("apiKey", acc.apiKey)
                    put("isActive", acc.isActive)
                    put("requestsUsed", acc.requestsUsed)
                    put("tokensUsed", acc.tokensUsed)
                }
                providerArr.put(o)
            }
            apisObj.put("providers", providerArr)

            val ghArr = JSONArray()
            apiKeyStore.getAllGitHubAccounts().forEach { gh ->
                val o = JSONObject().apply {
                    put("id", gh.id)
                    put("label", gh.label)
                    put("pat", gh.pat)
                    put("username", gh.username ?: "")
                    put("selectedScopes", JSONArray(gh.selectedScopes))
                    put("isActive", gh.isActive)
                }
                ghArr.put(o)
            }
            apisObj.put("github", ghArr)

            apiKeyStore.cloudflareApiToken?.let { apisObj.put("cloudflare_token", it) }
            apiKeyStore.cloudflareAccountId?.let { apisObj.put("cloudflare_account_id", it) }

            root.put("apis", apisObj)
        }

        // 2. Subscriptions (ChatGPT, Claude, Grok, etc.)
        if (options.includeSubscriptions) {
            val subArr = JSONArray()
            apiKeyStore.getAllWebViewAccounts().forEach { sub ->
                val o = JSONObject().apply {
                    put("id", sub.id)
                    put("providerType", sub.providerType.name)
                    put("label", sub.label)
                    put("loginUrl", sub.loginUrl)
                    put("baseUrl", sub.baseUrl)
                    put("isEnabled", sub.isEnabled)
                    put("requestsUsed", sub.requestsUsed)
                    put("tokensUsed", sub.tokensUsed)
                    put("isLoggedIn", sub.isLoggedIn)
                }
                subArr.put(o)
            }
            root.put("subscriptions", subArr)
        }

        // 3. Projects
        if (options.includeProjects) {
            val projArr = JSONArray()
            projectDao.getAllProjects().forEach { p ->
                val o = JSONObject().apply {
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
                }
                projArr.put(o)
            }
            root.put("projects", projArr)
        }

        // 4. Chats & Logs
        if (options.includeChatsAndLogs) {
            val convArr = JSONArray()
            conversationDao.getAllConversations().forEach { c ->
                val o = JSONObject().apply {
                    put("id", c.id)
                    put("project_id", c.projectId ?: "")
                    put("title", c.title)
                    put("summary", c.summary)
                    put("message_count", c.messageCount)
                    put("is_pinned", c.isPinned)
                    put("created_at", c.createdAt)
                    put("updated_at", c.updatedAt)
                }
                convArr.put(o)
            }
            root.put("conversations", convArr)

            val msgArr = JSONArray()
            conversationDao.getAllMessages().forEach { m ->
                val o = JSONObject().apply {
                    put("id", m.id)
                    put("conversation_id", m.conversationId)
                    put("role", m.role)
                    put("content", m.content)
                    put("type", m.type)
                    put("tool_name", m.toolName ?: "")
                    put("tool_call_id", m.toolCallId ?: "")
                    put("created_at", m.createdAt)
                }
                msgArr.put(o)
            }
            root.put("messages", msgArr)

            val logArr = JSONArray()
            taskLogDao.getAllLogs().forEach { l ->
                val o = JSONObject().apply {
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
                }
                logArr.put(o)
            }
            root.put("task_logs", logArr)
        }

        // 5. App Settings
        if (options.includeAppSettings) {
            val settingsObj = JSONObject().apply {
                put("github_owner", apiKeyStore.githubOwner ?: "")
                put("github_repo", apiKeyStore.githubRepo ?: "")
                put("selector_gist_url", apiKeyStore.selectorRegistryGistUrl ?: "")
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
            var projCount = 0
            var chatCount = 0
            var logCount = 0

            // 1. APIs
            if (options.includeApis && root.has("apis")) {
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
                            tokensUsed = o.optLong("tokensUsed", 0L)
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
            }

            // 2. Subscriptions
            if (options.includeSubscriptions && root.has("subscriptions")) {
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

            // 3. Projects
            if (options.includeProjects && root.has("projects")) {
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

            // 4. Chats & Logs
            if (options.includeChatsAndLogs) {
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
            }

            // 5. App Settings
            if (options.includeAppSettings && root.has("settings")) {
                val settings = root.getJSONObject("settings")
                if (settings.has("github_owner")) apiKeyStore.githubOwner = settings.getString("github_owner")
                if (settings.has("github_repo")) apiKeyStore.githubRepo = settings.getString("github_repo")
                if (settings.has("selector_gist_url")) apiKeyStore.selectorRegistryGistUrl = settings.getString("selector_gist_url")
            }

            RestoreResult(
                success = true,
                message = "Backup successfully restored",
                apisRestored = apisCount,
                subscriptionsRestored = subsCount,
                projectsRestored = projCount,
                chatsRestored = chatCount,
                logsRestored = logCount
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
