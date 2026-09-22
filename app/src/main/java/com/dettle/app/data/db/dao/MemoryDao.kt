package com.dettle.app.data.db.dao

import androidx.room.*
import com.dettle.app.data.db.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: MemoryEntity): Long

    @Update
    suspend fun update(memory: MemoryEntity)

    @Delete
    suspend fun delete(memory: MemoryEntity)

    @Query("SELECT * FROM memories ORDER BY importance DESC, lastAccessedAt DESC LIMIT :limit")
    suspend fun getTopMemories(limit: Int = 20): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE repoKey = :repoKey ORDER BY importance DESC LIMIT :limit")
    suspend fun getMemoriesForRepo(repoKey: String, limit: Int = 15): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE type = :type ORDER BY importance DESC LIMIT :limit")
    suspend fun getMemoriesByType(type: String, limit: Int = 10): List<MemoryEntity>

    @Query("""
        SELECT * FROM memories 
        WHERE content LIKE '%' || :keyword || '%' 
        OR tags LIKE '%' || :keyword || '%'
        ORDER BY importance DESC, accessCount DESC 
        LIMIT :limit
    """)
    suspend fun searchMemories(keyword: String, limit: Int = 10): List<MemoryEntity>

    @Query("UPDATE memories SET accessCount = accessCount + 1, lastAccessedAt = :now WHERE id = :id")
    suspend fun recordAccess(id: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM memories WHERE importance < 0.2 AND lastAccessedAt < :olderThan")
    suspend fun pruneStaleMemories(olderThan: Long)

    @Query("SELECT COUNT(*) FROM memories")
    suspend fun count(): Int

    @Query("SELECT * FROM memories ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MemoryEntity>>
    
    @Query("SELECT * FROM memories")
    suspend fun getAllMemories(): List<MemoryEntity>
}
