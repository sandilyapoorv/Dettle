package com.dettle.app.data.db.dao

import androidx.room.*
import com.dettle.app.data.db.entity.ProjectEntity
import com.dettle.app.data.db.entity.ProjectMemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    // Projects
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("SELECT * FROM projects WHERE is_archived = 0 ORDER BY last_used_at DESC")
    fun observeActiveProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Query("UPDATE projects SET last_used_at = :time WHERE id = :id")
    suspend fun touchProject(id: String, time: Long = System.currentTimeMillis())

    @Query("UPDATE projects SET is_archived = 1 WHERE id = :id")
    suspend fun archiveProject(id: String)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: String)

    // Project memories
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: ProjectMemoryEntity)

    @Query("SELECT * FROM project_memories WHERE project_id = :projectId ORDER BY created_at DESC")
    suspend fun getMemoriesForProject(projectId: String): List<ProjectMemoryEntity>

    @Query("SELECT * FROM project_memories WHERE project_id = :projectId AND key = :key LIMIT 1")
    suspend fun getMemoryByKey(projectId: String, key: String): ProjectMemoryEntity?

    @Query("DELETE FROM project_memories WHERE project_id = :projectId AND id = :memoryId")
    suspend fun deleteMemory(projectId: String, memoryId: Long)

    @Query("DELETE FROM project_memories WHERE project_id = :projectId")
    suspend fun clearProjectMemories(projectId: String)

    @Query("SELECT * FROM projects")
    suspend fun getAllProjects(): List<ProjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<ProjectEntity>)
}
