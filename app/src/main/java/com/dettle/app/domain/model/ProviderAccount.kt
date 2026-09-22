package com.dettle.app.domain.model

import java.util.UUID

/**
 * Represents an individual API key or login account for an AI provider.
 * Users can add unlimited accounts per provider (e.g. 10 ChatGPT keys, 5 Claude keys).
 */
data class ProviderAccount(
    val id: String = UUID.randomUUID().toString(),
    val provider: AIProviderType,
    val label: String,
    val apiKey: String,
    val isActive: Boolean = true,
    val requestsUsed: Long = 0,
    val tokensUsed: Long = 0,
    val rateLimitedUntilMs: Long = 0
)

/**
 * Represents a connected GitHub account with specific OAuth/PAT permission scopes.
 */
data class GitHubAccount(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val pat: String,
    val username: String? = null,
    val selectedScopes: List<String> = emptyList(),
    val isActive: Boolean = true
)

/**
 * Aggregate mathematics across all connected accounts.
 */
data class AggregateAccountMetrics(
    val totalAccounts: Int = 0,
    val activeAccounts: Int = 0,
    val totalRequests: Long = 0,
    val totalTokens: Long = 0,
    val providerBreakdown: Map<AIProviderType, ProviderMetrics> = emptyMap()
)

data class ProviderMetrics(
    val provider: AIProviderType,
    val accountCount: Int,
    val activeCount: Int,
    val totalRequests: Long,
    val totalTokens: Long
)
