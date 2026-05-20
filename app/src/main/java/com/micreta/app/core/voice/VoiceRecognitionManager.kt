package com.micreta.app.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.micreta.app.core.logging.EventLogger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Wrapper around Android's [SpeechRecognizer].
 *
 * Emits final transcripts through [results] (one-shot). Partial transcripts
 * are surfaced via [partial] so the UI can show what's being heard live.
 *
 * Note: SpeechRecognizer requires being created and called on the main thread.
 * The caller (typically a ViewModel) is responsible for that.
 */
class VoiceRecognitionManager(private val context: Context) {

    private val _available = MutableStateFlow(SpeechRecognizer.isRecognitionAvailable(context))
    val available: StateFlow<Boolean> = _available

    private val _listening = MutableStateFlow(false)
    val listening: StateFlow<Boolean> = _listening

    private val _partial = MutableStateFlow("")
    val partial: StateFlow<String> = _partial

    private val _results = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val results: SharedFlow<String> = _results.asSharedFlow()

    private val _errors = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    val errors: SharedFlow<Int> = _errors.asSharedFlow()

    private var recognizer: SpeechRecognizer? = null

    fun start() {
        if (!_available.value) {
            EventLogger.warn(TAG, "SpeechRecognizer not available on this device.")
            return
        }
        if (_listening.value) return
        val r = SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
        r.setRecognitionListener(buildListener())

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        r.startListening(intent)
        _listening.value = true
        _partial.value = ""
        EventLogger.info(TAG, "SpeechRecognizer started.")
    }

    fun stop() {
        recognizer?.let {
            try { it.stopListening() } catch (_: Throwable) {}
            try { it.destroy() } catch (_: Throwable) {}
        }
        recognizer = null
        _listening.value = false
        EventLogger.info(TAG, "SpeechRecognizer stopped.")
    }

    private fun buildListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            if (text.isNotBlank()) _partial.value = text
        }

        override fun onResults(results: Bundle?) {
            val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            val best = list.firstOrNull().orEmpty()
            EventLogger.info(TAG, "Final transcript: \"$best\"")
            if (best.isNotBlank()) _results.tryEmit(best)
            stop()
        }

        override fun onError(error: Int) {
            EventLogger.warn(TAG, "Speech error: ${errorName(error)}")
            _errors.tryEmit(error)
            stop()
        }
    }

    private fun errorName(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "AUDIO"
        SpeechRecognizer.ERROR_CLIENT -> "CLIENT"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "INSUFFICIENT_PERMISSIONS"
        SpeechRecognizer.ERROR_NETWORK -> "NETWORK"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "NETWORK_TIMEOUT"
        SpeechRecognizer.ERROR_NO_MATCH -> "NO_MATCH"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RECOGNIZER_BUSY"
        SpeechRecognizer.ERROR_SERVER -> "SERVER"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "SPEECH_TIMEOUT"
        else -> "code=$code"
    }

    companion object {
        private const val TAG = "VoiceRec"
    }
}
