package com.micreta.app.domain.model

/**
 * A scheduled maintenance reminder.
 *
 * Either or both of [intervalKm] and [intervalDays] can be set:
 *  - intervalKm only → fires when odometer ≥ baseKm + intervalKm
 *  - intervalDays only → fires when today ≥ baseAt + intervalDays
 *  - both → whichever happens first
 */
data class MaintenanceTask(
    val id: String,
    val title: String,
    val kind: Kind,
    val intervalKm: Int? = null,
    val intervalDays: Int? = null,
    val baseOdometerKm: Int = 0,
    val baseAtMs: Long = System.currentTimeMillis(),
    val enabled: Boolean = true
) {
    enum class Kind { OIL, FILTER, TYRES, ITV, INSURANCE, BELT, BRAKES, CLEAN, OTHER }

    /** True when the task should fire given the current odometer & date. */
    fun isDue(currentOdometerKm: Int?, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (!enabled) return false
        val byKm = intervalKm != null && currentOdometerKm != null &&
                currentOdometerKm >= baseOdometerKm + intervalKm
        val byTime = intervalDays != null &&
                nowMs >= baseAtMs + intervalDays * 86_400_000L
        return byKm || byTime
    }
}
