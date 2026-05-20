package com.micreta.app.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Single source of truth for the permission groups Micreta cares about.
 *
 * Split into:
 *  - [requiredOnBoot]   : kept intentionally empty for first-run exploration
 *  - [optionalMicrophone] : asked when the user starts voice
 *  - [optionalBluetooth] : asked when the user configures Bluetooth / OBD
 *  - [optionalLocation] : asked when the user toggles GPS-based features
 *  - [optionalCalendar] : asked when the user toggles the morning briefing
 *
 * Granular requests reduce drop-off — Android shows the rationale much more
 * politely when you only request what's truly needed at the moment.
 */
object PermissionsManager {

    /** No runtime permission is required just to open and explore Micreta. */
    fun requiredOnBoot(): List<String> = emptyList()

    fun optionalMicrophone(): List<String> = listOf(Manifest.permission.RECORD_AUDIO)

    fun optionalBluetooth(): List<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            add(Manifest.permission.BLUETOOTH)
            add(Manifest.permission.BLUETOOTH_ADMIN)
        }
    }

    fun optionalNotifications(): List<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** Foreground + (optionally) background location. */
    fun optionalLocation(): List<String> = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Background must be requested separately after foreground; we
            // don't bundle it here to keep the prompt simple.
        }
    }

    fun optionalCalendar(): List<String> = listOf(Manifest.permission.READ_CALENDAR)
    fun optionalActivityRecognition(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            listOf(Manifest.permission.ACTIVITY_RECOGNITION)
        else emptyList()

    fun missing(context: Context, perms: List<String> = requiredOnBoot()): List<String> =
        perms.filter { perm ->
            ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
        }

    fun hasAll(context: Context): Boolean = missing(context).isEmpty()
    fun hasMicrophone(context: Context): Boolean = has(context, Manifest.permission.RECORD_AUDIO)
    fun hasFineLocation(context: Context): Boolean = has(context, Manifest.permission.ACCESS_FINE_LOCATION)
    fun hasCalendar(context: Context): Boolean = has(context, Manifest.permission.READ_CALENDAR)

    fun hasBluetoothConnect(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) has(context, Manifest.permission.BLUETOOTH_CONNECT)
        else has(context, Manifest.permission.BLUETOOTH)

    private fun has(context: Context, perm: String): Boolean =
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
}
