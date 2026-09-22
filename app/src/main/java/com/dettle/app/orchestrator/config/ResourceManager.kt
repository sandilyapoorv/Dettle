package com.dettle.app.orchestrator.config

import android.content.ComponentCallbacks2
import android.util.Log
import com.dettle.app.data.webview.WebViewPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ResourceManager"

/**
 * Actively manages memory allocation across Dettle.
 * The user has specified strict hardware limits:
 * - 250 MB max permanent memory (Background)
 * - 1.3 GB peak memory (Foreground)
 * 
 * This manager listens to Android's memory trim events and application lifecycle
 * to dynamically spin up or destroy heavy components (like WebViews or deep caches).
 */
@Singleton
class ResourceManager @Inject constructor(
    private val webViewPool: WebViewPool
) {
    private var isForeground = false

    /**
     * Called by MainActivity onStart()
     */
    fun onAppForegrounded() {
        Log.d(TAG, "App Foregrounded. Expanding to 1.3GB RAM envelope.")
        isForeground = true
        
        CoroutineScope(Dispatchers.Main).launch {
            // Wake up headless WebViews for fast DOM scraping
            webViewPool.initialize()
        }
    }

    /**
     * Called by MainActivity onStop()
     */
    fun onAppBackgrounded() {
        Log.d(TAG, "App Backgrounded. Compacting to 250MB RAM envelope.")
        isForeground = false
        
        CoroutineScope(Dispatchers.Main).launch {
            // WebViews are massive RAM hogs (Chromium instances). 
            // We must destroy them to stay under 250MB.
            webViewPool.destroy()
        }
        
        // Hint the JVM to GC (though not guaranteed, it's good practice for aggressive compacting)
        System.gc()
    }

    /**
     * Called by Application.onTrimMemory()
     */
    fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            onAppBackgrounded()
        }
    }
}
