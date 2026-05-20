package com.micreta.app.domain.model

/**
 * Plain latitude/longitude pair with optional altitude / speed / heading.
 *
 * Decoupled from android.location.Location so the domain layer never depends
 * on framework types — easier to test and reuse from the V2 ESP32 client.
 */
data class GeoPoint(
    val lat: Double,
    val lon: Double,
    val accuracyM: Float? = null,
    val speedMps: Float? = null,
    val bearing: Float? = null,
    val timestampMs: Long = System.currentTimeMillis()
) {
    val speedKmh: Int? get() = speedMps?.let { (it * 3.6f).toInt().coerceAtLeast(0) }

    /** Haversine distance in metres between two points. */
    fun distanceTo(other: GeoPoint): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(other.lat - lat)
        val dLon = Math.toRadians(other.lon - lon)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(other.lat)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
