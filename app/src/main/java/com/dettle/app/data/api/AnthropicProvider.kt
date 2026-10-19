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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
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
                    when (msg.role) {
                        "assistant" -> {
                            put("role", JsonPrimitive("assistant"))
                            if (!msg.tool_calls.isNullOrBlank()) {
                                put("content", buildJsonArray {
                                    if (msg.content.isNotBlank()) {
                                        add(buildJsonObject {
                                            put("type", JsonPrimitive("text"))
                                            put("text", JsonPrimitive(msg.content))
                                        })
                                    }
                                    val parsedTools = runCatching {
                                        val el = json.parseToJsonElement(msg.tool_calls)
                                        if (el is kotlinx.serialization.json.JsonArray) el else kotlinx.serialization.json.JsonArray(listOf(el))
                                    }.getOrNull() ?: kotlinx.serialization.json.JsonArray(emptyList())

                                    parsedTools.forEach { toolItem ->
                                        val obj = toolItem.jsonObject
                                        val id = (obj["id"] as? JsonPrimitive)?.content ?: msg.tool_call_id ?: "toolu_1"
                                        val name = (obj["name"] ?: (obj["function"] as? kotlinx.serialization.json.JsonObject)?.get("name"))?.let { (it as? JsonPrimitive)?.content } ?: msg.name ?: ""
                                        val argsEl = (obj["arguments"] ?: (obj["function"] as? kotlinx.serialization.json.JsonObject)?.get("arguments") ?: obj["args"])
                                        val inputObj = when (argsEl) {
                                            is kotlinx.serialization.json.JsonObject -> argsEl
                                            is JsonPrimitive -> runCatching { json.parseToJsonElement(argsEl.content) as? kotlinx.serialization.json.JsonObject }.getOrNull() ?: kotlinx.serialization.json.JsonObject(emptyMap())
                                            else -> kotlinx.serialization.json.JsonObject(emptyMap())
                                        }
                                        add(buildJsonObject {
                                            put("type", JsonPrimitive("tool_use"))
                                            put("id", JsonPrimitive(id))
                                            put("name", JsonPrimitive(name))
                                            put("input", inputObj)
                                        })
                                    }
                                })
                            } else {
                                put("content", JsonPrimitive(msg.content))
                            }
                        }
                        "tool" -> {
                            put("role", JsonPrimitive("user"))
                            put("content", buildJsonArray {
                                add(buildJsonObject {
                                    put("type", JsonPrimitive("tool_result"))
                                    put("tool_use_id", JsonPrimitive(msg.tool_call_id ?: "toolu_1"))
                                    put("content", JsonPrimitive(msg.content))
                                })
                            })
                        }
                        else -> {
                            put("role", JsonPrimitive("user"))
                            put("content", JsonPrimitive(msg.content))
                        }
                    }
                }
            }

            val coalescedMessages = mutableListOf<JsonObject>()
            userAssistantMessages.forEach { msgObj ->
                val role = msgObj["role"]?.jsonPrimitive?.content ?: "user"
                val last = coalescedMessages.lastOrNull()
                val lastRole = last?.get("role")?.jsonPrimitive?.content
                if (last != null && lastRole == role) {
                    val lastContent = last["content"]
                    val newContent = msgObj["content"]
                    val mergedContent = when {
                        lastContent is kotlinx.serialization.json.JsonArray && newContent is kotlinx.serialization.json.JsonArray -> {
                            kotlinx.serialization.json.JsonArray(lastContent + newContent)
                        }
                        lastContent is kotlinx.serialization.json.JsonArray && newContent is JsonPrimitive -> {
                            kotlinx.serialization.json.JsonArray(lastContent + buildJsonObject {
                                put("type", JsonPrimitive("text"))
                                put("text", newContent)
                            })
                        }
                        lastContent is JsonPrimitive && newContent is kotlinx.serialization.json.JsonArray -> {
                            kotlinx.serialization.json.JsonArray(listOf(buildJsonObject {
                                put("type", JsonPrimitive("text"))
                                put("text", lastContent)
                            }) + newContent)
                        }
                        lastContent is JsonPrimitive && newContent is JsonPrimitive -> {
                            JsonPrimitive("${lastContent.content}\n\n${newContent.content}")
                        }
                        else -> newContent ?: lastContent
                    }
                    coalescedMessages[coalescedMessages.lastIndex] = buildJsonObject {
                        put("role", JsonPrimitive(role))
                        if (mergedContent != null) put("content", mergedContent)
                    }
                } else {
                    coalescedMessages.add(msgObj)
                }
            }

            if (coalescedMessages.firstOrNull()?.get("role")?.jsonPrimitive?.content == "assistant") {
                coalescedMessages.add(0, buildJsonObject {
                    put("role", JsonPrimitive("user"))
                    put("content", JsonPrimitive("Proceed."))
                })
            }

            val effectiveMaxTokens = maxTokens.coerceAtMost(model.maxOutputTokens)
            val requestBody = buildJsonObject {
                put("model", JsonPrimitive(model.modelId))
                put("messages", json.parseToJsonElement(json.encodeToString(coalescedMessages)))
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

            // Fix 4: Accumulate tool call fragments correctly.
            // Anthropic streams tool calls across multiple events:
            //   content_block_start  → type=tool_use, id, name
            //   content_block_delta  → type=input_json_delta, partial_json (many of these)
            //   message_delta        → stop_reason=tool_use (emit final assembled call here)
            var toolCallId = ""
            var toolCallName = ""
            val toolCallArgsBuffer = StringBuilder()
            var hasToolCall = false

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
                        "content_block_start" -> {
                            // Capture tool_use block header (id + name)
                            val contentBlock = chunk["content_block"]?.let { runCatching { it.jsonObject }.getOrNull() }
                            if (contentBlock?.get("type")?.jsonPrimitive?.content == "tool_use") {
                                toolCallId = contentBlock["id"]?.jsonPrimitive?.content ?: ""
                                toolCallName = contentBlock["name"]?.jsonPrimitive?.content ?: ""
                                toolCallArgsBuffer.clear()
                                hasToolCall = true
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
                                // Accumulate — don't emit yet, JSON is partial / incomplete
                                val partialJson = delta["partial_json"]?.jsonPrimitive?.content
                                if (!partialJson.isNullOrEmpty()) {
                                    toolCallArgsBuffer.append(partialJson)
                                }
                            }
                        }
                        "message_delta" -> {
                            chunk["usage"]?.let { runCatching { it.jsonObject }.getOrNull() }?.let { usage ->
                                completionTokens = usage["output_tokens"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                            }
                            val stopReason = chunk["delta"]?.let { runCatching { it.jsonObject }.getOrNull() }
                                ?.get("stop_reason")?.jsonPrimitive?.content ?: "stop"

                            // If we accumulated a tool call, emit the fully assembled JSON now
                            if (hasToolCall && toolCallName.isNotBlank()) {
                                val argsJson = toolCallArgsBuffer.toString().trim().let {
                                    if (it.isBlank() || (!it.startsWith("{") && !it.startsWith("["))) "{}" else it
                                }
                                val toolCallJson = """{"id":"$toolCallId","type":"function","name":"$toolCallName","arguments":$argsJson,"function":{"name":"$toolCallName","arguments":$argsJson}}"""
                                emit(StreamChunk.ToolCallDetected(toolCallJson))
                            }

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
