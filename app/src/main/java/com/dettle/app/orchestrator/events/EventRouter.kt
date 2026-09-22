package com.dettle.app.orchestrator.events

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.dettle.app.orchestrator.swarm.SwarmOrchestrator
import com.dettle.app.orchestrator.telemetry.AgentLogger
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "EventRouter"

/**
 * Routes background events to the appropriate AI systems (Swarm, Healing, etc.)
 */
@Singleton
class EventRouter @Inject constructor(
    private val context: Context,
    private val swarmOrchestrator: SwarmOrchestrator,
    private val agentLogger: AgentLogger
) {
    companion object {
        const val EVENT_GITHUB_ISSUE = "github_issue_opened"
        const val EVENT_CRASHLYTICS = "crashlytics_fatal_crash"
    }

    suspend fun handleAutonomousEvent(eventType: String, payload: String) {
        agentLogger.logSwarmEvent("EVENT_ROUTER", "Received autonomous trigger: $eventType")

        when (eventType) {
            EVENT_CRASHLYTICS -> handleProductionCrash(payload)
            EVENT_GITHUB_ISSUE -> handleGithubIssue(payload)
            else -> Log.w(TAG, "Unknown event type: $eventType")
        }
    }

    private suspend fun handleProductionCrash(stackTrace: String) {
        Log.d(TAG, "Initiating Autonomous Crash Resolution...")
        showLocalNotification(
            "Dettle AI: Crash Detected", 
            "A production crash occurred. The Swarm is analyzing the stack trace..."
        )

        // 1. Pass the exact stack trace to the Swarm Orchestrator
        val goal = """
            A fatal crash just occurred in production.
            Here is the stack trace:
            $stackTrace
            
            Identify the file, write the fix, and ensure it passes validation.
        """.trimIndent()

        // 2. The Swarm plans, fragments, codes, and self-heals
        val result = swarmOrchestrator.executeSwarmTask(goal)

        // 3. Notify the user that it fixed the code while they were busy
        showLocalNotification(
            "Dettle AI: Crash Fixed", 
            "The Swarm identified the null pointer and prepared a patch. Tap to review."
        )
        
        agentLogger.logSwarmEvent("EVENT_ROUTER", "Crash Resolution Complete", result)
    }

    private suspend fun handleGithubIssue(issueBody: String) {
        Log.d(TAG, "Initiating Autonomous Issue Resolution...")
        showLocalNotification(
            "Dettle AI: New Issue", 
            "A new GitHub issue was opened. Reading requirements..."
        )

        val goal = """
            A user just opened this issue on the repository:
            $issueBody
            
            Analyze the requirement, write the feature, and prepare the branch.
        """.trimIndent()

        swarmOrchestrator.executeSwarmTask(goal)

        showLocalNotification(
            "Dettle AI: Branch Ready", 
            "The Swarm finished coding the issue. The branch is ready for your review."
        )
    }

    private fun showLocalNotification(title: String, message: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "dettle_autonomous_events"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Autonomous AI Events",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Placeholder icon
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
