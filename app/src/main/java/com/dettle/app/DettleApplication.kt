package com.dettle.app

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

@HiltAndroidApp
class DettleApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e("DettleApplication", "FATAL UNCAUGHT EXCEPTION in thread: ${thread.name}", throwable)
                val crashFile = File(cacheDir, "crash_log.txt")
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                crashFile.writeText("Thread: ${thread.name}\nTimestamp: ${System.currentTimeMillis()}\nMessage: ${throwable.message}\n\n$sw")
            } catch (e: Exception) {
                Log.e("DettleApplication", "Failed to record crash dump", e)
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
