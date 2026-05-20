package com.micreta.app.core.safety

import com.micreta.app.core.location.LocationService
import com.micreta.app.core.logging.EventLogger
import com.micreta.app.core.traffic.SpeedLimitClient
import com.micreta.app.domain.model.SpeedLimit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Speed-limit watcher (F01).
 *
 * Subscribes to GPS, polls Overpass when we cross 100 m from the last
 * lookup, and emits [OverSpeedEvent] whenever (current speed - limit) ≥
 * tolerance. The watcher caches the limit aggressively to avoid hammering
 * Overpass (free tier — be respectful).
 *
 * A 5 s in-event cooldown prevents spamming the user with the same warning.
 */
class SpeedLimitWatcher(
    private val locationService: LocationService,
    private val speedLimitClient: SpeedLimitClient
) {

    private val _limit = MutableStateFlow<SpeedLimit?>(null)
    val limit: StateFlow<SpeedLimit?> = _limit

    private val _events = MutableSharedFlow<OverSpeedEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<OverSpeedEvent> = _events.asSharedFlow()

    @Volatile var toleranceKmh: Int = 5
    @Volatile var enabled: Boolean = true

    private var scope: CoroutineScope? = null
    private var watchJob: Job? = null
    private var lastWarnMs: Long = 0L

    fun start() {
        if (watchJob != null) return
        locationService.startUpdates()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        watchJob = scope!!.launch {
            locationService.location.collect { p ->
                if (!enabled || p == null) return@collect
                val limit = speedLimitClient.lookup(p)
                _limit.value = limit
                val curr = p.speedKmh ?: 0
                val limKmh = limit.kmh ?: return@collect
                val excess = curr - limKmh
                if (excess >= toleranceKmh) {
                    val now = System.currentTimeMillis()
                    if (now - lastWarnMs > 5_000L) {
                        lastWarnMs = now
                        _events.tryEmit(OverSpeedEvent(curr, limKmh))
                        EventLogger.info(TAG, "Over-speed event $curr / $limKmh")
                    }
                }
            }
        }
    }

    fun stop() {
        watchJob?.cancel(); watchJob = null
        scope?.cancel(); scope = null
    }

    data class OverSpeedEvent(val currentKmh: Int, val limitKmh: Int)

    companion object {
        private const val TAG = "SpeedWatch"
    }
}
