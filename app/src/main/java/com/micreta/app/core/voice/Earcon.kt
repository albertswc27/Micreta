package com.micreta.app.core.voice

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Short non-verbal audio cues (earcons).
 *
 * A brief beep when we start listening is faster and far less annoying than a
 * spoken "buenas tardes Albert, ¿a dónde vamos?" on every activation. One
 * lazily-created ToneGenerator is reused for the whole app lifetime.
 */
object Earcon {

    private val tone: ToneGenerator? by lazy {
        runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 70) }.getOrNull()
    }

    /** "I'm listening" — a short rising prompt beep. */
    fun listening() {
        runCatching { tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 140) }
    }

    /** Soft acknowledgement beep (command accepted). */
    fun ack() {
        runCatching { tone?.startTone(ToneGenerator.TONE_PROP_ACK, 120) }
    }
}
