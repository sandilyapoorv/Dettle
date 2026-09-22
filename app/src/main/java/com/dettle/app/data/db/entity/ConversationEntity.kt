package com.dettle.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * A named conversation session, optionally scoped to a Project.
 * Conversations within the same project share project memory but not message history.
 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "project_id") val projectId: String? = null,  // null = "Home" (no project)
    @ColumnInfo(name = "title") val title: String = "New Conversation",
    @ColumnInfo(name = "summary") val summary: String = "",   // auto-generated after N messages
    @ColumnInfo(name = "message_count") val messageCount: Int = 0,
    @ColumnInfo(name = "is_unleashed") val isUnleashed: Boolean? = null, // null = inherit from project, otherwise override
    @ColumnInfo(name = "is_pinned") val isPinned: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

/**
 * A single message belonging to a conversation, persisted in Room.
 * Mirrors ChatMessage from the UI layer but is the persistent source of truth.
 */
@Entity(tableName = "conversation_messages", foreignKeys = [
    androidx.room.ForeignKey(
        entity = ConversationEntity::class,
        parentColumns = ["id"],
        childColumns = ["conversation_id"],
        onDelete = androidx.room.ForeignKey.CASCADE
    )
])
data class ConversationMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "conversation_id", index = true) val conversationId: String,
    @ColumnInfo(name = "role") val role: String,        // "user" | "assistant" | "system" | "tool"
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "type") val type: String = "TEXT",  // MessageType name
    @ColumnInfo(name = "tool_name") val toolName: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
