package com.micreta.app.core.safety

import com.micreta.app.core.location.LocationService
import com.micreta.app.core.logging.EventLogger
import com.micreta.app.core.traffic.RadarClient
import com.micreta.app.domain.model.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Warns when approaching a fixed speed camera (C11).
 *
 * Caches nearby radars from [RadarClient] (refreshing as the car moves) and,
 * on each GPS update, emits a [RadarEvent] when a radar is within
 * [WARN_RADIUS_M] *ahead* of the heading and the car is actually moving. Each
 * radar warns once until the cache is refreshed (so it re-arms down the road),
 * and radars behind / on the opposite carriageway are filtered out by heading.
 */
class RadarWatcher(
    private val locationService: LocationService,
    private val radarClient: RadarClient
) {

    private val _events = MutableSharedFlow<RadarEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<RadarEvent> = _events.asSharedFlow()

    @Volatile var enabled: Boolean = true

    private var scope: CoroutineScope? = null
    private var watchJob: Job? = null
    private var radars: List<GeoPoint> = emptyList()
    private var lastFetchPoint: GeoPoint? = null
    private val warned = mutableSetOf<String>()

    fun start() {
        if (watchJob != null) return
        locationService.startUpdates()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        watchJob = scope!!.launch {
            locationService.location.collect { p -> if (enabled && p != null) onLocation(p) }
        }
    }

    fun stop() {
        watchJob?.cancel(); watchJob = null
        scope?.cancel(); scope = null
        radars = emptyList(); lastFetchPoint = null; warned.clear()
    }

    private suspend fun onLocation(p: GeoPoint) {
        // Refresh the radar cache the first time and every ~2 km of travel.
        if (lastFetchPoint == null || lastFetchPoint!!.distanceTo(p) > 2_000.0) {
            radars = radarClient.nearby(p)
            lastFetchPoint = p
            warned.clear() // re-arm as we enter a new area
            if (radars.isNotEmpty()) EventLogger.info(TAG, "Cached ${radars.size} radars nearby.")
        }
        val speed = p.speedKmh ?: 0
        if (speed < 20) return // ignore when stopped / very slow
        val heading = p.bearing
        for (r in radars) {
            val id = "${r.lat},${r.lon}"
            if (id in warned) continue
            val dist = p.distanceTo(r)
            if (dist > WARN_RADIUS_M) continue
            if (heading != null && angleDiff(heading.toDouble(), p.bearingTo(r)) > 75.0) continue
            warned.add(id)
            _events.tryEmit(RadarEvent(distanceM = dist.toInt()))
            EventLogger.info(TAG, "Radar ahead at ${dist.toInt()} m.")
        }
    }

    private fun angleDiff(a: Double, b: Double): Double {
        val d = abs((a - b + 180.0) % 360.0 - 180.0)
        return d
    }

    data class RadarEvent(val distanceM: Int)

    companion object {
        private const val TAG = "Radar"
        private const val WARN_RADIUS_M = 350.0
    }
}
