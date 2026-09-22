package com.dettle.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * A Project is a named context that scopes memories and conversations.
 * Think ChatGPT Projects — each project has its own system instructions,
 * project-specific memories, and conversation history.
 */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String = "",
    @ColumnInfo(name = "emoji") val emoji: String = "📁",
    @ColumnInfo(name = "color_hex") val colorHex: Long = 0xFF6200EE,
    @ColumnInfo(name = "system_instructions") val systemInstructions: String = "",
    @ColumnInfo(name = "enforced_protocol_json") val enforcedProtocolJson: String? = null,
    @ColumnInfo(name = "is_unleashed") val isUnleashed: Boolean = true, // Default to true (uncensored)
    @ColumnInfo(name = "linked_repo") val linkedRepo: String = "",  // "owner/repo" or ""
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_used_at") val lastUsedAt: Long = System.currentTimeMillis()
)

/**
 * Project-scoped memory entry.
 * Separate from global MemoryEntity — these are facts specific to ONE project.
 */
@Entity(tableName = "project_memories", foreignKeys = [
    androidx.room.ForeignKey(
        entity = ProjectEntity::class,
        parentColumns = ["id"],
        childColumns = ["project_id"],
        onDelete = androidx.room.ForeignKey.CASCADE
    )
])
data class ProjectMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "project_id", index = true) val projectId: String,
    @ColumnInfo(name = "key") val key: String,         // short label, e.g. "applicationId"
    @ColumnInfo(name = "value") val value: String,     // the actual memory
    @ColumnInfo(name = "confidence") val confidence: Float = 1.0f,  // 0-1, lower = uncertain
    @ColumnInfo(name = "source") val source: String = "ai",  // "ai" | "user"
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
