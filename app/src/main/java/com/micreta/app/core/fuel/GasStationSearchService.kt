package com.micreta.app.core.fuel

import android.content.Context
import android.location.Geocoder
import com.micreta.app.core.logging.EventLogger
import com.micreta.app.core.net.HttpJson
import com.micreta.app.domain.model.GasStationOption
import com.micreta.app.domain.model.GasStationResult
import com.micreta.app.domain.model.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

/**
 * Finds nearby fuel stations **with real prices** (C08).
 *
 * Primary source: the Spanish Ministry's open fuel-price API (no key, updated
 * every 30 min). We resolve the province from the GPS fix via [Geocoder],
 * query that province, compute distances locally and rank by price + distance.
 *
 * Fallback: if the province can't be resolved or the API fails, we use OSM
 * Overpass (real stations, but **no price** → "Precio no disponible"), and as a
 * last resort return [GasStationResult.FallbackSearch] so the UI offers a Waze
 * search. We never invent a price (CLAUDE.md).
 */
class GasStationSearchService(private val context: Context) {

    suspend fun search(origin: GeoPoint, max: Int = 3): GasStationResult = withContext(Dispatchers.IO) {
        val provinceId = resolveProvinceId(origin)
        if (provinceId != null) {
            val priced = queryMinistry(provinceId, origin, max)
            if (priced.isNotEmpty()) return@withContext GasStationResult.Options(priced)
            EventLogger.info(TAG, "Ministry returned no usable stations for province $provinceId; trying OSM.")
        } else {
            EventLogger.info(TAG, "Could not resolve province from location; trying OSM.")
        }
        val osm = queryOverpass(origin, max)
        if (osm.isNotEmpty()) GasStationResult.Options(osm)
        else GasStationResult.FallbackSearch(FALLBACK_QUERY)
    }

    // ---- Ministry (real prices) ----------------------------------------

    private fun resolveProvinceId(origin: GeoPoint): String? = try {
        @Suppress("DEPRECATION")
        val addresses = Geocoder(context, Locale("es", "ES")).getFromLocation(origin.lat, origin.lon, 1)
        val a = addresses?.firstOrNull()
        listOfNotNull(a?.subAdminArea, a?.adminArea, a?.locality)
            .firstNotNullOfOrNull { SpainProvinces.idFor(it) }
    } catch (t: Throwable) {
        EventLogger.warn(TAG, "Geocoder failed: ${t.message}")
        null
    }

    private suspend fun queryMinistry(provinceId: String, origin: GeoPoint, max: Int): List<GasStationOption> {
        val url = "$MINISTRY_BASE/EstacionesTerrestres/FiltroProvincia/$provinceId"
        val body = HttpJson.getText(url).getOrElse {
            EventLogger.warn(TAG, "Ministry query failed: ${it.message}")
            return emptyList()
        }
        return try {
            val list = JSONObject(body).optJSONArray("ListaEESSPrecio") ?: return emptyList()
            val all = (0 until list.length()).mapNotNull { i ->
                val o = list.optJSONObject(i) ?: return@mapNotNull null
                val lat = esNumber(o.optString("Latitud"))
                val lon = esNumber(o.optString("Longitud (WGS84)").ifBlank { o.optString("Longitud") })
                if (lat == null || lon == null) return@mapNotNull null
                val price = esNumber(o.optString("Precio Gasolina 95 E5"))
                val name = o.optString("Rótulo").trim().ifBlank { "Gasolinera" }
                val point = GeoPoint(lat, lon)
                GasStationOption(
                    id = o.optString("IDEESS").ifBlank { "$lat,$lon" },
                    name = capitalizeBrand(name),
                    address = listOf(o.optString("Dirección"), o.optString("Municipio"))
                        .filter { it.isNotBlank() }.joinToString(", ").ifBlank { null },
                    lat = lat,
                    lon = lon,
                    distanceKm = origin.distanceTo(point) / 1000.0,
                    priceLabel = price?.let { "%.3f €/L".format(Locale.US, it) } ?: PRICE_UNAVAILABLE,
                    brand = capitalizeBrand(name),
                    rawPrice = price
                )
            }
            // Keep nearby ones, then cheapest first (stations without a price last).
            all.filter { it.distanceKm <= NEARBY_KM }
                .sortedWith(compareBy({ it.rawPrice ?: Double.MAX_VALUE }, { it.distanceKm }))
                .take(max)
        } catch (t: Throwable) {
            EventLogger.warn(TAG, "Ministry parse error: ${t.message}")
            emptyList()
        }
    }

    /**
     * Returns the name (or "" if unnamed) of a fuel station within [radiusM] of
     * [point], or null if there is none. Used to detect that a trip ended at a
     * gas station so the next start can ask about the refuel.
     */
    suspend fun nearestFuelName(point: GeoPoint, radiusM: Int = 120): String? = withContext(Dispatchers.IO) {
        val query = "[out:json][timeout:6];node(around:$radiusM,${point.lat},${point.lon})[\"amenity\"=\"fuel\"];out tags 1;"
        val url = "https://overpass-api.de/api/interpreter?data=" + java.net.URLEncoder.encode(query, "UTF-8")
        val body = HttpJson.getText(url).getOrElse { return@withContext null }
        try {
            val els = JSONObject(body).optJSONArray("elements") ?: return@withContext null
            if (els.length() == 0) return@withContext null
            val tags = els.optJSONObject(0)?.optJSONObject("tags")
            (tags?.optString("name")?.ifBlank { null } ?: tags?.optString("brand")?.ifBlank { null }) ?: ""
        } catch (t: Throwable) {
            null
        }
    }

    // ---- OSM Overpass fallback (no prices) -----------------------------

    private suspend fun queryOverpass(origin: GeoPoint, max: Int): List<GasStationOption> {
        val query =
            "[out:json][timeout:8];" +
            "node(around:5000,${origin.lat},${origin.lon})[\"amenity\"=\"fuel\"];" +
            "out tags 40;"
        val url = "https://overpass-api.de/api/interpreter?data=" +
            java.net.URLEncoder.encode(query, "UTF-8")
        val body = HttpJson.getText(url).getOrElse {
            EventLogger.warn(TAG, "Overpass fuel query failed: ${it.message}")
            return emptyList()
        }
        return try {
            val elements = JSONObject(body).optJSONArray("elements") ?: return emptyList()
            (0 until elements.length()).mapNotNull { i ->
                val el = elements.optJSONObject(i) ?: return@mapNotNull null
                val lat = el.optDouble("lat", Double.NaN)
                val lon = el.optDouble("lon", Double.NaN)
                if (lat.isNaN() || lon.isNaN()) return@mapNotNull null
                val tags = el.optJSONObject("tags")
                val brand = tags?.optString("brand")?.ifBlank { null }
                val name = tags?.optString("name")?.ifBlank { null } ?: brand ?: "Gasolinera"
                GasStationOption(
                    id = el.optLong("id").toString(),
                    name = name,
                    address = tags?.optString("addr:street")?.ifBlank { null },
                    lat = lat,
                    lon = lon,
                    distanceKm = origin.distanceTo(GeoPoint(lat, lon)) / 1000.0,
                    priceLabel = PRICE_UNAVAILABLE,
                    brand = brand
                )
            }.sortedBy { it.distanceKm }.take(max)
        } catch (t: Throwable) {
            EventLogger.warn(TAG, "Overpass parse error: ${t.message}")
            emptyList()
        }
    }

    private fun esNumber(raw: String?): Double? =
        raw?.trim()?.replace(",", ".")?.toDoubleOrNull()

    private fun capitalizeBrand(s: String): String =
        s.lowercase(Locale("es", "ES")).split(" ").joinToString(" ") { w ->
            w.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }
        }

    companion object {
        const val PRICE_UNAVAILABLE = "Precio no disponible"
        const val FALLBACK_QUERY = "gasolinera"
        private const val NEARBY_KM = 15.0
        private const val MINISTRY_BASE =
            "https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes"
        private const val TAG = "GasStations"
    }
}
