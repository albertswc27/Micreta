package com.micreta.app.domain.model

/**
 * A surfaced condition derived from telemetry. UI renders these as warning
 * cards and Micreta announces the [spokenMessage] via TTS.
 */
sealed class ObdAlert(open val spokenMessage: String, open val severity: Severity) {

    enum class Severity { INFO, WARNING, CRITICAL }

    data class HighCoolantTemp(val tempC: Int) :
        ObdAlert("Cuidado, el motor está a $tempC grados. Está caliente.", Severity.CRITICAL)

    data class LowBattery(val voltage: Double) :
        ObdAlert("La batería marca $voltage voltios. Está baja.", Severity.WARNING)

    data class LowFuel(val fuelPct: Int) :
        ObdAlert("Queda $fuelPct por ciento de combustible. Conviene repostar.", Severity.WARNING)

    data class DtcDetected(val codes: List<String>) :
        ObdAlert(
            "He detectado ${codes.size} código${if (codes.size == 1) "" else "s"} de avería en el coche.",
            Severity.WARNING
        )

    data object ObdConnectionLost :
        ObdAlert("He perdido la conexión con el coche.", Severity.WARNING)
}
