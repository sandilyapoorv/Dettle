package com.dettle.app.orchestrator.overnight

import android.util.Log
import com.dettle.app.data.api.KeyPoolManager
import com.dettle.app.data.api.StreamChunk
import com.dettle.app.data.db.dao.MemoryDao
import com.dettle.app.data.db.dao.TaskLogDao
import com.dettle.app.data.db.entity.MemoryEntity
import com.dettle.app.domain.model.ApiMessage
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import com.dettle.app.data.db.dao.KnowledgeGraphDao
import com.dettle.app.orchestrator.memory.EmbeddingEngine
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class DreamFact(
    val type: String,
    val content: String,
    val repoKey: String = "",
    val importance: Float = 0.5f
)

@Serializable
data class GraphEdge(
    val source: String,
    val target: String,
    val relationship: String,
    val context: String = ""
)

@Serializable
data class DreamResult(
    val facts: List<DreamFact> = emptyList(),
    val graphEdges: List<GraphEdge> = emptyList()
)

@Singleton
class ExperienceConsolidator @Inject constructor(
    private val taskLogDao: TaskLogDao,
    private val memoryDao: MemoryDao,
    private val knowledgeGraphDao: KnowledgeGraphDao,
    private val keyPoolManager: KeyPoolManager,
    private val embeddingEngine: EmbeddingEngine
) {
    companion object {
        private const val TAG = "ExperienceConsolidator"
    }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun dream() {
        Log.d(TAG, "Starting dream phase (memory consolidation & graph extraction)")

        // 1. Fetch recent tasks (last 24 hours)
        val yesterday = System.currentTimeMillis() - 86_400_000L
        val allLogs = taskLogDao.getRecent(50)
        val recentLogs = allLogs.filter { it.createdAt > yesterday }

        if (recentLogs.isEmpty()) {
            Log.d(TAG, "No recent logs to consolidate.")
            return
        }

        // 2. Prepare the payload
        val logsText = recentLogs.joinToString("\n\n") { log ->
            """
            Task [${log.taskType}] - Repo: ${log.repoKey}
            Request: ${log.userRequest}
            Outcome: ${log.outcome}
            Summary: ${log.summary}
            """.trimIndent()
        }

        val systemPrompt = """
            You are the Memory Consolidator (Hippocampus) and Concept Linker (Neocortex) for an autonomous AI agent.
            Below is a log of tasks completed over the last 24 hours.
            
            1. Extract high-value abstract learnings, facts, user preferences, or repo styles from these logs.
            2. Extract relational knowledge graph edges (e.g. "Cloudflare" -> "Wrangler" [DEPENDS_ON]).
            
            Return ONLY a valid JSON object matching this schema exactly:
            {
                "facts": [
                    {
                        "type": "SKILL_LEARNED" or "REPO_STYLE" or "PREFERENCE" or "CONTEXT" or "TASK_OUTCOME" or "CORRECTION",
                        "content": "The concise fact to remember",
                        "repoKey": "owner/repo" (if specific to a repo, else empty string),
                        "importance": 0.8
                    }
                ],
                "graphEdges": [
                    {
                        "source": "ConceptA",
                        "target": "ConceptB",
                        "relationship": "DEPENDS_ON / CAUSES_ERROR / WORKS_WITH",
                        "context": "Short context of how they relate"
                    }
                ]
            }
        """.trimIndent()

        // 3. Ask LLM
        var responseText = ""
        try {
            keyPoolManager.chat(
                messages = listOf(ApiMessage(role = "user", content = logsText)),
                systemPrompt = systemPrompt,
                maxTokens = 2000
            ).collect { chunk ->
                if (chunk is StreamChunk.Token) {
                    responseText += chunk.text
                } else if (chunk is StreamChunk.Error) {
                    Log.e(TAG, "Error in dream phase chat: ${chunk.message}")
                }
            }

            // 4. Parse the response
            val cleanJson = responseText.trim().removePrefix("```json").removeSuffix("```").trim()
            if (cleanJson.isBlank() || !cleanJson.startsWith("{")) {
                Log.w(TAG, "LLM returned invalid JSON for consolidation: ${responseText.take(100)}...")
                return
            }

            val result = json.decodeFromString<DreamResult>(cleanJson)
            
            // 5. Save facts as dense memory
            var savedFacts = 0
            result.facts.forEach { fact ->
                if (fact.content.isNotBlank()) {
                    val vector = embeddingEngine.generateEmbedding(fact.content)
                    memoryDao.insert(
                        MemoryEntity(
                            type = fact.type,
                            content = fact.content,
                            repoKey = fact.repoKey,
                            importance = fact.importance,
                            vector = vector
                        )
                    )
                    savedFacts++
                }
            }
            
            // 6. Save graph edges
            var savedEdges = 0
            result.graphEdges.forEach { edge ->
                knowledgeGraphDao.upsertRelationship(
                    source = edge.source,
                    target = edge.target,
                    rel = edge.relationship,
                    context = edge.context
                )
                savedEdges++
            }
            
            Log.d(TAG, "Dream phase complete. Consolidated $savedFacts facts and $savedEdges concept edges.")
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception during dream phase: ${e.message}", e)
        }
    }
}
