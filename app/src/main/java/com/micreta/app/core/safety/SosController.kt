package com.micreta.app.core.safety

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import com.micreta.app.core.logging.EventLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Voice-triggered SOS (F11).
 *
 * Flow:
 *  1. User says "Micreta, llama a emergencias".
 *  2. We start a countdown (default 5 s) — UI surfaces a cancel button
 *     and Micreta speaks "voy a llamar en 5 segundos, di 'cancela'".
 *  3. After the countdown we open the system phone dialer with the SOS
 *     number prefilled. We **do not auto-dial** — Android's CALL_PHONE is
 *     scoped to ACTION_CALL which can be revoked or restricted. We open
 *     ACTION_DIAL so the user just taps "Call". This is safer (no risk of
 *     accidental call) and matches OEM behaviour (BMW, Volvo).
 *
 * Cancel either with the UI button or the "cancela" voice command.
 */
class SosController(private val context: Context) {

    enum class State { IDLE, ARMED, CALLING, CANCELLED }

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds

    private var countdownJob: Job? = null
    private var scope: CoroutineScope? = null

    /** Start a countdown that will dial [phone] when it reaches zero. */
    fun arm(phone: String, seconds: Int = DEFAULT_COUNTDOWN, onCall: (() -> Unit)? = null) {
        cancel()
        _state.value = State.ARMED
        _remainingSeconds.value = seconds
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        countdownJob = scope!!.launch {
            for (s in seconds downTo 1) {
                _remainingSeconds.value = s
                delay(1000)
                if (_state.value != State.ARMED) return@launch
            }
            _remainingSeconds.value = 0
            _state.value = State.CALLING
            dial(phone)
            onCall?.invoke()
        }
        EventLogger.info(TAG, "SOS armed to call $phone in $seconds s.")
    }

    fun cancel() {
        countdownJob?.cancel()
        countdownJob = null
        scope?.cancel(); scope = null
        if (_state.value == State.ARMED) {
            _state.value = State.CANCELLED
            EventLogger.info(TAG, "SOS cancelled.")
        }
        _remainingSeconds.value = 0
    }

    /** Reset state back to IDLE after the UI acknowledged. */
    fun acknowledge() {
        _state.value = State.IDLE
    }

    private fun dial(phone: String) {
        val canCall = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
        val action = if (canCall) Intent.ACTION_DIAL else Intent.ACTION_DIAL
        val intent = Intent(action, Uri.parse("tel:$phone")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            EventLogger.info(TAG, "Opened dialer for $phone.")
        } catch (t: Throwable) {
            EventLogger.error(TAG, "Failed to open dialer: ${t.message}")
        }
    }

    companion object {
        private const val TAG = "SOS"
        const val DEFAULT_COUNTDOWN = 5
    }
}
