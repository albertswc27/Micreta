package com.micreta.app.core.bluetooth

/** Lightweight projection of [android.bluetooth.BluetoothDevice]. */
data class BluetoothDeviceInfo(
    val name: String?,
    val address: String
) {
    val displayName: String get() = (name?.takeIf { it.isNotBlank() } ?: "Sin nombre") + " ($address)"
}
