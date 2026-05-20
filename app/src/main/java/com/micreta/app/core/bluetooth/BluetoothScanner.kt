package com.micreta.app.core.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.micreta.app.core.logging.EventLogger

/**
 * Lists the devices Android already considers "paired".
 *
 * For Micreta the user picks the car / OBD adapter from this list — we don't
 * do active discovery. Pairing should be done from Android Settings once, then
 * the device will appear here.
 */
class BluetoothScanner(private val context: Context) {

    private val adapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }

    fun isBluetoothSupported(): Boolean = adapter != null
    fun isBluetoothEnabled(): Boolean = adapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun pairedDevices(): List<BluetoothDeviceInfo> {
        val a = adapter ?: return emptyList()
        if (!a.isEnabled) return emptyList()
        return try {
            a.bondedDevices.orEmpty().map { BluetoothDeviceInfo(name = runCatching { it.name }.getOrNull(), address = it.address) }
        } catch (se: SecurityException) {
            EventLogger.warn(TAG, "Bluetooth permission missing: ${se.message}")
            emptyList()
        }
    }

    companion object {
        private const val TAG = "BTScan"
    }
}
