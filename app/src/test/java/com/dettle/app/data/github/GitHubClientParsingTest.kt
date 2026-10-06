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
        val cleaned = if (raw.trim().startsWith("```")) {
            raw.trim().substringAfter("\n").substringBeforeLast("```").trim()
        } else raw.trim()

        return try {
            val element = json.parseToJsonElement(cleaned)
            when (element) {
                is JsonArray -> {
                    element.mapNotNull { el ->
                        val obj = el as? JsonObject ?: return@mapNotNull null
                        val path = (obj["path"] as? JsonPrimitive)?.content?.trim()
                        val content = (obj["content"] as? JsonPrimitive)?.content
                        if (!path.isNullOrBlank() && content != null) {
                            FileChange(path = path, content = content)
                        } else null
                    }
                }
                is JsonObject -> {
                    val path = (element["path"] as? JsonPrimitive)?.content?.trim()
                    val content = (element["content"] as? JsonPrimitive)?.content
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
}
