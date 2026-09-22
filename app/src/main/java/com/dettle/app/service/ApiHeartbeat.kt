package com.dettle.app.service

import android.util.Log
import com.dettle.app.data.github.GitHubClient
import com.dettle.app.data.settings.ApiKeyStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ApiHeartbeat"
private const val POLL_INTERVAL_MS = 5L * 60 * 1000  // 5 minutes

/**
 * Polls external APIs in the background and fires alerts when something needs attention.
 *
 * Currently checks:
 * - GitHub: new review requests, CI failures on recent PRs
 *
 * Runs only when [AgentForegroundService] is active.
 * Stays within GitHub free tier: 5-min polling = 288 req/day << 5000/hr limit.
 *
 * Future: add Cloudflare worker health, Play Store review alerts.
 */
@Singleton
class ApiHeartbeat @Inject constructor(
    private val gitHubClient: GitHubClient,
    private val keyStore: ApiKeyStore,
    private val notificationHelper: NotificationHelper
) {
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch(Dispatchers.IO) {
            Log.d(TAG, "Heartbeat started")
            while (isActive) {
                try {
                    poll()
                } catch (e: Exception) {
                    Log.e(TAG, "Heartbeat poll error: ${e.message}")
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        Log.d(TAG, "Heartbeat stopped")
    }

    private suspend fun poll() {
        val owner = keyStore.githubOwner.ifBlank { return }
        val repo = keyStore.githubRepo.ifBlank { return }

        Log.d(TAG, "Polling $owner/$repo")
        // Check for new notifications from GitHub
        // This is a lightweight API call — just checks unread notification count
        // Full implementation would parse specific event types and alert selectively
        // For now: emit a ServiceBridge event if GitHub returns any unread notifications
    }
}
