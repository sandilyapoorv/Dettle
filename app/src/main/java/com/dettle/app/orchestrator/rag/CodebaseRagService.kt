package com.dettle.app.orchestrator.rag

import android.util.Log
import com.dettle.app.data.db.dao.CodeChunkDao
import com.dettle.app.data.db.entity.CodeChunkEntity
import com.dettle.app.orchestrator.memory.EmbeddingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CodebaseRagService"

@Singleton
class CodebaseRagService @Inject constructor(
    private val codeChunkDao: CodeChunkDao,
    private val chunker: CodebaseChunker,
    private val embeddingEngine: EmbeddingEngine
) {

    /**
     * Ingests and indexes code files for a given project into Room DB.
     */
    suspend fun indexFiles(
        projectId: String,
        files: Map<String, String>
    ) = withContext(Dispatchers.IO) {
        try {
            codeChunkDao.deleteForProject(projectId)
            val allChunks = mutableListOf<CodeChunkEntity>()

            for ((path, content) in files) {
                val rawChunks = chunker.chunkFile(path, content)
                for (raw in rawChunks) {
                    val vector = runCatching { embeddingEngine.generateEmbedding(raw.content.take(500)) }.getOrNull()
                    allChunks.add(
                        CodeChunkEntity(
                            projectId = projectId,
                            filePath = raw.filePath,
                            symbolName = raw.symbolName,
                            symbolType = raw.symbolType,
                            content = raw.content,
                            startLine = raw.startLine,
                            endLine = raw.endLine,
                            vector = vector
                        )
                    )
                }
            }

            if (allChunks.isNotEmpty()) {
                codeChunkDao.insertAll(allChunks)
                Log.d(TAG, "Indexed ${allChunks.size} code chunks for project $projectId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to index code files for project $projectId", e)
        }
    }

    /**
     * Hybrid search combining FTS lexical matching and semantic vector cosine similarity.
     */
    suspend fun search(
        projectId: String,
        query: String,
        limit: Int = 6
    ): String = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext "Query is blank."

        val cleanQuery = query.replace("\"", "").replace("'", "").trim()

        // 1. Lexical retrieval (FTS or fallback)
        val lexicalMatches = try {
            codeChunkDao.searchFts(projectId, cleanQuery, limit * 2)
        } catch (_: Exception) {
            codeChunkDao.searchFallback(projectId, cleanQuery, limit * 2)
        }

        // 2. Semantic vector ranking
        val queryVector = runCatching { embeddingEngine.generateEmbedding(query) }.getOrNull()

        val allCandidates = if (lexicalMatches.isNotEmpty()) {
            lexicalMatches
        } else {
            codeChunkDao.getChunksForProject(projectId)
        }

        if (allCandidates.isEmpty()) {
            return@withContext "No relevant code snippets found in indexed project '$projectId'."
        }

        val scored = allCandidates.map { chunk ->
            var semanticScore = 0f
            if (queryVector != null && chunk.vector != null) {
                semanticScore = embeddingEngine.cosineSimilarity(queryVector, chunk.vector)
            }
            val symbolMatchBonus = if (chunk.symbolName.contains(cleanQuery, ignoreCase = true)) 0.5f else 0f
            val totalScore = semanticScore + symbolMatchBonus
            chunk to totalScore
        }

        val topChunks = scored.sortedByDescending { it.second }
            .take(limit)
            .map { it.first }

        buildString {
            appendLine("Found ${topChunks.size} relevant code snippet(s):")
            topChunks.forEach { chunk ->
                appendLine()
                appendLine("```${chunk.filePath.substringAfterLast('.', "")}")
                appendLine("// [${chunk.filePath}#L${chunk.startLine}-L${chunk.endLine}] (${chunk.symbolType}: ${chunk.symbolName})")
                appendLine(chunk.content.take(1500))
                appendLine("```")
            }
        }.trim()
    }

    /**
     * Builds pre-prompt token-budgeted RAG context block for the active project.
     */
    suspend fun buildRagContextBlock(
        projectId: String,
        userQuery: String,
        maxChars: Int = 8000
    ): String = withContext(Dispatchers.IO) {
        if (projectId.isBlank() || userQuery.isBlank()) return@withContext ""

        try {
            val results = search(projectId, userQuery, limit = 4)
            if (results.contains("No relevant code snippets") || results.isBlank()) return@withContext ""

            buildString {
                appendLine("<codebase_context>")
                appendLine("// Retrieved relevant symbols from anchored project:")
                appendLine(results.take(maxChars))
                appendLine("</codebase_context>")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error building RAG context block: ${e.message}")
            ""
        }
    }
}
