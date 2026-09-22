package com.dettle.app.data.webview

import android.webkit.JavascriptInterface
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

private const val TAG = "DOMBridge"

/**
 * JavaScript ↔ Kotlin bridge for WebView automation.
 *
 * Injected into each WebView as `window.AndroidBridge`.
 * The JS automation script calls these methods as text streams in.
 *
 * Usage in JS:
 *   window.AndroidBridge.onChunkReceived("Hello ")
 *   window.AndroidBridge.onChunkReceived("world!")
 *   window.AndroidBridge.onComplete("Hello world!")
 */
class DOMAutomationBridge(private val providerId: String) {

    private val _events = MutableSharedFlow<BridgeEvent>(extraBufferCapacity = 256)
    val events: SharedFlow<BridgeEvent> = _events.asSharedFlow()

    @JavascriptInterface
    fun onChunkReceived(chunk: String) {
        _events.tryEmit(BridgeEvent.Chunk(chunk))
    }

    @JavascriptInterface
    fun onComplete(fullText: String) {
        Log.d(TAG, "[$providerId] Response complete (${fullText.length} chars)")
        _events.tryEmit(BridgeEvent.Complete(fullText))
    }

    @JavascriptInterface
    fun onError(message: String) {
        Log.w(TAG, "[$providerId] Error: $message")
        _events.tryEmit(BridgeEvent.Error(message))
    }

    @JavascriptInterface
    fun onNeedsLogin() {
        Log.w(TAG, "[$providerId] Session expired — needs re-auth")
        _events.tryEmit(BridgeEvent.NeedsLogin)
    }

    @JavascriptInterface
    fun onReady() {
        Log.d(TAG, "[$providerId] Page ready")
        _events.tryEmit(BridgeEvent.Ready)
    }

    @JavascriptInterface
    fun onToolCallDetected(rawJson: String) {
        Log.d(TAG, "[$providerId] Tool call: $rawJson")
        _events.tryEmit(BridgeEvent.ToolCall(rawJson))
    }

    @JavascriptInterface
    fun log(message: String) {
        Log.d(TAG, "[$providerId] JS: $message")
    }
}

sealed class BridgeEvent {
    data class Chunk(val text: String) : BridgeEvent()
    data class Complete(val fullText: String) : BridgeEvent()
    data class Error(val message: String) : BridgeEvent()
    data class ToolCall(val rawJson: String) : BridgeEvent()
    object NeedsLogin : BridgeEvent()
    object Ready : BridgeEvent()
}
