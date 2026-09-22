package com.dettle.app.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.dettle.app.data.api.KeyPoolManager
import com.dettle.app.data.api.ProviderStatus
import com.dettle.app.data.drive.GoogleDriveConnector
import com.dettle.app.data.settings.ApiKeyStore
import com.dettle.app.domain.model.AIProviderType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val keyStore: ApiKeyStore,
    private val keyPoolManager: KeyPoolManager,
    private val driveConnector: GoogleDriveConnector
) : ViewModel() {

    var state by mutableStateOf(SettingsState())
        private set

    init {
        loadState()
    }

    private fun loadState() {
        state = state.copy(
            groqKey = "",
            groqSaved = keyStore.hasKey(AIProviderType.GROQ),
            geminiKey = "",
            geminiSaved = keyStore.hasKey(AIProviderType.GEMINI),
            openRouterKey = "",
            openRouterSaved = keyStore.hasKey(AIProviderType.OPENROUTER),
            sambaNovaKey = "",
            sambaNovaSaved = keyStore.hasKey(AIProviderType.SAMBANOVA),
            githubModelsKey = "",
            githubModelsSaved = keyStore.hasKey(AIProviderType.GITHUB_MODELS),
            githubPat = "",
            githubPatSaved = keyStore.githubPat != null,
            githubOwner = keyStore.githubOwner ?: "",
            cloudflareToken = "",
            cloudflareSaved = keyStore.cloudflareApiToken != null,
            cloudflareAccountId = keyStore.cloudflareAccountId ?: "",
            providerStatuses = keyPoolManager.getProviderStatuses(),
            // Google Drive
            driveConnected = driveConnector.isConnected(),
            driveUserEmail = keyStore.driveUserEmail ?: ""
        )
    }

    /** Called from the UI after Google Sign-In completes */
    fun onDriveSignInResult(email: String, idToken: String) {
        keyStore.driveIdToken = idToken
        keyStore.driveUserEmail = email
        state = state.copy(driveConnected = true, driveUserEmail = email)
    }

    fun disconnectDrive() {
        driveConnector.signOut()
        state = state.copy(driveConnected = false, driveUserEmail = "")
    }

    fun saveGroqKey(key: String) {
        keyStore.setKey(AIProviderType.GROQ, key)
        state = state.copy(groqSaved = true)
    }

    fun saveGeminiKey(key: String) {
        keyStore.setKey(AIProviderType.GEMINI, key)
        state = state.copy(geminiSaved = true)
    }

    fun saveOpenRouterKey(key: String) {
        keyStore.setKey(AIProviderType.OPENROUTER, key)
        state = state.copy(openRouterSaved = true)
    }

    fun saveSambaNovaKey(key: String) {
        keyStore.setKey(AIProviderType.SAMBANOVA, key)
        state = state.copy(sambaNovaSaved = true)
    }

    fun saveGitHubModelsKey(key: String) {
        keyStore.setKey(AIProviderType.GITHUB_MODELS, key)
        state = state.copy(githubModelsSaved = true)
    }

    fun saveGithubPat(pat: String) {
        keyStore.githubPat = pat
        state = state.copy(githubPatSaved = true)
    }

    fun setGithubOwner(owner: String) {
        keyStore.githubOwner = owner
        state = state.copy(githubOwner = owner)
    }

    fun saveCloudflareToken(token: String) {
        keyStore.cloudflareApiToken = token
        state = state.copy(cloudflareSaved = true)
    }

    fun setCloudflareAccountId(id: String) {
        keyStore.cloudflareAccountId = id
        state = state.copy(cloudflareAccountId = id)
    }
}

data class SettingsState(
    val groqKey: String = "",
    val groqSaved: Boolean = false,
    val geminiKey: String = "",
    val geminiSaved: Boolean = false,
    val openRouterKey: String = "",
    val openRouterSaved: Boolean = false,
    val sambaNovaKey: String = "",
    val sambaNovaSaved: Boolean = false,
    val githubModelsKey: String = "",
    val githubModelsSaved: Boolean = false,
    val githubPat: String = "",
    val githubPatSaved: Boolean = false,
    val githubOwner: String = "",
    val cloudflareToken: String = "",
    val cloudflareSaved: Boolean = false,
    val cloudflareAccountId: String = "",
    val providerStatuses: List<ProviderStatus> = emptyList(),
    // Google Drive
    val driveConnected: Boolean = false,
    val driveUserEmail: String = ""
)
