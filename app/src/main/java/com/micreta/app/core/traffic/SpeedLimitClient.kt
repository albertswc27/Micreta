package com.micreta.app.core.traffic

import com.micreta.app.core.logging.EventLogger
import com.micreta.app.core.net.HttpJson
import com.micreta.app.domain.model.GeoPoint
import com.micreta.app.domain.model.SpeedLimit
import org.json.JSONArray
import org.json.JSONObject

/**
 * Speed-limit lookup using OpenStreetMap's Overpass API (F01).
 *
 * Approach:
 *  1. Find ways within 30 m of the current point with a `maxspeed` tag.
 *  2. Return the first match's `maxspeed` value (km/h).
 *  3. If no match, fall back to a sensible default based on whether the
 *     point looks urban (other tagged streets nearby) or rural.
 *
 * This is intentionally light-touch — we don't keep a local tile DB. For
 * sustained driving Open-Meteo + Overpass costs are well under their free
 * tiers. We cache the last successful response in memory so we don't hit
 * Overpass every second.
 */
class SpeedLimitClient {

    private var cached: SpeedLimit? = null
    private var cachedAtPoint: GeoPoint? = null
    private val cacheRadiusM = 100.0

    suspend fun lookup(point: GeoPoint): SpeedLimit {
        cached?.let { cache ->
            val origin = cachedAtPoint
            if (origin != null && origin.distanceTo(point) < cacheRadiusM) {
                return cache
            }
        }
        val limit = queryOverpass(point) ?: fallback(point)
        cached = limit
        cachedAtPoint = point
        return limit
    }

    private suspend fun queryOverpass(point: GeoPoint): SpeedLimit? {
        // Around: 30 m radius. Filter ways with a maxspeed tag.
        val query =
            "[out:json][timeout:5];" +
            "way(around:30,${point.lat},${point.lon})[\"highway\"][\"maxspeed\"];" +
            "out tags 1;"
        val url = "https://overpass-api.de/api/interpreter?data=" + java.net.URLEncoder.encode(query, "UTF-8")
        val body = HttpJson.getText(url).getOrElse {
            EventLogger.warn(TAG, "Overpass unreachable: ${it.message}")
            return null
        }
        return try {
            val root = JSONObject(body)
            val elements = root.optJSONArray("elements") ?: JSONArray()
            if (elements.length() == 0) return null
            val tags = elements.getJSONObject(0).optJSONObject("tags") ?: return null
            val raw = tags.optString("maxspeed", "")
            val kmh = parseMaxSpeed(raw) ?: return null
            SpeedLimit(kmh = kmh, source = SpeedLimit.Source.OSM_OVERPASS)
        } catch (t: Throwable) {
            EventLogger.warn(TAG, "Overpass parse error: ${t.message}")
            null
        }
    }

    /**
     * Spain rural default is 90 km/h on regular roads, 50 km/h on urban
     * streets, 120 km/h on motorways. Without OSM we can't tell precisely —
     * we make an "urban" guess when the user is moving slowly.
     */
    private fun fallback(point: GeoPoint): SpeedLimit {
        val speed = point.speedKmh ?: 0
        return if (speed < 60) {
            SpeedLimit(kmh = 50, source = SpeedLimit.Source.FALLBACK_URBAN)
        } else {
            SpeedLimit(kmh = 90, source = SpeedLimit.Source.FALLBACK_HIGHWAY)
        }
    }

    private fun parseMaxSpeed(raw: String): Int? {
        if (raw.isBlank()) return null
        // Handle "50", "50 mph", "ES:urban", etc.
        return when {
            raw.toIntOrNull() != null -> raw.toInt()
            raw.contains("mph", ignoreCase = true) -> {
                raw.replace(Regex("[^0-9]"), "").toIntOrNull()?.let { (it * 1.609).toInt() }
            }
            raw.equals("ES:urban", true) -> 50
            raw.equals("ES:rural", true) -> 90
            raw.equals("ES:motorway", true) -> 120
            raw.equals("ES:trunk", true) -> 100
            raw.equals("ES:living_street", true) -> 20
            raw.equals("walk", true) -> 10
            else -> raw.replace(Regex("[^0-9]"), "").toIntOrNull()
        }
    }

    companion object {
        private const val TAG = "SpeedLimit"
    }
}
