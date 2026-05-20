package com.micreta.app.core.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.micreta.app.core.logging.EventLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.micreta.app.data.preferences.SettingsRepository

/**
 * Detects when the phone connects to a Bluetooth device the user has
 * configured as "my car". Triggers driving mode via [CarDetectionEvents].
 *
 * Registered statically in the manifest so it can wake the app on connect.
 */
class BluetoothCarReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val device: BluetoothDevice? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
        val bluetoothDevice = device ?: return
        val address = try {
            bluetoothDevice.address
        } catch (se: SecurityException) {
            EventLogger.warn(TAG, "Bluetooth address permission missing: ${se.message}")
            null
        } ?: return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val settings = SettingsRepository(appContext).settings.first()
                val expected = settings.carBluetoothMac
                val deviceName = try { bluetoothDevice.name } catch (_: SecurityException) { null }
                val isCar = expected != null && expected.equals(address, ignoreCase = true)
                when (action) {
                    BluetoothDevice.ACTION_ACL_CONNECTED ->
                        BluetoothCarStateMachine.onAclConnected(address, deviceName, isCar)
                    BluetoothDevice.ACTION_ACL_DISCONNECTED ->
                        BluetoothCarStateMachine.onAclDisconnected(address, isCar)
                }
                if (expected == null || !settings.activateOnBluetooth) {
                    EventLogger.info(TAG, "BT event $action $address ignored (no car configured / disabled).")
                    return@launch
                }
                if (isCar) {
                    when (action) {
                        BluetoothDevice.ACTION_ACL_CONNECTED -> {
                            EventLogger.info(TAG, "Authorised car Bluetooth connected -> driving mode.")
                            CarDetectionEvents.notify(CarDetectionEvents.Trigger.BluetoothConnected(address))
                        }
                        BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                            EventLogger.info(TAG, "Authorised car Bluetooth disconnected.")
                            CarDetectionEvents.notify(CarDetectionEvents.Trigger.BluetoothDisconnected(address))
                        }
                    }
                } else {
                    EventLogger.info(TAG, "BT event $action from non-car device $deviceName; ignored.")
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BTReceiver"
    }
}

/**
 * Shared bus of car-detection triggers. The [MicretaForegroundService] (or any
 * subscriber) collects [trigger] and reacts.
 */
object CarDetectionEvents {
    private val _trigger = MutableStateFlow<Trigger?>(null)
    val trigger: StateFlow<Trigger?> = _trigger

    fun notify(t: Trigger) { _trigger.value = t }
    fun reset() { _trigger.value = null }

    sealed class Trigger {
        data class BluetoothConnected(val address: String) : Trigger()
        data class BluetoothDisconnected(val address: String) : Trigger()
        data object ChargingConnected : Trigger()
        data object ChargingDisconnected : Trigger()
        data class GpsSpeed(val speedKmh: Int) : Trigger()
        data object Manual : Trigger()
    }
}
