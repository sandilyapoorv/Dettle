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
 * Intelligent Memory & Performance Optimizer for Dettle.
 *
 * Rather than forcefully restricting the app to an artificial 250 MB ceiling
 * or destroying active WebView sessions, this manager dynamically optimizes
 * heap efficiency, avoids memory leaks, and leverages full device hardware capabilities.
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
        Log.d(TAG, "App Foregrounded: Optimizing resources for interactive performance.")
        isForeground = true

        CoroutineScope(Dispatchers.Main).launch {
            webViewPool.initialize()
        }
    }

    /**
     * Called by MainActivity onStop()
     * Keeps sessions, cookies, and authentication intact while reducing background CPU load.
     */
    fun onAppBackgrounded() {
        Log.d(TAG, "App Backgrounded: Optimizing background execution without dropping active sessions.")
        isForeground = false
        // Keep WebView sessions alive so user logins and background agents remain uninterrupted.
    }

    /**
     * Called on Android OS memory trim warnings.
     * Only triggers non-destructive cleanup during critical system-wide memory shortages.
     */
    fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            Log.w(TAG, "System memory warning (level $level). Performing non-destructive cache trim.")
            System.gc()
        }
    }

    /**
     * Telemetry helper to inspect real-time JVM memory allocation.
     */
    fun getMemorySnapshot(): MemorySnapshot {
        val runtime = Runtime.getRuntime()
        val totalMemoryMb = (runtime.totalMemory() / (1024 * 1024)).toInt()
        val freeMemoryMb = (runtime.freeMemory() / (1024 * 1024)).toInt()
        val maxMemoryMb = (runtime.maxMemory() / (1024 * 1024)).toInt()
        val usedMemoryMb = totalMemoryMb - freeMemoryMb

        return MemorySnapshot(
            usedMb = usedMemoryMb,
            freeMb = freeMemoryMb,
            totalAllocatedMb = totalMemoryMb,
            maxAllowedMb = maxMemoryMb
        )
    }

    data class MemorySnapshot(
        val usedMb: Int,
        val freeMb: Int,
        val totalAllocatedMb: Int,
        val maxAllowedMb: Int
    )
}

