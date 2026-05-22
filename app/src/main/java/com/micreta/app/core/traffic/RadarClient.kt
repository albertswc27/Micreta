package com.micreta.app.core.traffic

import com.micreta.app.core.logging.EventLogger
import com.micreta.app.core.net.HttpJson
import com.micreta.app.domain.model.GeoPoint
import org.json.JSONObject

/**
 * Fixed speed-camera lookup via OpenStreetMap Overpass (`highway=speed_camera`),
 * reusing the same approach as speed limits / gas stations (C11).
 *
 * OSM covers most fixed radars in Spain (community-mapped). The official DGT
 * dataset could be imported later for fuller coverage; this needs no extra
 * dependency or API key.
 */
class RadarClient {

    suspend fun nearby(point: GeoPoint, radiusM: Int = 4000): List<GeoPoint> {
        val query =
            "[out:json][timeout:8];node(around:$radiusM,${point.lat},${point.lon})[\"highway\"=\"speed_camera\"];out;"
        val url = "https://overpass-api.de/api/interpreter?data=" +
            java.net.URLEncoder.encode(query, "UTF-8")
        val body = HttpJson.getText(url).getOrElse {
            EventLogger.warn(TAG, "Overpass radar query failed: ${it.message}")
            return emptyList()
        }
        return try {
            val elements = JSONObject(body).optJSONArray("elements") ?: return emptyList()
            (0 until elements.length()).mapNotNull { i ->
                val el = elements.optJSONObject(i) ?: return@mapNotNull null
                val lat = el.optDouble("lat", Double.NaN)
                val lon = el.optDouble("lon", Double.NaN)
                if (lat.isNaN() || lon.isNaN()) null else GeoPoint(lat, lon)
            }
        } catch (t: Throwable) {
            EventLogger.warn(TAG, "Overpass radar parse error: ${t.message}")
            emptyList()
        }
    }

    companion object {
        private const val TAG = "Radar"
    }
}
