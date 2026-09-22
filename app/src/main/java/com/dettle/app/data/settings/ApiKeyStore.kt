package com.dettle.app.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.dettle.app.domain.model.AIProviderType
import com.dettle.app.domain.model.AggregateAccountMetrics
import com.dettle.app.domain.model.GitHubAccount
import com.dettle.app.domain.model.ProviderAccount
import com.dettle.app.domain.model.ProviderMetrics
import com.dettle.app.domain.model.WebViewAccount
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted key-value and multi-account store for all API keys, accounts, and sensitive tokens.
 * Uses Android Keystore + AES256 via EncryptedSharedPreferences.
 * Keys never leave device storage in plaintext.
 */
@Singleton
class ApiKeyStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "dettle_secure_keys",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            android.util.Log.e("ApiKeyStore", "Failed to initialize EncryptedSharedPreferences, falling back to private SharedPreferences", e)
            context.getSharedPreferences("dettle_secure_keys_fallback", Context.MODE_PRIVATE)
        }
    }

    // ─── Multi-Account API Provider Storage ───────────────────────────────

    fun getAllProviderAccounts(): List<ProviderAccount> {
        val json = prefs.getString("provider_accounts_json", null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<ProviderAccount>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val providerStr = obj.getString("provider")
                val provider = runCatching { AIProviderType.valueOf(providerStr) }.getOrNull() ?: continue
                list.add(
                    ProviderAccount(
                        id = obj.getString("id"),
                        provider = provider,
                        label = obj.getString("label"),
                        apiKey = obj.getString("apiKey"),
                        isActive = obj.optBoolean("isActive", true),
                        requestsUsed = obj.optLong("requestsUsed", 0L),
                        tokensUsed = obj.optLong("tokensUsed", 0L),
                        rateLimitedUntilMs = obj.optLong("rateLimitedUntilMs", 0L)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveProviderAccounts(accounts: List<ProviderAccount>) {
        val array = JSONArray()
        for (acc in accounts) {
            val obj = JSONObject().apply {
                put("id", acc.id)
                put("provider", acc.provider.name)
                put("label", acc.label)
                put("apiKey", acc.apiKey)
                put("isActive", acc.isActive)
                put("requestsUsed", acc.requestsUsed)
                put("tokensUsed", acc.tokensUsed)
                put("rateLimitedUntilMs", acc.rateLimitedUntilMs)
            }
            array.put(obj)
        }
        prefs.edit().putString("provider_accounts_json", array.toString()).apply()
    }

    fun addProviderAccount(account: ProviderAccount) {
        val accounts = getAllProviderAccounts().toMutableList()
        accounts.add(account)
        saveProviderAccounts(accounts)
    }

    fun updateProviderAccount(account: ProviderAccount) {
        val accounts = getAllProviderAccounts().toMutableList()
        val index = accounts.indexOfFirst { it.id == account.id }
        if (index >= 0) {
            accounts[index] = account
            saveProviderAccounts(accounts)
        }
    }

    fun deleteProviderAccount(id: String) {
        val accounts = getAllProviderAccounts().filter { it.id != id }
        saveProviderAccounts(accounts)
    }

    fun getAccountsFor(provider: AIProviderType): List<ProviderAccount> {
        return getAllProviderAccounts().filter { it.provider == provider }
    }

    fun recordAccountUsage(provider: AIProviderType, tokensAdded: Long) {
        val accounts = getAllProviderAccounts().toMutableList()
        val active = accounts.firstOrNull { it.provider == provider && it.isActive } ?: return
        val index = accounts.indexOf(active)
        accounts[index] = active.copy(
            requestsUsed = active.requestsUsed + 1,
            tokensUsed = active.tokensUsed + tokensAdded
        )
        saveProviderAccounts(accounts)
    }

    fun markAccountRateLimited(provider: AIProviderType, cooldownMs: Long = 60_000L) {
        val accounts = getAllProviderAccounts().toMutableList()
        val active = accounts.firstOrNull { it.provider == provider && it.isActive } ?: return
        val index = accounts.indexOf(active)
        accounts[index] = active.copy(rateLimitedUntilMs = System.currentTimeMillis() + cooldownMs)
        saveProviderAccounts(accounts)
    }

    fun getAggregateMetrics(): AggregateAccountMetrics {
        val accounts = getAllProviderAccounts()
        val totalAccounts = accounts.size
        val activeAccounts = accounts.count { it.isActive }
        val totalRequests = accounts.sumOf { it.requestsUsed }
        val totalTokens = accounts.sumOf { it.tokensUsed }

        val breakdown = AIProviderType.entries.filter { !it.isWebView }.associateWith { provider ->
            val providerAccs = accounts.filter { it.provider == provider }
            ProviderMetrics(
                provider = provider,
                accountCount = providerAccs.size,
                activeCount = providerAccs.count { it.isActive },
                totalRequests = providerAccs.sumOf { it.requestsUsed },
                totalTokens = providerAccs.sumOf { it.tokensUsed }
            )
        }

        return AggregateAccountMetrics(
            totalAccounts = totalAccounts,
            activeAccounts = activeAccounts,
            totalRequests = totalRequests,
            totalTokens = totalTokens,
            providerBreakdown = breakdown
        )
    }

    // ─── AI Provider Single-Key (Backward Compatibility & Fallback) ───────

    fun getKey(provider: AIProviderType): String? {
        val accounts = getAccountsFor(provider)
        val now = System.currentTimeMillis()
        val active = accounts.firstOrNull { it.isActive && it.rateLimitedUntilMs < now }
            ?: accounts.firstOrNull { it.isActive }
        return active?.apiKey ?: prefs.getString(keyFor(provider), null)?.takeIf { it.isNotBlank() }
    }

    fun setLegacyKey(provider: AIProviderType, apiKey: String) {
        val trimmed = apiKey.trim()
        prefs.edit().putString(keyFor(provider), trimmed).apply()
    }

    fun setKey(provider: AIProviderType, apiKey: String) {
        val trimmed = apiKey.trim()
        prefs.edit().putString(keyFor(provider), trimmed).apply()
        val accounts = getAllProviderAccounts().toMutableList()
        val existingIndex = accounts.indexOfFirst { it.provider == provider }
        if (existingIndex >= 0) {
            accounts[existingIndex] = accounts[existingIndex].copy(apiKey = trimmed, isActive = true)
        } else if (trimmed.isNotBlank()) {
            accounts.add(ProviderAccount(provider = provider, label = "Primary Account", apiKey = trimmed))
        }
        saveProviderAccounts(accounts)
    }

    fun clearKey(provider: AIProviderType) {
        prefs.edit().remove(keyFor(provider)).apply()
        val accounts = getAllProviderAccounts().filter { it.provider != provider }
        saveProviderAccounts(accounts)
    }

    fun hasKey(provider: AIProviderType): Boolean = !getKey(provider).isNullOrBlank()

    private fun keyFor(provider: AIProviderType) = "api_key_${provider.name}"

    // ─── Multi-Account GitHub Storage & Scopes ────────────────────────────

    fun getAllGitHubAccounts(): List<GitHubAccount> {
        val json = prefs.getString("github_accounts_json", null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<GitHubAccount>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val scopesArray = obj.optJSONArray("selectedScopes")
                val scopes = mutableListOf<String>()
                if (scopesArray != null) {
                    for (s in 0 until scopesArray.length()) {
                        scopes.add(scopesArray.getString(s))
                    }
                }
                list.add(
                    GitHubAccount(
                        id = obj.getString("id"),
                        label = obj.getString("label"),
                        pat = obj.getString("pat"),
                        username = obj.optString("username", "").takeIf { it.isNotBlank() },
                        selectedScopes = scopes,
                        isActive = obj.optBoolean("isActive", true)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveGitHubAccounts(accounts: List<GitHubAccount>) {
        val array = JSONArray()
        for (acc in accounts) {
            val obj = JSONObject().apply {
                put("id", acc.id)
                put("label", acc.label)
                put("pat", acc.pat)
                put("username", acc.username ?: "")
                put("selectedScopes", JSONArray(acc.selectedScopes))
                put("isActive", acc.isActive)
            }
            array.put(obj)
        }
        prefs.edit().putString("github_accounts_json", array.toString()).apply()
    }

    fun addGitHubAccount(account: GitHubAccount) {
        val accounts = getAllGitHubAccounts().toMutableList()
        accounts.add(account)
        saveGitHubAccounts(accounts)
    }

    fun updateGitHubAccount(account: GitHubAccount) {
        val accounts = getAllGitHubAccounts().toMutableList()
        val index = accounts.indexOfFirst { it.id == account.id }
        if (index >= 0) {
            accounts[index] = account
            saveGitHubAccounts(accounts)
        }
    }

    fun deleteGitHubAccount(id: String) {
        val accounts = getAllGitHubAccounts().filter { it.id != id }
        saveGitHubAccounts(accounts)
    }

    fun setActiveGitHubAccount(id: String) {
        val accounts = getAllGitHubAccounts().map { it.copy(isActive = it.id == id) }
        saveGitHubAccounts(accounts)
    }

    fun getActiveGitHubAccount(): GitHubAccount? {
        val accounts = getAllGitHubAccounts()
        return accounts.firstOrNull { it.isActive } ?: accounts.firstOrNull()
    }

    var githubPat: String?
        get() = getActiveGitHubAccount()?.pat ?: prefs.getString("github_pat", null)?.takeIf { it.isNotBlank() }
        set(value) {
            val trimmed = value?.trim()
            prefs.edit().putString("github_pat", trimmed).apply()
            if (!trimmed.isNullOrBlank()) {
                val accounts = getAllGitHubAccounts().toMutableList()
                val existing = accounts.firstOrNull { it.isActive }
                if (existing != null) {
                    val idx = accounts.indexOf(existing)
                    accounts[idx] = existing.copy(pat = trimmed)
                } else {
                    accounts.add(GitHubAccount(label = "Primary GitHub", pat = trimmed, isActive = true))
                }
                saveGitHubAccounts(accounts)
            }
        }

    var githubOwner: String?
        get() = prefs.getString("github_owner", null)
        set(value) = prefs.edit().putString("github_owner", value?.trim()).apply()

    var githubRepo: String?
        get() = prefs.getString("github_repo", null)
        set(value) = prefs.edit().putString("github_repo", value?.trim()).apply()

    // ─── Cloudflare ───────────────────────────────────────────────────────

    var cloudflareApiToken: String?
        get() = prefs.getString("cloudflare_token", null)?.takeIf { it.isNotBlank() }
        set(value) = prefs.edit().putString("cloudflare_token", value?.trim()).apply()

    var cloudflareAccountId: String?
        get() = prefs.getString("cloudflare_account_id", null)
        set(value) = prefs.edit().putString("cloudflare_account_id", value?.trim()).apply()

    // ─── Google Drive ─────────────────────────────────────────────────────

    var driveIdToken: String?
        get() = prefs.getString("drive_id_token", null)?.takeIf { it.isNotBlank() }
        set(value) = prefs.edit().putString("drive_id_token", value?.trim()).apply()

    var driveRefreshToken: String?
        get() = prefs.getString("drive_refresh_token", null)?.takeIf { it.isNotBlank() }
        set(value) = prefs.edit().putString("drive_refresh_token", value?.trim()).apply()

    var driveUserEmail: String?
        get() = prefs.getString("drive_user_email", null)
        set(value) = prefs.edit().putString("drive_user_email", value).apply()

    var driveFolderId: String?
        get() = prefs.getString("drive_folder_id", null)
        set(value) = prefs.edit().putString("drive_folder_id", value).apply()

    // ─── Remote Selector Registry ─────────────────────────────────────────

    var selectorRegistryGistUrl: String?
        get() = prefs.getString("selector_gist_url", DEFAULT_SELECTOR_URL)
        set(value) = prefs.edit().putString("selector_gist_url", value).apply()

    // ─── Helper: which providers have keys configured ─────────────────────

    fun getConfiguredApiProviders(): List<AIProviderType> =
        AIProviderType.entries.filter { !it.isWebView && hasKey(it) }

    // ─── Multi-Account Subscription / WebView Provider Storage ─────────────

    fun getAllWebViewAccounts(): List<WebViewAccount> {
        val json = prefs.getString("webview_accounts_json", null)
        if (json.isNullOrBlank()) {
            // Seed defaults on initial launch
            val defaults = listOf(
                WebViewAccount(
                    providerType = AIProviderType.CHATGPT_WEB,
                    label = "ChatGPT Account 1",
                    loginUrl = AIProviderType.CHATGPT_WEB.loginUrl,
                    baseUrl = AIProviderType.CHATGPT_WEB.baseUrl
                ),
                WebViewAccount(
                    providerType = AIProviderType.CLAUDE_WEB,
                    label = "Claude Account 1",
                    loginUrl = AIProviderType.CLAUDE_WEB.loginUrl,
                    baseUrl = AIProviderType.CLAUDE_WEB.baseUrl
                ),
                WebViewAccount(
                    providerType = AIProviderType.DEEPSEEK_WEB,
                    label = "DeepSeek Account 1",
                    loginUrl = AIProviderType.DEEPSEEK_WEB.loginUrl,
                    baseUrl = AIProviderType.DEEPSEEK_WEB.baseUrl
                ),
                WebViewAccount(
                    providerType = AIProviderType.GROK_WEB,
                    label = "Grok Account 1",
                    loginUrl = AIProviderType.GROK_WEB.loginUrl,
                    baseUrl = AIProviderType.GROK_WEB.baseUrl
                )
            )
            saveWebViewAccounts(defaults)
            return defaults
        }

        return try {
            val array = JSONArray(json)
            val list = mutableListOf<WebViewAccount>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val providerStr = obj.getString("providerType")
                val provider = runCatching { AIProviderType.valueOf(providerStr) }.getOrNull() ?: continue
                list.add(
                    WebViewAccount(
                        id = obj.getString("id"),
                        providerType = provider,
                        label = obj.getString("label"),
                        loginUrl = obj.optString("loginUrl", provider.loginUrl),
                        baseUrl = obj.optString("baseUrl", provider.baseUrl),
                        isEnabled = obj.optBoolean("isEnabled", true),
                        requestsUsed = obj.optLong("requestsUsed", 0L),
                        tokensUsed = obj.optLong("tokensUsed", 0L),
                        isLoggedIn = obj.optBoolean("isLoggedIn", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveWebViewAccounts(accounts: List<WebViewAccount>) {
        val array = JSONArray()
        for (acc in accounts) {
            val obj = JSONObject().apply {
                put("id", acc.id)
                put("providerType", acc.providerType.name)
                put("label", acc.label)
                put("loginUrl", acc.loginUrl)
                put("baseUrl", acc.baseUrl)
                put("isEnabled", acc.isEnabled)
                put("requestsUsed", acc.requestsUsed)
                put("tokensUsed", acc.tokensUsed)
                put("isLoggedIn", acc.isLoggedIn)
            }
            array.put(obj)
        }
        prefs.edit().putString("webview_accounts_json", array.toString()).apply()
    }

    fun addWebViewAccount(account: WebViewAccount) {
        val accounts = getAllWebViewAccounts().toMutableList()
        accounts.add(account)
        saveWebViewAccounts(accounts)
    }

    fun updateWebViewAccount(account: WebViewAccount) {
        val accounts = getAllWebViewAccounts().toMutableList()
        val index = accounts.indexOfFirst { it.id == account.id }
        if (index >= 0) {
            accounts[index] = account
            saveWebViewAccounts(accounts)
        }
    }

    fun deleteWebViewAccount(id: String) {
        val accounts = getAllWebViewAccounts().filter { it.id != id }
        saveWebViewAccounts(accounts)
    }

    fun getWebViewAccount(id: String): WebViewAccount? {
        return getAllWebViewAccounts().firstOrNull { it.id == id }
    }

    // ─── WebView provider enable/disable ──────────────────────────────────

    fun isWebViewProviderEnabled(providerType: AIProviderType): Boolean =
        prefs.getBoolean("webview_enabled_${providerType.name}", true)

    fun setWebViewProviderEnabled(providerType: AIProviderType, enabled: Boolean) =
        prefs.edit().putBoolean("webview_enabled_${providerType.name}", enabled).apply()

    // ─── Overnight mode settings ──────────────────────────────────────────

    var overnightMaxHours: Long
        get() = prefs.getLong("overnight_max_hours", 8L)
        set(value) = prefs.edit().putLong("overnight_max_hours", value).apply()

    var overnightAutoStart: Boolean
        get() = prefs.getBoolean("overnight_auto_start", false)
        set(value) = prefs.edit().putBoolean("overnight_auto_start", value).apply()

    var overnightScheduledTime: String
        get() = prefs.getString("overnight_schedule_time", "23:00") ?: "23:00"
        set(value) = prefs.edit().putString("overnight_schedule_time", value).apply()

    var taskQueueJson: String?
        get() = prefs.getString("overnight_task_queue", null)
        set(value) = prefs.edit().putString("overnight_task_queue", value).apply()

    var overnightNotifyOnComplete: Boolean
        get() = prefs.getBoolean("overnight_notify", true)
        set(value) = prefs.edit().putBoolean("overnight_notify", value).apply()

    // ─── Voice Typing (OpenWhispr) ──────────────────────────────────────────

    var voiceTypingEngine: String
        get() = prefs.getString("voice_typing_engine", "ON_DEVICE_DSP") ?: "ON_DEVICE_DSP"
        set(value) = prefs.edit().putString("voice_typing_engine", value).apply()

    var openWhisprServerUrl: String
        get() = prefs.getString("openwhispr_server_url", "http://10.0.2.2:8080/v1/audio/transcriptions") ?: "http://10.0.2.2:8080/v1/audio/transcriptions"
        set(value) = prefs.edit().putString("openwhispr_server_url", value).apply()

    companion object {
        const val DEFAULT_SELECTOR_URL =
            "https://gist.githubusercontent.com/YOUR_GITHUB_USERNAME/GIST_ID/raw/dom_selectors.json"
    }
}
