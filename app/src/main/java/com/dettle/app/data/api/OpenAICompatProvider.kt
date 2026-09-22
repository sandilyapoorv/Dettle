package com.dettle.app.data.api

import com.dettle.app.domain.model.AIModel
import com.dettle.app.domain.model.ApiMessage
import com.dettle.app.domain.model.Tool
import com.dettle.app.domain.model.ToolParameters
import com.dettle.app.domain.model.ToolProperty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSources

/**
 * OpenAI-compatible provider — works for Groq, OpenRouter, SambaNova, GitHub Models.
 * All of these use the OpenAI chat completions API format with SSE streaming.
 */
class OpenAICompatProvider(
    override val model: AIModel,
    private val apiKey: String,
    private val client: OkHttpClient,
    private val json: Json,
    private val extraHeaders: Map<String, String> = emptyMap()
) : AIProvider {

    private var isRateLimited = false
    private var rateLimitResetMs = 0L

    override fun isAvailable(): Boolean {
        if (!isRateLimited) return true
        if (System.currentTimeMillis() > rateLimitResetMs) {
            isRateLimited = false
            return true
        }
        return false
    }

    override suspend fun chat(
        messages: List<ApiMessage>,
        tools: List<Tool>,
        systemPrompt: String?,
        maxTokens: Int
    ): Flow<StreamChunk> = flow {

        val allMessages = buildList {
            if (systemPrompt != null) {
                add(ApiMessage(role = "system", content = systemPrompt))
            }
            addAll(messages)
        }

        val requestBody = buildRequestBody(allMessages, tools, maxTokens)
        val request = Request.Builder()
            .url("${model.provider.baseUrl}/chat/completions")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .apply { extraHeaders.forEach { (k, v) -> header(k, v) } }
            .build()

        val response = client.newCall(request).execute()

        when {
            response.code == 429 -> {
                val retryAfter = response.header("Retry-After")?.toLongOrNull()?.times(1000) ?: 60_000L
                isRateLimited = true
                rateLimitResetMs = System.currentTimeMillis() + retryAfter
                response.close()
                emit(StreamChunk.Error("Rate limit hit on ${model.provider.displayName}", isRateLimit = true))
                return@flow
            }
            !response.isSuccessful -> {
                val errorBody = response.body?.string() ?: "Unknown error"
                response.close()
                emit(StreamChunk.Error("${model.provider.displayName} error ${response.code}: $errorBody"))
                return@flow
            }
        }

        val source = response.body?.source() ?: run {
            emit(StreamChunk.Error("Empty response body from ${model.provider.displayName}"))
            return@flow
        }

        var totalPromptTokens = 0
        var totalCompletionTokens = 0

        try {
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (!line.startsWith("data: ")) continue
                val data = line.removePrefix("data: ").trim()
                if (data == "[DONE]") {
                    emit(StreamChunk.Done(
                        finishReason = "stop",
                        usage = TokenUsage(totalPromptTokens, totalCompletionTokens, totalPromptTokens + totalCompletionTokens)
                    ))
                    break
                }

                try {
                    val chunk = json.parseToJsonElement(data).jsonObject
                    val choices = chunk["choices"]?.let { json.parseToJsonElement(it.toString()) }
                    val delta = choices?.jsonObject?.get("0")?.jsonObject?.get("delta")?.jsonObject

                    // Regular text token
                    val content = delta?.get("content")?.jsonPrimitive?.content
                    if (!content.isNullOrEmpty()) {
                        emit(StreamChunk.Token(content))
                    }

                    // Tool call detected
                    val toolCall = delta?.get("tool_calls")
                    if (toolCall != null) {
                        emit(StreamChunk.ToolCallDetected(toolCall.toString()))
                    }

                    // Usage tracking
                    chunk["usage"]?.jsonObject?.let { usage ->
                        totalPromptTokens = usage["prompt_tokens"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                        totalCompletionTokens = usage["completion_tokens"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    }

                    // Finish reason
                    val finishReason = choices?.jsonObject?.get("0")?.jsonObject?.get("finish_reason")?.jsonPrimitive?.content
                    if (finishReason == "stop" || finishReason == "tool_calls") {
                        emit(StreamChunk.Done(
                            finishReason = finishReason,
                            usage = TokenUsage(totalPromptTokens, totalCompletionTokens, totalPromptTokens + totalCompletionTokens)
                        ))
                        break
                    }
                } catch (_: Exception) {
                    // Skip malformed chunks (streaming can have partial JSON)
                }
            }
        } finally {
            response.close()
        }
    }

    private fun buildRequestBody(
        messages: List<ApiMessage>,
        tools: List<Tool>,
        maxTokens: Int
    ): String {
        val messagesJson = messages.map { msg ->
            buildJsonObject {
                put("role", JsonPrimitive(msg.role))
                put("content", JsonPrimitive(msg.content))
                if (msg.tool_call_id != null) put("tool_call_id", JsonPrimitive(msg.tool_call_id))
                if (msg.name != null) put("name", JsonPrimitive(msg.name))
            }
        }

        val body = buildJsonObject {
            put("model", JsonPrimitive(model.modelId))
            put("messages", json.parseToJsonElement(json.encodeToString(messagesJson)))
            put("max_tokens", JsonPrimitive(maxTokens))
            put("stream", JsonPrimitive(true))
            put("stream_options", json.parseToJsonElement("""{"include_usage": true}"""))
            if (tools.isNotEmpty()) {
                put("tools", json.parseToJsonElement(buildToolsJson(tools)))
                put("tool_choice", JsonPrimitive("auto"))
            }
        }
        return body.toString()
    }

    private fun buildToolsJson(tools: List<Tool>): String {
        val toolDefs = tools.map { tool ->
            buildJsonObject {
                put("type", JsonPrimitive("function"))
                put("function", buildJsonObject {
                    put("name", JsonPrimitive(tool.name))
                    put("description", JsonPrimitive(tool.description))
                    put("parameters", buildJsonObject {
                        put("type", JsonPrimitive("object"))
                        put("properties", buildJsonObject {
                            tool.parameters.properties.forEach { (propName, propDef) ->
                                put(propName, buildJsonObject {
                                    put("type", JsonPrimitive(propDef.type))
                                    put("description", JsonPrimitive(propDef.description))
                                    if (!propDef.enum.isNullOrEmpty()) {
                                        put("enum", json.parseToJsonElement(
                                            json.encodeToString(propDef.enum)
                                        ))
                                    }
                                })
                            }
                        })
                        put("required", json.parseToJsonElement(
                            json.encodeToString(tool.parameters.required)
                        ))
                    })
                })
            }
        }
        return json.encodeToString(toolDefs)
    }
}
