package com.dettle.app.data.api

import com.dettle.app.domain.model.AIModel
import com.dettle.app.domain.model.ApiMessage
import com.dettle.app.domain.model.Tool
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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

        val url = "${model.provider.baseUrl}/models/${model.modelId}:streamGenerateContent" +
                "?key=$apiKey&alt=sse"

        val request = Request.Builder()
            .url(url)
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .header("Content-Type", "application/json")
            .build()

        val response = client.newCall(request).execute()

        when {
            response.code == 429 -> {
                isRateLimited = true
                rateLimitResetMs = System.currentTimeMillis() + 60_000L
                response.close()
                emit(StreamChunk.Error("Rate limit hit on Gemini", isRateLimit = true))
                return@flow
            }
            !response.isSuccessful -> {
                val error = response.body?.string() ?: "Unknown error"
                response.close()
                emit(StreamChunk.Error("Gemini error ${response.code}: $error"))
                return@flow
            }
        }

        val source = response.body?.source() ?: run {
            emit(StreamChunk.Error("Empty Gemini response"))
            return@flow
        }

        var promptTokens = 0
        var completionTokens = 0

        try {
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
                    val candidates = parsed.toString() // simplified — real implementation parses properly

                    // Parse the actual text content
                    val jsonObj = parsed
                    val candidatesArr = jsonObj.toString() // placeholder for full parsing

                    // In a real implementation, navigate: candidates[0].content.parts[0].text
                    // For now, emit a token if we can extract it
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
        } finally {
            response.close()
        }

        emit(StreamChunk.Done(
            finishReason = "stop",
            usage = TokenUsage(promptTokens, completionTokens, promptTokens + completionTokens)
        ))
    }

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
