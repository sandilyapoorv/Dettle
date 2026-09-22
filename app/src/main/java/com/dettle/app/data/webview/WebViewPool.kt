package com.dettle.app.data.webview

import android.content.Context
import android.util.Log
import com.dettle.app.data.settings.ApiKeyStore
import com.dettle.app.domain.model.AIModel
import com.dettle.app.domain.model.AIProviderType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "WebViewPool"

/**
 * Manages the pool of hidden WebViews — one per provider.
 *
 * The pool is initialized when the user has at least one WebView provider enabled.
 * Each session is created ONCE and kept alive by AgentForegroundService.
 *
 * Provider priority order (tried in sequence when API providers are exhausted):
 *   ChatGPT → Claude → DeepSeek → Grok → Gemini Web → Kimi → Mistral → Qwen
 *
 * This is Track B: the user's own paid subscriptions provide the compute.
 * Combined: ~25M+ tokens/day from multiple $20/mo subscriptions.
 *
 * WebViews must be created on the Main thread — this class handles that.
 *
 * When a session needs re-auth:
 *   1. Pool flags the session as needs_login
 *   2. AuthVaultScreen shows that session's WebView visibly
 *   3. User logs in normally
 *   4. JS detects loginCheck element → fires onReady
 *   5. Session marks itself as available again
 */
@Singleton
class WebViewPool @Inject constructor(
    @ApplicationContext private val context: Context,
    private val selectorRegistry: SelectorRegistry,
    private val keyStore: ApiKeyStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val sessions = mutableMapOf<AIProviderType, WebViewSession>()
    private val _needsReauthFor = MutableStateFlow<AIProviderType?>(null)
    val needsReauthFor: StateFlow<AIProviderType?> = _needsReauthFor.asStateFlow()

    private var isInitialized = false

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Initialize all enabled WebView sessions.
     * Fetches remote selectors first, then creates sessions on Main thread.
     * Call from AgentForegroundService.onCreate().
     */
    fun initialize(gistUrl: String? = null) {
        if (isInitialized) return
        isInitialized = true

        scope.launch {
            // Fetch remote selectors if URL is configured
            gistUrl?.let { selectorRegistry.fetch(it) }

            // Create a session for each WebView provider the user has enabled
            WEBVIEW_PROVIDERS.forEach { providerType ->
                if (isProviderEnabled(providerType)) {
                    createSession(providerType)
                }
            }

            Log.d(TAG, "Pool initialized with ${sessions.size} sessions: ${sessions.keys.map { it.name }}")
        }
    }

    /**
     * Returns the next available WebView session for the waterfall,
     * or null if no WebView providers are ready.
     */
    fun getNextAvailable(): WebViewSession? {
        return WEBVIEW_PROVIDERS
            .mapNotNull { sessions[it] }
            .firstOrNull { it.isAvailable() }
    }

    /**
     * Returns a specific provider session if available.
     */
    fun getSession(providerType: AIProviderType): WebViewSession? =
        sessions[providerType]?.takeIf { it.isAvailable() }

    /**
     * Returns ALL sessions — available or not — for the status UI.
     */
    fun getAllSessions(): List<WebViewSession> = sessions.values.toList()

    /**
     * Returns the WebView for a provider that needs re-auth,
     * so it can be embedded in AuthVaultScreen.
     */
    fun getSessionNeedingReauth(): WebViewSession? {
        val provider = _needsReauthFor.value ?: return null
        return sessions[provider]
    }

    /**
     * Call after user has successfully logged back in.
     */
    fun clearReauthFlag() {
        _needsReauthFor.value = null
    }

    /**
     * Refresh selectors from remote Gist (e.g. if automation is failing).
     */
    fun refreshSelectors(gistUrl: String) {
        scope.launch { selectorRegistry.fetch(gistUrl) }
    }

    /** Tear down all sessions — call from AgentForegroundService.onDestroy() */
    fun destroy() {
        sessions.values.forEach { it.destroy() }
        sessions.clear()
        isInitialized = false
        Log.d(TAG, "Pool destroyed")
    }

    // ── Status ─────────────────────────────────────────────────────────────

    data class PoolStatus(
        val providerType: AIProviderType,
        val isAvailable: Boolean,
        val isLoggedIn: Boolean,
        val needsReauth: Boolean
    )

    fun getPoolStatus(): List<PoolStatus> = WEBVIEW_PROVIDERS.map { provider ->
        val session = sessions[provider]
        PoolStatus(
            providerType = provider,
            isAvailable = session?.isAvailable() ?: false,
            isLoggedIn = session?.isLoggedIn() ?: false,
            needsReauth = _needsReauthFor.value == provider
        )
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private fun createSession(providerType: AIProviderType) {
        val selectors = selectorRegistry.get(providerType.name.lowercase())
        val model = AIModel(
            id = "webview-${providerType.name.lowercase()}",
            displayName = providerType.displayName,
            providerType = providerType,
            contextWindow = 128_000,
            dailyRequestLimit = -1,    // Unlimited (user's own subscription)
            dailyTokenLimit = -1,
            costPerMToken = 0.0,
            supportsTools = false      // Tools via XML injection, not native API
        )

        val session = WebViewSession(
            context = context,
            providerType = providerType,
            model = model,
            selectors = selectors,
            onNeedsReauth = { provider ->
                Log.w(TAG, "Session needs re-auth: ${provider.name}")
                _needsReauthFor.value = provider
            }
        )

        sessions[providerType] = session
        session.initialize()
        Log.d(TAG, "Created session for ${providerType.displayName}")
    }

    private fun isProviderEnabled(providerType: AIProviderType): Boolean {
        // A WebView provider is enabled if:
        // - User hasn't explicitly disabled it (stored in settings)
        // - In Phase 2 default: all are enabled
        return keyStore.isWebViewProviderEnabled(providerType)
    }

    companion object {
        /** Priority order for WebView provider waterfall */
        val WEBVIEW_PROVIDERS = listOf(
            AIProviderType.CHATGPT_WEB,
            AIProviderType.CLAUDE_WEB,
            AIProviderType.DEEPSEEK_WEB,
            AIProviderType.GROK_WEB,
            AIProviderType.GEMINI_WEB,
            AIProviderType.KIMI_WEB,
            AIProviderType.MISTRAL_WEB,
            AIProviderType.QWEN_WEB
        )
    }
}
