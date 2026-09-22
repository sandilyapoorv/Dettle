package com.dettle.app.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dettle.app.domain.project.Project
import com.dettle.app.domain.project.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val projectRepository: ProjectRepository
) : ViewModel() {

    val projects: StateFlow<List<Project>> = projectRepository.observeProjects()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedProject = MutableStateFlow<Project?>(null)
    val selectedProject = _selectedProject.asStateFlow()

    fun selectProject(project: Project) {
        _selectedProject.value = project
        viewModelScope.launch {
            projectRepository.touchProject(project.id)
            // Note: In real implementation, save to DataStore so ChatScreen knows active project
        }
    }

    fun clearSelectedProject() {
        _selectedProject.value = null
    }

    fun createProject(
        name: String,
        description: String,
        emoji: String,
        colorHex: Long,
        systemInstructions: String,
        linkedRepo: String
    ) {
        viewModelScope.launch {
            val newProject = projectRepository.createProject(
                name, description, emoji, colorHex, systemInstructions, linkedRepo
            )
            selectProject(newProject)
        }
    }

    fun archiveProject(id: String) {
        viewModelScope.launch {
            projectRepository.archiveProject(id)
            if (_selectedProject.value?.id == id) {
                _selectedProject.value = null
            }
        }
    }
}
