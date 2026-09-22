package com.dettle.app.data.webview

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "SelectorRegistry"

/**
 * Fetches the remote DOM selector map from a GitHub Gist.
 *
 * This is the key to making WebView automation durable:
 * - CSS selectors are NOT hardcoded in Kotlin
 * - They live in a JSON file you control on GitHub Gist
 * - When ChatGPT or Claude updates their UI, you edit the Gist (30 seconds)
 * - App fetches the update on next startup → automation works again
 * - No Play Store update needed
 *
 * Gist URL is configured in Settings → stored in ApiKeyStore.
 * Falls back to hardcoded defaults if fetch fails.
 */
@Singleton
class SelectorRegistry @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json
) {
    private var selectors: Map<String, ProviderSelectors> = defaultSelectors()
    private var lastFetchMs = 0L
    private val cacheTtlMs = 6 * 60 * 60 * 1000L  // Re-fetch every 6 hours

    suspend fun fetch(gistUrl: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (now - lastFetchMs < cacheTtlMs) return@withContext  // Cache valid

        try {
            val request = Request.Builder()
                .url(gistUrl)
                .header("Cache-Control", "no-cache")
                .build()
            val body = client.newCall(request).execute().body?.string() ?: return@withContext
            val remote = json.decodeFromString<RemoteSelectorMap>(body)
            selectors = buildMap {
                remote.chatgpt?.let { put("chatgpt", it) }
                remote.claude?.let { put("claude", it) }
                remote.deepseek?.let { put("deepseek", it) }
                remote.grok?.let { put("grok", it) }
                remote.gemini?.let { put("gemini", it) }
                remote.kimi?.let { put("kimi", it) }
                remote.mistral?.let { put("mistral", it) }
                remote.qwen?.let { put("qwen", it) }
            }
            lastFetchMs = now
            Log.d(TAG, "Selectors updated (version: ${remote.version})")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch selectors, using defaults: ${e.message}")
        }
    }

    fun get(providerId: String): ProviderSelectors =
        selectors[providerId] ?: defaultSelectors()[providerId] ?: ProviderSelectors()

    /** Hardcoded fallback — update these whenever a provider breaks */
    private fun defaultSelectors() = mapOf(
        "chatgpt" to ProviderSelectors(
            input = "#prompt-textarea",
            send = "[data-testid='send-button']",
            response = "[data-message-author-role='assistant'] .markdown",
            streamingIndicator = ".result-streaming",
            loginCheck = "[data-testid='profile-button']"
        ),
        "claude" to ProviderSelectors(
            input = ".ProseMirror[contenteditable='true']",
            send = "button[aria-label='Send Message']",
            response = ".font-claude-message",
            streamingIndicator = "[data-is-streaming='true']",
            loginCheck = "[data-testid='user-menu']"
        ),
        "deepseek" to ProviderSelectors(
            input = "textarea#chat-input",
            send = "div[role='button'][aria-label='Send']",
            response = ".ds-markdown",
            streamingIndicator = ".loading-dots",
            loginCheck = ".user-avatar"
        ),
        "grok" to ProviderSelectors(
            input = "textarea[placeholder*='Ask']",
            send = "button[type='submit']",
            response = ".message-bubble[data-author='grok']",
            streamingIndicator = ".streaming-indicator",
            loginCheck = "[data-testid='UserAvatar-Container']"
        ),
        "gemini" to ProviderSelectors(
            input = "rich-textarea .ql-editor",
            send = "button[aria-label='Send message']",
            response = ".model-response-text",
            streamingIndicator = ".loading-indicator",
            loginCheck = ".gb_A.gb_B"
        ),
        "kimi" to ProviderSelectors(
            input = "textarea.chat-input",
            send = "button.send-button",
            response = ".message-content.assistant",
            streamingIndicator = ".generating",
            loginCheck = ".user-info"
        ),
        "mistral" to ProviderSelectors(
            input = "textarea[placeholder*='message']",
            send = "button[type='submit']",
            response = ".assistant-message",
            streamingIndicator = ".cursor-blink",
            loginCheck = ".user-avatar"
        ),
        "qwen" to ProviderSelectors(
            input = "textarea.ant-input",
            send = "button.send-btn",
            response = ".chat-message-content.assistant",
            streamingIndicator = ".loading",
            loginCheck = ".user-info"
        )
    )
}

@Serializable
data class RemoteSelectorMap(
    val version: String = "",
    val chatgpt: ProviderSelectors? = null,
    val claude: ProviderSelectors? = null,
    val deepseek: ProviderSelectors? = null,
    val grok: ProviderSelectors? = null,
    val gemini: ProviderSelectors? = null,
    val kimi: ProviderSelectors? = null,
    val mistral: ProviderSelectors? = null,
    val qwen: ProviderSelectors? = null
)

@Serializable
data class ProviderSelectors(
    /** Input field CSS selector */
    val input: String = "",
    /** Send button CSS selector */
    val send: String = "",
    /** Response container CSS selector */
    val response: String = "",
    /** Element present while AI is still generating */
    val streamingIndicator: String = "",
    /** Element that only exists when user is logged in */
    val loginCheck: String = ""
)
