package com.micreta.app.domain.model

/** Compact weather snapshot from Open-Meteo. Imperial units never used. */
data class WeatherSnapshot(
    val temperatureC: Double,
    val apparentTempC: Double,
    val windKmh: Double,
    val precipitationMm: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    val fetchedAtMs: Long = System.currentTimeMillis()
) {
    /** Spoken-friendly summary. */
    val spoken: String
        get() {
            val baseTemp = "${temperatureC.toInt()} grados"
            val descriptor = when (weatherCode) {
                0 -> "despejado"
                1, 2 -> "ligeramente nublado"
                3 -> "nublado"
                in 45..48 -> "con niebla"
                in 51..57 -> "con lluvia ligera"
                in 61..67 -> "con lluvia"
                in 71..77 -> "con nieve"
                in 80..82 -> "con chubascos"
                in 95..99 -> "con tormenta"
                else -> "tiempo variable"
            }
            return "$baseTemp y $descriptor"
        }
}
