package com.micreta.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.micreta.app.core.storage.micretaDataStore
import com.micreta.app.domain.model.MaintenanceTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Maintenance reminders (G group). Also tracks the user's manual odometer
 * value — since the Micra K13 doesn't reliably expose odometer over OBD-II,
 * the user logs it manually (typically right after a refuel).
 */
class MaintenanceRepository(private val context: Context) {

    private val key = stringPreferencesKey("maintenance_tasks_json")
    private val odoKey = intPreferencesKey("manual_odometer_km")

    val tasks: Flow<List<MaintenanceTask>> = context.micretaDataStore.data.map { decode(it[key]) }
    val odometerKm: Flow<Int?> = context.micretaDataStore.data.map { it[odoKey] }

    suspend fun upsert(task: MaintenanceTask) {
        context.micretaDataStore.edit { prefs ->
            val current = decode(prefs[key]).toMutableList()
            val idx = current.indexOfFirst { it.id == task.id }
            if (idx >= 0) current[idx] = task else current.add(task)
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

    suspend fun setOdometer(km: Int) {
        context.micretaDataStore.edit { prefs -> prefs[odoKey] = km }
    }

    suspend fun seedDefaultsIfEmpty() {
        context.micretaDataStore.edit { prefs ->
            if (decode(prefs[key]).isNotEmpty()) return@edit
            val now = System.currentTimeMillis()
            val seed = listOf(
                MaintenanceTask(
                    id = UUID.randomUUID().toString(),
                    title = "Cambio de aceite",
                    kind = MaintenanceTask.Kind.OIL,
                    intervalKm = 15_000,
                    intervalDays = 365,
                    baseAtMs = now
                ),
                MaintenanceTask(
                    id = UUID.randomUUID().toString(),
                    title = "ITV",
                    kind = MaintenanceTask.Kind.ITV,
                    intervalDays = 365,
                    baseAtMs = now
                ),
                MaintenanceTask(
                    id = UUID.randomUUID().toString(),
                    title = "Seguro",
                    kind = MaintenanceTask.Kind.INSURANCE,
                    intervalDays = 365,
                    baseAtMs = now
                )
            )
            prefs[key] = encode(seed)
        }
    }

    private fun encode(list: List<MaintenanceTask>): String {
        val arr = JSONArray()
        list.forEach { t ->
            val o = JSONObject()
                .put("id", t.id)
                .put("title", t.title)
                .put("kind", t.kind.name)
                .put("baseOdometerKm", t.baseOdometerKm)
                .put("baseAtMs", t.baseAtMs)
                .put("enabled", t.enabled)
            t.intervalKm?.let { o.put("intervalKm", it) }
            t.intervalDays?.let { o.put("intervalDays", it) }
            arr.put(o)
        }
        return arr.toString()
    }

    private fun decode(raw: String?): List<MaintenanceTask> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        MaintenanceTask(
                            id = o.getString("id"),
                            title = o.getString("title"),
                            kind = runCatching { MaintenanceTask.Kind.valueOf(o.getString("kind")) }.getOrDefault(MaintenanceTask.Kind.OTHER),
                            intervalKm = if (o.has("intervalKm")) o.getInt("intervalKm") else null,
                            intervalDays = if (o.has("intervalDays")) o.getInt("intervalDays") else null,
                            baseOdometerKm = o.optInt("baseOdometerKm", 0),
                            baseAtMs = o.optLong("baseAtMs", System.currentTimeMillis()),
                            enabled = o.optBoolean("enabled", true)
                        )
                    )
                }
            }
        } catch (t: Throwable) {
            emptyList()
        }
    }
}
