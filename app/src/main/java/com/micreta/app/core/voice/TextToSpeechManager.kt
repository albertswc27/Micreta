package com.micreta.app.core.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.micreta.app.core.logging.EventLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Thin wrapper around Android's [TextToSpeech]. Defaults to Spanish (Spain).
 *
 * v0.2.0:
 *  - Audio ducking (D06): requests transient AudioFocus while speaking so
 *    Spotify / YT Music / podcasts dip down automatically.
 *  - Uses USAGE_ASSISTANCE_NAVIGATION_GUIDANCE so the system treats Micreta
 *    the way it treats GPS prompts (ducks even car-handsfree connections).
 *
 * The engine takes a moment to initialize — calls to [speak] before init
 * completes are queued internally and flushed once ready.
 */
class TextToSpeechManager(context: Context) {

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val audioAttributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .build()
    private var focusRequest: AudioFocusRequest? = null
    @Volatile var duckingEnabled: Boolean = true

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking

    private val pending = ArrayDeque<String>()

    /** utteranceId → completion, so a caller can await a specific phrase
     *  finishing (used to coordinate TTS↔STT so Micreta never hears herself). */
    private val awaiting = ConcurrentHashMap<String, CompletableDeferred<Unit>>()

    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = tts.setLanguage(Locale("es", "ES"))
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    EventLogger.warn(TAG, "Spanish TTS not available, falling back to default.")
                    tts.setLanguage(Locale.getDefault())
                }
                tts.setSpeechRate(1.0f)
                tts.setPitch(1.05f)
                tts.setAudioAttributes(audioAttributes)
                _ready.value = true
                EventLogger.info(TAG, "TTS ready.")
                // Flush pending queue
                while (pending.isNotEmpty()) speak(pending.removeFirst())
            } else {
                EventLogger.error(TAG, "TTS init failed with status=$status")
            }
        }

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _speaking.value = true
                requestFocus()
            }
            override fun onDone(utteranceId: String?) {
                _speaking.value = false
                releaseFocus()
                utteranceId?.let { awaiting.remove(it)?.complete(Unit) }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _speaking.value = false
                releaseFocus()
                utteranceId?.let { awaiting.remove(it)?.complete(Unit) }
            }
            override fun onError(utteranceId: String?, errorCode: Int) {
                _speaking.value = false
                releaseFocus()
                EventLogger.error(TAG, "TTS error=$errorCode utt=$utteranceId")
                utteranceId?.let { awaiting.remove(it)?.complete(Unit) }
            }
        })
    }

    private fun requestFocus() {
        if (!duckingEnabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener { /* nothing — we just request, no callbacks */ }
                .build()
            focusRequest = req
            audioManager.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    private fun releaseFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    /** Speak [text]. If TTS isn't ready yet, the message is queued. */
    fun speak(text: String) {
        if (text.isBlank()) return
        if (!_ready.value) {
            pending.addLast(text)
            return
        }
        val id = UUID.randomUUID().toString()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        EventLogger.info(TAG, "TTS speak: \"$text\"")
    }

    /**
     * Speaks [text] and suspends until the engine reports completion (onDone /
     * onError), capped by [timeoutMs] so the caller never blocks forever if the
     * engine misbehaves. Used to coordinate with the SpeechRecognizer so
     * Micreta never starts listening while she is still talking.
     */
    suspend fun speakAndAwait(text: String, timeoutMs: Long = 6_000L) {
        if (text.isBlank()) return
        if (!_ready.value) {
            // Engine still initializing — queue via speak() and wait a short beat.
            speak(text)
            delay(minOf(timeoutMs, 1_500L))
            return
        }
        val id = UUID.randomUUID().toString()
        val done = CompletableDeferred<Unit>()
        awaiting[id] = done
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        EventLogger.info(TAG, "TTS speak (await): \"$text\"")
        withTimeoutOrNull(timeoutMs) { done.await() }
        awaiting.remove(id)
    }

    fun stop() {
        tts.stop()
        _speaking.value = false
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
        _ready.value = false
    }

    companion object {
        private const val TAG = "TTS"
    }
}
