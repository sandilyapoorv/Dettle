package com.dettle.app.data.api

import com.dettle.app.domain.model.AIModel
import com.dettle.app.domain.model.ApiMessage
import com.dettle.app.domain.model.ThinkingType
import com.dettle.app.domain.model.Tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.Response
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * OpenAI-compatible provider — works for Groq, OpenAI, DeepSeek, Mistral,
 * SambaNova, Cerebras, xAI, OpenRouter, and GitHub Models.
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
        var response: Response? = null
        try {
            val allMessages = buildList {
                if (systemPrompt != null && model.supportsSystemPrompt) {
                    add(ApiMessage(role = "system", content = systemPrompt))
                }
                addAll(messages)
            }

            val requestBody = buildRequestBody(allMessages, tools, maxTokens)
            val cleanApiKey = apiKey.trim().replace("\r", "").replace("\n", "")
            val request = Request.Builder()
                .url("${model.provider.baseUrl}/chat/completions")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .header("Authorization", "Bearer $cleanApiKey")
                .header("Content-Type", "application/json")
                .apply { extraHeaders.forEach { (k, v) -> header(k, v) } }
                .build()

            val callResponse = client.newCall(request).execute()
            response = callResponse

            when {
                callResponse.code == 429 -> {
                    val retryAfter = callResponse.header("Retry-After")?.toLongOrNull()?.times(1000) ?: 60_000L
                    isRateLimited = true
                    rateLimitResetMs = System.currentTimeMillis() + retryAfter
                    emit(StreamChunk.Error("Rate limit hit on ${model.provider.displayName}", isRateLimit = true))
                    return@flow
                }
                !callResponse.isSuccessful -> {
                    val errorBody = callResponse.body?.string() ?: "Unknown error"
                    emit(StreamChunk.Error("${model.provider.displayName} error ${callResponse.code}: $errorBody"))
                    return@flow
                }
            }

            val source = callResponse.body?.source() ?: run {
                emit(StreamChunk.Error("Empty response body from ${model.provider.displayName}"))
                return@flow
            }

            var totalPromptTokens = 0
            var totalCompletionTokens = 0

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
                    val choices = chunk["choices"]?.let {
                        runCatching { it.jsonArray }.getOrNull()
                    }
                    val firstChoice = choices?.firstOrNull()?.let {
                        runCatching { it.jsonObject }.getOrNull()
                    }
                    val delta = firstChoice?.get("delta")?.let {
                        runCatching { it.jsonObject }.getOrNull()
                    }

                    // Regular text token
                    val content = delta?.get("content")?.let {
                        runCatching { it.jsonPrimitive.content }.getOrNull()
                    }
                    if (!content.isNullOrEmpty()) {
                        emit(StreamChunk.Token(content))
                    }

                    // Reasoning content token (e.g. DeepSeek R1, Groq, Ollama)
                    val reasoningContent = delta?.get("reasoning_content")?.let {
                        runCatching { it.jsonPrimitive.content }.getOrNull()
                    }
                    if (!reasoningContent.isNullOrEmpty()) {
                        emit(StreamChunk.Token(reasoningContent))
                    }

                    // Tool call detected
                    val toolCall = delta?.get("tool_calls")
                    if (toolCall != null) {
                        emit(StreamChunk.ToolCallDetected(toolCall.toString()))
                    }

                    // Usage tracking
                    chunk["usage"]?.let {
                        runCatching { it.jsonObject }.getOrNull()
                    }?.let { usage ->
                        val pTokens = usage["prompt_tokens"]?.jsonPrimitive?.content?.toIntOrNull()
                        val cTokens = usage["completion_tokens"]?.jsonPrimitive?.content?.toIntOrNull()
                        if (pTokens != null) totalPromptTokens = pTokens
                        if (cTokens != null) totalCompletionTokens = cTokens
                    }

                    // Finish reason
                    val finishReason = firstChoice?.get("finish_reason")?.let {
                        runCatching { it.jsonPrimitive.content }.getOrNull()
                    }
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
        } catch (e: Exception) {
            emit(StreamChunk.Error("${model.provider.displayName} network error: ${e.localizedMessage ?: e.message ?: "Unknown error"}"))
        } finally {
            response?.close()
        }
    }.flowOn(Dispatchers.IO)

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
            put("max_tokens", JsonPrimitive(maxTokens.coerceAtMost(model.maxOutputTokens)))
            put("stream", JsonPrimitive(true))
            put("stream_options", json.parseToJsonElement("""{"include_usage": true}"""))
            if (model.supportsTemperature) {
                put("temperature", JsonPrimitive(0.7))
            }
            if (model.supportsThinking && model.thinkingType == ThinkingType.OPENAI_EFFORT) {
                val effort = if (model.defaultThinkingLevel.isNotBlank()) model.defaultThinkingLevel else "medium"
                put("reasoning_effort", JsonPrimitive(effort))
            }
            if (tools.isNotEmpty() && model.supportsToolCalling) {
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
