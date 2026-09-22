package com.dettle.app.ui.repos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dettle.app.data.github.GitHubClient
import com.dettle.app.data.github.RepoFile
import com.dettle.app.data.settings.ApiKeyStore
import com.dettle.app.orchestrator.RepoMapper
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
    private val keyStore: ApiKeyStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReposUiState())
    val uiState: StateFlow<ReposUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(githubOwner = keyStore.githubOwner ?: "") }
    }

    fun setRepo(owner: String, repo: String) {
        _uiState.update {
            it.copy(currentOwner = owner, currentRepo = repo, files = emptyList(), selectedFile = null, error = null)
        }
        loadRepoTree(owner, repo)
    }

    private fun loadRepoTree(owner: String, repo: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val map = repoMapper.buildMap(owner, repo)
                // Also get flat file list for the tree view
                val files = gitHubClient.getRepoFileTree(owner, repo)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        files = files,
                        repoSummary = map.take(2000) // Token-compressed map preview
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
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
                _uiState.update { it.copy(isLoadingFile = false, error = e.message) }
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
