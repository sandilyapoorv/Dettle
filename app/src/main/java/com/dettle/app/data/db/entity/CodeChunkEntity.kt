package com.dettle.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a semantic chunk of source code in the anchored project.
 * Stored in SQLite with vector embeddings for hybrid (BM25 + Cosine) RAG retrieval.
 */
@Entity(
    tableName = "code_chunks",
    indices = [
        Index(value = ["project_id", "file_path"]),
        Index(value = ["symbol_name"])
    ]
)
data class CodeChunkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "project_id") val projectId: String,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "symbol_name") val symbolName: String,
    @ColumnInfo(name = "symbol_type") val symbolType: String, // FUNCTION, CLASS, INTERFACE, STRUCT, FILE
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "start_line") val startLine: Int,
    @ColumnInfo(name = "end_line") val endLine: Int,
    @ColumnInfo(name = "vector") val vector: FloatArray? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CodeChunkEntity
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
