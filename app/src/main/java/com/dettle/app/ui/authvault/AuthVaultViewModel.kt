package com.dettle.app.ui.authvault

import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dettle.app.data.settings.ApiKeyStore
import com.dettle.app.data.webview.WebViewPool
import com.dettle.app.domain.model.AIProviderType
import com.dettle.app.domain.model.WebViewAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthVaultViewModel @Inject constructor(
    private val webViewPool: WebViewPool,
    private val keyStore: ApiKeyStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthVaultUiState())
    val uiState: StateFlow<AuthVaultUiState> = _uiState.asStateFlow()

    init {
        refreshStatuses()

        // Watch for re-auth requests from WebViewPool
        viewModelScope.launch {
            webViewPool.needsReauthFor.collect { accountId ->
                if (accountId != null) {
                    _uiState.update { it.copy(
                        reauthAlert = accountId,
                        providerStatuses = webViewPool.getPoolStatus()
                    )}
                }
            }
        }
    }

    fun refreshStatuses() {
        _uiState.update { it.copy(providerStatuses = webViewPool.getPoolStatus()) }
    }

    fun startLogin(account: WebViewAccount) {
        val session = webViewPool.getOrCreateSession(account)
        session.show()
        session.loadLoginUrl()
        _uiState.update { it.copy(
            activeLoginAccount = account,
            reauthAlert = null,
            loginProgress = 10
        ) }
        session.onProgressUpdate = { progress ->
            _uiState.update { it.copy(loginProgress = progress) }
        }
    }

    fun onLoginDone() {
        val account = _uiState.value.activeLoginAccount ?: return
        val session = webViewPool.getOrCreateSession(account)
        session.onProgressUpdate = null
        session.hide()
        webViewPool.clearReauthFlag()

        // Mark account as logged in
        keyStore.updateWebViewAccount(account.copy(isLoggedIn = true))

        _uiState.update { it.copy(activeLoginAccount = null, loginProgress = 0) }
        refreshStatuses()
    }

    fun onLoginCancelled() {
        val account = _uiState.value.activeLoginAccount ?: return
        val session = webViewPool.getOrCreateSession(account)
        session.onProgressUpdate = null
        session.hide()
        _uiState.update { it.copy(activeLoginAccount = null, loginProgress = 0) }
    }

    fun getWebViewForLogin(account: WebViewAccount): WebView {
        val session = webViewPool.getOrCreateSession(account)
        return session.webView
    }

    fun reloadLogin() {
        val account = _uiState.value.activeLoginAccount ?: return
        val session = webViewPool.getOrCreateSession(account)
        session.reload()
    }

    fun addSubscriptionAccount(
        providerType: AIProviderType,
        label: String,
        customLoginUrl: String? = null,
        customBaseUrl: String? = null
    ) {
        val newAccount = WebViewAccount(
            providerType = providerType,
            label = label.ifBlank { "${providerType.displayName} Account" },
            loginUrl = customLoginUrl?.takeIf { it.isNotBlank() } ?: providerType.loginUrl,
            baseUrl = customBaseUrl?.takeIf { it.isNotBlank() } ?: providerType.baseUrl
        )
        keyStore.addWebViewAccount(newAccount)
        refreshStatuses()
    }

    fun deleteSubscriptionAccount(accountId: String) {
        webViewPool.destroySession(accountId)
        keyStore.deleteWebViewAccount(accountId)
        refreshStatuses()
    }

    fun toggleAccountEnabled(accountId: String) {
        val account = keyStore.getWebViewAccount(accountId) ?: return
        keyStore.updateWebViewAccount(account.copy(isEnabled = !account.isEnabled))
        if (account.isEnabled) {
            webViewPool.destroySession(accountId)
        }
        refreshStatuses()
    }
}

data class AuthVaultUiState(
    val providerStatuses: List<WebViewPool.PoolStatus> = emptyList(),
    val activeLoginAccount: WebViewAccount? = null,
    val reauthAlert: String? = null,
    val loginProgress: Int = 0
)
