package com.dettle.app.orchestrator

import android.util.Log
import com.dettle.app.domain.model.Tool
import com.dettle.app.domain.model.ToolCall
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID

/**
 * Robust parser for AI tool calls across all supported models:
 * - Gemini: `{"name": "...", "args": { ... }}`
 * - Anthropic: `{"name": "...", "arguments": { ... }}` (or function.arguments)
 * - OpenAI/Groq/DeepSeek: `{"id": "...", "type": "function", "function": {"name": "...", "arguments": "{...}"}}`
 * - Plain text / WebViews: `<tool_call>{ ... }</tool_call>`
 * - Raw markdown code blocks: ````json { "name": ... } ````
 */
object ToolCallParser {
    private const val TAG = "ToolCallParser"

    fun parse(rawJson: String, json: Json = Json { ignoreUnknownKeys = true }): ToolCall? {
        val cleaned = rawJson.trim().let { s ->
            if (s.startsWith("```")) {
                s.substringAfter("\n").substringBeforeLast("```").trim()
            } else s
        }
        return try {
            val element = json.parseToJsonElement(cleaned)
            val first = when (element) {
                is JsonArray -> element.firstOrNull() as? JsonObject ?: return null
                is JsonObject -> element
                else -> return null
            }

            val functionObj = first["function"] as? JsonObject
            val name = (functionObj?.get("name") as? JsonPrimitive)?.content
                ?: (first["name"] as? JsonPrimitive)?.content
                ?: return null

            val id = (first["id"] as? JsonPrimitive)?.content?.ifBlank { null }
                ?: (functionObj?.get("id") as? JsonPrimitive)?.content?.ifBlank { null }
                ?: UUID.randomUUID().toString()

            val args: Map<String, String> = when {
                functionObj?.containsKey("arguments") == true -> extractArgumentsMap(functionObj["arguments"], json)
                first.containsKey("args") -> extractArgumentsMap(first["args"], json)
                first.containsKey("arguments") -> extractArgumentsMap(first["arguments"], json)
                else -> emptyMap()
            }

            ToolCall(id = id, name = name, arguments = args)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse structured tool call: $e\nJSON input: $rawJson")
            null
        }
    }

    fun extractFromText(text: String, json: Json = Json { ignoreUnknownKeys = true }): ToolCall? {
        val regex = """<tool_call>\s*([\s\S]*?)\s*</tool_call>""".toRegex()
        val match = regex.find(text) ?: return null
        return try {
            var raw = match.groupValues[1].trim()
            if (raw.startsWith("```")) {
                raw = raw.substringAfter("\n").substringBeforeLast("```").trim()
            }
            parse(raw, json)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse text tool call: $e")
            null
        }
    }

    fun extractRawOrMarkdown(text: String, tools: List<Tool>, json: Json = Json { ignoreUnknownKeys = true }): ToolCall? {
        val toolNames = tools.map { it.name }.toSet()
        // 1. Check for ```json ... ``` code fence
        val fenceRegex = """```(?:json)?\s*(\{[\s\S]*?\})\s*```""".toRegex()
        val fenceMatch = fenceRegex.find(text)
        if (fenceMatch != null) {
            val candidate = parse(fenceMatch.groupValues[1], json)
            if (candidate != null && toolNames.contains(candidate.name)) {
                return candidate
            }
        }

        // 2. Check for outermost JSON object with a known tool name
        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace > firstBrace) {
            val jsonCandidate = text.substring(firstBrace, lastBrace + 1)
            val candidate = parse(jsonCandidate, json)
            if (candidate != null && toolNames.contains(candidate.name)) {
                return candidate
            }
        }
        return null
    }

    private fun extractArgumentsMap(element: JsonElement?, json: Json): Map<String, String> {
        if (element == null) return emptyMap()
        val obj = when (element) {
            is JsonObject -> element
            is JsonPrimitive -> runCatching {
                json.parseToJsonElement(element.content) as? JsonObject
            }.getOrNull()
            else -> null
        } ?: return emptyMap()

        return obj.entries.associate { (k, v) ->
            k to when (v) {
                is JsonPrimitive -> v.content
                else -> v.toString()
            }
        }
    }
}
