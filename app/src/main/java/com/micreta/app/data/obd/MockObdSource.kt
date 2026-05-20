package com.micreta.app.data.obd

import com.micreta.app.domain.model.VehicleStatus
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Generates plausible telemetry for demo mode and for the OBD screen when no
 * adapter is connected. Values drift smoothly across calls so it doesn't look
 * like a random number generator.
 */
class MockObdSource {

    private var rpm = 900
    private var speed = 0
    private var coolant = 70
    private var load = 15
    private var throttle = 5
    private var fuel = 64
    private var iat = 22
    private var voltage = 14.1
    private var odometer = 142_530

    fun nextTick(): VehicleStatus {
        // RPM wanders between 800–4500. Higher RPM ⇒ higher speed.
        rpm = (rpm + Random.nextInt(-400, 500)).coerceIn(800, 4500)
        speed = (speed + Random.nextInt(-5, 7)).coerceIn(0, 130)
        load = (load + Random.nextInt(-5, 8)).coerceIn(5, 90)
        throttle = (throttle + Random.nextInt(-4, 8)).coerceIn(0, 95)
        coolant = (coolant + Random.nextInt(-1, 2)).coerceIn(60, 105)
        iat = (iat + Random.nextInt(-1, 2)).coerceIn(-5, 45)
        // Voltage stable around 14.0V when engine on, slow drift.
        voltage = max(11.8, min(14.6, voltage + Random.nextDouble(-0.05, 0.05)))
        // Fuel slowly decreases.
        if (Random.nextInt(20) == 0) fuel = max(0, fuel - 1)
        // Odometer ticks up while moving.
        if (speed > 0 && Random.nextInt(5) == 0) odometer += 1

        // Occasional fake DTC for demo realism — emits a code 1 in 50 ticks.
        val dtcs = if (Random.nextInt(50) == 0) listOf("P0133") else emptyList()

        val estimatedRange = if (fuel > 0) (fuel * 6) else null // ~6 km per % (rough)

        return VehicleStatus(
            rpm = rpm,
            speedKmh = speed,
            coolantTempC = coolant,
            engineLoadPct = load,
            throttlePct = throttle,
            fuelLevelPct = fuel,
            intakeAirTempC = iat,
            batteryVoltage = voltage,
            dtcCodes = dtcs,
            estimatedRangeKm = estimatedRange,
            odometerKm = odometer,
            source = VehicleStatus.Source.MOCK
        )
    }
}
