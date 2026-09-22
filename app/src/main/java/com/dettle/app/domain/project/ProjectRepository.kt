package com.dettle.app.domain.project

import com.dettle.app.data.db.dao.ProjectDao
import com.dettle.app.data.db.entity.ProjectEntity
import com.dettle.app.data.db.entity.ProjectMemoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class Project(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val colorHex: Long,
    val systemInstructions: String,
    val linkedRepo: String,
    val isArchived: Boolean,
    val createdAt: Long,
    val lastUsedAt: Long
)

data class ProjectMemory(
    val id: Long,
    val projectId: String,
    val key: String,
    val value: String,
    val confidence: Float,
    val source: String
)

@Singleton
class ProjectRepository @Inject constructor(
    private val dao: ProjectDao
) {
    fun observeProjects(): Flow<List<Project>> =
        dao.observeActiveProjects().map { list -> list.map { it.toDomain() } }

    suspend fun createProject(
        name: String,
        description: String = "",
        emoji: String = "📁",
        colorHex: Long = 0xFF6200EE,
        systemInstructions: String = "",
        linkedRepo: String = ""
    ): Project {
        val entity = ProjectEntity(
            name = name,
            description = description,
            emoji = emoji,
            colorHex = colorHex,
            systemInstructions = systemInstructions,
            linkedRepo = linkedRepo
        )
        dao.insertProject(entity)
        return entity.toDomain()
    }

    suspend fun updateProject(project: Project) {
        dao.updateProject(project.toEntity())
    }

    suspend fun updateInstructions(projectId: String, instructions: String) {
        val existing = dao.getProjectById(projectId) ?: return
        dao.updateProject(existing.copy(systemInstructions = instructions))
    }

    suspend fun getProject(id: String): Project? = dao.getProjectById(id)?.toDomain()

    suspend fun archiveProject(id: String) = dao.archiveProject(id)

    suspend fun touchProject(id: String) = dao.touchProject(id)

    // Memory
    suspend fun getProjectMemories(projectId: String): List<ProjectMemory> =
        dao.getMemoriesForProject(projectId).map { it.toDomain() }

    suspend fun saveMemory(projectId: String, key: String, value: String, source: String = "ai") {
        val existing = dao.getMemoryByKey(projectId, key)
        dao.insertMemory(
            ProjectMemoryEntity(
                id = existing?.id ?: 0,
                projectId = projectId,
                key = key,
                value = value,
                source = source
            )
        )
    }

    suspend fun deleteMemory(projectId: String, memoryId: Long) =
        dao.deleteMemory(projectId, memoryId)
}

private fun ProjectEntity.toDomain() = Project(
    id, name, description, emoji, colorHex, systemInstructions, linkedRepo, isArchived, createdAt, lastUsedAt
)
private fun Project.toEntity() = ProjectEntity(
    id = id,
    name = name,
    description = description,
    emoji = emoji,
    colorHex = colorHex,
    systemInstructions = systemInstructions,
    linkedRepo = linkedRepo,
    isArchived = isArchived,
    createdAt = createdAt,
    lastUsedAt = lastUsedAt
)
private fun ProjectMemoryEntity.toDomain() = ProjectMemory(id, projectId, key, value, confidence, source)
