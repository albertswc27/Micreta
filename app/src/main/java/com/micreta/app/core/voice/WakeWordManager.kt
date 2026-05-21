package com.micreta.app.core.voice

/**
 * Abstraction for an always-on "Hola Micreta" wake-word engine.
 *
 * V1 ships a [DisabledWakeWordManager] (no engine wired yet — a real engine
 * such as Picovoice Porcupine needs an AccessKey + a trained model). The UI
 * uses [available] to keep the "Activar Hola Micreta" setting disabled and to
 * show "todavía no disponible", so the app never pretends to listen for a wake
 * word it can't actually detect.
 */
interface WakeWordManager {
    /** True only when a real on-device wake-word engine is available. */
    val available: Boolean

    /** Begin listening for the wake word; [onDetected] fires on a match. No-op when unavailable. */
    fun start(onDetected: () -> Unit)

    /** Stop listening. */
    fun stop()
}

/** Default no-op engine: wake word not available in this build. */
class DisabledWakeWordManager : WakeWordManager {
    override val available: Boolean = false
    override fun start(onDetected: () -> Unit) { /* no engine */ }
    override fun stop() { /* no-op */ }
}
