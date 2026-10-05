package com.dettle.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * SQLite FTS4 virtual table for fast full-text / symbol searching of code chunks.
 */
@Fts4(contentEntity = CodeChunkEntity::class)
@Entity(tableName = "code_chunks_fts")
data class CodeChunkFtsEntity(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowid: Long,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "symbol_name") val symbolName: String,
    @ColumnInfo(name = "content") val content: String
)
