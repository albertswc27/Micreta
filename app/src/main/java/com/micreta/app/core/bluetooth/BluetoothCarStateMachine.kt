package com.micreta.app.core.bluetooth

import com.micreta.app.core.logging.EventLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Closed state machine for Bluetooth-driven car detection (A01 hardening).
 *
 * Valid transitions:
 *   DISCONNECTED  → DETECTING        (ACL_CONNECTED of another device while car is configured)
 *   DETECTING     → CONNECTED_CAR    (configured MAC matched)
 *   DETECTING     → DISCONNECTED     (other device, ignored)
 *   CONNECTED_CAR → DRIVING_MODE     (foreground service started)
 *   DRIVING_MODE  → DISCONNECTED     (ACL_DISCONNECTED of the configured MAC)
 *
 * The point is: a Bluetooth headset / speaker should never push us into
 * driving mode. Only the explicitly chosen "Mi Micra" device does.
 */
object BluetoothCarStateMachine {

    enum class State { DISCONNECTED, DETECTING, CONNECTED_CAR, DRIVING_MODE }

    private val _state = MutableStateFlow(State.DISCONNECTED)
    val state: StateFlow<State> = _state

    private val _lastDeviceLog = MutableStateFlow<String?>(null)
    val lastDeviceLog: StateFlow<String?> = _lastDeviceLog

    @Synchronized
    fun onAclConnected(address: String, name: String?, isAuthorisedCar: Boolean) {
        _lastDeviceLog.value = "${name ?: "Sin nombre"} ($address) ${if (isAuthorisedCar) "AUTH" else "OTHER"}"
        if (isAuthorisedCar) {
            transition(State.CONNECTED_CAR)
        } else if (_state.value == State.DISCONNECTED) {
            transition(State.DETECTING)
            // We immediately go back — this was just an audio device.
            transition(State.DISCONNECTED)
        }
    }

    @Synchronized
    fun onAclDisconnected(address: String, isAuthorisedCar: Boolean) {
        if (isAuthorisedCar) {
            transition(State.DISCONNECTED)
        }
    }

    @Synchronized
    fun enterDrivingMode() {
        transition(State.DRIVING_MODE)
    }

    @Synchronized
    fun exitDrivingMode() {
        // We stay CONNECTED_CAR if the BT link is still up — the user manually
        // exited driving mode but the car BT is still connected. Otherwise
        // fall back to DISCONNECTED.
        transition(if (_state.value == State.DRIVING_MODE) State.CONNECTED_CAR else _state.value)
    }

    private fun transition(next: State) {
        val prev = _state.value
        if (prev == next) return
        _state.value = next
        EventLogger.info(TAG, "BT state $prev → $next")
    }

    private const val TAG = "BTStateMachine"
}
