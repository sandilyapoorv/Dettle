package com.dettle.app.data.webview

import android.content.Context
import android.util.Log
import com.dettle.app.data.settings.ApiKeyStore
import com.dettle.app.domain.model.AIModel
import com.dettle.app.domain.model.AIProviderType
import com.dettle.app.domain.model.WebViewAccount
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "WebViewPool"

/**
 * Manages the pool of hidden WebViews.
 * Upgraded in v1.0.6:
 *  - Fully lazy on-demand initialization to guarantee 120 FPS performance and avoid CPU/RAM thrashing.
 *  - Dynamic multi-account support: user can have multiple accounts per subscription provider
 *    (e.g., 5 ChatGPT accounts, 3 Claude accounts) or custom web providers.
 *  - Dynamic removal/deletion of any subscription provider.
 */
@Singleton
class WebViewPool @Inject constructor(
    @ApplicationContext private val context: Context,
    private val selectorRegistry: SelectorRegistry,
    private val keyStore: ApiKeyStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Keyed by unique account ID so users can run multiple accounts per provider
    private val sessions = mutableMapOf<String, WebViewSession>()
    private val _needsReauthFor = MutableStateFlow<String?>(null) // Account ID
    val needsReauthFor: StateFlow<String?> = _needsReauthFor.asStateFlow()

    private var isInitialized = false

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Initializes the selector registry.
     * Note: Sessions are strictly lazy and created on-demand to ensure 120 FPS
     * and prevent 8 concurrent Chromium processes from bogging down the Android runtime.
     */
    fun initialize(gistUrl: String? = null) {
        if (isInitialized) return
        isInitialized = true

        scope.launch {
            gistUrl?.let { selectorRegistry.fetch(it) }
            Log.d(TAG, "WebViewPool initialized (lazy on-demand mode active for 120 FPS)")
        }
    }

    /**
     * Returns the next available WebView session for the waterfall.
     */
    fun getNextAvailable(): WebViewSession? {
        val accounts = keyStore.getAllWebViewAccounts().filter { it.isEnabled }
        for (acc in accounts) {
            val session = sessions[acc.id]
            if (session != null && session.isAvailable()) {
                return session
            }
        }
        return null
    }

    /**
     * Returns a specific account session if available.
     */
    fun getSession(accountId: String): WebViewSession? =
        sessions[accountId]?.takeIf { it.isAvailable() }

    /**
     * Returns an existing session or creates and initializes one on-demand on the main thread.
     */
    fun getOrCreateSession(account: WebViewAccount): WebViewSession {
        var session = sessions[account.id]
        if (session == null) {
            session = createSessionForAccount(account)
            sessions[account.id] = session
        }
        return session
    }

    /**
     * Overload for AIProviderType for backwards compatibility.
     */
    fun getOrCreateSession(providerType: AIProviderType): WebViewSession {
        val accounts = keyStore.getAllWebViewAccounts()
        val account = accounts.firstOrNull { it.providerType == providerType }
            ?: WebViewAccount(
                providerType = providerType,
                label = "${providerType.displayName} Account 1"
            ).also { keyStore.addWebViewAccount(it) }
        return getOrCreateSession(account)
    }

    /**
     * Destroys an active session when an account is removed or deleted.
     */
    fun destroySession(accountId: String) {
        sessions[accountId]?.destroy()
        sessions.remove(accountId)
        if (_needsReauthFor.value == accountId) {
            _needsReauthFor.value = null
        }
        Log.d(TAG, "Destroyed session for account $accountId")
    }

    /**
     * Returns ALL active sessions.
     */
    fun getAllSessions(): List<WebViewSession> = sessions.values.toList()

    fun clearReauthFlag() {
        _needsReauthFor.value = null
    }

    fun refreshSelectors(gistUrl: String) {
        scope.launch { selectorRegistry.fetch(gistUrl) }
    }

    fun destroy() {
        sessions.values.forEach { it.destroy() }
        sessions.clear()
        isInitialized = false
        Log.d(TAG, "Pool destroyed")
    }

    // ── Status ─────────────────────────────────────────────────────────────

    data class PoolStatus(
        val account: WebViewAccount,
        val isAvailable: Boolean,
        val isLoggedIn: Boolean,
        val needsReauth: Boolean
    ) {
        val providerType: AIProviderType get() = account.providerType
    }

    fun getPoolStatus(): List<PoolStatus> {
        val accounts = keyStore.getAllWebViewAccounts()
        return accounts.map { account ->
            val session = sessions[account.id]
            PoolStatus(
                account = account,
                isAvailable = session?.isAvailable() ?: false,
                isLoggedIn = session?.isLoggedIn() ?: account.isLoggedIn,
                needsReauth = _needsReauthFor.value == account.id
            )
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private fun createSessionForAccount(account: WebViewAccount): WebViewSession {
        val selectors = selectorRegistry.get(account.providerType.name.lowercase())
        val model = AIModel(
            provider = account.providerType,
            modelId = "webview-${account.id}",
            displayName = account.label,
            contextWindow = 128_000,
            dailyTokenLimit = -1,
            dailyRequestLimit = -1,
            rpmLimit = -1,
            supportsToolCalling = false
        )

        val session = WebViewSession(
            context = context,
            providerType = account.providerType,
            model = model,
            selectors = selectors,
            onNeedsReauth = {
                Log.w(TAG, "Session needs re-auth: ${account.label} (${account.id})")
                _needsReauthFor.value = account.id
            }
        )

        Log.d(TAG, "Created on-demand session for ${account.label} (${account.providerType.displayName})")
        return session
    }
}
