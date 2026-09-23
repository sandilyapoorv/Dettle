package com.dettle.app.data.api

import android.util.Log
import com.dettle.app.domain.model.AIModel
import com.dettle.app.domain.model.ApiMessage
import com.dettle.app.domain.model.FreeModels
import com.dettle.app.domain.model.Tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "KeyPoolManager"

/** Tracks per-model usage and cooldown state */
data class ProviderState(
    val model: AIModel,
    var isOnCooldown: Boolean = false,
    var cooldownUntilMs: Long = 0L,
    var totalRequestsToday: Int = 0,
    var totalTokensToday: Int = 0,
    var lastUsedMs: Long = 0L
) {
    fun isAvailable(): Boolean {
        if (isOnCooldown && System.currentTimeMillis() > cooldownUntilMs) {
            isOnCooldown = false
        }
        if (isOnCooldown) return false
        if (model.dailyRequestLimit > 0 && totalRequestsToday >= model.dailyRequestLimit) return false
        if (model.dailyTokenLimit > 0 && totalTokensToday >= model.dailyTokenLimit) return false
        return true
    }
}

/**
 * Manages a pool of AI providers with:
 * - Round-robin routing across providers in waterfall priority order
 * - Per-provider 429 cooldown (60s default)
 * - Daily limit tracking (resets at midnight)
 * - Automatic failover when one provider is exhausted
 *
 * Usage: call [chat] — it picks the best available provider automatically.
 */
@Singleton
class KeyPoolManager @Inject constructor(
    private val providerFactory: AIProviderFactory,
    private val agentLogger: com.dettle.app.orchestrator.telemetry.AgentLogger
) {
    private val states = FreeModels.WATERFALL_ORDER
        .map { ProviderState(it) }
        .toMutableList()

    private var lastResetDay = -1

    /**
     * Execute a chat request using the best available provider.
     * Automatically fails over if a provider returns 429 or is exhausted.
     */
    suspend fun chat(
        messages: List<ApiMessage>,
        tools: List<Tool> = emptyList(),
        systemPrompt: String? = null,
        maxTokens: Int = 4096,
        preferModel: AIModel? = null
    ): Flow<StreamChunk> = flow {

        resetDailyCountsIfNeeded()

        // Build ordered list: preferred model first, then waterfall
        val ordered = buildList {
            if (preferModel != null) {
                var prefState = states.find { it.model.modelId == preferModel.modelId }
                if (prefState == null) {
                    prefState = ProviderState(preferModel)
                    states.add(prefState)
                }
                add(prefState)
            }
            addAll(states.filter { it.model.modelId != preferModel?.modelId })
        }

        var lastError: String? = null
        for (state in ordered) {
            if (!state.isAvailable()) {
                Log.d(TAG, "Skipping ${state.model.displayName}: unavailable (cooldown or daily limit)")
                continue
            }

            val provider = providerFactory.create(state.model) ?: continue
            Log.d(TAG, "Using provider: ${state.model.displayName}")
            state.lastUsedMs = System.currentTimeMillis()
            state.totalRequestsToday++

            var hitRateLimit = false
            var hitError = false
            var streamBegan = false
            var totalTokensUsed = 0
            val startTime = System.currentTimeMillis()
            var fullResponse = ""

            // Log outbound prompt
            val messagesStr = messages.joinToString("\n") { "[${it.role.uppercase()}]: ${it.content?.take(500)}" }
            agentLogger.logPrompt(state.model.modelId, systemPrompt, messagesStr)

            try {
                provider.chat(messages, tools, systemPrompt, maxTokens)
                    .onEach { chunk ->
                        when (chunk) {
                            is StreamChunk.Token -> {
                                streamBegan = true
                                fullResponse += chunk.text
                            }
                            is StreamChunk.Done -> {
                                agentLogger.logResponse(
                                    modelId = state.model.modelId,
                                    rawResponse = fullResponse,
                                    durationMs = System.currentTimeMillis() - startTime,
                                    tokens = chunk.usage?.totalTokens ?: 0
                                )
                                totalTokensUsed = chunk.usage?.totalTokens ?: 0
                                state.totalTokensToday += totalTokensUsed
                                Log.d(TAG, "${state.model.displayName}: used $totalTokensUsed tokens today (total: ${state.totalTokensToday})")
                            }
                            is StreamChunk.Error -> {
                                if (chunk.isRateLimit) {
                                    hitRateLimit = true
                                    state.isOnCooldown = true
                                    state.cooldownUntilMs = System.currentTimeMillis() + 60_000L
                                    Log.w(TAG, "Rate limited on ${state.model.displayName}, cooling down 60s")
                                } else {
                                    hitError = true
                                    lastError = chunk.message
                                    Log.w(TAG, "Error on ${state.model.displayName}: ${chunk.message}")
                                }
                            }
                            else -> {}
                        }
                    }
                    .collect { chunk ->
                        if (chunk is StreamChunk.Error) {
                            lastError = chunk.message
                            if (!streamBegan) {
                                return@collect  // Will try next provider in pool
                            }
                        }
                        emit(chunk)
                    }

                if (!hitRateLimit && !hitError) return@flow  // Success — done
                if (streamBegan) return@flow // Tokens were already emitted to UI
            } catch (e: Exception) {
                Log.e(TAG, "Provider ${state.model.displayName} failed with exception: ${e.message}", e)
                lastError = e.message ?: "Provider error"
                if (streamBegan) return@flow
            }
        }

        // All providers exhausted
        emit(StreamChunk.Error(
            lastError ?: "All providers are rate-limited or unavailable. Please verify your API keys in Settings.",
            isRateLimit = false
        ))
    }.flowOn(Dispatchers.IO)

    /** Get current status of all providers (for the Settings/Agents screen) */
    fun getProviderStatuses(): List<ProviderStatus> {
        resetDailyCountsIfNeeded()
        return states.map { state ->
            ProviderStatus(
                model = state.model,
                isAvailable = state.isAvailable(),
                isOnCooldown = state.isOnCooldown,
                cooldownRemainingMs = if (state.isOnCooldown) maxOf(0, state.cooldownUntilMs - System.currentTimeMillis()) else 0,
                requestsUsedToday = state.totalRequestsToday,
                tokensUsedToday = state.totalTokensToday,
                dailyRequestLimit = state.model.dailyRequestLimit,
                dailyTokenLimit = state.model.dailyTokenLimit
            )
        }
    }

    private fun resetDailyCountsIfNeeded() {
        val today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        if (today != lastResetDay) {
            lastResetDay = today
            states.forEach { state ->
                state.totalRequestsToday = 0
                state.totalTokensToday = 0
                state.isOnCooldown = false
                Log.d(TAG, "Daily counts reset for ${state.model.displayName}")
            }
        }
    }
}

data class ProviderStatus(
    val model: AIModel,
    val isAvailable: Boolean,
    val isOnCooldown: Boolean,
    val cooldownRemainingMs: Long,
    val requestsUsedToday: Int,
    val tokensUsedToday: Int,
    val dailyRequestLimit: Int,
    val dailyTokenLimit: Int
) {
    val requestsPercentUsed: Float get() =
        if (dailyRequestLimit > 0) requestsUsedToday.toFloat() / dailyRequestLimit else 0f
    val tokensPercentUsed: Float get() =
        if (dailyTokenLimit > 0) tokensUsedToday.toFloat() / dailyTokenLimit else 0f
}
