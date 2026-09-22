package com.dettle.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dettle.app.MainActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized notification management for Dettle.
 *
 * Channels:
 *  - AGENT     : foreground service status (LOW importance, no sound)
 *  - ALERTS    : important agent updates (HIGH importance, makes sound)
 *  - REMINDERS : scheduled AI-set reminders (DEFAULT importance)
 *  - PROJECTS  : PR merged, CI failed, repo events (HIGH importance)
 */
@Singleton
class NotificationHelper @Inject constructor(
    private val context: Context
) {
    companion object {
        const val CHANNEL_AGENT     = "dettle_agent"
        const val CHANNEL_ALERTS    = "dettle_alerts"
        const val CHANNEL_REMINDERS = "dettle_reminders"
        const val CHANNEL_PROJECTS  = "dettle_projects"

        const val ID_FOREGROUND   = 1001
        private var nextId = 2000
        fun nextNotificationId() = nextId++
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        listOf(
            NotificationChannel(CHANNEL_AGENT, "Dettle Agent", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Background agent status"
                setShowBadge(false)
            },
            NotificationChannel(CHANNEL_ALERTS, "Agent Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Important updates from the AI agent"
            },
            NotificationChannel(CHANNEL_REMINDERS, "Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Reminders set by the AI"
            },
            NotificationChannel(CHANNEL_PROJECTS, "Project Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "PR, CI, and repo events"
            }
        ).forEach { manager.createNotificationChannel(it) }
    }

    /**
     * Send an alert notification immediately.
     * Used when the agent calls the send_notification tool.
     */
    fun sendAlert(
        title: String,
        body: String,
        channel: String = CHANNEL_ALERTS,
        id: Int = nextNotificationId(),
        deepLink: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            deepLink?.let { putExtra("deep_link", it) }
        }
        val pending = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channel)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            android.util.Log.w("NotificationHelper", "Notification permission not granted")
        }
    }

    /** Send a reminder (scheduled by ReminderWorker) */
    fun sendReminder(title: String, body: String) {
        sendAlert(title, body, channel = CHANNEL_REMINDERS)
    }

    /** Send a project event alert (PR merged, CI failed, etc.) */
    fun sendProjectAlert(title: String, body: String, projectName: String = "") {
        sendAlert(
            title = if (projectName.isNotBlank()) "[$projectName] $title" else title,
            body = body,
            channel = CHANNEL_PROJECTS
        )
    }
}
