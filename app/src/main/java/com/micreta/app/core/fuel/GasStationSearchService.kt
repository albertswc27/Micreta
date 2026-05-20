package com.micreta.app.core.fuel

import com.micreta.app.core.logging.EventLogger
import com.micreta.app.core.net.HttpJson
import com.micreta.app.domain.model.GasStationOption
import com.micreta.app.domain.model.GasStationResult
import com.micreta.app.domain.model.GeoPoint
import org.json.JSONObject

/**
 * Finds nearby fuel stations using OpenStreetMap's Overpass API
 * (`amenity=fuel`). Reuses the same Overpass approach already used for speed
 * limits (F01), so no new dependency or API key is needed.
 *
 * Honesty constraints (CLAUDE.md): OSM provides names + coordinates but not
 * live prices, so [GasStationOption.priceLabel] is always
 * [PRICE_UNAVAILABLE] here — we never invent a price. When the network fails
 * or no stations are found we return [GasStationResult.FallbackSearch] so the
 * UI can offer a Waze search instead.
 *
 * Limitation: prices require a dedicated API (e.g. the Spanish Ministry's
 * Geoportal de Gasolineras, C08 in the roadmap). Until that is integrated,
 * options are sorted by distance only.
 */
class GasStationSearchService {

    suspend fun search(origin: GeoPoint, radiusM: Int = 5_000, max: Int = 3): GasStationResult {
        val query =
            "[out:json][timeout:8];" +
            "node(around:$radiusM,${origin.lat},${origin.lon})[\"amenity\"=\"fuel\"];" +
            "out tags 40;"
        val url = "https://overpass-api.de/api/interpreter?data=" +
            java.net.URLEncoder.encode(query, "UTF-8")

        val body = HttpJson.getText(url).getOrElse {
            EventLogger.warn(TAG, "Overpass fuel query failed: ${it.message}")
            return GasStationResult.FallbackSearch(FALLBACK_QUERY)
        }

        return try {
            val elements = JSONObject(body).optJSONArray("elements")
            if (elements == null || elements.length() == 0) {
                GasStationResult.FallbackSearch(FALLBACK_QUERY)
            } else {
                val options = (0 until elements.length()).mapNotNull { i ->
                    val el = elements.optJSONObject(i) ?: return@mapNotNull null
                    val lat = el.optDouble("lat", Double.NaN)
                    val lon = el.optDouble("lon", Double.NaN)
                    if (lat.isNaN() || lon.isNaN()) return@mapNotNull null
                    val tags = el.optJSONObject("tags")
                    val brand = tags?.optString("brand")?.ifBlank { null }
                    val name = tags?.optString("name")?.ifBlank { null } ?: brand ?: "Gasolinera"
                    val point = GeoPoint(lat, lon)
                    GasStationOption(
                        id = el.optLong("id").toString(),
                        name = name,
                        address = tags?.optString("addr:street")?.ifBlank { null },
                        lat = lat,
                        lon = lon,
                        distanceKm = origin.distanceTo(point) / 1000.0,
                        priceLabel = PRICE_UNAVAILABLE,
                        brand = brand
                    )
                }.sortedBy { it.distanceKm }.take(max)

                if (options.isEmpty()) GasStationResult.FallbackSearch(FALLBACK_QUERY)
                else GasStationResult.Options(options)
            }
        } catch (t: Throwable) {
            EventLogger.warn(TAG, "Overpass fuel parse error: ${t.message}")
            GasStationResult.FallbackSearch(FALLBACK_QUERY)
        }
    }

    companion object {
        const val PRICE_UNAVAILABLE = "Precio no disponible"
        const val FALLBACK_QUERY = "gasolinera"
        private const val TAG = "GasStations"
    }
}
