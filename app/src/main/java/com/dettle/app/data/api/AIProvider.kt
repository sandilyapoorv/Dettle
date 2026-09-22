package com.dettle.app.data.api

import com.dettle.app.domain.model.AIModel
import com.dettle.app.domain.model.ApiMessage
import com.dettle.app.domain.model.Tool
import kotlinx.coroutines.flow.Flow

/** Unified interface every AI provider must implement */
interface AIProvider {
    val model: AIModel

    /**
     * Send a chat request and stream the response token by token.
     * Emits text chunks as they arrive (SSE streaming).
     * Throws [RateLimitException] on 429.
     * Throws [ProviderException] on other errors.
     */
    suspend fun chat(
        messages: List<ApiMessage>,
        tools: List<Tool> = emptyList(),
        systemPrompt: String? = null,
        maxTokens: Int = 4096
    ): Flow<StreamChunk>

    /** Quick check: is this provider currently usable? */
    fun isAvailable(): Boolean
}

/** A single chunk emitted from a streaming response */
sealed class StreamChunk {
    data class Token(val text: String) : StreamChunk()
    data class ToolCallDetected(val rawJson: String) : StreamChunk()  // Provider returned a tool call
    data class Done(val finishReason: String = "stop", val usage: TokenUsage? = null) : StreamChunk()
    data class Error(val message: String, val isRateLimit: Boolean = false) : StreamChunk()
}

data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

class RateLimitException(provider: String, retryAfterMs: Long = 60_000) :
    Exception("Rate limit hit on $provider. Retry after ${retryAfterMs}ms")

class ProviderException(provider: String, message: String, cause: Throwable? = null) :
    Exception("[$provider] $message", cause)
