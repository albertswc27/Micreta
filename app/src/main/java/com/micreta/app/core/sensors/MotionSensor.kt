package com.micreta.app.core.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.micreta.app.core.logging.EventLogger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.math.sqrt

/**
 * Wraps the phone's linear-accelerometer to emit "harsh event" pulses.
 *
 * Two thresholds (m/s² of total linear acceleration magnitude, gravity removed):
 *  - HARSH_THRESHOLD       (≈ 0.4 g = 3.9 m/s²) — flag harsh accel/brake
 *  - EMERGENCY_THRESHOLD   (≈ 0.7 g = 6.9 m/s²) — flag emergency brake / crash candidate
 *
 * A simple 0.5 s debounce prevents the same event from registering 50 times.
 *
 * The phone's orientation in the car matters: in landscape on a windshield
 * mount, the Y axis points up and X points forward. We don't need to
 * disambiguate the direction here — magnitude is enough for "something
 * abrupt happened". The Trip layer combines it with GPS speed to decide
 * whether it was accel or brake.
 */
class MotionSensor(context: Context) {

    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val linearAccel: Sensor? = manager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    private val _events = MutableSharedFlow<HarshEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<HarshEvent> = _events.asSharedFlow()

    private var lastEventMs: Long = 0L
    private var listening = false

    private val listener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values.getOrNull(0) ?: return
            val y = event.values.getOrNull(1) ?: return
            val z = event.values.getOrNull(2) ?: return
            val magnitude = sqrt(x * x + y * y + z * z)
            val now = System.currentTimeMillis()
            if (magnitude >= HARSH_THRESHOLD && now - lastEventMs > DEBOUNCE_MS) {
                lastEventMs = now
                val severity = if (magnitude >= EMERGENCY_THRESHOLD) Severity.EMERGENCY else Severity.HARSH
                _events.tryEmit(HarshEvent(magnitude = magnitude, severity = severity, timestampMs = now))
                EventLogger.info(TAG, "Harsh event m/s²=${"%.2f".format(magnitude)} sev=$severity")
            }
        }
    }

    fun start() {
        if (listening) return
        val s = linearAccel ?: run {
            EventLogger.warn(TAG, "Linear acceleration sensor not available on this device.")
            return
        }
        manager?.registerListener(listener, s, SensorManager.SENSOR_DELAY_GAME)
        listening = true
        EventLogger.info(TAG, "MotionSensor started.")
    }

    fun stop() {
        if (!listening) return
        manager?.unregisterListener(listener)
        listening = false
        EventLogger.info(TAG, "MotionSensor stopped.")
    }

    data class HarshEvent(
        val magnitude: Float,
        val severity: Severity,
        val timestampMs: Long
    )

    enum class Severity { HARSH, EMERGENCY }

    companion object {
        private const val TAG = "Motion"
        private const val HARSH_THRESHOLD = 3.9f       // ~0.4 g
        private const val EMERGENCY_THRESHOLD = 6.9f   // ~0.7 g
        private const val DEBOUNCE_MS = 500L
    }
}
