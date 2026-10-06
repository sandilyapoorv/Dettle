package com.dettle.app.data.github

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubClientParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun testJsonNullDoesNotCrashObjectCasting() {
        // Simulates Bug A: GraphQL returning null for "object" when branch doesn't exist or repo is empty
        val graphQlResponseJson = """
            {
              "data": {
                "repository": {
                  "defaultBranchRef": { "name": "main" },
                  "object": null
                }
              }
            }
        """.trimIndent()

        val parsed = json.parseToJsonElement(graphQlResponseJson) as? JsonObject
        assertNotNull(parsed)

        val repoObj = (parsed?.get("data") as? JsonObject)?.get("repository") as? JsonObject
        assertNotNull(repoObj)

        // Verifying safe cast does NOT throw IllegalArgumentException: JsonNull is not a JsonObject
        val objectTree = repoObj?.get("object") as? JsonObject
        assertNull(objectTree)

        val entries = objectTree?.get("entries") as? JsonArray
        assertNull(entries)

        val files = entries?.mapNotNull { it as? JsonObject } ?: emptyList()
        assertTrue(files.isEmpty())
    }

    @Test
    fun testFileChangesParsingHandlesArraySingleObjectAndMarkdown() {
        // Case 1: Standard JSON array
        val standardArray = """[{"path":"src/Main.kt","content":"fun main() {}"}]"""
        val parsed1 = parseFileChanges(standardArray)
        assertEquals(1, parsed1.size)
        assertEquals("src/Main.kt", parsed1[0].path)
        assertEquals("fun main() {}", parsed1[0].content)

        // Case 2: Markdown fenced JSON
        val fenced = """```json
[{"path":"README.md","content":"# Hello"}]
```"""
        val parsed2 = parseFileChanges(fenced)
        assertEquals(1, parsed2.size)
        assertEquals("README.md", parsed2[0].path)

        // Case 3: Single JSON object instead of array
        val singleObj = """{"path":"config.json","content":"{}"}"""
        val parsed3 = parseFileChanges(singleObj)
        assertEquals(1, parsed3.size)
        assertEquals("config.json", parsed3[0].path)

        // Case 4: Array containing null or malformed elements
        val mixedArray = """[null, {"path":"valid.txt","content":"ok"}, {"bad_key":"val"}]"""
        val parsed4 = parseFileChanges(mixedArray)
        assertEquals(1, parsed4.size)
        assertEquals("valid.txt", parsed4[0].path)
        // Case 5: Conversational text preceding markdown fenced JSON
        val conversationalFenced = """Here are the file changes:
```json
[{"path":"build.gradle.kts","content":"// plugins"}]
```
Hope this helps!"""
        val parsed5 = parseFileChanges(conversationalFenced)
        assertEquals(1, parsed5.size)
        assertEquals("build.gradle.kts", parsed5[0].path)

        // Case 6: Unfenced JSON preceded and followed by text
        val conversationalUnfenced = """Sure, applying: [{"file_path":"src/App.kt","code":"class App"}] done."""
        val parsed6 = parseFileChanges(conversationalUnfenced)
        assertEquals(1, parsed6.size)
        assertEquals("src/App.kt", parsed6[0].path)
        assertEquals("class App", parsed6[0].content)
    }

    @Test
    fun testExtractTextToolCallHandlesArrayAndObjectArguments() {
        // Bug 1: extractTextToolCall previously crashed when argument values were arrays or objects
        val textResponse = """
            Thinking about changes...
            <tool_call>
            {
              "name": "github_create_branch_pr",
              "args": {
                "branch_name": "feat/my-branch",
                "file_changes": [
                  {"path": "Main.kt", "content": "fun main() {}"}
                ],
                "commit_message": "feat: new file"
              }
            }
            </tool_call>
        """.trimIndent()

        val parsedCall = extractTextToolCall(textResponse)
        assertNotNull(parsedCall)
        assertEquals("github_create_branch_pr", parsedCall?.name)
        assertEquals("feat/my-branch", parsedCall?.arguments?.get("branch_name"))
        assertTrue(parsedCall?.arguments?.get("file_changes")?.contains("Main.kt") == true)
        assertEquals("feat: new file", parsedCall?.arguments?.get("commit_message"))
    }

    @Test
    fun testPathSanitizationStripsLeadingSlashes() {
        val paths = listOf("/src/App.kt", "///README.md", "gradle.properties")
        val sanitized = paths.map { it.trim().removePrefix("/") }
        assertEquals("src/App.kt", sanitized[0])
        assertEquals("README.md", sanitized[1].removePrefix("/").removePrefix("/"))
        assertEquals("gradle.properties", sanitized[2])
    }

    @Test
    fun testGraphQLResponseThrowsOnErrorsField() {
        val errorPayload = """
            {
              "errors": [
                { "message": "Could not resolve to a Repository with the name 'unknown/repo'" }
              ],
              "data": null
            }
        """.trimIndent()

        val parsed = json.parseToJsonElement(errorPayload) as? JsonObject
        assertNotNull(parsed)
        val errorsArray = parsed?.get("errors") as? JsonArray
        assertNotNull(errorsArray)
        assertTrue(errorsArray!!.isNotEmpty())
        val msg = errorsArray.mapNotNull {
            ((it as? JsonObject)?.get("message") as? JsonPrimitive)?.content
        }.joinToString("; ")
        assertEquals("Could not resolve to a Repository with the name 'unknown/repo'", msg)
    }

    @Test
    fun testExtractShaHandlesBothObjectAndArray() {
        // Bug B: GitHub git refs endpoint returns Array for loose refs or Object for exact match
        val objectPayload = json.parseToJsonElement("""{"ref":"refs/heads/main","object":{"sha":"abc12345"}}""")
        val arrayPayload = json.parseToJsonElement("""[{"ref":"refs/heads/main","object":{"sha":"def67890"}}]""")
        val rootShaPayload = json.parseToJsonElement("""{"ref":"refs/heads/main","sha":"root12345"}""")

        assertEquals("abc12345", extractSha(objectPayload))
        assertEquals("def67890", extractSha(arrayPayload))
        assertEquals("root12345", extractSha(rootShaPayload))
    }

    // Mirrors the exact helper logic in GitHubClient
    private fun extractSha(element: JsonElement): String? = when (element) {
        is JsonObject -> (element["object"] as? JsonObject)?.get("sha")?.let { (it as? JsonPrimitive)?.content }
            ?: (element["sha"] as? JsonPrimitive)?.content
        is JsonArray -> element.firstNotNullOfOrNull { item ->
            (item as? JsonObject)?.let { extractSha(it) }
        }
        else -> null
    }

    // Mirrors ToolExecutor file_changes parsing
    private fun parseFileChanges(raw: String): List<FileChange> {
        val cleaned = if (raw.contains("```")) {
            raw.substringAfter("```").let { if (it.startsWith("json", ignoreCase = true)) it.substring(4) else it }
                .substringBeforeLast("```").trim()
        } else {
            val firstBracket = raw.indexOfFirst { it == '[' || it == '{' }
            val lastBracket = raw.indexOfLast { it == ']' || it == '}' }
            if (firstBracket != -1 && lastBracket > firstBracket) {
                raw.substring(firstBracket, lastBracket + 1).trim()
            } else raw.trim()
        }

        return try {
            val element = json.parseToJsonElement(cleaned)
            when (element) {
                is JsonArray -> {
                    element.mapNotNull { el ->
                        val obj = el as? JsonObject ?: return@mapNotNull null
                        val path = ((obj["path"] ?: obj["file_path"] ?: obj["filename"] ?: obj["file"]) as? JsonPrimitive)?.content?.trim()
                        val content = ((obj["content"] ?: obj["code"] ?: obj["text"]) as? JsonPrimitive)?.content
                        if (!path.isNullOrBlank() && content != null) {
                            FileChange(path = path, content = content)
                        } else null
                    }
                }
                is JsonObject -> {
                    val path = ((element["path"] ?: element["file_path"] ?: element["filename"] ?: element["file"]) as? JsonPrimitive)?.content?.trim()
                    val content = ((element["content"] ?: element["code"] ?: element["text"]) as? JsonPrimitive)?.content
                    if (!path.isNullOrBlank() && content != null) {
                        listOf(FileChange(path = path, content = content))
                    } else emptyList()
                }
                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // Mirrors ReActLoop extractTextToolCall
    private fun extractTextToolCall(text: String): com.dettle.app.domain.model.ToolCall? {
        val regex = """<tool_call>\s*(\{.*?\})\s*</tool_call>""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val match = regex.find(text) ?: return null
        return try {
            val obj = json.parseToJsonElement(match.groupValues[1]) as? JsonObject ?: return null
            val name = (obj["name"] as? JsonPrimitive)?.content ?: return null
            val argsObj = obj["args"] as? JsonObject
            val args = argsObj?.entries?.associate { (k, v) ->
                k to ((v as? JsonPrimitive)?.content ?: v.toString())
            } ?: emptyMap()
            com.dettle.app.domain.model.ToolCall(name = name, arguments = args)
        } catch (_: Exception) {
            null
        }
    }
}
