package com.dettle.app.data.db.dao

import androidx.room.*
import com.dettle.app.data.db.entity.TaskLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: TaskLogEntity): Long

    @Query("SELECT * FROM task_logs ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 50): List<TaskLogEntity>

    @Query("SELECT * FROM task_logs WHERE repoKey = :repoKey ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getForRepo(repoKey: String, limit: Int = 20): List<TaskLogEntity>

    @Query("SELECT * FROM task_logs ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TaskLogEntity>>

    @Query("DELETE FROM task_logs WHERE createdAt < :olderThan")
    suspend fun pruneOlderThan(olderThan: Long)
}
