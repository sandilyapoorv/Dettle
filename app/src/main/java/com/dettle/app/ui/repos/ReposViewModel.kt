package com.dettle.app.ui.repos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dettle.app.data.github.GitHubClient
import com.dettle.app.data.github.GitHubRepoSummary
import com.dettle.app.data.github.RepoFile
import com.dettle.app.data.settings.ApiKeyStore
import com.dettle.app.orchestrator.RepoMapper
import com.dettle.app.orchestrator.project.ProjectContextManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReposViewModel @Inject constructor(
    private val gitHubClient: GitHubClient,
    private val repoMapper: RepoMapper,
    private val keyStore: ApiKeyStore,
    private val projectContextManager: ProjectContextManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReposUiState())
    val uiState: StateFlow<ReposUiState> = _uiState.asStateFlow()

    init {
        val hasToken = !keyStore.githubPat.isNullOrBlank()
        val owner = keyStore.githubOwner ?: ""
        _uiState.update { it.copy(githubOwner = owner, isTokenConfigured = hasToken) }

        // Observe active project anchor
        viewModelScope.launch {
            projectContextManager.activeProject.collect { proj ->
                if (proj != null && proj.repo.isNotBlank()) {
                    _uiState.update {
                        it.copy(
                            currentOwner = proj.owner,
                            currentRepo = proj.repo,
                            ownerInput = proj.owner,
                            repoInput = proj.repo
                        )
                    }
                    if (_uiState.value.files.isEmpty()) {
                        loadRepoTree(proj.owner, proj.repo)
                    }
                }
            }
        }

        // Auto-fetch repositories of the connected account
        if (hasToken) {
            loadUserRepos()
        }
    }

    fun loadUserRepos(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val hasToken = !keyStore.githubPat.isNullOrBlank()
            if (!hasToken) {
                _uiState.update { it.copy(isTokenConfigured = false, userRepos = emptyList()) }
                return@launch
            }

            _uiState.update { it.copy(isLoadingRepos = true, error = null, isTokenConfigured = true) }
            val reposResult = gitHubClient.getUserRepositories(forceRefresh)
            reposResult.fold(
                onSuccess = { repos ->
                    val detectedOwner = repos.firstOrNull()?.owner ?: keyStore.githubOwner ?: ""
                    _uiState.update {
                        it.copy(
                            isLoadingRepos = false,
                            userRepos = repos,
                            githubOwner = detectedOwner,
                            error = null
                        )
                    }
                    // If no repo is currently viewed and repos exist, default to active project or first repo
                    if (_uiState.value.currentRepo.isBlank() && repos.isNotEmpty()) {
                        val active = projectContextManager.activeProject.value
                        val target = repos.firstOrNull { it.fullName == "${active?.owner}/${active?.repo}" } ?: repos.first()
                        selectRepo(target)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingRepos = false,
                            error = error.localizedMessage ?: "Failed to load GitHub repositories"
                        )
                    }
                }
            )
        }
    }

    fun selectRepo(repoSummary: GitHubRepoSummary) {
        _uiState.update {
            it.copy(
                currentOwner = repoSummary.owner,
                currentRepo = repoSummary.name,
                ownerInput = repoSummary.owner,
                repoInput = repoSummary.name,
                files = emptyList(),
                selectedFile = null,
                error = null
            )
        }
        viewModelScope.launch {
            projectContextManager.setOrCreateProject(
                name = repoSummary.name,
                repo = "${repoSummary.owner}/${repoSummary.name}",
                branch = repoSummary.defaultBranch
            )
        }
        loadRepoTree(repoSummary.owner, repoSummary.name)
    }

    fun setRepo(owner: String, repo: String) {
        _uiState.update {
            it.copy(currentOwner = owner, currentRepo = repo, files = emptyList(), selectedFile = null, error = null)
        }
        viewModelScope.launch {
            projectContextManager.setOrCreateProject(name = repo, repo = "$owner/$repo")
        }
        loadRepoTree(owner, repo)
    }

    private fun loadRepoTree(owner: String, repo: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val map = repoMapper.buildRepoMap(owner, repo).getOrNull().orEmpty()
                val files = gitHubClient.getRepoFileTree(owner, repo)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        files = files,
                        repoSummary = map.take(2000)
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Failed to load repository files") }
            }
        }
    }

    fun openFile(owner: String, repo: String, path: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFile = true, selectedFile = null) }
            try {
                val content = gitHubClient.readFile(owner, repo, path)
                _uiState.update {
                    it.copy(
                        isLoadingFile = false,
                        selectedFile = RepoFileView(path = path, content = content)
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingFile = false, error = e.localizedMessage ?: "Failed to read file") }
            }
        }
    }

    fun closeFile() {
        _uiState.update { it.copy(selectedFile = null) }
    }

    fun setOwnerInput(input: String) {
        _uiState.update { it.copy(ownerInput = input) }
    }

    fun setRepoInput(input: String) {
        _uiState.update { it.copy(repoInput = input) }
    }

    fun loadRepo() {
        val owner = _uiState.value.ownerInput.trim()
        val repo = _uiState.value.repoInput.trim()
        if (owner.isNotBlank() && repo.isNotBlank()) {
            setRepo(owner, repo)
        }
    }
}

data class ReposUiState(
    val githubOwner: String = "",
    val isTokenConfigured: Boolean = false,
    val isLoadingRepos: Boolean = false,
    val userRepos: List<GitHubRepoSummary> = emptyList(),
    val ownerInput: String = "",
    val repoInput: String = "",
    val currentOwner: String = "",
    val currentRepo: String = "",
    val isLoading: Boolean = false,
    val isLoadingFile: Boolean = false,
    val files: List<RepoFile> = emptyList(),
    val repoSummary: String = "",
    val selectedFile: RepoFileView? = null,
    val error: String? = null
)

data class RepoFileView(val path: String, val content: String)
