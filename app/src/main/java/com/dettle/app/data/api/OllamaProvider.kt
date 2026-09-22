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
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Connects to a local or remote Ollama server.
 * This is the ultimate "uncensored" fallback, as you can run models
 * like Dolphin or WizardLM with zero external API constraints.
 */
class OllamaProvider(
    override val model: AIModel,
    private val client: OkHttpClient,
    private val json: Json,
    private val host: String = model.provider.baseUrl
) : AIProvider {

    override fun isAvailable(): Boolean = true // Always available unless server is down

    override suspend fun chat(
        messages: List<ApiMessage>,
        tools: List<Tool>,
        systemPrompt: String?,
        maxTokens: Int
    ): Flow<StreamChunk> = flow {
        var response: Response? = null
        try {
            // Build Ollama request (/api/chat)
            val messagesArray = buildJsonArray {
                if (systemPrompt != null) {
                    add(buildJsonObject {
                        put("role", JsonPrimitive("system"))
                        put("content", JsonPrimitive(systemPrompt))
                    })
                }
                for (msg in messages) {
                    add(buildJsonObject {
                        put("role", JsonPrimitive(msg.role))
                        put("content", JsonPrimitive(msg.content ?: ""))
                    })
                }
            }

            val requestBody = buildJsonObject {
                put("model", JsonPrimitive(model.modelId))
                put("messages", messagesArray)
                put("stream", JsonPrimitive(true))
                put("options", buildJsonObject {
                    put("num_predict", JsonPrimitive(maxTokens))
                    put("temperature", JsonPrimitive(0.7))
                })
            }.toString()

            val request = Request.Builder()
                .url("$host/chat")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .header("Content-Type", "application/json")
                .build()

            val callResponse = client.newCall(request).execute()
            response = callResponse

            if (!callResponse.isSuccessful) {
                val error = callResponse.body?.string() ?: "Unknown Ollama error"
                emit(StreamChunk.Error("Ollama error ${callResponse.code}: $error"))
                return@flow
            }

            val source = callResponse.body?.source() ?: run {
                emit(StreamChunk.Error("Empty Ollama response"))
                return@flow
            }

            var promptTokens = 0
            var completionTokens = 0

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.isEmpty()) continue

                try {
                    val jsonObj = json.parseToJsonElement(line)
                    val rawJson = jsonObj.toString()
                    val textRegex = """"content"\s*:\s*"((?:[^"\\]|\\.)*)"""".toRegex()
                    val textMatch = textRegex.find(rawJson)?.groupValues?.get(1)
                    
                    if (textMatch != null) {
                        val text = textMatch.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\")
                        emit(StreamChunk.Token(text))
                    }

                    // Check if done and grab usage
                    val doneRegex = """"done"\s*:\s*true""".toRegex()
                    if (doneRegex.containsMatchIn(rawJson)) {
                        val promptRegex = """"prompt_eval_count"\s*:\s*(\d+)""".toRegex()
                        val evalRegex = """"eval_count"\s*:\s*(\d+)""".toRegex()
                        promptTokens = promptRegex.find(rawJson)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                        completionTokens = evalRegex.find(rawJson)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    }

                } catch (e: Exception) {
                    // Skip malformed
                }
            }

            emit(StreamChunk.Done("stop", TokenUsage(promptTokens, completionTokens, promptTokens + completionTokens)))
        } catch (e: Exception) {
            emit(StreamChunk.Error("Ollama network error: ${e.localizedMessage ?: e.message ?: "Unknown error"}"))
        } finally {
            response?.close()
        }
    }.flowOn(Dispatchers.IO)
}
