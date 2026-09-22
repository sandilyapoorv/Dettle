package com.dettle.app.ui.authvault

import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dettle.app.data.webview.WebViewPool
import com.dettle.app.domain.model.AIProviderType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthVaultViewModel @Inject constructor(
    private val webViewPool: WebViewPool
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthVaultUiState())
    val uiState: StateFlow<AuthVaultUiState> = _uiState.asStateFlow()

    init {
        refreshStatuses()

        // Watch for re-auth requests from WebViewPool
        viewModelScope.launch {
            webViewPool.needsReauthFor.collect { provider ->
                if (provider != null) {
                    _uiState.update { it.copy(
                        reauthAlert = provider,
                        providerStatuses = webViewPool.getPoolStatus()
                    )}
                }
            }
        }
    }

    fun refreshStatuses() {
        _uiState.update { it.copy(providerStatuses = webViewPool.getPoolStatus()) }
    }

    fun startLogin(providerType: AIProviderType) {
        val session = webViewPool.getOrCreateSession(providerType)
        session.show()
        session.initialize()
        _uiState.update { it.copy(activeLoginProvider = providerType, reauthAlert = null) }
    }

    fun onLoginDone() {
        val provider = _uiState.value.activeLoginProvider ?: return
        val session = webViewPool.getOrCreateSession(provider)
        session.hide()
        webViewPool.clearReauthFlag()
        _uiState.update { it.copy(activeLoginProvider = null) }
        refreshStatuses()
    }

    fun onLoginCancelled() {
        val provider = _uiState.value.activeLoginProvider ?: return
        val session = webViewPool.getOrCreateSession(provider)
        session.hide()
        _uiState.update { it.copy(activeLoginProvider = null) }
    }

    fun getWebViewForLogin(providerType: AIProviderType): WebView {
        val session = webViewPool.getOrCreateSession(providerType)
        session.initialize()
        return session.webView
    }
}

data class AuthVaultUiState(
    val providerStatuses: List<WebViewPool.PoolStatus> = emptyList(),
    val activeLoginProvider: AIProviderType? = null,
    val reauthAlert: AIProviderType? = null
)
