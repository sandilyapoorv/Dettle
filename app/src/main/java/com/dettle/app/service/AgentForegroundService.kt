package com.dettle.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.dettle.app.MainActivity
import com.dettle.app.data.settings.ApiKeyStore
import com.dettle.app.data.webview.WebViewPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import com.dettle.app.orchestrator.learning.LearningEngine
import com.dettle.app.service.ApiHeartbeat

/**
 * Foreground service keeping the app, WebViewPool, and overnight loop alive.
 * Upgraded to Personal AI Background Daemon.
 *
 * Android will kill our process (and all WebViews + cookies) when the user
 * switches apps unless we hold a foreground service. The persistent notification
 * is required by Android OS for this.
 *
 * Lifecycle:
 *  - Started when user sends their first message (or manually via toggle)
 *  - WebViewPool initializes all provider sessions here
 *  - Overnight loop: starts/stops via ACTION_START_OVERNIGHT / ACTION_STOP_OVERNIGHT
 *  - Background daemon: ApiHeartbeat, LearningEngine, ServiceBridge
 */
@AndroidEntryPoint
class AgentForegroundService : Service() {

    @Inject lateinit var webViewPool: WebViewPool
    @Inject lateinit var keyStore: ApiKeyStore
    @Inject lateinit var overnightLoop: OvernightLoop
    @Inject lateinit var learningEngine: LearningEngine
    @Inject lateinit var apiHeartbeat: ApiHeartbeat

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Dettle Agent is active"))

        // Initialize WebView pool — loads all provider sessions in the background
        val gistUrl = keyStore.selectorRegistryGistUrl
        webViewPool.initialize(gistUrl)
        
        // Start background daemon processes
        learningEngine.startProcessing(serviceScope)
        apiHeartbeat.start(serviceScope)
        ServiceBridge.tryEmit(DaemonEvent.ServiceStarted)
    }

    override fun onDestroy() {
        super.onDestroy()
        ServiceBridge.tryEmit(DaemonEvent.ServiceStopped)
        webViewPool.destroy()
        overnightLoop.stop()
        apiHeartbeat.stop()
        serviceScope.cancel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                updateNotification("Dettle Agent running...")
            }
            ACTION_STOP -> {
                overnightLoop.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_UPDATE_STATUS -> {
                val status = intent.getStringExtra(EXTRA_STATUS) ?: "Active"
                updateNotification(status)
            }
            ACTION_START_OVERNIGHT -> {
                overnightLoop.start()
                updateNotification("🌙 Overnight run active...")
            }
            ACTION_STOP_OVERNIGHT -> {
                overnightLoop.interrupt()
                updateNotification("Dettle Agent running...")
            }
        }
        return START_STICKY  // Restart if killed by system
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(status: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Dettle")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.ic_menu_compass) // Replace with custom icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(status: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(status))
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Dettle Agent",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the AI agent running in the background"
            setShowBadge(false)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "dettle_agent_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.dettle.START"
        const val ACTION_STOP = "com.dettle.STOP"
        const val ACTION_UPDATE_STATUS = "com.dettle.UPDATE_STATUS"
        const val ACTION_START_OVERNIGHT = "com.dettle.START_OVERNIGHT"
        const val ACTION_STOP_OVERNIGHT = "com.dettle.STOP_OVERNIGHT"
        const val EXTRA_STATUS = "extra_status"

        fun startIntent(context: Context) = Intent(context, AgentForegroundService::class.java)
            .apply { action = ACTION_START }

        fun stopIntent(context: Context) = Intent(context, AgentForegroundService::class.java)
            .apply { action = ACTION_STOP }

        fun startOvernightIntent(context: Context) = Intent(context, AgentForegroundService::class.java)
            .apply { action = ACTION_START_OVERNIGHT }

        fun stopOvernightIntent(context: Context) = Intent(context, AgentForegroundService::class.java)
            .apply { action = ACTION_STOP_OVERNIGHT }

        fun updateStatusIntent(context: Context, status: String) =
            Intent(context, AgentForegroundService::class.java).apply {
                action = ACTION_UPDATE_STATUS
                putExtra(EXTRA_STATUS, status)
            }
    }
}
