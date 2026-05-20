package com.micreta.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.micreta.app.core.storage.micretaDataStore
import com.micreta.app.domain.model.CustomCommand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/**
 * User-defined voice commands (B10). A phrase (or alias) is mapped to an
 * action selected from a closed set Micreta supports natively. This avoids
 * the user wiring intents that the parser can't execute.
 */
class CustomCommandsRepository(private val context: Context) {

    private val key = stringPreferencesKey("custom_commands_json")

    val commands: Flow<List<CustomCommand>> = context.micretaDataStore.data.map { decode(it[key]) }

    suspend fun upsert(cmd: CustomCommand) {
        context.micretaDataStore.edit { prefs ->
            val current = decode(prefs[key]).toMutableList()
            val idx = current.indexOfFirst { it.id == cmd.id }
            if (idx >= 0) current[idx] = cmd else current.add(cmd)
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

    private fun encode(list: List<CustomCommand>): String {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(
                JSONObject()
                    .put("id", c.id)
                    .put("phrase", c.phrase)
                    .put("action", c.action.name)
                    .put("payload", c.payload)
                    .put("enabled", c.enabled)
            )
        }
        return arr.toString()
    }

    private fun decode(raw: String?): List<CustomCommand> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        CustomCommand(
                            id = o.getString("id"),
                            phrase = o.getString("phrase"),
                            action = runCatching {
                                CustomCommand.Action.valueOf(o.getString("action"))
                            }.getOrDefault(CustomCommand.Action.SPEAK),
                            payload = o.optString("payload", ""),
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
