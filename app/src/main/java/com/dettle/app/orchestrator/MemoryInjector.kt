package com.dettle.app.orchestrator

import com.dettle.app.data.db.dao.MemoryDao
import com.dettle.app.data.db.entity.MemoryEntity
import com.dettle.app.data.db.entity.MemoryType
import com.dettle.app.domain.model.TaskContext
import com.dettle.app.orchestrator.memory.EmbeddingEngine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retrieves relevant memories and injects them into system prompts using Local Vector RAG.
 */
@Singleton
class MemoryInjector @Inject constructor(
    private val memoryDao: MemoryDao,
    private val embeddingEngine: EmbeddingEngine
) {
    /**
     * Builds a memory block to prepend to any system prompt using semantic search.
     */
    suspend fun buildMemoryBlock(
        context: TaskContext,
        repoKey: String = "",
        userPrompt: String = ""
    ): String {
        val allMemories = memoryDao.getAllMemories()
        if (allMemories.isEmpty()) return ""

        // Generate embedding for the user's prompt to find relevant memories
        val promptVector = if (userPrompt.isNotBlank()) embeddingEngine.generateEmbedding(userPrompt) else null
        
        val scoredMemories = allMemories.map { memory ->
            var similarity = 0f
            if (promptVector != null && memory.vector != null) {
                similarity = embeddingEngine.cosineSimilarity(promptVector, memory.vector)
            }
            
            // Score = 60% semantic similarity + 30% importance + 10% access frequency
            val accessFrequency = minOf(memory.accessCount / 100f, 1f)
            var finalScore = (similarity * 0.6f) + (memory.importance * 0.3f) + (accessFrequency * 0.1f)
            
            // Big bonus if repoKey matches perfectly
            if (repoKey.isNotBlank() && memory.repoKey == repoKey) {
                finalScore += 0.5f 
            }
            
            memory to finalScore
        }

        // Take top 8 most relevant memories
        val unique = scoredMemories
            .sortedByDescending { it.second }
            .take(8)
            .map { it.first }

        if (unique.isEmpty()) return ""

        // Record that these memories were accessed
        unique.forEach { memoryDao.recordAccess(it.id) }

        return buildString {
            appendLine("=== MEMORY (from past interactions) ===")
            unique.forEach { memory ->
                val typeTag = when (memory.type) {
                    MemoryType.CORRECTION.name -> "CORRECTION"
                    MemoryType.PREFERENCE.name -> "PREFERENCE"
                    MemoryType.REPO_STYLE.name -> "CODE STYLE"
                    MemoryType.TASK_OUTCOME.name -> "PAST TASK"
                    MemoryType.SKILL_LEARNED.name -> "LEARNED"
                    else -> "CONTEXT"
                }
                appendLine("• [$typeTag] ${memory.content}")
            }
            appendLine("=== END MEMORY ===")
            appendLine()
        }
    }

    /** Store a correction the user made — highest importance */
    suspend fun storeCorrection(content: String, repoKey: String = "") {
        val vector = embeddingEngine.generateEmbedding(content)
        memoryDao.insert(MemoryEntity(
            type = MemoryType.CORRECTION.name,
            content = content,
            repoKey = repoKey,
            importance = 0.95f,
            vector = vector,
            tags = listOf("correction", "user-feedback")
        ))
    }

    /** Store detected code style for a repo */
    suspend fun storeRepoStyle(repoKey: String, style: String) {
        val vector = embeddingEngine.generateEmbedding(style)
        memoryDao.insert(MemoryEntity(
            type = MemoryType.REPO_STYLE.name,
            content = style,
            repoKey = repoKey,
            importance = 0.8f,
            vector = vector,
            tags = listOf("style", "repo:$repoKey")
        ))
    }

    /** Store the outcome of a significant task */
    suspend fun storeTaskOutcome(summary: String, repoKey: String = "") {
        val vector = embeddingEngine.generateEmbedding(summary)
        memoryDao.insert(MemoryEntity(
            type = MemoryType.TASK_OUTCOME.name,
            content = summary,
            repoKey = repoKey,
            importance = 0.6f,
            vector = vector,
            tags = listOf("outcome")
        ))
    }

    /** Store something the agent learned (new technique, tool behavior, etc.) */
    suspend fun storeSkillLearned(learning: String) {
        val vector = embeddingEngine.generateEmbedding(learning)
        memoryDao.insert(MemoryEntity(
            type = MemoryType.SKILL_LEARNED.name,
            content = learning,
            importance = 0.75f,
            vector = vector,
            tags = listOf("skill", "learned")
        ))
    }

    /** Auto-detect and store corrections from user messages */
    fun detectCorrection(userMessage: String): String? {
        val correctionPatterns = listOf(
            Regex("don't (use|do|call|write)", RegexOption.IGNORE_CASE),
            Regex("never (use|do|call|write)", RegexOption.IGNORE_CASE),
            Regex("stop (using|doing)", RegexOption.IGNORE_CASE),
            Regex("always (use|do|prefer)", RegexOption.IGNORE_CASE),
            Regex("you (should|must|need to)", RegexOption.IGNORE_CASE),
            Regex("that('s| is) wrong", RegexOption.IGNORE_CASE),
            Regex("that('s| is) not (right|correct)", RegexOption.IGNORE_CASE),
            Regex("instead (of|use)", RegexOption.IGNORE_CASE)
        )
        return if (correctionPatterns.any { it.containsMatchIn(userMessage) }) userMessage else null
    }
}
