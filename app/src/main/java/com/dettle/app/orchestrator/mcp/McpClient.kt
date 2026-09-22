package com.dettle.app.orchestrator.mcp

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "McpClient"

/**
 * MCP (Model Context Protocol) client.
 *
 * Connects to MCP servers over HTTP transport.
 * Supported servers (Phase 3 stubs, Phase 4 full integration):
 *
 * 1. Serena (oraios/serena) — codebase navigation
 *    Install: uv tool install serena-agent
 *    Provides: list_files, read_file, find_symbol, get_diagnostics
 *
 * 2. Context7 (Upstash cloud MCP) — live documentation lookup
 *    Endpoint: https://mcp.context7.com/mcp
 *    Provides: resolve-library-id, get-library-docs
 *
 * The MCP JSON-RPC 2.0 protocol:
 *   Request:  {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"..","arguments":{}}}
 *   Response: {"jsonrpc":"2.0","id":1,"result":{"content":[{"type":"text","text":"..."}]}}
 */
@Singleton
class McpClient @Inject constructor(
    private val httpClient: OkHttpClient,
    private val json: Json
) {
    private var serenaBaseUrl: String? = null      // Set when Serena is running locally
    private var context7ApiKey: String? = null     // Set from settings

    private val context7Url = "https://mcp.context7.com/mcp"
    private var requestId = 1

    // ── Configuration ──────────────────────────────────────────────────────

    fun configureSerena(baseUrl: String) {
        serenaBaseUrl = baseUrl
        Log.d(TAG, "Serena configured at: $baseUrl")
    }

    fun configureContext7(apiKey: String) {
        context7ApiKey = apiKey
        Log.d(TAG, "Context7 configured")
    }

    // ── Serena tools ──────────────────────────────────────────────────────

    suspend fun serenaListFiles(path: String = "."): Result<String> {
        val baseUrl = serenaBaseUrl ?: return Result.failure(Exception("Serena not configured"))
        return callMcpTool(baseUrl = baseUrl, toolName = "list_files", args = mapOf("path" to path))
    }

    suspend fun serenaReadFile(filePath: String): Result<String> {
        val baseUrl = serenaBaseUrl ?: return Result.failure(Exception("Serena not configured"))
        return callMcpTool(baseUrl = baseUrl, toolName = "read_file", args = mapOf("path" to filePath))
    }

    suspend fun serenaFindSymbol(symbol: String): Result<String> {
        val baseUrl = serenaBaseUrl ?: return Result.failure(Exception("Serena not configured"))
        return callMcpTool(baseUrl = baseUrl, toolName = "find_symbol", args = mapOf("symbol" to symbol))
    }

    suspend fun serenaDiagnostics(): Result<String> {
        val baseUrl = serenaBaseUrl ?: return Result.failure(Exception("Serena not configured"))
        return callMcpTool(baseUrl = baseUrl, toolName = "get_diagnostics", args = emptyMap())
    }

    // ── Context7 tools ────────────────────────────────────────────────────

    suspend fun context7ResolveLibrary(libraryName: String): Result<String> =
        callMcpTool(
            baseUrl = context7Url,
            toolName = "resolve-library-id",
            args = mapOf("libraryName" to libraryName),
            apiKey = context7ApiKey
        )

    suspend fun context7GetDocs(libraryId: String, topic: String = ""): Result<String> =
        callMcpTool(
            baseUrl = context7Url,
            toolName = "get-library-docs",
            args = buildMap {
                put("context7CompatibleLibraryID", libraryId)
                if (topic.isNotBlank()) put("topic", topic)
                put("tokens", "8000")
            },
            apiKey = context7ApiKey
        )

    // ── Internal ──────────────────────────────────────────────────────────

    private suspend fun callMcpTool(
        baseUrl: String,
        toolName: String,
        args: Map<String, String>,
        apiKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val id = requestId++
            val argsJson = args.entries.joinToString(",") { (k, v) -> "\"$k\":\"$v\"" }
            val body = """
                {
                    "jsonrpc": "2.0",
                    "id": $id,
                    "method": "tools/call",
                    "params": {
                        "name": "$toolName",
                        "arguments": {$argsJson}
                    }
                }
            """.trimIndent()

            val requestBuilder = Request.Builder()
                .url("$baseUrl/messages")
                .post(body.toRequestBody("application/json".toMediaType()))
                .header("Content-Type", "application/json")

            if (apiKey != null) {
                requestBuilder.header("Authorization", "Bearer $apiKey")
            }

            val response = httpClient.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("MCP error ${response.code}: $responseBody"))
            }

            val parsed = json.decodeFromString<McpResponse>(responseBody)
            val text = parsed.result?.content?.firstOrNull()?.text
                ?: return@withContext Result.failure(Exception("Empty MCP response"))

            Log.d(TAG, "MCP [$toolName] → ${text.length} chars")
            Result.success(text)
        } catch (e: Exception) {
            Log.e(TAG, "MCP [$toolName] failed", e)
            Result.failure(e)
        }
    }

    // ── Status ────────────────────────────────────────────────────────────

    fun getStatus(): McpStatus = McpStatus(
        serenaConfigured = serenaBaseUrl != null,
        context7Configured = context7ApiKey != null,
        serenaUrl = serenaBaseUrl ?: "Not configured",
        context7Ready = context7ApiKey != null
    )
}

// ── JSON models ───────────────────────────────────────────────────────────

@Serializable data class McpResponse(
    val jsonrpc: String = "",
    val id: Int = 0,
    val result: McpResult? = null
)
@Serializable data class McpResult(val content: List<McpContent> = emptyList())
@Serializable data class McpContent(val type: String = "", val text: String = "")

data class McpStatus(
    val serenaConfigured: Boolean,
    val context7Configured: Boolean,
    val serenaUrl: String,
    val context7Ready: Boolean
)
