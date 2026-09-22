package com.dettle.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A memory item stored by the agent.
 *
 * Types:
 * - CORRECTION:    User corrected the agent ("don't use X, use Y instead")
 * - REPO_STYLE:    Detected code style for a repo ("this repo uses Kotlin coroutines, not RxJava")
 * - TASK_OUTCOME:  Result of a past task ("deployed mysite.pages.dev successfully")
 * - PREFERENCE:    User expressed preference ("I prefer concise responses")
 * - SKILL_LEARNED: Agent learned something new ("Claude Web uses ProseMirror, need contenteditable")
 * - CONTEXT:       General context about a project or repo
 */
@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,               // MemoryType enum name
    val content: String,            // The actual memory text
    val repoKey: String = "",       // "owner/repo" — empty if global
    val tags: List<String> = emptyList(),
    val importance: Float = 0.5f,  // 0.0 = trivial, 1.0 = critical
    val vector: FloatArray? = null, // The semantic embedding
    val accessCount: Int = 0,       // How many times this memory was retrieved
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis()
)

enum class MemoryType {
    CORRECTION, REPO_STYLE, TASK_OUTCOME, PREFERENCE, SKILL_LEARNED, CONTEXT
}
