package com.dettle.app.ui.deployments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dettle.app.data.cloudflare.CloudflareClient
import com.dettle.app.data.cloudflare.PagesDeployment
import com.dettle.app.data.db.dao.DeploymentDao
import com.dettle.app.data.db.entity.DeploymentEntity
import com.dettle.app.data.settings.ApiKeyStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeploymentsViewModel @Inject constructor(
    private val cloudflareClient: CloudflareClient,
    private val deploymentDao: DeploymentDao,
    private val keyStore: ApiKeyStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeploymentsUiState())
    val uiState: StateFlow<DeploymentsUiState> = _uiState

    val deployments: StateFlow<List<DeploymentEntity>> = deploymentDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isCloudflareConfigured: Boolean
        get() = !keyStore.cloudflareApiToken.isNullOrBlank() &&
                !keyStore.cloudflareAccountId.isNullOrBlank()

    init {
        if (isCloudflareConfigured) {
            verifyToken()
        } else {
            _uiState.update { it.copy(tokenStatus = "Not configured — add API token in Settings") }
        }
    }

    private fun verifyToken() {
        viewModelScope.launch {
            _uiState.update { it.copy(isVerifying = true) }
            val result = cloudflareClient.verifyToken()
            _uiState.update {
                it.copy(
                    isVerifying = false,
                    tokenStatus = if (result.isSuccess) "Token valid (${result.getOrNull()})"
                                  else "Token invalid — check Settings"
                )
            }
        }
    }

    fun deployFiles(projectName: String, files: Map<String, String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeploying = true, deployError = null) }
            val result = cloudflareClient.deployToPages(projectName, files)
            if (result.isSuccess) {
                val dep = result.getOrNull()!!
                deploymentDao.insert(DeploymentEntity(
                    id = dep.id,
                    projectName = dep.projectName,
                    type = "PAGES",
                    url = dep.url,
                    productionUrl = dep.productionUrl,
                    fileCount = dep.fileCount
                ))
                _uiState.update {
                    it.copy(isDeploying = false, lastDeployUrl = dep.productionUrl)
                }
            } else {
                _uiState.update {
                    it.copy(isDeploying = false, deployError = result.exceptionOrNull()?.message)
                }
            }
        }
    }
}

data class DeploymentsUiState(
    val isDeploying: Boolean = false,
    val isVerifying: Boolean = false,
    val tokenStatus: String = "",
    val lastDeployUrl: String? = null,
    val deployError: String? = null
)
