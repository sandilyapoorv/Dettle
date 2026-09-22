package com.dettle.app.data.db.dao

import androidx.room.*
import com.dettle.app.data.db.entity.DeploymentEntity
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

@Dao
interface DeploymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(deployment: DeploymentEntity)

    @Query("SELECT * FROM deployments ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DeploymentEntity>>

    @Query("SELECT * FROM deployments WHERE projectName = :projectName ORDER BY createdAt DESC LIMIT 10")
    suspend fun getForProject(projectName: String): List<DeploymentEntity>

    @Query("UPDATE deployments SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
}
