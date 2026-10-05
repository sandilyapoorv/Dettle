package com.dettle.app.data.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Accumulates streaming function/tool call chunks from OpenAI-compatible SSE streams.
 *
 * In OpenAI/Groq/OpenRouter SSE streaming, tool calls arrive across multiple chunks:
 * - Chunk 1: index, id, function.name
 * - Chunk 2..N: function.arguments (partial fragments)
 *
 * This accumulator pieces fragments together by index and produces a well-formed
 * JSON string once the stream completes or finish_reason is "tool_calls".
 */
class StreamingToolCallAccumulator(
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    private class CallBuilder(
        var id: String = "",
        var name: String = "",
        val argsBuffer: StringBuilder = StringBuilder()
    )

    private val builders = mutableMapOf<Int, CallBuilder>()

    /**
     * Ingests a streaming JsonArray or JsonElement representing choices[0].delta.tool_calls.
     */
    fun ingestDelta(toolCallsElement: JsonElement) {
        val array = when (toolCallsElement) {
            is JsonArray -> toolCallsElement
            is JsonObject -> JsonArray(listOf(toolCallsElement))
            else -> return
        }

        for (item in array) {
            val obj = runCatching { item.jsonObject }.getOrNull() ?: continue
            val index = obj["index"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val builder = builders.getOrPut(index) { CallBuilder() }

            obj["id"]?.jsonPrimitive?.content?.let { id ->
                if (id.isNotBlank()) builder.id = id
            }

            val functionObj = obj["function"]?.let { runCatching { it.jsonObject }.getOrNull() }
            if (functionObj != null) {
                functionObj["name"]?.jsonPrimitive?.content?.let { name ->
                    if (name.isNotBlank()) builder.name = name
                }
                functionObj["arguments"]?.jsonPrimitive?.content?.let { argChunk ->
                    builder.argsBuffer.append(argChunk)
                }
            } else {
                obj["name"]?.jsonPrimitive?.content?.let { name ->
                    if (name.isNotBlank()) builder.name = name
                }
                obj["arguments"]?.jsonPrimitive?.content?.let { argChunk ->
                    builder.argsBuffer.append(argChunk)
                }
            }
        }
    }

    /** Returns true if at least one tool call has been detected */
    fun hasToolCalls(): Boolean = builders.isNotEmpty()

    /**
     * Formats all accumulated tool calls into a well-formed JSON array string matching OpenAI format.
     */
    fun buildJsonArrayString(): String {
        if (builders.isEmpty()) return "[]"
        val callList = builders.toSortedMap().values.map { call ->
            val finalId = call.id.ifBlank { "call_${System.currentTimeMillis()}" }
            val rawArgs = call.argsBuffer.toString().trim()
            val safeArgs = if (rawArgs.isBlank() || (!rawArgs.startsWith("{") && !rawArgs.startsWith("["))) "{}" else rawArgs
            """{"id":"$finalId","type":"function","function":{"name":"${call.name}","arguments":$safeArgs}}"""
        }
        return "[${callList.joinToString(",")}]"
    }

    /**
     * Returns the primary (first) completed tool call JSON object string, or null if none.
     */
    fun buildPrimaryToolCallJson(): String? {
        val primary = builders.toSortedMap().values.firstOrNull() ?: return null
        val finalId = primary.id.ifBlank { "call_${System.currentTimeMillis()}" }
        val rawArgs = primary.argsBuffer.toString().trim()
        val safeArgs = if (rawArgs.isBlank() || (!rawArgs.startsWith("{") && !rawArgs.startsWith("["))) "{}" else rawArgs
        return """{"id":"$finalId","type":"function","name":"${primary.name}","arguments":$safeArgs,"function":{"name":"${primary.name}","arguments":$safeArgs}}"""
    }

    fun clear() {
        builders.clear()
    }
}
