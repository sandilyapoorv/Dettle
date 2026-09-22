package com.dettle.app.data.webview

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.dettle.app.data.api.AIProvider
import com.dettle.app.data.api.StreamChunk
import com.dettle.app.domain.model.AIModel
import com.dettle.app.domain.model.AIProviderType
import com.dettle.app.domain.model.ApiMessage
import com.dettle.app.domain.model.Tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "WebViewSession"
private const val RESPONSE_TIMEOUT_MS = 120_000L   // 2 minutes max wait per response
private const val PAGE_LOAD_TIMEOUT_MS = 20_000L   // 20s for initial page load

/**
 * A single hidden WebView session for one AI provider (ChatGPT, Claude, etc.).
 *
 * The WebView is created INVISIBLE (visibility = GONE) but fully active.
 * Cookies persist across app restarts via CookieManager with cookie persistence enabled.
 *
 * Lifecycle:
 *  - Created once per provider in WebViewPool
 *  - Kept alive by AgentForegroundService
 *  - If session expires (detected by JS loginCheck), onNeedsReauth fires
 *    and the pool shows this WebView to the user for manual re-login
 *
 * Tool calling:
 *  - Injects a persona prompt that forces AI to output:
 *    <tool_call>{"name":"...","args":{...}}</tool_call>
 *  - JS MutationObserver detects this tag and calls AndroidBridge.onToolCallDetected()
 */
class WebViewSession(
    private val context: Context,
    val providerType: AIProviderType,
    val model: AIModel,
    private val selectors: ProviderSelectors,
    val onNeedsReauth: (AIProviderType) -> Unit
) : AIProvider {

    private var _webView: WebView? = null
    private val bridge = DOMAutomationBridge(providerType.name)
    private var isPageReady = false
    private var _isRateLimited = false
    private var rateLimitResetMs = 0L

    // Channel for coordinating responses (one at a time per session)
    private val responseChannel = Channel<BridgeEvent>(Channel.UNLIMITED)

    fun destroy() {
        _webView?.destroy()
        _webView = null
    }

    val webView: WebView get() = _webView ?: createWebView()

    override val model: AIModel get() = this.model

    // ── AIProvider implementation ──────────────────────────────────────────

    override fun isAvailable(): Boolean {
        if (_isRateLimited && System.currentTimeMillis() > rateLimitResetMs) _isRateLimited = false
        return !_isRateLimited && isPageReady
    }

    override suspend fun chat(
        messages: List<ApiMessage>,
        tools: List<Tool>,
        systemPrompt: String?,
        maxTokens: Int
    ): Flow<StreamChunk> = flow {

        val prompt = buildPromptForWebView(messages, tools, systemPrompt)

        val result = withTimeoutOrNull(RESPONSE_TIMEOUT_MS) {
            sendPromptAndCollect(prompt)
        }

        if (result == null) {
            emit(StreamChunk.Error("Timeout waiting for response from ${providerType.displayName}"))
            return@flow
        }

        for (chunk in result) {
            emit(chunk)
        }
    }

    // ── Session management ─────────────────────────────────────────────────

    /**
     * Load the provider URL and inject automation on page ready.
     * Must be called on the main thread.
     */
    fun initialize() {
        val wv = webView  // Creates if needed
        wv.loadUrl(providerType.baseUrl)
        Log.d(TAG, "Loading ${providerType.displayName}: ${providerType.baseUrl}")
    }

    /** Returns true if the user appears to be logged in */
    fun isLoggedIn(): Boolean = isPageReady  // Refined by JS loginCheck

    /** Make this WebView visible for user interaction (re-auth flow) */
    fun show() {
        _webView?.visibility = android.view.View.VISIBLE
    }

    /** Hide WebView after login is confirmed */
    fun hide() {
        _webView?.visibility = android.view.View.GONE
    }

    fun destroy() {
        _webView?.apply {
            stopLoading()
            destroy()
        }
        _webView = null
        isPageReady = false
    }

    // ── Internal ──────────────────────────────────────────────────────────

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView {
        val wv = WebView(context)
        wv.visibility = android.view.View.GONE

        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true          // Required for persistent sessions
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = CHROME_USER_AGENT  // Appear as normal Chrome browser
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false
            javaScriptCanOpenWindowsAutomatically = false
            allowFileAccess = false
            allowContentAccess = false
        }

        // Persist cookies across app restarts
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(wv, true)
        }

        // Register the Kotlin bridge — accessible as window.AndroidBridge in JS
        wv.addJavascriptInterface(bridge, "AndroidBridge")

        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                Log.d(TAG, "[${providerType.name}] Page finished: $url")
                // Flush cookies to disk immediately
                CookieManager.getInstance().flush()
                // Inject automation script
                injectAutomationScript(view)
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                // Allow normal navigation within the provider's domain
                val host = request.url.host ?: return false
                val providerHost = providerType.baseUrl
                    .removePrefix("https://")
                    .removePrefix("http://")
                    .substringBefore("/")
                return !host.contains(providerHost)
            }
        }

        // Collect bridge events into the response channel
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
            bridge.events.collect { event ->
                when (event) {
                    is BridgeEvent.Ready -> isPageReady = true
                    is BridgeEvent.NeedsLogin -> {
                        isPageReady = false
                        onNeedsReauth(providerType)
                    }
                    else -> responseChannel.trySend(event)
                }
            }
        }

        _webView = wv
        return wv
    }

    private fun injectAutomationScript(view: WebView) {
        val script = WebViewAutomationScript.buildInitScript(selectors)
        view.evaluateJavascript(script, null)
    }

    /**
     * Sends a prompt and collects all StreamChunks until done or error.
     */
    private suspend fun sendPromptAndCollect(prompt: String): List<StreamChunk> {
        val result = mutableListOf<StreamChunk>()

        withContext(Dispatchers.Main) {
            val sendScript = WebViewAutomationScript.buildSendScript(prompt)
            webView.evaluateJavascript(sendScript, null)
        }

        // Drain the channel collecting chunks until Complete or Error
        while (true) {
            val event = responseChannel.receive()
            when (event) {
                is BridgeEvent.Chunk -> result.add(StreamChunk.Token(event.text))
                is BridgeEvent.ToolCall -> result.add(StreamChunk.ToolCallDetected(event.rawJson))
                is BridgeEvent.Complete -> {
                    result.add(StreamChunk.Done(finishReason = "stop"))
                    break
                }
                is BridgeEvent.Error -> {
                    if (event.message.contains("rate", ignoreCase = true) ||
                        event.message.contains("limit", ignoreCase = true)) {
                        _isRateLimited = true
                        rateLimitResetMs = System.currentTimeMillis() + 300_000L  // 5 min cooldown
                        result.add(StreamChunk.Error(event.message, isRateLimit = true))
                    } else {
                        result.add(StreamChunk.Error(event.message))
                    }
                    break
                }
                is BridgeEvent.NeedsLogin -> {
                    isPageReady = false
                    onNeedsReauth(providerType)
                    result.add(StreamChunk.Error("Session expired — please re-login to ${providerType.displayName}"))
                    break
                }
                else -> {}
            }
        }
        return result
    }

    /**
     * Converts the full conversation history + tools into a single prompt string.
     *
     * WebView providers don't have native tool calling or multi-turn API —
     * we serialize the full conversation into one chat message with instructions.
     */
    private fun buildPromptForWebView(
        messages: List<ApiMessage>,
        tools: List<Tool>,
        systemPrompt: String?
    ): String = buildString {
        // System instructions
        if (systemPrompt != null) {
            appendLine("=== SYSTEM INSTRUCTIONS ===")
            appendLine(systemPrompt)
            appendLine()
        }

        // Tool calling instructions
        if (tools.isNotEmpty()) {
            appendLine("=== TOOL CALLING ===")
            appendLine("You have access to these tools. When you need to call a tool, output EXACTLY:")
            appendLine("<tool_call>{\"name\": \"tool_name\", \"args\": {\"param\": \"value\"}}</tool_call>")
            appendLine()
            appendLine("Available tools:")
            tools.forEach { tool ->
                appendLine("• **${tool.name}**: ${tool.description}")
                tool.parameters.properties.entries.take(3).forEach { (param, def) ->
                    appendLine("  - $param (${def.type}): ${def.description}")
                }
            }
            appendLine()
        }

        // Conversation history (last 8 turns to stay within context)
        val recentMessages = messages.takeLast(8)
        if (recentMessages.size > 1) {
            appendLine("=== CONVERSATION ===")
            recentMessages.dropLast(1).forEach { msg ->
                val role = if (msg.role == "user") "User" else "Assistant"
                appendLine("**$role**: ${msg.content}")
                appendLine()
            }
        }

        // Current message
        appendLine("=== REQUEST ===")
        appendLine(messages.lastOrNull()?.content ?: "")
    }

    companion object {
        // Realistic Chrome on Android user agent — prevents "browser not supported" blocks
        private const val CHROME_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
    }
}
