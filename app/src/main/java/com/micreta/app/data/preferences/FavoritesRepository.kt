package com.micreta.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.micreta.app.core.storage.micretaDataStore
import com.micreta.app.domain.model.FavoritePlace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Stores [FavoritePlace] entries as a JSON array inside DataStore.
 *
 * We avoid kotlinx-serialization to keep the Gradle config minimal — org.json
 * ships with Android, no extra plugin or annotation processor needed.
 */
class FavoritesRepository(private val context: Context) {

    private val key = stringPreferencesKey("favorites_json")

    val favorites: Flow<List<FavoritePlace>> = context.micretaDataStore.data.map { prefs ->
        decode(prefs[key])
    }

    suspend fun upsert(place: FavoritePlace) {
        context.micretaDataStore.edit { prefs ->
            val current = decode(prefs[key]).toMutableList()
            val idx = current.indexOfFirst { it.id == place.id }
            if (idx >= 0) current[idx] = place else current.add(place)
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

    suspend fun seedDefaultsIfEmpty() {
        context.micretaDataStore.edit { prefs ->
            if (decode(prefs[key]).isNotEmpty()) return@edit
            val seeded = listOf(
                FavoritePlace(
                    id = UUID.randomUUID().toString(),
                    name = "Casa",
                    address = "",
                    voiceAliases = listOf("a casa", "mi casa", "vamos a casa")
                ),
                FavoritePlace(
                    id = UUID.randomUUID().toString(),
                    name = "Universidad",
                    address = "Universitat Autònoma de Barcelona",
                    voiceAliases = listOf("UAB", "uni", "facultad")
                ),
                FavoritePlace(
                    id = UUID.randomUUID().toString(),
                    name = "Trabajo",
                    address = "",
                    voiceAliases = listOf("curro", "oficina", "el trabajo")
                ),
                FavoritePlace(
                    id = UUID.randomUUID().toString(),
                    name = "Gimnasio",
                    address = "",
                    voiceAliases = listOf("gym", "al gimnasio")
                ),
                FavoritePlace(
                    id = UUID.randomUUID().toString(),
                    name = "Taller",
                    address = "",
                    voiceAliases = listOf("mecánico", "al taller")
                )
            )
            prefs[key] = encode(seeded)
        }
    }

    private fun decode(raw: String?): List<FavoritePlace> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val aliasesArr = o.optJSONArray("aliases") ?: JSONArray()
                    val aliases = buildList<String> {
                        for (j in 0 until aliasesArr.length()) add(aliasesArr.getString(j))
                    }
                    add(
                        FavoritePlace(
                            id = o.getString("id"),
                            name = o.getString("name"),
                            address = o.optString("address", ""),
                            voiceAliases = aliases
                        )
                    )
                }
            }
        } catch (t: Throwable) {
            emptyList()
        }
    }

    private fun encode(list: List<FavoritePlace>): String {
        val arr = JSONArray()
        list.forEach { f ->
            val o = JSONObject()
            o.put("id", f.id)
            o.put("name", f.name)
            o.put("address", f.address)
            val aliases = JSONArray()
            f.voiceAliases.forEach { aliases.put(it) }
            o.put("aliases", aliases)
            arr.put(o)
        }
        return arr.toString()
    }
}
