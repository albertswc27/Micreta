package com.micreta.app.domain.model

/**
 * Live in-memory state of the current driving session. Persisted as a
 * [TripSummary] when the session ends.
 *
 * Fields are intentionally compact — only what the screens, voice prompts
 * and eco-score need. Anything heavier (full GPS track) is opt-in for V2.
 */
data class TripSession(
    val id: String,
    val startedAtMs: Long,
    val startLocation: GeoPoint? = null,
    val distanceM: Double = 0.0,
    val maxSpeedKmh: Int = 0,
    val harshAccelerations: Int = 0,
    val harshBrakings: Int = 0,
    val overSpeedEvents: Int = 0,
    val totalIdleMs: Long = 0L,
    val lastFuelLevelPct: Int? = null,
    val firstFuelLevelPct: Int? = null,
    val coolantSamples: List<Int> = emptyList(),
    val rpmSamples: List<Int> = emptyList()
) {
    val durationMs: Long get() = System.currentTimeMillis() - startedAtMs
    val distanceKm: Double get() = distanceM / 1000.0
}
