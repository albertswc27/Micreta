package com.micreta.app.domain.model

/**
 * Finalised trip record. What the user sees in "Historial" and what voice
 * uses for the spoken trip summary ("23 km, 6.2 L/100, eco-score 78").
 *
 * Eco-score: 0–100. >80 is calm, <50 is sporty/aggressive. Computed from
 * the rate of harsh events per kilometre.
 */
data class TripSummary(
    val id: String,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val distanceKm: Double,
    val maxSpeedKmh: Int,
    val avgSpeedKmh: Int,
    val harshAccelerations: Int,
    val harshBrakings: Int,
    val overSpeedEvents: Int,
    val ecoScore: Int,
    val estimatedConsumptionL100: Double? = null,
    val startLat: Double? = null,
    val startLon: Double? = null,
    val endLat: Double? = null,
    val endLon: Double? = null
) {
    val durationMs: Long get() = endedAtMs - startedAtMs
    val durationMin: Int get() = (durationMs / 60_000L).toInt()
}
