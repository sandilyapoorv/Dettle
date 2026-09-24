package com.dettle.app.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dettle.app.data.backup.BackupManager
import com.dettle.app.data.backup.BackupOptions
import com.dettle.app.data.backup.BackupSummary
import com.dettle.app.data.backup.RestoreResult
import android.net.Uri
import kotlinx.coroutines.launch
import com.dettle.app.data.api.KeyPoolManager
import com.dettle.app.data.api.ProviderStatus
import com.dettle.app.data.drive.GoogleDriveConnector
import com.dettle.app.data.settings.ApiKeyStore
import com.dettle.app.domain.model.AIProviderType
import com.dettle.app.domain.model.AggregateAccountMetrics
import com.dettle.app.domain.model.GitHubAccount
import com.dettle.app.domain.model.ProviderAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val keyStore: ApiKeyStore,
    private val keyPoolManager: KeyPoolManager,
    private val driveConnector: GoogleDriveConnector,
    val backupManager: BackupManager,
    val themeManager: com.dettle.app.ui.theme.ThemeManager
) : ViewModel() {

    val themeConfig = themeManager.themeConfig

    fun setTheme(theme: com.dettle.app.ui.theme.AppTheme) = themeManager.setTheme(theme)
    fun setThemeMode(mode: com.dettle.app.ui.theme.ThemeMode) = themeManager.setMode(mode)
    fun setPureOled(enabled: Boolean) = themeManager.setPureOled(enabled)
    fun setCustomAccent(colorHex: Long?) = themeManager.setCustomAccent(colorHex)
    fun resetTheme() = themeManager.resetToDefaults()

    var state by mutableStateOf(SettingsState())
        private set

    var backupSummary by mutableStateOf<BackupSummary?>(null)
        private set

    init {
        loadState()
        refreshBackupSummary()
    }

    fun refreshBackupSummary() {
        viewModelScope.launch {
            backupSummary = backupManager.getLiveSummary()
        }
    }

    suspend fun createBackupJson(options: BackupOptions): String =
        backupManager.createBackupJson(options)

    suspend fun restoreBackup(json: String, options: BackupOptions): RestoreResult {
        val result = backupManager.restoreBackup(json, options)
        if (result.success) {
            loadState()
            refreshBackupSummary()
        }
        return result
    }

    fun exportBackupToFile(json: String): Uri =
        backupManager.exportToFile(json)

    fun readBackupUri(uri: Uri): String =
        backupManager.readJsonFromUri(uri)

    fun loadState() {
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
            // Multi-account & aggregate mathematics
            providerAccounts = keyStore.getAllProviderAccounts(),
            aggregateMetrics = keyStore.getAggregateMetrics(),
            gitHubAccounts = keyStore.getAllGitHubAccounts(),
            // Google Drive
            driveConnected = driveConnector.isConnected(),
            driveUserEmail = keyStore.driveUserEmail ?: "",
            // Voice Typing
            voiceTypingEngine = keyStore.voiceTypingEngine,
            openWhisprUrl = keyStore.openWhisprServerUrl,
            // Unleashed / Uncensored Engine
            isUnleashed = keyStore.isUnleashed
        )
    }

    fun setUnleashed(enabled: Boolean) {
        keyStore.isUnleashed = enabled
        state = state.copy(isUnleashed = enabled)
    }

    // ─── Multi-Account API Pool Actions ───────────────────────────────────

    fun addProviderAccount(provider: AIProviderType, label: String, apiKey: String) {
        val trimmedKey = apiKey.trim()
        val finalLabel = label.trim().ifBlank { "${provider.displayName} Key" }
        val newAccount = ProviderAccount(
            provider = provider,
            label = finalLabel,
            apiKey = trimmedKey
        )
        keyStore.addProviderAccount(newAccount)
        // Also update legacy single-key store for compatibility without overwriting earlier accounts
        keyStore.setLegacyKey(provider, trimmedKey)
        loadState()
    }

    fun toggleProviderAccount(accountId: String, isActive: Boolean) {
        val account = keyStore.getAllProviderAccounts().find { it.id == accountId } ?: return
        keyStore.updateProviderAccount(account.copy(isActive = isActive))
        loadState()
    }

    fun deleteProviderAccount(accountId: String) {
        keyStore.deleteProviderAccount(accountId)
        loadState()
    }

    // ─── Multi-Account GitHub & Custom Scope Link ─────────────────────────

    fun toggleGitHubScope(scope: String) {
        val current = state.selectedGitHubScopes.toMutableList()
        if (current.contains(scope)) {
            current.remove(scope)
        } else {
            current.add(scope)
        }
        state = state.copy(selectedGitHubScopes = current)
    }

    fun buildCustomGitHubTokenUrl(): String {
        val scopesString = state.selectedGitHubScopes.joinToString(",")
        return "https://github.com/settings/tokens/new?description=Dettle+Android+Agent&scopes=$scopesString"
    }

    fun addGitHubAccount(label: String, pat: String, username: String?) {
        val trimmedPat = pat.trim()
        val finalLabel = label.trim().ifBlank { "GitHub Account" }
        val newAccount = GitHubAccount(
            label = finalLabel,
            pat = trimmedPat,
            username = username?.trim()?.ifBlank { null },
            selectedScopes = state.selectedGitHubScopes,
            isActive = true
        )
        keyStore.addGitHubAccount(newAccount)
        keyStore.githubPat = trimmedPat
        loadState()
    }

    fun setActiveGitHubAccount(id: String) {
        keyStore.setActiveGitHubAccount(id)
        loadState()
    }

    fun deleteGitHubAccount(id: String) {
        keyStore.deleteGitHubAccount(id)
        loadState()
    }

    // ─── Drive ────────────────────────────────────────────────────────────

    fun onDriveSignInResult(email: String, idToken: String) {
        keyStore.driveIdToken = idToken
        keyStore.driveUserEmail = email
        state = state.copy(driveConnected = true, driveUserEmail = email)
    }

    fun disconnectDrive() {
        driveConnector.signOut()
        state = state.copy(driveConnected = false, driveUserEmail = "")
    }

    // ─── Legacy Single Key Setters ────────────────────────────────────────

    fun saveGroqKey(key: String) {
        addProviderAccount(AIProviderType.GROQ, "Groq Key", key)
    }

    fun saveGeminiKey(key: String) {
        addProviderAccount(AIProviderType.GEMINI, "Gemini Key", key)
    }

    fun saveOpenRouterKey(key: String) {
        addProviderAccount(AIProviderType.OPENROUTER, "OpenRouter Key", key)
    }

    fun saveSambaNovaKey(key: String) {
        addProviderAccount(AIProviderType.SAMBANOVA, "SambaNova Key", key)
    }

    fun saveGitHubModelsKey(key: String) {
        addProviderAccount(AIProviderType.GITHUB_MODELS, "GitHub Models Key", key)
    }

    fun saveGithubPat(pat: String) {
        addGitHubAccount("Primary Account", pat, null)
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

    fun setVoiceTypingEngine(engine: String) {
        keyStore.voiceTypingEngine = engine
        state = state.copy(voiceTypingEngine = engine)
    }

    fun setOpenWhisprUrl(url: String) {
        keyStore.openWhisprServerUrl = url
        state = state.copy(openWhisprUrl = url)
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
    // Multi-account pooling & metrics
    val providerAccounts: List<ProviderAccount> = emptyList(),
    val aggregateMetrics: AggregateAccountMetrics = AggregateAccountMetrics(),
    val gitHubAccounts: List<GitHubAccount> = emptyList(),
    val selectedGitHubScopes: List<String> = listOf("repo", "workflow", "read:org", "user:email"),
    // Google Drive
    val driveConnected: Boolean = false,
    val driveUserEmail: String = "",
    // Voice Typing (OpenWhispr)
    val voiceTypingEngine: String = "ON_DEVICE_DSP",
    val openWhisprUrl: String = "http://10.0.2.2:8080/v1/audio/transcriptions",
    // Unleashed Engine
    val isUnleashed: Boolean = false
)
