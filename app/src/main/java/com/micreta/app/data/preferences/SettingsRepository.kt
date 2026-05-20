package com.micreta.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.micreta.app.core.storage.micretaDataStore
import com.micreta.app.domain.model.AppSettings
import com.micreta.app.domain.model.PersonalityProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persists [AppSettings] in DataStore. All reads go through the [settings] flow;
 * UI just collects it and stays in sync.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val assistantName = stringPreferencesKey("assistant_name")
        val carName = stringPreferencesKey("car_name")
        val carBtMac = stringPreferencesKey("car_bt_mac")
        val carBtName = stringPreferencesKey("car_bt_name")
        val obdBtMac = stringPreferencesKey("obd_bt_mac")
        val obdBtName = stringPreferencesKey("obd_bt_name")
        val musicAppPackage = stringPreferencesKey("music_app_package")
        val activateOnCharging = booleanPreferencesKey("activate_on_charging")
        val activateOnBluetooth = booleanPreferencesKey("activate_on_bluetooth")
        val activateOnGpsSpeed = booleanPreferencesKey("activate_on_gps_speed")
        val autoNightMode = booleanPreferencesKey("auto_night_mode")
        val demoMode = booleanPreferencesKey("demo_mode")
        val personality = stringPreferencesKey("personality_profile")
        val etaContactName = stringPreferencesKey("eta_contact_name")
        val etaContactPhone = stringPreferencesKey("eta_contact_phone")
        val sosPhoneNumber = stringPreferencesKey("sos_phone_number")
        val speedLimitWarnEnabled = booleanPreferencesKey("speed_limit_warn_enabled")
        val speedLimitToleranceKmh = intPreferencesKey("speed_limit_tolerance_kmh")
        val strictDoNotDisturb = booleanPreferencesKey("strict_do_not_disturb")
        val audioDuckingEnabled = booleanPreferencesKey("audio_ducking_enabled")
        val resumeLastMediaOnDrive = booleanPreferencesKey("resume_last_media_on_drive")
        val tripsEnabled = booleanPreferencesKey("trips_enabled")
        val customCommandsEnabled = booleanPreferencesKey("custom_commands_enabled")
        val autoListenOnCarBluetooth = booleanPreferencesKey("auto_listen_on_car_bt")
    }

    val settings: Flow<AppSettings> = context.micretaDataStore.data.map { prefs ->
        AppSettings(
            assistantName = prefs[Keys.assistantName] ?: "Micreta",
            carName = prefs[Keys.carName] ?: "Micra K13",
            carBluetoothMac = prefs[Keys.carBtMac],
            carBluetoothName = prefs[Keys.carBtName],
            obdBluetoothMac = prefs[Keys.obdBtMac],
            obdBluetoothName = prefs[Keys.obdBtName],
            musicAppPackage = prefs[Keys.musicAppPackage],
            activateOnCharging = prefs[Keys.activateOnCharging] ?: false,
            activateOnBluetooth = prefs[Keys.activateOnBluetooth] ?: true,
            activateOnGpsSpeed = prefs[Keys.activateOnGpsSpeed] ?: false,
            autoNightMode = prefs[Keys.autoNightMode] ?: true,
            demoMode = prefs[Keys.demoMode] ?: false,
            personality = runCatching { PersonalityProfile.valueOf(prefs[Keys.personality] ?: "") }
                .getOrDefault(PersonalityProfile.FRIENDLY),
            etaContactName = prefs[Keys.etaContactName] ?: "",
            etaContactPhone = prefs[Keys.etaContactPhone] ?: "",
            sosPhoneNumber = prefs[Keys.sosPhoneNumber] ?: "112",
            speedLimitWarnEnabled = prefs[Keys.speedLimitWarnEnabled] ?: true,
            speedLimitToleranceKmh = prefs[Keys.speedLimitToleranceKmh] ?: 5,
            strictDoNotDisturb = prefs[Keys.strictDoNotDisturb] ?: true,
            audioDuckingEnabled = prefs[Keys.audioDuckingEnabled] ?: true,
            resumeLastMediaOnDrive = prefs[Keys.resumeLastMediaOnDrive] ?: true,
            tripsEnabled = prefs[Keys.tripsEnabled] ?: true,
            customCommandsEnabled = prefs[Keys.customCommandsEnabled] ?: true,
            autoListenOnCarBluetooth = prefs[Keys.autoListenOnCarBluetooth] ?: true
        )
    }

    suspend fun setAssistantName(name: String) = edit { it[Keys.assistantName] = name }
    suspend fun setCarName(name: String) = edit { it[Keys.carName] = name }
    suspend fun setCarBluetooth(mac: String?, name: String?) = edit {
        if (mac == null) it.remove(Keys.carBtMac) else it[Keys.carBtMac] = mac
        if (name == null) it.remove(Keys.carBtName) else it[Keys.carBtName] = name
    }
    suspend fun setObdBluetooth(mac: String?, name: String?) = edit {
        if (mac == null) it.remove(Keys.obdBtMac) else it[Keys.obdBtMac] = mac
        if (name == null) it.remove(Keys.obdBtName) else it[Keys.obdBtName] = name
    }
    suspend fun setMusicAppPackage(pkg: String?) = edit {
        if (pkg == null) it.remove(Keys.musicAppPackage) else it[Keys.musicAppPackage] = pkg
    }
    suspend fun setActivateOnCharging(value: Boolean) = edit { it[Keys.activateOnCharging] = value }
    suspend fun setActivateOnBluetooth(value: Boolean) = edit { it[Keys.activateOnBluetooth] = value }
    suspend fun setActivateOnGpsSpeed(value: Boolean) = edit { it[Keys.activateOnGpsSpeed] = value }
    suspend fun setAutoNightMode(value: Boolean) = edit { it[Keys.autoNightMode] = value }
    suspend fun setDemoMode(value: Boolean) = edit { it[Keys.demoMode] = value }
    suspend fun setPersonality(profile: PersonalityProfile) = edit { it[Keys.personality] = profile.name }
    suspend fun setEtaContact(name: String, phone: String) = edit {
        it[Keys.etaContactName] = name
        it[Keys.etaContactPhone] = phone
    }
    suspend fun setSosPhoneNumber(phone: String) = edit { it[Keys.sosPhoneNumber] = phone }
    suspend fun setSpeedLimitWarnEnabled(value: Boolean) = edit { it[Keys.speedLimitWarnEnabled] = value }
    suspend fun setSpeedLimitTolerance(kmh: Int) = edit { it[Keys.speedLimitToleranceKmh] = kmh }
    suspend fun setStrictDoNotDisturb(value: Boolean) = edit { it[Keys.strictDoNotDisturb] = value }
    suspend fun setAudioDuckingEnabled(value: Boolean) = edit { it[Keys.audioDuckingEnabled] = value }
    suspend fun setResumeLastMediaOnDrive(value: Boolean) = edit { it[Keys.resumeLastMediaOnDrive] = value }
    suspend fun setTripsEnabled(value: Boolean) = edit { it[Keys.tripsEnabled] = value }
    suspend fun setCustomCommandsEnabled(value: Boolean) = edit { it[Keys.customCommandsEnabled] = value }
    suspend fun setAutoListenOnCarBluetooth(value: Boolean) = edit { it[Keys.autoListenOnCarBluetooth] = value }

    private suspend inline fun edit(crossinline block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.micretaDataStore.edit { block(it) }
    }
}
