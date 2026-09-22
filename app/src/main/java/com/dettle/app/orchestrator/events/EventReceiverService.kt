package com.dettle.app.orchestrator.events

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "EventReceiverService"

/**
 * Entry point for event-driven autonomy.
 *
 * Firebase Cloud Messaging is optional and not bundled in this build, so this
 * is a standard Hilt Service. External wake-ups (FCM, alarms, explicit intents)
 * can still deliver event_type + payload extras.
 */
@AndroidEntryPoint
class EventReceiverService : Service() {

    @Inject
    lateinit var eventRouter: EventRouter

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val eventType = intent?.getStringExtra("event_type") ?: return START_NOT_STICKY
        val payload = intent.getStringExtra("payload").orEmpty()
        Log.d(TAG, "Routing event type: $eventType")
        serviceScope.launch {
            eventRouter.handleAutonomousEvent(eventType, payload)
        }
        return START_NOT_STICKY
    }
}
