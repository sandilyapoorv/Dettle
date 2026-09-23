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
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Anthropic Claude provider.
 * Implements the Anthropic Messages API (/v1/messages) with SSE streaming,
 * adaptive / extended thinking (budget_tokens), and tool use support.
 */
class AnthropicProvider(
    override val model: AIModel,
    private val apiKey: String,
    private val client: OkHttpClient,
    private val json: Json
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
            val userAssistantMessages = messages.filter { it.role != "system" }.map { msg ->
                buildJsonObject {
                    put("role", JsonPrimitive(if (msg.role == "assistant") "assistant" else "user"))
                    put("content", JsonPrimitive(msg.content))
                }
            }

            val effectiveMaxTokens = maxTokens.coerceAtMost(model.maxOutputTokens)
            val requestBody = buildJsonObject {
                put("model", JsonPrimitive(model.modelId))
                put("messages", json.parseToJsonElement(json.encodeToString(userAssistantMessages)))
                put("max_tokens", JsonPrimitive(effectiveMaxTokens))
                put("stream", JsonPrimitive(true))

                if (systemPrompt != null) {
                    put("system", JsonPrimitive(systemPrompt))
                }

                if (model.supportsThinking && model.thinkingType == ThinkingType.ANTHROPIC_BUDGET) {
                    val budget = (if (model.defaultThinkingBudget > 0) model.defaultThinkingBudget else 2048)
                        .coerceAtMost(effectiveMaxTokens - 1024)
                        .coerceAtLeast(1024)
                    put("thinking", buildJsonObject {
                        put("type", JsonPrimitive("enabled"))
                        put("budget_tokens", JsonPrimitive(budget))
                    })
                }

                if (tools.isNotEmpty() && model.supportsToolCalling) {
                    put("tools", buildJsonArray {
                        tools.forEach { tool ->
                            add(buildJsonObject {
                                put("name", JsonPrimitive(tool.name))
                                put("description", JsonPrimitive(tool.description))
                                put("input_schema", buildJsonObject {
                                    put("type", JsonPrimitive("object"))
                                    put("properties", buildJsonObject {
                                        tool.parameters.properties.forEach { (propName, propDef) ->
                                            put(propName, buildJsonObject {
                                                put("type", JsonPrimitive(propDef.type))
                                                put("description", JsonPrimitive(propDef.description))
                                            })
                                        }
                                    })
                                    if (tool.parameters.required.isNotEmpty()) {
                                        put("required", buildJsonArray {
                                            tool.parameters.required.forEach { add(JsonPrimitive(it)) }
                                        })
                                    }
                                })
                            })
                        }
                    })
                }
            }.toString()

            val cleanApiKey = apiKey.trim().replace("\r", "").replace("\n", "")
            val request = Request.Builder()
                .url("${model.provider.baseUrl}/messages")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .header("x-api-key", cleanApiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .build()

            val callResponse = client.newCall(request).execute()
            response = callResponse

            when {
                callResponse.code == 429 -> {
                    val retryAfter = callResponse.header("retry-after")?.toLongOrNull()?.times(1000) ?: 60_000L
                    isRateLimited = true
                    rateLimitResetMs = System.currentTimeMillis() + retryAfter
                    emit(StreamChunk.Error("Rate limit hit on Claude", isRateLimit = true))
                    return@flow
                }
                !callResponse.isSuccessful -> {
                    val errorBody = callResponse.body?.string() ?: "Unknown error"
                    emit(StreamChunk.Error("Claude error ${callResponse.code}: $errorBody"))
                    return@flow
                }
            }

            val source = callResponse.body?.source() ?: run {
                emit(StreamChunk.Error("Empty response from Claude"))
                return@flow
            }

            var promptTokens = 0
            var completionTokens = 0

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (!line.startsWith("data: ")) continue
                val data = line.removePrefix("data: ").trim()
                if (data.isEmpty() || data == "[DONE]") continue

                try {
                    val chunk = json.parseToJsonElement(data).jsonObject
                    val type = chunk["type"]?.jsonPrimitive?.content

                    when (type) {
                        "message_start" -> {
                            chunk["message"]?.let { runCatching { it.jsonObject }.getOrNull() }?.let { msg ->
                                msg["usage"]?.let { runCatching { it.jsonObject }.getOrNull() }?.let { usage ->
                                    promptTokens = usage["input_tokens"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                                }
                            }
                        }
                        "content_block_delta" -> {
                            val delta = chunk["delta"]?.let { runCatching { it.jsonObject }.getOrNull() }
                            val deltaType = delta?.get("type")?.jsonPrimitive?.content

                            if (deltaType == "text_delta") {
                                val text = delta["text"]?.jsonPrimitive?.content
                                if (!text.isNullOrEmpty()) {
                                    emit(StreamChunk.Token(text))
                                }
                            } else if (deltaType == "thinking_delta") {
                                val thinking = delta["thinking"]?.jsonPrimitive?.content
                                if (!thinking.isNullOrEmpty()) {
                                    emit(StreamChunk.Token(thinking))
                                }
                            } else if (deltaType == "input_json_delta") {
                                val partialJson = delta["partial_json"]?.jsonPrimitive?.content
                                if (!partialJson.isNullOrEmpty()) {
                                    emit(StreamChunk.ToolCallDetected(partialJson))
                                }
                            }
                        }
                        "message_delta" -> {
                            chunk["usage"]?.let { runCatching { it.jsonObject }.getOrNull() }?.let { usage ->
                                completionTokens = usage["output_tokens"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                            }
                            val stopReason = chunk["delta"]?.let { runCatching { it.jsonObject }.getOrNull() }
                                ?.get("stop_reason")?.jsonPrimitive?.content ?: "stop"

                            emit(StreamChunk.Done(
                                finishReason = stopReason,
                                usage = TokenUsage(promptTokens, completionTokens, promptTokens + completionTokens)
                            ))
                        }
                    }
                } catch (_: Exception) {
                    // Skip malformed chunks
                }
            }
        } catch (e: Exception) {
            emit(StreamChunk.Error("Claude network error: ${e.localizedMessage ?: e.message ?: "Unknown error"}"))
        } finally {
            response?.close()
        }
    }.flowOn(Dispatchers.IO)
}
