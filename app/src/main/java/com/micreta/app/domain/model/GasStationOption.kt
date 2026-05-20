package com.micreta.app.domain.model

/**
 * One fuel-station candidate for the "gasolinera más barata" flow (P3).
 *
 * [priceLabel] is null or "Precio no disponible" unless a real price source is
 * wired in — Micreta never invents prices (CLAUDE.md).
 */
data class GasStationOption(
    val id: String,
    val name: String,
    val address: String? = null,
    val lat: Double,
    val lon: Double,
    val distanceKm: Double,
    val priceLabel: String? = null,
    val brand: String? = null,
    val updatedAt: Long? = null
)

/** Outcome of a gas-station search. */
sealed class GasStationResult {
    /** Up to N structured options (real stations; prices may be unknown). */
    data class Options(val options: List<GasStationOption>) : GasStationResult()

    /** No structured results — caller should fall back to a Waze search. */
    data class FallbackSearch(val query: String) : GasStationResult()
}
