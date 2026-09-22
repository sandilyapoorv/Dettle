package com.dettle.app.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
        private const val ERROR_LANGUAGE_NOT_SUPPORTED = 12
        private const val ERROR_LANGUAGE_UNAVAILABLE = 13
        private const val ERROR_CANNOT_CHECK_SUPPORT = 14
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    private val _state = MutableStateFlow<VoiceTypingState>(VoiceTypingState.Idle)
    val state: StateFlow<VoiceTypingState> = _state.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private var onPartialResultCallback: ((String) -> Unit)? = null
    private var onFinalResultCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null
    private var onFallbackToSystemPromptCallback: (() -> Unit)? = null

    private var retryCount = 0

    val isRecognitionAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun createSystemSpeechIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now to type...")
        }
    }

    fun startListening(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: ((String) -> Unit)? = null,
        onFallbackToSystemPrompt: (() -> Unit)? = null
    ) {
        mainHandler.post {
            onPartialResultCallback = onPartial
            onFinalResultCallback = onFinal
            onErrorCallback = onError
            onFallbackToSystemPromptCallback = onFallbackToSystemPrompt
            retryCount = 0

            if (!isRecognitionAvailable) {
                Log.w(TAG, "Speech recognition service not found via SpeechRecognizer; falling back to system intent")
                onFallbackToSystemPrompt?.invoke()
                    ?: onError?.invoke("Speech recognition is not available on this device.")
                return@post
            }

            startListeningInternal(fallbackMode = false)
        }
    }

    private fun startListeningInternal(fallbackMode: Boolean) {
        stopListeningInternal()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener(fallbackMode))
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                if (!fallbackMode) {
                    val langTag = Locale.getDefault().toLanguageTag()
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
                }
            }

            speechRecognizer?.startListening(intent)
            _isListening.value = true
            _state.value = VoiceTypingState.Listening(0f)
            Log.d(TAG, "SpeechRecognizer started listening (fallbackMode=$fallbackMode)")
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting SpeechRecognizer: ${e.message}", e)
            _isListening.value = false
            _state.value = VoiceTypingState.Error(e.message ?: "Failed to start speech recognition")
            onFallbackToSystemPromptCallback?.invoke()
                ?: onErrorCallback?.invoke(e.message ?: "Failed to start speech recognition")
        }
    }

    fun stopListening() {
        mainHandler.post {
            stopListeningInternal()
        }
    }

    private fun stopListeningInternal() {
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

    private fun createListener(fallbackMode: Boolean) = object : RecognitionListener {
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
            Log.w(TAG, "SpeechRecognizer onError: code $error (fallbackMode=$fallbackMode, retryCount=$retryCount)")

            // Handle Error 12 (ERROR_LANGUAGE_NOT_SUPPORTED) or Error 13 (ERROR_LANGUAGE_UNAVAILABLE)
            if ((error == ERROR_LANGUAGE_UNAVAILABLE || error == ERROR_LANGUAGE_NOT_SUPPORTED || error == SpeechRecognizer.ERROR_CLIENT) && retryCount == 0 && !fallbackMode) {
                retryCount++
                Log.i(TAG, "Language pack not available offline (code $error). Automatically retrying with system default language...")
                mainHandler.post {
                    startListeningInternal(fallbackMode = true)
                }
                return
            }

            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                SpeechRecognizer.ERROR_NETWORK -> "Network connection required for voice typing"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Voice network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                SpeechRecognizer.ERROR_SERVER -> "Recognition server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard"
                ERROR_LANGUAGE_NOT_SUPPORTED -> "Language not supported"
                ERROR_LANGUAGE_UNAVAILABLE -> "Language model not available on device"
                ERROR_CANNOT_CHECK_SUPPORT -> "Cannot check speech support"
                else -> "Speech recognition error (code $error)"
            }

            _isListening.value = false
            _rmsDb.value = 0f
            _state.value = VoiceTypingState.Error(errorMessage)

            // Non-fatal scenarios (timeout / silence) simply return to Idle
            if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                _state.value = VoiceTypingState.Idle
                return
            }

            // If background SpeechRecognizer fails with language or client error even after retry, trigger the system dialog
            if (error == ERROR_LANGUAGE_UNAVAILABLE || error == ERROR_LANGUAGE_NOT_SUPPORTED || error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_SERVER) {
                if (onFallbackToSystemPromptCallback != null) {
                    Log.i(TAG, "Delegating to system speech input dialog due to error $error")
                    onFallbackToSystemPromptCallback?.invoke()
                    return
                }
            }

            onErrorCallback?.invoke(errorMessage)
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
