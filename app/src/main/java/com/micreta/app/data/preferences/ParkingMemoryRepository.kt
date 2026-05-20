package com.micreta.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.micreta.app.core.storage.micretaDataStore
import com.micreta.app.domain.model.ParkingMemory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

/**
 * Last known parking location (C09). Single record — we only care about the
 * most recent place the user left the car.
 */
class ParkingMemoryRepository(private val context: Context) {

    private val key = stringPreferencesKey("parking_memory_json")

    val parking: Flow<ParkingMemory?> = context.micretaDataStore.data.map { decode(it[key]) }

    suspend fun save(memory: ParkingMemory) {
        context.micretaDataStore.edit { prefs -> prefs[key] = encode(memory) }
    }

    suspend fun clear() {
        context.micretaDataStore.edit { prefs -> prefs.remove(key) }
    }

    private fun encode(m: ParkingMemory): String = JSONObject()
        .put("lat", m.lat)
        .put("lon", m.lon)
        .put("savedAtMs", m.savedAtMs)
        .put("tripId", m.tripId ?: JSONObject.NULL)
        .put("odometerKm", m.odometerKm ?: JSONObject.NULL)
        .put("addressHint", m.addressHint)
        .toString()

    private fun decode(raw: String?): ParkingMemory? {
        if (raw.isNullOrBlank()) return null
        return try {
            val o = JSONObject(raw)
            ParkingMemory(
                lat = o.getDouble("lat"),
                lon = o.getDouble("lon"),
                savedAtMs = o.getLong("savedAtMs"),
                tripId = if (o.isNull("tripId")) null else o.getString("tripId"),
                odometerKm = if (o.isNull("odometerKm")) null else o.getInt("odometerKm"),
                addressHint = o.optString("addressHint", "")
            )
        } catch (t: Throwable) {
            null
        }
    }
}
