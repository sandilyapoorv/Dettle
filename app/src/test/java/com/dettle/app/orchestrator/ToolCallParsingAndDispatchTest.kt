package com.dettle.app.orchestrator

import com.dettle.app.domain.model.AgentTools
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolCallParsingAndDispatchTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testGeminiFormatWithComplexArrayArguments() {
        val geminiPayload = """
            {
                "name": "github_create_branch_pr",
                "args": {
                    "owner": "dettle-org",
                    "repo": "dettle-app",
                    "branch_name": "feature/edge-auth",
                    "file_changes": [
                        {"path": "AuthService.kt", "content": "class AuthService"},
                        {"path": "TokenStore.kt", "content": "class TokenStore"}
                    ],
                    "commit_message": "Implement edge authentication",
                    "pr_title": "feat: edge authentication",
                    "pr_body": "Added AuthService and TokenStore."
                }
            }
        """.trimIndent()

        val toolCall = ToolCallParser.parse(geminiPayload, json)
        assertNotNull(toolCall)
        assertEquals("github_create_branch_pr", toolCall?.name)
        assertEquals("dettle-org", toolCall?.arguments?.get("owner"))
        assertEquals("feature/edge-auth", toolCall?.arguments?.get("branch_name"))
        
        val fileChanges = toolCall?.arguments?.get("file_changes")
        assertNotNull(fileChanges)
        assertTrue(fileChanges!!.contains("AuthService.kt"))
        assertTrue(fileChanges.contains("TokenStore.kt"))
    }

    @Test
    fun testAnthropicFormatWithComplexArguments() {
        val anthropicPayload = """
            {
                "id": "toolu_01A8BcDeFg",
                "type": "function",
                "name": "cloudflare_publish_worker",
                "arguments": {
                    "worker_name": "api-gateway",
                    "script_content": "export default { fetch(req) { return new Response('OK'); } }"
                }
            }
        """.trimIndent()

        val toolCall = ToolCallParser.parse(anthropicPayload, json)
        assertNotNull(toolCall)
        assertEquals("toolu_01A8BcDeFg", toolCall?.id)
        assertEquals("cloudflare_publish_worker", toolCall?.name)
        assertEquals("api-gateway", toolCall?.arguments?.get("worker_name"))
        assertTrue(toolCall?.arguments?.get("script_content")?.contains("export default") == true)
    }

    @Test
    fun testOpenAIFormatWithStringEncodedJsonArguments() {
        val openaiPayload = """
            {
                "id": "call_999xyz",
                "type": "function",
                "function": {
                    "name": "github_read_file",
                    "arguments": "{\"owner\":\"dettle\",\"repo\":\"core\",\"path\":\"README.md\",\"branch\":\"main\"}"
                }
            }
        """.trimIndent()

        val toolCall = ToolCallParser.parse(openaiPayload, json)
        assertNotNull(toolCall)
        assertEquals("call_999xyz", toolCall?.id)
        assertEquals("github_read_file", toolCall?.name)
        assertEquals("dettle", toolCall?.arguments?.get("owner"))
        assertEquals("core", toolCall?.arguments?.get("repo"))
        assertEquals("README.md", toolCall?.arguments?.get("path"))
    }

    @Test
    fun testOpenAIFormatWithNestedArrayInsideStringEncodedArguments() {
        val openaiPayload = """
            {
                "id": "call_array_test",
                "type": "function",
                "function": {
                    "name": "github_create_branch_pr",
                    "arguments": "{\"owner\":\"dettle\",\"repo\":\"core\",\"branch_name\":\"feat/ci\",\"file_changes\":[{\"path\":\".github/workflows/ci.yml\",\"content\":\"name: CI\"}],\"commit_message\":\"add ci\",\"pr_title\":\"ci\",\"pr_body\":\"b\"}"
                }
            }
        """.trimIndent()

        val toolCall = ToolCallParser.parse(openaiPayload, json)
        assertNotNull(toolCall)
        assertEquals("github_create_branch_pr", toolCall?.name)
        val files = toolCall?.arguments?.get("file_changes")
        assertNotNull(files)
        assertTrue(files!!.contains(".github/workflows/ci.yml"))
    }

    @Test
    fun testExtractTextToolCallWithXmlTags() {
        val assistantText = """
            I will now check the files in the workspace.
            <tool_call>
            {
                "name": "workspace_list_files",
                "args": {
                    "dir": "src/main"
                }
            }
            </tool_call>
            Waiting for results...
        """.trimIndent()

        val toolCall = ToolCallParser.extractFromText(assistantText, json)
        assertNotNull(toolCall)
        assertEquals("workspace_list_files", toolCall?.name)
        assertEquals("src/main", toolCall?.arguments?.get("dir"))
    }

    @Test
    fun testExtractTextToolCallWrappedInMarkdownCodeFences() {
        val assistantText = """
            <tool_call>
            ```json
            {
                "name": "codebase_search",
                "arguments": {
                    "query": "jwt verification logic"
                }
            }
            ```
            </tool_call>
        """.trimIndent()

        val toolCall = ToolCallParser.extractFromText(assistantText, json)
        assertNotNull(toolCall)
        assertEquals("codebase_search", toolCall?.name)
        assertEquals("jwt verification logic", toolCall?.arguments?.get("query"))
    }

    @Test
    fun testExtractRawMarkdownToolCallWithoutXmlTags() {
        val assistantText = """
            Sure, let me deploy the website to Cloudflare Pages right now:
            ```json
            {
                "name": "cloudflare_deploy_preview",
                "arguments": {
                    "project_name": "portfolio-site",
                    "files": "{\"index.html\":\"<h1>Hello</h1>\"}"
                }
            }
            ```
            Executing now.
        """.trimIndent()

        val toolCall = ToolCallParser.extractRawOrMarkdown(assistantText, AgentTools.ALL, json)
        assertNotNull(toolCall)
        assertEquals("cloudflare_deploy_preview", toolCall?.name)
        assertEquals("portfolio-site", toolCall?.arguments?.get("project_name"))
    }

    @Test
    fun testExtractRawJsonWithoutFencesOrXmlTags() {
        val assistantText = """
            Calling tool: {"name": "github_get_repo", "arguments": {"owner": "my-org", "repo": "mobile-app"}}
        """.trimIndent()

        val toolCall = ToolCallParser.extractRawOrMarkdown(assistantText, AgentTools.ALL, json)
        assertNotNull(toolCall)
        assertEquals("github_get_repo", toolCall?.name)
        assertEquals("my-org", toolCall?.arguments?.get("owner"))
        assertEquals("mobile-app", toolCall?.arguments?.get("repo"))
    }

    @Test
    fun testJsonArrayToolCallsRoot() {
        val jsonArrayPayload = """
            [
                {
                    "id": "call_1",
                    "type": "function",
                    "function": {
                        "name": "final_answer",
                        "arguments": "{\"summary\":\"Deployed to Cloudflare Pages successfully!\"}"
                    }
                }
            ]
        """.trimIndent()

        val toolCall = ToolCallParser.parse(jsonArrayPayload, json)
        assertNotNull(toolCall)
        assertEquals("final_answer", toolCall?.name)
        assertEquals("Deployed to Cloudflare Pages successfully!", toolCall?.arguments?.get("summary"))
    }
}
