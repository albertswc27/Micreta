package com.micreta.app.domain.model

/**
 * Snapshot of vehicle telemetry. Every field is nullable on purpose:
 * not every Micra K13 ECU exposes every standard PID. `null` means "no
 * disponible" — we never fabricate a value just to fill the slot.
 */
data class VehicleStatus(
    val rpm: Int? = null,
    val speedKmh: Int? = null,
    val coolantTempC: Int? = null,
    val engineLoadPct: Int? = null,
    val throttlePct: Int? = null,
    val fuelLevelPct: Int? = null,
    val intakeAirTempC: Int? = null,
    val batteryVoltage: Double? = null,
    val dtcCodes: List<String> = emptyList(),
    val estimatedRangeKm: Int? = null, // derived; flagged in UI as estimated
    val odometerKm: Int? = null,
    val source: Source = Source.NONE,
    val timestampMs: Long = System.currentTimeMillis()
) {
    enum class Source { NONE, MOCK, OBD_BLUETOOTH }

    val isEmpty: Boolean
        get() = rpm == null && speedKmh == null && coolantTempC == null &&
                engineLoadPct == null && throttlePct == null && fuelLevelPct == null &&
                batteryVoltage == null && dtcCodes.isEmpty()
}
