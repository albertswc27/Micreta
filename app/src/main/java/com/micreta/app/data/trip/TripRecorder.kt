package com.micreta.app.data.trip

import com.micreta.app.core.location.LocationService
import com.micreta.app.core.logging.EventLogger
import com.micreta.app.core.sensors.MotionSensor
import com.micreta.app.data.preferences.ParkingMemoryRepository
import com.micreta.app.domain.model.GeoPoint
import com.micreta.app.domain.model.ParkingMemory
import com.micreta.app.domain.model.TripSession
import com.micreta.app.domain.model.TripSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.max

/**
 * Records the live driving session entirely from the phone's own sensors
 * (GPS + linear accel). No OBD required.
 *
 * Inputs:
 *  - [LocationService.location] feeds distance, max speed, parking memory.
 *  - [MotionSensor.events]      feeds harsh accel / brake counters.
 *
 * Output:
 *  - [session] is the live in-memory state (UI can subscribe).
 *  - [stop] returns a finalised [TripSummary] and persists it to [TripRepository].
 *
 * The eco-score formula is intentionally simple — events per km penalised
 * up to a floor. Tweaking the weights later won't break stored summaries.
 */
class TripRecorder(
    private val locationService: LocationService,
    private val motionSensor: MotionSensor,
    private val tripRepo: TripRepository,
    private val parkingRepo: ParkingMemoryRepository,
    private val onOverSpeed: () -> Unit = {}
) {

    private val _session = MutableStateFlow<TripSession?>(null)
    val session: StateFlow<TripSession?> = _session

    private var scope: CoroutineScope? = null
    private var locJob: Job? = null
    private var motionJob: Job? = null
    private var lastPoint: GeoPoint? = null

    fun start() {
        if (_session.value != null) return // already running
        val now = System.currentTimeMillis()
        val initial = TripSession(
            id = UUID.randomUUID().toString(),
            startedAtMs = now,
            startLocation = locationService.lastKnown()
        )
        _session.value = initial
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        locationService.startUpdates()
        motionSensor.start()

        locJob = scope!!.launch {
            locationService.location.collect { p ->
                if (p != null) onLocation(p)
            }
        }
        motionJob = scope!!.launch {
            motionSensor.events.collect { ev -> onHarshEvent(ev) }
        }

        EventLogger.info(TAG, "Trip started id=${initial.id}")
    }

    /** Stops the recorder, persists the trip and returns its summary. */
    suspend fun stop(): TripSummary? {
        val current = _session.value ?: return null
        motionSensor.stop()
        locationService.stopUpdates()
        locJob?.cancel(); locJob = null
        motionJob?.cancel(); motionJob = null
        scope?.cancel(); scope = null

        val endedAt = System.currentTimeMillis()
        val durationSec = ((endedAt - current.startedAtMs) / 1000L).coerceAtLeast(1L)
        val distanceKm = current.distanceKm
        val avgSpeed = (distanceKm * 3600 / durationSec).toInt().coerceAtLeast(0)
        val eco = computeEcoScore(current)
        // Prefer the freshest fix, but fall back to the last point seen during
        // the trip so we still remember where the car was parked.
        val end = locationService.lastKnown() ?: lastPoint ?: current.startLocation

        val summary = TripSummary(
            id = current.id,
            startedAtMs = current.startedAtMs,
            endedAtMs = endedAt,
            distanceKm = distanceKm,
            maxSpeedKmh = current.maxSpeedKmh,
            avgSpeedKmh = avgSpeed,
            harshAccelerations = current.harshAccelerations,
            harshBrakings = current.harshBrakings,
            overSpeedEvents = current.overSpeedEvents,
            ecoScore = eco,
            estimatedConsumptionL100 = estimateConsumption(current),
            startLat = current.startLocation?.lat,
            startLon = current.startLocation?.lon,
            endLat = end?.lat,
            endLon = end?.lon
        )

        // Persist trip + parking memory.
        tripRepo.append(summary)
        end?.let {
            parkingRepo.save(
                ParkingMemory(
                    lat = it.lat,
                    lon = it.lon,
                    savedAtMs = endedAt,
                    tripId = summary.id
                )
            )
        }

        _session.value = null
        EventLogger.info(TAG, "Trip stopped id=${summary.id} dist=${"%.2f".format(distanceKm)}km eco=$eco")
        return summary
    }

    fun cancel() {
        motionSensor.stop()
        locationService.stopUpdates()
        locJob?.cancel(); locJob = null
        motionJob?.cancel(); motionJob = null
        scope?.cancel(); scope = null
        _session.value = null
        EventLogger.info(TAG, "Trip cancelled.")
    }

    // --- internals -------------------------------------------------------

    private fun onLocation(p: GeoPoint) {
        val s = _session.value ?: return
        val previous = lastPoint
        val newDistance = if (previous != null) {
            val delta = previous.distanceTo(p)
            // Drop GPS jitter < 5 m to avoid inflating distance when parked.
            if (delta < 5.0) s.distanceM else s.distanceM + delta
        } else s.distanceM

        val newMaxSpeed = max(s.maxSpeedKmh, p.speedKmh ?: 0)
        _session.value = s.copy(
            distanceM = newDistance,
            maxSpeedKmh = newMaxSpeed,
            startLocation = s.startLocation ?: p
        )
        lastPoint = p
    }

    private fun onHarshEvent(ev: MotionSensor.HarshEvent) {
        val s = _session.value ?: return
        val speed = lastPoint?.speedKmh ?: 0
        // Heuristic: if speed is increasing recently → accel, else brake.
        // Without diff history we approximate by absolute speed: low speed
        // + harsh = accel from stop, higher speed + harsh = brake.
        val isBrake = speed >= 30
        _session.value = if (isBrake)
            s.copy(harshBrakings = s.harshBrakings + 1)
        else
            s.copy(harshAccelerations = s.harshAccelerations + 1)
    }

    fun registerOverSpeedEvent() {
        val s = _session.value ?: return
        _session.value = s.copy(overSpeedEvents = s.overSpeedEvents + 1)
        onOverSpeed()
    }

    /**
     * Score 0–100. Start at 100, subtract penalties.
     *  - 6 per harsh accel
     *  - 8 per harsh brake (penalised more than accel)
     *  - 4 per over-speed event
     * Then normalise per 10 km so short bumpy trips don't get nuked.
     */
    private fun computeEcoScore(s: TripSession): Int {
        val km = (s.distanceKm).coerceAtLeast(0.5)
        val penalty = (s.harshAccelerations * 6 + s.harshBrakings * 8 + s.overSpeedEvents * 4)
        val normalised = penalty * 10.0 / km
        return (100 - normalised).toInt().coerceIn(0, 100)
    }

    /**
     * Without MAF/MAP PIDs we can't know real consumption. We estimate from
     * urban / highway speed profile + a base figure tuned for the Micra K13
     * 1.2 (≈ 5.5 L/100). Marked as estimated in the UI.
     */
    private fun estimateConsumption(s: TripSession): Double? {
        if (s.distanceKm < 0.5) return null
        val avgKmh = (s.distanceKm * 3600 / ((s.durationMs / 1000L).coerceAtLeast(1))).coerceIn(0.0, 200.0)
        val base = when {
            avgKmh < 25 -> 7.5  // ciudad densa
            avgKmh < 50 -> 6.0  // ciudad fluida
            avgKmh < 90 -> 5.2  // mixto / extraurbano
            else -> 6.0          // autovía a 120
        }
        val harshPenalty = 0.05 * (s.harshAccelerations + s.harshBrakings)
        return (base + harshPenalty).coerceIn(3.5, 12.0)
    }

    companion object {
        private const val TAG = "Trip"
    }
}
