package com.dettle.app.orchestrator.project

import com.dettle.app.data.db.dao.ProjectDao
import com.dettle.app.data.db.entity.ProjectEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class AnchoredProject(
    val id: String,
    val name: String,
    val owner: String,
    val repo: String,
    val branch: String? = null,
    val cloudflareProjectName: String = "dettle-site",
    val instructions: String = ""
)

@Singleton
class ProjectContextManager @Inject constructor(
    private val projectDao: ProjectDao
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val _activeProject = MutableStateFlow<AnchoredProject?>(null)
    val activeProject: StateFlow<AnchoredProject?> = _activeProject.asStateFlow()

    init {
        scope.launch {
            val all = projectDao.getAllProjects()
            val initial = all.firstOrNull { !it.isArchived }
            if (initial != null) {
                bindProject(initial)
            }
        }
    }

    suspend fun setActiveProject(projectId: String) {
        val entity = projectDao.getProjectById(projectId) ?: return
        projectDao.touchProject(projectId)
        bindProject(entity)
    }

    suspend fun setOrCreateProject(name: String, linkedRepo: String): AnchoredProject {
        val existing = projectDao.getAllProjects().firstOrNull { it.name.equals(name, ignoreCase = true) }
        val target = if (existing != null) {
            existing.copy(linkedRepo = linkedRepo, lastUsedAt = System.currentTimeMillis()).also {
                projectDao.updateProject(it)
            }
        } else {
            val created = ProjectEntity(
                name = name,
                linkedRepo = linkedRepo,
                lastUsedAt = System.currentTimeMillis()
            )
            projectDao.insertProject(created)
            created
        }
        val anchored = bindProject(target)
        return anchored
    }

    private fun bindProject(entity: ProjectEntity): AnchoredProject {
        val parts = entity.linkedRepo.split("/")
        val owner = parts.getOrNull(0) ?: ""
        val repo = parts.getOrNull(1) ?: entity.linkedRepo
        val cfName = entity.name.lowercase().replace(Regex("[^a-z0-9-]"), "-").take(32).ifBlank { "dettle-site" }

        val anchored = AnchoredProject(
            id = entity.id,
            name = entity.name,
            owner = owner,
            repo = repo,
            branch = null,
            cloudflareProjectName = cfName,
            instructions = entity.systemInstructions
        )
        _activeProject.value = anchored
        return anchored
    }
}
