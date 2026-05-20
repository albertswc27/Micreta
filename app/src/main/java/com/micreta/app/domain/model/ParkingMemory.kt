package com.micreta.app.domain.model

/**
 * Last known parking location, captured the moment the car Bluetooth
 * disconnects or driving mode ends.
 */
data class ParkingMemory(
    val lat: Double,
    val lon: Double,
    val savedAtMs: Long,
    val tripId: String? = null,
    val odometerKm: Int? = null,
    val addressHint: String = ""
)
