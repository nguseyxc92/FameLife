package com.example.media

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

private const val TAG = "SpeechAdapter"

enum class SpeechEngineState {
    IDLE,
    LISTENING,
    MUTED,
    UNAVAILABLE
}

class SpeechRecognitionAdapter(
    private val context: Context,
    private val onPartialTranscript: (String) -> Unit,
    private val onFinalTranscript: (String) -> Unit,
    private val onStateChanged: (SpeechEngineState) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var shouldKeepListening = false
    private var languageTag = "en-US"

    fun startListening(languageTag: String = "en-US") {
        this.languageTag = languageTag
        mainHandler.post {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.w(TAG, "On-device SpeechRecognizer service not available on this device.")
                onStateChanged(SpeechEngineState.UNAVAILABLE)
                return@post
            }
            shouldKeepListening = true
            ensureRecognizer()
            beginCycle()
        }
    }

    fun stopListening(isMuted: Boolean = true) {
        shouldKeepListening = false
        mainHandler.post {
            try {
                recognizer?.stopListening()
                recognizer?.cancel()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping recognizer", e)
            }
            onStateChanged(if (isMuted) SpeechEngineState.MUTED else SpeechEngineState.IDLE)
        }
    }

    fun release() {
        shouldKeepListening = false
        mainHandler.post {
            try {
                recognizer?.destroy()
            } catch (e: Exception) {
                Log.w(TAG, "Error destroying recognizer", e)
            }
            recognizer = null
            onStateChanged(SpeechEngineState.IDLE)
        }
    }

    private fun ensureRecognizer() {
        if (recognizer != null) return
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    onStateChanged(SpeechEngineState.LISTENING)
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    if (!shouldKeepListening) return
                    val restartDelay = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 350L
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 700L
                        else -> 1400L
                    }
                    mainHandler.postDelayed({
                        if (shouldKeepListening) beginCycle()
                    }, restartDelay)
                }

                override fun onResults(results: Bundle?) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.trim()
                    if (!text.isNullOrBlank()) {
                        onFinalTranscript(text)
                    }
                    if (shouldKeepListening) {
                        mainHandler.postDelayed({ beginCycle() }, 250L)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.trim()
                    if (!partial.isNullOrBlank()) {
                        onPartialTranscript(partial)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun beginCycle() {
        if (!shouldKeepListening) return
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed starting listening cycle", e)
            onStateChanged(SpeechEngineState.UNAVAILABLE)
        }
    }
}
