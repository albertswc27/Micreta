package com.micreta.app.data.trip

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.micreta.app.core.storage.micretaDataStore
import com.micreta.app.domain.model.TripSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists finished trips (E04 Historial de viajes).
 *
 * JSON-encoded in DataStore — keeps the Gradle config free of database
 * dependencies (Room/SQLite) for now. If the trip history grows past a few
 * hundred entries we'll swap the backing store; the public API doesn't change.
 *
 * Retention is bounded at [MAX_TRIPS] to keep startup fast; oldest trips drop.
 */
class TripRepository(private val context: Context) {

    private val key = stringPreferencesKey("trip_history_json")

    val trips: Flow<List<TripSummary>> = context.micretaDataStore.data.map { prefs ->
        decode(prefs[key])
    }

    suspend fun append(summary: TripSummary) {
        context.micretaDataStore.edit { prefs ->
            val current = decode(prefs[key]).toMutableList()
            current.add(0, summary) // newest first
            while (current.size > MAX_TRIPS) current.removeAt(current.size - 1)
            prefs[key] = encode(current)
        }
    }

    suspend fun clear() {
        context.micretaDataStore.edit { prefs -> prefs.remove(key) }
    }

    /** Aggregate stats for the user dashboard. */
    suspend fun aggregateStats(): Stats {
        val all = trips.first()
        if (all.isEmpty()) return Stats()
        val totalKm = all.sumOf { it.distanceKm }
        val totalDurationMin = all.sumOf { it.durationMin.toLong() }
        val avgEco = (all.sumOf { it.ecoScore.toLong() } / all.size).toInt()
        return Stats(
            tripCount = all.size,
            totalKm = totalKm,
            totalDurationMin = totalDurationMin.toInt(),
            avgEcoScore = avgEco,
            totalHarshAccelerations = all.sumOf { it.harshAccelerations },
            totalHarshBrakings = all.sumOf { it.harshBrakings },
            totalOverSpeedEvents = all.sumOf { it.overSpeedEvents }
        )
    }

    data class Stats(
        val tripCount: Int = 0,
        val totalKm: Double = 0.0,
        val totalDurationMin: Int = 0,
        val avgEcoScore: Int = 0,
        val totalHarshAccelerations: Int = 0,
        val totalHarshBrakings: Int = 0,
        val totalOverSpeedEvents: Int = 0
    )

    // --- JSON ------------------------------------------------------------

    private fun encode(list: List<TripSummary>): String {
        val arr = JSONArray()
        list.forEach { t ->
            val o = JSONObject()
                .put("id", t.id)
                .put("startedAtMs", t.startedAtMs)
                .put("endedAtMs", t.endedAtMs)
                .put("distanceKm", t.distanceKm)
                .put("maxSpeedKmh", t.maxSpeedKmh)
                .put("avgSpeedKmh", t.avgSpeedKmh)
                .put("harshAccelerations", t.harshAccelerations)
                .put("harshBrakings", t.harshBrakings)
                .put("overSpeedEvents", t.overSpeedEvents)
                .put("ecoScore", t.ecoScore)
            t.estimatedConsumptionL100?.let { o.put("estimatedConsumptionL100", it) }
            t.startLat?.let { o.put("startLat", it) }
            t.startLon?.let { o.put("startLon", it) }
            t.endLat?.let { o.put("endLat", it) }
            t.endLon?.let { o.put("endLon", it) }
            arr.put(o)
        }
        return arr.toString()
    }

    private fun decode(raw: String?): List<TripSummary> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        TripSummary(
                            id = o.getString("id"),
                            startedAtMs = o.getLong("startedAtMs"),
                            endedAtMs = o.getLong("endedAtMs"),
                            distanceKm = o.getDouble("distanceKm"),
                            maxSpeedKmh = o.getInt("maxSpeedKmh"),
                            avgSpeedKmh = o.getInt("avgSpeedKmh"),
                            harshAccelerations = o.optInt("harshAccelerations"),
                            harshBrakings = o.optInt("harshBrakings"),
                            overSpeedEvents = o.optInt("overSpeedEvents"),
                            ecoScore = o.optInt("ecoScore", 100),
                            estimatedConsumptionL100 = if (o.has("estimatedConsumptionL100")) o.getDouble("estimatedConsumptionL100") else null,
                            startLat = if (o.has("startLat")) o.getDouble("startLat") else null,
                            startLon = if (o.has("startLon")) o.getDouble("startLon") else null,
                            endLat = if (o.has("endLat")) o.getDouble("endLat") else null,
                            endLon = if (o.has("endLon")) o.getDouble("endLon") else null
                        )
                    )
                }
            }
        } catch (t: Throwable) {
            emptyList()
        }
    }

    companion object {
        private const val MAX_TRIPS = 250
    }
}
