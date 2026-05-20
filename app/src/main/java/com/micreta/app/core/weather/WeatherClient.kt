package com.micreta.app.core.weather

import com.micreta.app.core.logging.EventLogger
import com.micreta.app.core.net.HttpJson
import com.micreta.app.domain.model.GeoPoint
import com.micreta.app.domain.model.WeatherSnapshot

/**
 * Open-Meteo client (I03 Resumen meteorológico, F13 Modo lluvia).
 *
 * Open-Meteo is free, no API key, generous rate-limit, GDPR-friendly.
 * https://open-meteo.com/
 *
 * Endpoint:
 *   GET https://api.open-meteo.com/v1/forecast
 *       ?latitude=..&longitude=..
 *       &current=temperature_2m,apparent_temperature,wind_speed_10m,precipitation,weather_code,is_day
 */
class WeatherClient {

    suspend fun fetch(point: GeoPoint): WeatherSnapshot? {
        val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=${"%.4f".format(point.lat)}" +
                "&longitude=${"%.4f".format(point.lon)}" +
                "&current=temperature_2m,apparent_temperature,wind_speed_10m,precipitation,weather_code,is_day"
        val response = HttpJson.get(url).getOrElse {
            EventLogger.warn(TAG, "Open-Meteo unreachable: ${it.message}")
            return null
        }
        val current = response.optJSONObject("current") ?: return null
        return WeatherSnapshot(
            temperatureC = current.optDouble("temperature_2m", Double.NaN).let { if (it.isNaN()) 0.0 else it },
            apparentTempC = current.optDouble("apparent_temperature", Double.NaN).let { if (it.isNaN()) 0.0 else it },
            windKmh = current.optDouble("wind_speed_10m", 0.0),
            precipitationMm = current.optDouble("precipitation", 0.0),
            weatherCode = current.optInt("weather_code", 0),
            isDay = current.optInt("is_day", 1) == 1
        )
    }

    companion object {
        private const val TAG = "Weather"
    }
}
