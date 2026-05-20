package com.micreta.app.domain.model

/**
 * Speed limit applicable to the user's current position.
 *
 * If [kmh] is null we don't know the limit for that road segment — the UI
 * shows "—" instead of inventing a number, and the over-speed alert is
 * suppressed.
 */
data class SpeedLimit(
    val kmh: Int?,
    val source: Source = Source.UNKNOWN,
    val fetchedAtMs: Long = System.currentTimeMillis()
) {
    enum class Source { OSM_OVERPASS, FALLBACK_URBAN, FALLBACK_HIGHWAY, UNKNOWN }
}
