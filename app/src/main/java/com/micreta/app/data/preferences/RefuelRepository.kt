package com.micreta.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.micreta.app.core.storage.micretaDataStore
import com.micreta.app.domain.model.RefuelEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/**
 * Refuel log (G06). Used to compute real-world consumption between fills
 * and total monthly cost (G07 future).
 */
class RefuelRepository(private val context: Context) {

    private val key = stringPreferencesKey("refuel_log_json")

    val entries: Flow<List<RefuelEntry>> = context.micretaDataStore.data.map { decode(it[key]) }

    suspend fun add(entry: RefuelEntry) {
        context.micretaDataStore.edit { prefs ->
            val current = decode(prefs[key]).toMutableList()
            current.add(0, entry)
            prefs[key] = encode(current)
        }
    }

    suspend fun remove(id: String) {
        context.micretaDataStore.edit { prefs ->
            val current = decode(prefs[key]).toMutableList()
            current.removeAll { it.id == id }
            prefs[key] = encode(current)
        }
    }

    private fun encode(list: List<RefuelEntry>): String {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("timestampMs", e.timestampMs)
                    .put("odometerKm", e.odometerKm)
                    .put("litres", e.litres)
                    .put("totalCostEur", e.totalCostEur)
                    .put("fuelType", e.fuelType)
                    .put("stationName", e.stationName)
            )
        }
        return arr.toString()
    }

    private fun decode(raw: String?): List<RefuelEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        RefuelEntry(
                            id = o.getString("id"),
                            timestampMs = o.getLong("timestampMs"),
                            odometerKm = o.getInt("odometerKm"),
                            litres = o.getDouble("litres"),
                            totalCostEur = o.getDouble("totalCostEur"),
                            fuelType = o.optString("fuelType", "Gasolina 95"),
                            stationName = o.optString("stationName", "")
                        )
                    )
                }
            }
        } catch (t: Throwable) {
            emptyList()
        }
    }
}
