package com.dettle.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.dettle.app.ui.navigation.DettleNavGraph
import com.dettle.app.ui.theme.DettleTheme
import com.dettle.app.ui.theme.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themeManager: ThemeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        enableEdgeToEdge()
        setContent {
            val themeConfig by themeManager.themeConfig.collectAsState()
            DettleTheme(themeConfig = themeConfig) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DettleNavGraph()
                }
            }
        }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        when (ev.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
                android.util.Log.d("DettleTouch", "👇 TOUCH_DOWN at (${ev.x.toInt()}, ${ev.y.toInt()})")
            }
            android.view.MotionEvent.ACTION_UP -> {
                android.util.Log.d("DettleClick", "👆 TAP / CLICK at (${ev.x.toInt()}, ${ev.y.toInt()})")
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}
