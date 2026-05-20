package com.micreta.app.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.micreta.app.core.logging.EventLogger
import com.micreta.app.domain.model.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Thin wrapper around FusedLocationProviderClient.
 *
 * Two roles in v0.2.0:
 *  1. Feed [TripRecorder] with smooth speed + position samples (used by
 *     the eco-score, max-speed, and parking memory features).
 *  2. Drive GPS-based activation (A03) when Bluetooth pairing isn't enough.
 *
 * High accuracy + 2 s interval is the right trade-off for driving — much
 * faster drains battery without adding useful precision, much slower
 * misses harsh events.
 */
class LocationService(private val context: Context) {

    private val _location = MutableStateFlow<GeoPoint?>(null)
    val location: StateFlow<GeoPoint?> = _location

    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val last = result.lastLocation ?: return
            _location.value = GeoPoint(
                lat = last.latitude,
                lon = last.longitude,
                accuracyM = if (last.hasAccuracy()) last.accuracy else null,
                speedMps = if (last.hasSpeed()) last.speed else null,
                bearing = if (last.hasBearing()) last.bearing else null,
                timestampMs = last.time
            )
        }
    }

    private var listening = false

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun startUpdates() {
        if (listening) return
        if (!hasPermission()) {
            EventLogger.warn(TAG, "Location permission missing; not starting updates.")
            return
        }
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
            .setMinUpdateDistanceMeters(2f)
            .setMinUpdateIntervalMillis(1_000L)
            .build()
        client.requestLocationUpdates(req, callback, Looper.getMainLooper())
        listening = true
        EventLogger.info(TAG, "Location updates started.")
    }

    fun stopUpdates() {
        if (!listening) return
        client.removeLocationUpdates(callback)
        listening = false
        EventLogger.info(TAG, "Location updates stopped.")
    }

    /** Returns the most recent emitted GeoPoint, or null if we never received one. */
    fun lastKnown(): GeoPoint? = _location.value

    /**
     * Returns a usable fix on demand: the last known point if we have one,
     * otherwise starts updates and waits up to [timeoutMs] for the first fix.
     * Null when permission is missing or no fix arrives in time.
     */
    suspend fun awaitFix(timeoutMs: Long = 8_000L): GeoPoint? {
        lastKnown()?.let { return it }
        if (!hasPermission()) return null
        startUpdates()
        return withTimeoutOrNull(timeoutMs) { location.filterNotNull().first() }
    }

    companion object {
        private const val TAG = "Location"
    }
}
