package com.dettle.app.service

import android.util.Log
import com.dettle.app.domain.model.ApiMessage
import com.dettle.app.orchestrator.LoopEvent
import com.dettle.app.orchestrator.ReActLoop
import com.dettle.app.orchestrator.mode.GoalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ActiveGoalRunner"

/**
 * Runs a GOAL mode loop in the background service process.
 * 
 * If the user switches away from the app, this keeps running.
 * Progress is emitted via ServiceBridge so the UI can update if open,
 * and critical events trigger notifications via NotificationHelper.
 */
@Singleton
class ActiveGoalRunner @Inject constructor(
    private val reActLoop: ReActLoop,
    private val goalRepository: GoalRepository,
    private val notificationHelper: NotificationHelper
) {
    private var currentJob: Job? = null
    private var activeGoalId: String? = null

    fun startGoal(goalId: String, scope: CoroutineScope) {
        if (currentJob?.isActive == true) {
            Log.w(TAG, "Goal already running. Ignoring start request for $goalId")
            return
        }
        activeGoalId = goalId

        currentJob = scope.launch(Dispatchers.IO) {
            Log.d(TAG, "Starting goal: $goalId")
            
            try {
                // Read goal state
                val goal = goalRepository.getGoal(goalId) ?: return@launch
                
                // Construct history (in a real app, this would rehydrate full history)
                val history = mutableListOf<ApiMessage>()
                
                // For now, we simulate starting the loop. 
                // Full integration requires updating ReActLoop to take AgentMode/Goal explicitly.
                
                notificationHelper.sendAlert(
                    title = "Goal Started",
                    body = "Working on: ${goal.description.take(50)}...",
                    channel = NotificationHelper.CHANNEL_AGENT
                )

                // TODO: Fully wire reActLoop.run(...) here once signature is updated
                
            } catch (e: Exception) {
                Log.e(TAG, "Goal execution failed", e)
                notificationHelper.sendAlert(
                    title = "Goal Failed",
                    body = e.message ?: "Unknown error",
                    channel = NotificationHelper.CHANNEL_ALERTS
                )
            } finally {
                activeGoalId = null
            }
        }
    }

    suspend fun stopGoal() {
        currentJob?.cancelAndJoin()
        currentJob = null
        activeGoalId = null
        Log.d(TAG, "Goal stopped")
    }
}
