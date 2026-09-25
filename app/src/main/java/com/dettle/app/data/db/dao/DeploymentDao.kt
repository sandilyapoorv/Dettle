package com.dettle.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dettle.app.data.db.entity.DeploymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeploymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(deployment: DeploymentEntity)

    @Delete
    suspend fun delete(deployment: DeploymentEntity)

    @Query("SELECT * FROM deployments ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DeploymentEntity>>

    @Query("SELECT * FROM deployments ORDER BY createdAt DESC")
    fun getAllDeployments(): Flow<List<DeploymentEntity>>

    @Query("SELECT * FROM deployments WHERE projectName = :projectName ORDER BY createdAt DESC")
    fun getDeploymentsByProject(projectName: String): Flow<List<DeploymentEntity>>

    @Query("SELECT * FROM deployments WHERE projectName = :projectName ORDER BY createdAt DESC LIMIT 10")
    suspend fun getForProject(projectName: String): List<DeploymentEntity>

    @Query("SELECT * FROM deployments ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentDeployments(limit: Int = 20): List<DeploymentEntity>

    @Query("UPDATE deployments SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM deployments WHERE createdAt < :beforeMs")
    suspend fun deleteOlderThan(beforeMs: Long)

    @Query("SELECT * FROM deployments")
    suspend fun getAllDeploymentsList(): List<DeploymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeployments(deployments: List<DeploymentEntity>)
}
