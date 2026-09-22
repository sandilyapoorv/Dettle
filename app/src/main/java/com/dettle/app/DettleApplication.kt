package com.dettle.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DettleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
