package com.dettle.app.domain.model

import java.util.UUID

/**
 * Represents an individual subscription/web provider account.
 * Users can add unlimited accounts (e.g. 5 ChatGPT accounts, 3 Claude accounts, custom web providers)
 * and delete any account on demand.
 */
data class WebViewAccount(
    val id: String = UUID.randomUUID().toString(),
    val providerType: AIProviderType,
    val label: String,
    val loginUrl: String = providerType.loginUrl,
    val baseUrl: String = providerType.baseUrl,
    val isEnabled: Boolean = true,
    val requestsUsed: Long = 0,
    val tokensUsed: Long = 0,
    val isLoggedIn: Boolean = false
)
