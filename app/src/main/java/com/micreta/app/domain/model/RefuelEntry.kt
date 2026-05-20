package com.micreta.app.domain.model

/**
 * One refuelling record. Used to compute real-world consumption between
 * fills (km between refuels / litres added) and total monthly cost.
 */
data class RefuelEntry(
    val id: String,
    val timestampMs: Long,
    val odometerKm: Int,
    val litres: Double,
    val totalCostEur: Double,
    val fuelType: String = "Gasolina 95",
    val stationName: String = "",
    val pricePerLitre: Double = if (litres > 0) totalCostEur / litres else 0.0
)
