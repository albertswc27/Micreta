package com.micreta.app.core.activation

import com.micreta.app.core.bluetooth.CarDetectionEvents
import com.micreta.app.core.location.LocationService
import com.micreta.app.core.logging.EventLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * GPS-speed driven activation (A03).
 *
 * Watches the phone's fused location. When sustained speed exceeds the
 * threshold for [HOLD_SAMPLES] consecutive samples we emit a
 * [CarDetectionEvents.Trigger.GpsSpeed]. A simple counter avoids triggering
 * on a single spike (walking next to a bus, going uphill on a bike, etc.).
 *
 * Stops itself if the user disables [enabled] from Settings.
 */
class GpsSpeedActivationWatcher(
    private val locationService: LocationService
) {

    @Volatile var enabled: Boolean = false
    @Volatile var thresholdKmh: Int = 15

    private var scope: CoroutineScope? = null
    private var job: Job? = null
    private var hits: Int = 0

    fun start() {
        if (job != null) return
        if (!enabled) return
        locationService.startUpdates()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        job = scope!!.launch {
            locationService.location.collect { p ->
                val speed = p?.speedKmh ?: 0
                if (!enabled) return@collect
                if (speed >= thresholdKmh) {
                    hits++
                    if (hits >= HOLD_SAMPLES) {
                        EventLogger.info(TAG, "GPS speed $speed km/h sustained → trigger driving mode.")
                        CarDetectionEvents.notify(CarDetectionEvents.Trigger.GpsSpeed(speed))
                        hits = 0
                    }
                } else if (speed < (thresholdKmh / 2)) {
                    hits = 0
                }
            }
        }
        EventLogger.info(TAG, "GpsSpeedActivationWatcher started threshold=$thresholdKmh.")
    }

    fun stop() {
        job?.cancel(); job = null
        scope?.cancel(); scope = null
        hits = 0
    }

    companion object {
        private const val TAG = "GpsActivation"
        /** ~6 samples × 2 s = ~12 s sustained over threshold before we trigger. */
        private const val HOLD_SAMPLES = 6
    }
}
