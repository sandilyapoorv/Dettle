package com.dettle.app.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates instant, zero-asset procedural audio feedback and micro-haptics
 * using Android's native ToneGenerator and Vibrator.
 * 100% offline, zero MB assets, zero latency.
 */
@Singleton
class ProceduralAudioService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var toneGenerator: ToneGenerator? = null

    init {
        runCatching {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        }
    }

    private val vibrator: Vibrator? by lazy {
        runCatching {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }.getOrNull()
    }

    /** Plays an ascending melodic chime on task or build success */
    fun playSuccessChime() {
        scope.launch {
            runCatching {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
                vibrate(40)
                delay(90)
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 100)
            }
        }
    }

    /** Plays an arpeggiated level-up fanfare */
    fun playLevelUpFanfare() {
        scope.launch {
            runCatching {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE, 80)
                vibrate(50)
                delay(90)
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
                delay(90)
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 160)
                vibrate(80)
            }
        }
    }

    /** Plays a low alert buzz on policy block or streak warning */
    fun playAlertBuzz() {
        scope.launch {
            runCatching {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 180)
                vibrate(120)
            }
        }
    }

    private fun vibrate(durationMs: Long) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        }
    }
}
