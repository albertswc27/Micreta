package com.micreta.app.data.obd

/**
 * Local OBD-II DTC dictionary (E06).
 *
 * Standardized P0xxx codes that any OBD-II compliant car may emit. The full
 * SAE J2012 dictionary has ~18k entries; we ship a curated subset (~200) of
 * the most common faults so the binary stays small. Codes we don't know are
 * surfaced verbatim with a generic family hint (P03xx → "Encendido /
 * detección de fallo de fuego").
 *
 * All strings in Spanish — Micreta speaks Spanish first.
 */
object DtcDictionary {

    data class Entry(val code: String, val description: String, val severity: Severity)
    enum class Severity { INFO, WARNING, CRITICAL }

    /** Lookup a single code. Falls back to a family hint if unknown. */
    fun lookup(code: String): Entry {
        val upper = code.uppercase()
        catalog[upper]?.let { return it }
        return Entry(upper, familyHint(upper), familySeverity(upper))
    }

    fun describeAll(codes: List<String>): List<Entry> = codes.map { lookup(it) }

    private fun familyHint(code: String): String {
        if (code.length < 4) return "Código desconocido"
        val family = code.substring(0, 2) // P0, P1, C0, B0, U0...
        return when (family) {
            "P0" -> when (code[2]) {
                '1' -> "Mezcla de aire/combustible o sistema EVAP"
                '2' -> "Sistema de inyección o combustible"
                '3' -> "Encendido o detección de fallo de fuego"
                '4' -> "Control de emisiones (catalizador, EGR, lambda)"
                '5' -> "Velocidad del vehículo, ralentí o entradas auxiliares"
                '6' -> "Ordenador / circuito de salida"
                '7', '8' -> "Transmisión"
                else -> "Fallo genérico del motor"
            }
            "P1" -> "Fallo específico del fabricante (motor / transmisión)"
            "C0" -> "Chasis (frenos, dirección, suspensión)"
            "B0" -> "Carrocería (airbag, climatización, cierre)"
            "U0" -> "Red de comunicación entre módulos (CAN bus)"
            else -> "Código no catalogado"
        }
    }

    private fun familySeverity(code: String): Severity = when {
        code.startsWith("P00") -> Severity.WARNING // engine misfire etc
        code.startsWith("P03") -> Severity.WARNING // misfire family
        code.startsWith("C0") -> Severity.CRITICAL // brakes/steering
        code.startsWith("B0") -> Severity.WARNING
        else -> Severity.INFO
    }

    private val catalog: Map<String, Entry> = buildMap {
        // Engine / fuel & air metering
        put("P0100", Entry("P0100", "Circuito del sensor de flujo de aire (MAF) defectuoso", Severity.WARNING))
        put("P0101", Entry("P0101", "Sensor MAF fuera de rango", Severity.WARNING))
        put("P0102", Entry("P0102", "Sensor MAF señal baja", Severity.WARNING))
        put("P0103", Entry("P0103", "Sensor MAF señal alta", Severity.WARNING))
        put("P0110", Entry("P0110", "Sensor de temperatura de aire (IAT) defectuoso", Severity.WARNING))
        put("P0112", Entry("P0112", "Sensor IAT señal baja", Severity.INFO))
        put("P0113", Entry("P0113", "Sensor IAT señal alta", Severity.INFO))
        put("P0115", Entry("P0115", "Sensor de temperatura refrigerante (ECT) defectuoso", Severity.WARNING))
        put("P0117", Entry("P0117", "Sensor ECT señal baja", Severity.WARNING))
        put("P0118", Entry("P0118", "Sensor ECT señal alta", Severity.WARNING))
        put("P0120", Entry("P0120", "Posición del acelerador / pedal — circuito defectuoso", Severity.WARNING))
        put("P0125", Entry("P0125", "Refrigerante no alcanza temperatura para cerrar lazo", Severity.INFO))
        put("P0128", Entry("P0128", "Refrigerante bajo temperatura — termostato sospechoso", Severity.INFO))
        put("P0130", Entry("P0130", "Sensor O₂ banco 1 sensor 1 defectuoso", Severity.WARNING))
        put("P0133", Entry("P0133", "Sensor O₂ banco 1 sensor 1 respuesta lenta", Severity.WARNING))
        put("P0134", Entry("P0134", "Sensor O₂ banco 1 sensor 1 sin actividad", Severity.WARNING))
        put("P0135", Entry("P0135", "Calefactor de sonda O₂ banco 1 sensor 1 defectuoso", Severity.WARNING))
        put("P0136", Entry("P0136", "Sensor O₂ banco 1 sensor 2 defectuoso", Severity.WARNING))
        put("P0141", Entry("P0141", "Calefactor de sonda O₂ banco 1 sensor 2 defectuoso", Severity.WARNING))
        put("P0150", Entry("P0150", "Sensor O₂ banco 2 sensor 1 defectuoso", Severity.WARNING))

        // Fuel system
        put("P0171", Entry("P0171", "Mezcla pobre — banco 1", Severity.WARNING))
        put("P0172", Entry("P0172", "Mezcla rica — banco 1", Severity.WARNING))
        put("P0174", Entry("P0174", "Mezcla pobre — banco 2", Severity.WARNING))
        put("P0175", Entry("P0175", "Mezcla rica — banco 2", Severity.WARNING))
        put("P0190", Entry("P0190", "Sensor de presión del riel de combustible", Severity.WARNING))

        // Ignition / misfire
        put("P0300", Entry("P0300", "Fallo de encendido múltiple aleatorio", Severity.CRITICAL))
        put("P0301", Entry("P0301", "Fallo de encendido en cilindro 1", Severity.WARNING))
        put("P0302", Entry("P0302", "Fallo de encendido en cilindro 2", Severity.WARNING))
        put("P0303", Entry("P0303", "Fallo de encendido en cilindro 3", Severity.WARNING))
        put("P0304", Entry("P0304", "Fallo de encendido en cilindro 4", Severity.WARNING))
        put("P0305", Entry("P0305", "Fallo de encendido en cilindro 5", Severity.WARNING))
        put("P0306", Entry("P0306", "Fallo de encendido en cilindro 6", Severity.WARNING))
        put("P0325", Entry("P0325", "Sensor de detonación (knock) banco 1 defectuoso", Severity.WARNING))
        put("P0335", Entry("P0335", "Sensor de posición de cigüeñal — circuito defectuoso", Severity.WARNING))
        put("P0340", Entry("P0340", "Sensor de posición del árbol de levas defectuoso", Severity.WARNING))

        // Emission control
        put("P0400", Entry("P0400", "Flujo EGR insuficiente", Severity.WARNING))
        put("P0420", Entry("P0420", "Eficiencia del catalizador por debajo del umbral", Severity.WARNING))
        put("P0430", Entry("P0430", "Eficiencia del catalizador banco 2 baja", Severity.WARNING))
        put("P0441", Entry("P0441", "Sistema EVAP — flujo incorrecto", Severity.INFO))
        put("P0442", Entry("P0442", "Fuga pequeña en sistema EVAP (tapón gasolina suelto)", Severity.INFO))
        put("P0455", Entry("P0455", "Fuga grande en sistema EVAP", Severity.WARNING))

        // Vehicle speed / idle
        put("P0500", Entry("P0500", "Sensor de velocidad del vehículo (VSS) defectuoso", Severity.WARNING))
        put("P0501", Entry("P0501", "Sensor VSS fuera de rango", Severity.WARNING))
        put("P0505", Entry("P0505", "Control de ralentí (IAC) defectuoso", Severity.WARNING))
        put("P0507", Entry("P0507", "Ralentí más alto de lo esperado", Severity.INFO))

        // Output / computer
        put("P0600", Entry("P0600", "Falla de comunicación del bus serial", Severity.WARNING))
        put("P0601", Entry("P0601", "Error de memoria de la ECU", Severity.CRITICAL))
        put("P0606", Entry("P0606", "ECU — fallo interno del procesador", Severity.CRITICAL))

        // Transmission (typical for AT — Micra K13 mostly MT but listed)
        put("P0700", Entry("P0700", "Sistema de transmisión — fallo genérico", Severity.WARNING))
        put("P0715", Entry("P0715", "Sensor de velocidad de entrada de transmisión", Severity.WARNING))
        put("P0720", Entry("P0720", "Sensor de velocidad de salida de transmisión", Severity.WARNING))
        put("P0741", Entry("P0741", "Convertidor de par — solenoide pegado abierto", Severity.WARNING))

        // Network / CAN bus
        put("U0001", Entry("U0001", "Bus CAN de alta velocidad — comunicación", Severity.WARNING))
        put("U0100", Entry("U0100", "Pérdida de comunicación con ECM/PCM", Severity.CRITICAL))
        put("U0101", Entry("U0101", "Pérdida de comunicación con TCM", Severity.WARNING))
        put("U0121", Entry("U0121", "Pérdida de comunicación con ABS", Severity.CRITICAL))

        // Body / chassis common
        put("B0001", Entry("B0001", "Airbag conductor — fallo de circuito", Severity.CRITICAL))
        put("C0035", Entry("C0035", "Sensor de velocidad de rueda delantera izquierda", Severity.WARNING))
        put("C0040", Entry("C0040", "Sensor de velocidad de rueda delantera derecha", Severity.WARNING))
        put("C1201", Entry("C1201", "Sistema ABS / control de tracción — fallo", Severity.CRITICAL))
    }
}
