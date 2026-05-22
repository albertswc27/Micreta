package com.micreta.app.domain.model

/**
 * User-tunable settings.
 *
 * `carBluetoothMac` is the MAC address of the device the user selected as
 * "my car" — connecting to it triggers driving mode.
 *
 * `obdBluetoothMac` is the MAC of the ELM327/Vgate iCar adapter.
 */
data class AppSettings(
    val assistantName: String = "Micreta",
    val carName: String = "Micra K13",
    val carBluetoothMac: String? = null,
    val carBluetoothName: String? = null,
    val obdBluetoothMac: String? = null,
    val obdBluetoothName: String? = null,
    val musicAppPackage: String? = null, // e.g. com.spotify.music
    val activateOnCharging: Boolean = false,
    val activateOnBluetooth: Boolean = true,
    val activateOnGpsSpeed: Boolean = false,           // A03
    val autoNightMode: Boolean = true,                 // A09
    val demoMode: Boolean = false,
    val personality: PersonalityProfile = PersonalityProfile.FRIENDLY,
    val etaContactName: String = "",                   // C03 (free-text; user-friendly)
    val etaContactPhone: String = "",                  // C03 — empty → share via picker instead
    val sosPhoneNumber: String = "112",                // F11
    val speedLimitWarnEnabled: Boolean = true,         // F01
    val speedLimitToleranceKmh: Int = 5,               // F01 — alert >limit+5
    val strictDoNotDisturb: Boolean = true,            // F06
    val audioDuckingEnabled: Boolean = true,           // D06
    val resumeLastMediaOnDrive: Boolean = true,        // D03
    val tripsEnabled: Boolean = true,                  // E04/E05 master switch
    val customCommandsEnabled: Boolean = true,         // B10
    val autoListenOnCarBluetooth: Boolean = true,      // P2 — offer voice when car BT connects
    val wakeWordEnabled: Boolean = false,              // A06 — off until an engine is available
    val radarWarnEnabled: Boolean = true               // C11 — warn near fixed speed cameras
)
