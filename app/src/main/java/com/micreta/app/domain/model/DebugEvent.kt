package com.micreta.app.domain.model

/**
 * Append-only events surfaced in the Debug screen. Kept intentionally simple
 * — this is the panel you look at when something doesn't behave right in the car.
 */
data class DebugEvent(
    val timestampMs: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String,
    val level: Level = Level.INFO
) {
    enum class Level { INFO, WARN, ERROR }
}
