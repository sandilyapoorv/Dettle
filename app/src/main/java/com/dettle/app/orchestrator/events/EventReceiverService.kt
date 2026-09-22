package com.dettle.app.orchestrator.events

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "EventReceiverService"

/**
 * The crucial link for Event-Driven Autonomy on Android.
 * 
 * Since an Android phone isn't a server with a static IP, it can't directly 
 * receive HTTP webhooks from GitHub or Crashlytics.
 * 
 * Instead, GitHub/Crashlytics send a webhook to a tiny Cloudflare worker, 
 * which sends a silent FCM (Firebase Cloud Messaging) Push Notification to this device.
 * 
 * This service wakes up INSTANTLY when the push arrives, consuming 0 battery 
 * while idle, and hands the event to the Swarm.
 */
@AndroidEntryPoint
class EventReceiverService : FirebaseMessagingService() {

    @Inject
    lateinit var eventRouter: EventRouter

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        Log.d(TAG, "WAKE UP: Received silent background event via FCM.")
        
        // Data payload contains the webhook details (e.g., github_pr, crash_log)
        val data = message.data
        if (data.isNotEmpty()) {
            val eventType = data["event_type"] ?: "UNKNOWN"
            val payload = data["payload"] ?: ""
            
            Log.d(TAG, "Routing event type: $eventType")
            
            // Launch in background scope so the service can complete its FCM callback
            serviceScope.launch {
                eventRouter.handleAutonomousEvent(eventType, payload)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token generated: $token")
        // In a full implementation, you would send this token to your Cloudflare worker 
        // so it knows which device to wake up when GitHub webhooks fire.
    }
}
