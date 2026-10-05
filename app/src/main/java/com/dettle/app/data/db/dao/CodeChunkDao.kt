package com.dettle.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dettle.app.data.db.entity.CodeChunkEntity

@Dao
interface CodeChunkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chunks: List<CodeChunkEntity>)

    @Query("SELECT * FROM code_chunks WHERE project_id = :projectId")
    suspend fun getChunksForProject(projectId: String): List<CodeChunkEntity>

    @Query("""
        SELECT code_chunks.* FROM code_chunks
        JOIN code_chunks_fts ON code_chunks.id = code_chunks_fts.rowid
        WHERE code_chunks_fts MATCH :query AND code_chunks.project_id = :projectId
        LIMIT :limit
    """)
    suspend fun searchFts(projectId: String, query: String, limit: Int = 10): List<CodeChunkEntity>

    @Query("SELECT * FROM code_chunks WHERE project_id = :projectId AND (symbol_name LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') LIMIT :limit")
    suspend fun searchFallback(projectId: String, query: String, limit: Int = 10): List<CodeChunkEntity>

    @Query("DELETE FROM code_chunks WHERE project_id = :projectId")
    suspend fun deleteForProject(projectId: String)

    @Query("DELETE FROM code_chunks WHERE project_id = :projectId AND file_path = :filePath")
    suspend fun deleteForFile(projectId: String, filePath: String)
}
