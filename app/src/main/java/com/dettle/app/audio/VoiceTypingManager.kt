package com.dettle.app.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

sealed class VoiceTypingState {
    object Idle : VoiceTypingState()
    data class Listening(val rmsDb: Float = 0f) : VoiceTypingState()
    object Processing : VoiceTypingState()
    data class Error(val message: String) : VoiceTypingState()
}

@Singleton
class VoiceTypingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "VoiceTypingManager"
    }

    private var speechRecognizer: SpeechRecognizer? = null

    private val _state = MutableStateFlow<VoiceTypingState>(VoiceTypingState.Idle)
    val state: StateFlow<VoiceTypingState> = _state.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private var onPartialResultCallback: ((String) -> Unit)? = null
    private var onFinalResultCallback: ((String) -> Unit)? = null

    val isRecognitionAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: ((String) -> Unit)? = null
    ) {
        if (!isRecognitionAvailable) {
            val errorMsg = "Speech recognition is not supported or enabled on this device."
            _state.value = VoiceTypingState.Error(errorMsg)
            onError?.invoke(errorMsg)
            return
        }

        stopListening()

        onPartialResultCallback = onPartial
        onFinalResultCallback = onFinal

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener(onError))
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                // Prefer offline on-device processing if available on modern Android
                putExtra("android.speech.extra.PREFER_OFFLINE", true)
            }

            speechRecognizer?.startListening(intent)
            _isListening.value = true
            _state.value = VoiceTypingState.Listening(0f)
            Log.d(TAG, "SpeechRecognizer started listening")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start speech recognizer: ${e.message}", e)
            _isListening.value = false
            _state.value = VoiceTypingState.Error(e.message ?: "Failed to start voice typing")
            onError?.invoke(e.message ?: "Failed to start voice typing")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping recognizer: ${e.message}")
        } finally {
            speechRecognizer = null
            _isListening.value = false
            _rmsDb.value = 0f
            _state.value = VoiceTypingState.Idle
        }
    }

    private fun createListener(onErrorCallback: ((String) -> Unit)?) = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
            _state.value = VoiceTypingState.Listening(0f)
        }

        override fun onBeginningOfSpeech() {
            _state.value = VoiceTypingState.Listening(1f)
        }

        override fun onRmsChanged(rmsdB: Float) {
            val normalized = (rmsdB.coerceAtLeast(0f) / 10f).coerceIn(0f, 1f)
            _rmsDb.value = normalized
            if (_isListening.value) {
                _state.value = VoiceTypingState.Listening(normalized)
            }
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _state.value = VoiceTypingState.Processing
        }

        override fun onError(error: Int) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client recognition error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                SpeechRecognizer.ERROR_NETWORK -> "Network communication error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                SpeechRecognizer.ERROR_SERVER -> "Recognition server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard"
                else -> "Speech recognition error code: $error"
            }

            Log.w(TAG, "SpeechRecognizer error: $errorMessage (code: $error)")
            _isListening.value = false
            _rmsDb.value = 0f
            _state.value = VoiceTypingState.Error(errorMessage)

            // Do not report timeout/no-match as fatal, just reset to idle
            if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                _state.value = VoiceTypingState.Idle
            } else {
                onErrorCallback?.invoke(errorMessage)
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedText = matches?.firstOrNull()?.trim() ?: ""
            Log.d(TAG, "Final speech result: $recognizedText")

            _isListening.value = false
            _rmsDb.value = 0f
            _state.value = VoiceTypingState.Idle

            if (recognizedText.isNotEmpty()) {
                onFinalResultCallback?.invoke(recognizedText)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partialText = matches?.firstOrNull()?.trim() ?: return
            Log.d(TAG, "Partial speech result: $partialText")
            onPartialResultCallback?.invoke(partialText)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
