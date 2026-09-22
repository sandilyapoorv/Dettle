package com.dettle.app.data.api

import com.dettle.app.domain.model.AIModel
import com.dettle.app.domain.model.ApiMessage
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
 * Gemini has a different request/response format from OpenAI.
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
                    put("maxOutputTokens", JsonPrimitive(maxTokens))
                    put("temperature", JsonPrimitive(0.7))
                })
                if (systemPrompt != null) {
                    put("systemInstruction", buildJsonObject {
                        put("parts", buildJsonArray {
                            add(buildJsonObject { put("text", JsonPrimitive(systemPrompt)) })
                        })
                    })
                }
            }.toString()

            val cleanApiKey = apiKey.trim().replace("\r", "").replace("\n", "")
            val url = "${model.provider.baseUrl}/models/${model.modelId}:streamGenerateContent" +
                    "?key=$cleanApiKey&alt=sse"

            val request = Request.Builder()
                .url(url)
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .header("Content-Type", "application/json")
                .build()

            val callResponse = client.newCall(request).execute()
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
                    val chunk = json.parseToJsonElement(data).let {
                        it.toString()
                    }
                    // Extract text from Gemini's nested response format:
                    // candidates[0].content.parts[0].text
                    val parsed = json.parseToJsonElement(data)
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
        // Gemini format: {"candidates":[{"content":{"parts":[{"text":"..."}],"role":"model"},...}],...}
        return try {
            val regex = """"text"\s*:\s*"((?:[^"\\]|\\.)*)"""".toRegex()
            regex.find(rawJson)?.groupValues?.get(1)
                ?.replace("\\n", "\n")
                ?.replace("\\\"", "\"")
                ?.replace("\\\\", "\\")
                ?: ""
        } catch (_: Exception) { "" }
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
