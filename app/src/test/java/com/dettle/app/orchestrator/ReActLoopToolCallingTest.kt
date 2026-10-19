package com.dettle.app.orchestrator

import com.dettle.app.data.api.OpenAICompatProvider
import com.dettle.app.data.github.FileChange
import com.dettle.app.domain.model.ApiMessage
import com.dettle.app.domain.model.FreeModels
import com.dettle.app.domain.model.ToolCall
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReActLoopToolCallingTest {

    private val json = Json { ignoreUnknownKeys = true }

    // Helper functions mirroring ReActLoop logic to test parsing independently
    private fun JsonElementToArg(element: kotlinx.serialization.json.JsonElement): String = when (element) {
        is kotlinx.serialization.json.JsonPrimitive -> element.content
        else -> element.toString()
    }

    private fun extractArgs(element: kotlinx.serialization.json.JsonElement?): Map<String, String> {
        if (element == null) return emptyMap()
        val obj = when (element) {
            is JsonObject -> element
            is kotlinx.serialization.json.JsonPrimitive -> runCatching {
                json.parseToJsonElement(element.content) as? JsonObject
            }.getOrNull()
            else -> null
        } ?: return emptyMap()

        return obj.entries.associate { (k, v) -> k to JsonElementToArg(v) }
    }

    @Test
    fun testOpenAiNestedArrayFileChangesParseSuccess() {
        val openAiRawJson = """
            {
              "id": "call_gh_pr_99",
              "type": "function",
              "name": "github_create_branch_pr",
              "function": {
                "name": "github_create_branch_pr",
                "arguments": {
                  "owner": "sandilyapoorv",
                  "repo": "Divube",
                  "branch_name": "feature/direct-stream",
                  "commit_message": "Add stream worker",
                  "pr_title": "Direct stream worker",
                  "pr_body": "Full stream worker support",
                  "file_changes": [
                    {"path": "worker.js", "content": "export default {};"},
                    {"path": "README.md", "content": "# Divube"}
                  ]
                }
              }
            }
        """.trimIndent()

        val parsed = json.parseToJsonElement(openAiRawJson).jsonObject
        val functionObj = parsed["function"]?.jsonObject
        assertNotNull(functionObj)
        val args = extractArgs(functionObj?.get("arguments"))

        assertEquals("sandilyapoorv", args["owner"])
        assertEquals("Divube", args["repo"])
        assertEquals("feature/direct-stream", args["branch_name"])

        val fileChangesStr = args["file_changes"]
        assertNotNull(fileChangesStr)
        assertTrue(fileChangesStr!!.startsWith("["))
        assertTrue(fileChangesStr.contains("worker.js"))

        // Verify that ToolExecutor's file_changes parser can unpack this cleanly
        val fileChangesEl = json.parseToJsonElement(fileChangesStr).jsonArray
        val fileChanges = fileChangesEl.mapNotNull { item ->
            val obj = item.jsonObject
            val path = obj["path"]?.jsonPrimitive?.content
            val content = obj["content"]?.jsonPrimitive?.content
            if (path != null && content != null) FileChange(path, content) else null
        }
        assertEquals(2, fileChanges.size)
        assertEquals("worker.js", fileChanges[0].path)
        assertEquals("export default {};", fileChanges[0].content)
        assertEquals("README.md", fileChanges[1].path)
    }

    @Test
    fun testGeminiFunctionCallNestedArgsParseSuccess() {
        val geminiRawJson = """
            {
              "name": "cloudflare_deploy_preview",
              "args": {
                "project_name": "divube-web",
                "files": {
                  "index.html": "<!DOCTYPE html><html><body>Divube</body></html>",
                  "style.css": "body { background: #000; }"
                }
              }
            }
        """.trimIndent()

        val parsed = json.parseToJsonElement(geminiRawJson).jsonObject
        val name = parsed["name"]?.jsonPrimitive?.content
        assertEquals("cloudflare_deploy_preview", name)

        val args = extractArgs(parsed["args"])
        assertEquals("divube-web", args["project_name"])

        val filesStr = args["files"]
        assertNotNull(filesStr)
        assertTrue(filesStr!!.contains("index.html"))
        assertTrue(filesStr.contains("style.css"))

        val filesObj = json.parseToJsonElement(filesStr).jsonObject
        assertEquals("<!DOCTYPE html><html><body>Divube</body></html>", filesObj["index.html"]?.jsonPrimitive?.content)
    }

    @Test
    fun testClaudeToolUseAccumulationStructure() {
        val claudeRawJson = """
            {
              "id": "toolu_01Abc",
              "name": "github_trigger_action",
              "arguments": {
                "owner": "sandilyapoorv",
                "repo": "Divube",
                "workflow_id": "release.yml",
                "ref": "main",
                "inputs": {
                  "version": "1.0.3",
                  "publish": "true"
                }
              }
            }
        """.trimIndent()

        val parsed = json.parseToJsonElement(claudeRawJson).jsonObject
        assertEquals("toolu_01Abc", parsed["id"]?.jsonPrimitive?.content)
        assertEquals("github_trigger_action", parsed["name"]?.jsonPrimitive?.content)

        val args = extractArgs(parsed["arguments"])
        assertEquals("sandilyapoorv", args["owner"])
        assertEquals("release.yml", args["workflow_id"])

        val inputsStr = args["inputs"]
        assertNotNull(inputsStr)
        val inputsObj = json.parseToJsonElement(inputsStr!!).jsonObject
        assertEquals("1.0.3", inputsObj["version"]?.jsonPrimitive?.content)
    }

    @Test
    fun testMultiTurnHistorySequenceIntegrity() {
        // Test that ApiMessage preserves assistant tool_calls and tool result sequence
        val history = mutableListOf<ApiMessage>()

        // 1. User prompt
        history.add(ApiMessage(role = "user", content = "Deploy to Cloudflare Pages"))

        // 2. Assistant emits tool call
        val toolCallJson = """{"id":"call_cf_1","name":"cloudflare_deploy_preview","arguments":{"project_name":"my-site"}}"""
        history.add(ApiMessage(
            role = "assistant",
            content = "Deploying your site to Cloudflare Pages now...",
            tool_call_id = "call_cf_1",
            name = "cloudflare_deploy_preview",
            tool_calls = toolCallJson
        ))

        // 3. Tool executes and returns result
        history.add(ApiMessage(
            role = "tool",
            content = "Cloudflare Pages Preview Deployed Successfully! URL: https://preview.pages.dev",
            tool_call_id = "call_cf_1",
            name = "cloudflare_deploy_preview"
        ))

        assertEquals(3, history.size)
        assertEquals("user", history[0].role)
        assertEquals("assistant", history[1].role)
        assertEquals("call_cf_1", history[1].tool_call_id)
        assertNotNull(history[1].tool_calls)
        assertEquals("tool", history[2].role)
        assertEquals("call_cf_1", history[2].tool_call_id)
    }

    @Test
    fun testOpenAICompatProviderNormalizesToolCallsStrictSchema() {
        val provider = OpenAICompatProvider(
            model = FreeModels.GROQ_LLAMA_70B,
            apiKey = "gsk_test",
            client = OkHttpClient(),
            json = json
        )

        // Case 1: Tool call without "type": "function" (missing type bug)
        val legacyToolCall = """{"id":"call_legacy_1","name":"github_list_repos","arguments":{"force_refresh":"true"}}"""
        val messagesCase1 = listOf(
            ApiMessage(role = "user", content = "List repos"),
            ApiMessage(
                role = "assistant",
                content = "Calling tool: github_list_repos",
                tool_call_id = "call_legacy_1",
                name = "github_list_repos",
                tool_calls = legacyToolCall
            ),
            ApiMessage(
                role = "tool",
                content = "Repo1, Repo2",
                tool_call_id = "call_legacy_1",
                name = "github_list_repos"
            )
        )

        val body1Json = json.parseToJsonElement(provider.buildRequestBody(messagesCase1, emptyList(), 1000)).jsonObject
        val messages1 = body1Json["messages"]!!.jsonArray
        val assistantMsg1 = messages1[1].jsonObject
        val toolCalls1 = assistantMsg1["tool_calls"]!!.jsonArray
        assertEquals(1, toolCalls1.size)
        val toolCall1 = toolCalls1[0].jsonObject

        // Verify strict OpenAI/Groq requirements
        assertEquals("function", toolCall1["type"]?.jsonPrimitive?.content)
        assertEquals("call_legacy_1", toolCall1["id"]?.jsonPrimitive?.content)
        val functionObj1 = toolCall1["function"]?.jsonObject
        assertNotNull(functionObj1)
        assertEquals("github_list_repos", functionObj1!!["name"]?.jsonPrimitive?.content)
        // arguments must be a JSON-encoded string
        assertTrue(functionObj1["arguments"] is JsonPrimitive)
        assertTrue(functionObj1["arguments"]!!.jsonPrimitive.isString)
        assertTrue(functionObj1["arguments"]!!.jsonPrimitive.content.contains("force_refresh"))

        // Case 2: Gemini-style tool call (only name & args, no type, no id)
        val geminiToolCall = """{"name":"github_list_repos","args":{"force_refresh":true}}"""
        val messagesCase2 = listOf(
            ApiMessage(role = "user", content = "List repos"),
            ApiMessage(
                role = "assistant",
                content = "",
                tool_call_id = "call_gemini_uuid",
                name = "github_list_repos",
                tool_calls = geminiToolCall
            ),
            ApiMessage(
                role = "tool",
                content = "Repo1",
                tool_call_id = "call_gemini_uuid",
                name = "github_list_repos"
            )
        )

        val body2Json = json.parseToJsonElement(provider.buildRequestBody(messagesCase2, emptyList(), 1000)).jsonObject
        val messages2 = body2Json["messages"]!!.jsonArray
        val assistantMsg2 = messages2[1].jsonObject
        val toolCalls2 = assistantMsg2["tool_calls"]!!.jsonArray
        assertEquals(1, toolCalls2.size)
        val toolCall2 = toolCalls2[0].jsonObject

        assertEquals("function", toolCall2["type"]?.jsonPrimitive?.content)
        assertEquals("call_gemini_uuid", toolCall2["id"]?.jsonPrimitive?.content)
        val functionObj2 = toolCall2["function"]?.jsonObject
        assertNotNull(functionObj2)
        assertEquals("github_list_repos", functionObj2!!["name"]?.jsonPrimitive?.content)
        assertTrue(functionObj2["arguments"]!!.jsonPrimitive.content.contains("force_refresh"))

        // Case 3: Tool message has tool_call_id guaranteed
        val toolMsg2 = messages2[2].jsonObject
        assertEquals("tool", toolMsg2["role"]?.jsonPrimitive?.content)
        assertEquals("call_gemini_uuid", toolMsg2["tool_call_id"]?.jsonPrimitive?.content)
    }
}
