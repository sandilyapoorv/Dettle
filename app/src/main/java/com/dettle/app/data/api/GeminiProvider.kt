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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Google AI Studio (Gemini) provider.
 * Uses the Gemini generateContent streaming API.
 * Supports Gemini 3.8 Flash, 3.5 Flash-Lite, 3.1 Pro, thinkingConfig (thinking_level / thinking_budget),
 * function calling declarations, and thought extraction.
 */
class GeminiProvider(
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
            // Build Gemini-format request
            val contents = messages.filter { it.role != "system" }.map { msg ->
                buildJsonObject {
                    put("role", JsonPrimitive(if (msg.role == "assistant") "model" else "user"))
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", JsonPrimitive(msg.content)) })
                    })
                }
            }

            val requestBody = buildJsonObject {
                put("contents", buildJsonArray { contents.forEach { add(it) } })
                put("safetySettings", buildJsonArray {
                    val blockNone = JsonPrimitive("BLOCK_NONE")
                    add(buildJsonObject { put("category", JsonPrimitive("HARM_CATEGORY_HARASSMENT")); put("threshold", blockNone) })
                    add(buildJsonObject { put("category", JsonPrimitive("HARM_CATEGORY_HATE_SPEECH")); put("threshold", blockNone) })
                    add(buildJsonObject { put("category", JsonPrimitive("HARM_CATEGORY_SEXUALLY_EXPLICIT")); put("threshold", blockNone) })
                    add(buildJsonObject { put("category", JsonPrimitive("HARM_CATEGORY_DANGEROUS_CONTENT")); put("threshold", blockNone) })
                })
                put("generationConfig", buildJsonObject {
                    put("maxOutputTokens", JsonPrimitive(maxTokens.coerceAtMost(model.maxOutputTokens)))
                    if (model.supportsTemperature) {
                        put("temperature", JsonPrimitive(0.7))
                    }
                    if (model.supportsThinking) {
                        put("thinkingConfig", buildJsonObject {
                            when (model.thinkingType) {
                                ThinkingType.GEMINI_LEVEL -> {
                                    val level = if (model.defaultThinkingLevel.isNotBlank()) model.defaultThinkingLevel else "medium"
                                    put("thinkingLevel", JsonPrimitive(level))
                                }
                                ThinkingType.GEMINI_BUDGET -> {
                                    val budget = if (model.defaultThinkingBudget > 0) model.defaultThinkingBudget else 2048
                                    put("thinkingBudget", JsonPrimitive(budget))
                                }
                                else -> {}
                            }
                            put("includeThoughts", JsonPrimitive(true))
                        })
                    }
                })
                if (tools.isNotEmpty()) {
                    put("tools", buildJsonArray {
                        add(buildJsonObject {
                            put("functionDeclarations", buildJsonArray {
                                tools.forEach { tool ->
                                    add(buildJsonObject {
                                        put("name", JsonPrimitive(tool.name))
                                        put("description", JsonPrimitive(tool.description))
                                        put("parameters", buildJsonObject {
                                            put("type", JsonPrimitive("OBJECT"))
                                            put("properties", buildJsonObject {
                                                tool.parameters.properties.forEach { (propName, propDef) ->
                                                    put(propName, buildJsonObject {
                                                        put("type", JsonPrimitive(propDef.type.uppercase()))
                                                        put("description", JsonPrimitive(propDef.description))
                                                        if (!propDef.enum.isNullOrEmpty()) {
                                                            put("enum", buildJsonArray {
                                                                propDef.enum.forEach { add(JsonPrimitive(it)) }
                                                            })
                                                        }
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
                        })
                    })
                }
                if (systemPrompt != null) {
                    put("systemInstruction", buildJsonObject {
                        put("parts", buildJsonArray {
                            add(buildJsonObject { put("text", JsonPrimitive(systemPrompt)) })
                        })
                    })
                }
            }.toString()

            val cleanApiKey = apiKey.trim().replace("\r", "").replace("\n", "")

            // Map known retired models directly to avoid unnecessary 404 roundtrips
            val primaryModel = when (model.modelId) {
                "gemini-2.0-flash", "gemini-2.0-flash-exp", "gemini-1.5-flash" -> "gemini-3.8-flash"
                "gemini-2.0-flash-thinking-exp" -> "gemini-3.1-pro-preview"
                else -> model.modelId
            }

            // Build candidate model list with prioritized fallback on 404
            val candidateModels = mutableListOf(
                primaryModel,
                "gemini-3.8-flash",
                "gemini-3.5-flash-lite",
                "gemini-3.1-pro-preview",
                "gemini-3-flash-preview",
                "gemini-2.5-flash",
                "gemini-2.5-pro",
                "gemini-1.5-flash",
                "gemini-1.5-pro"
            ).distinct().toMutableList()

            var callResponse: Response? = null
            var lastErrorBody = "Unknown error"

            var index = 0
            while (index < candidateModels.size) {
                val candidate = candidateModels[index]
                val url = "${model.provider.baseUrl}/models/$candidate:streamGenerateContent" +
                        "?key=$cleanApiKey&alt=sse"

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .header("Content-Type", "application/json")
                    .build()

                val testResp = client.newCall(request).execute()
                if (testResp.code == 404) {
                    val errorBody = testResp.body?.string() ?: ""
                    lastErrorBody = errorBody
                    testResp.close()

                    // Extract replacement model if Google suggested one:
                    // e.g., "Please update your code to use models/gemini-3.8-flash"
                    val suggested = Regex("""models/([a-zA-Z0-9\.\-_]+)""").find(errorBody)?.groupValues?.getOrNull(1)
                    if (!suggested.isNullOrBlank() && !candidateModels.contains(suggested)) {
                        candidateModels.add(index + 1, suggested)
                    }
                    index++
                    continue
                }

                callResponse = testResp
                break
            }

            if (callResponse == null) {
                emit(StreamChunk.Error("Gemini error 404: $lastErrorBody"))
                return@flow
            }

            response = callResponse

            when {
                callResponse.code == 429 -> {
                    isRateLimited = true
                    rateLimitResetMs = System.currentTimeMillis() + 60_000L
                    emit(StreamChunk.Error("Rate limit hit on Gemini", isRateLimit = true))
                    return@flow
                }
                !callResponse.isSuccessful -> {
                    val error = callResponse.body?.string() ?: "Unknown error"
                    emit(StreamChunk.Error("Gemini error ${callResponse.code}: $error"))
                    return@flow
                }
            }

            val source = callResponse.body?.source() ?: run {
                emit(StreamChunk.Error("Empty Gemini response"))
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
                    // Check for functionCall tool execution
                    val toolCallJson = extractGeminiToolCall(data)
                    if (toolCallJson != null) {
                        emit(StreamChunk.ToolCallDetected(toolCallJson))
                    }

                    // Extract regular text and thoughts
                    val text = extractGeminiText(data)
                    if (text.isNotEmpty()) {
                        emit(StreamChunk.Token(text))
                    }

                    // Extract usage metadata
                    val usageTokens = extractGeminiUsage(data)
                    if (usageTokens != null) {
                        promptTokens = usageTokens.first
                        completionTokens = usageTokens.second
                    }

                } catch (_: Exception) { /* skip malformed chunk */ }
            }

            emit(StreamChunk.Done(
                finishReason = "stop",
                usage = TokenUsage(promptTokens, completionTokens, promptTokens + completionTokens)
            ))
        } catch (e: Exception) {
            emit(StreamChunk.Error("Gemini network error: ${e.localizedMessage ?: e.message ?: "Unknown error"}"))
        } finally {
            response?.close()
        }
    }.flowOn(Dispatchers.IO)

    /** Extract text from Gemini's nested SSE JSON format */
    private fun extractGeminiText(rawJson: String): String {
        return try {
            val regex = """"text"\s*:\s*"((?:[^"\\]|\\.)*)"""".toRegex()
            regex.find(rawJson)?.groupValues?.get(1)
                ?.replace("\\n", "\n")
                ?.replace("\\\"", "\"")
                ?.replace("\\\\", "\\")
                ?: ""
        } catch (_: Exception) { "" }
    }

    /** Extract functionCall tool invocation if emitted by Gemini */
    private fun extractGeminiToolCall(rawJson: String): String? {
        return try {
            if (rawJson.contains("\"functionCall\"")) {
                val callRegex = """"functionCall"\s*:\s*(\{[^}]+\})""".toRegex()
                callRegex.find(rawJson)?.groupValues?.get(1)
            } else null
        } catch (_: Exception) { null }
    }

    /** Extract prompt/completion token counts from Gemini usage metadata */
    private fun extractGeminiUsage(rawJson: String): Pair<Int, Int>? {
        return try {
            val promptRegex = """"promptTokenCount"\s*:\s*(\d+)""".toRegex()
            val candidateRegex = """"candidatesTokenCount"\s*:\s*(\d+)""".toRegex()
            val prompt = promptRegex.find(rawJson)?.groupValues?.get(1)?.toIntOrNull() ?: return null
            val completion = candidateRegex.find(rawJson)?.groupValues?.get(1)?.toIntOrNull() ?: return null
            Pair(prompt, completion)
        } catch (_: Exception) { null }
    }
}
