package com.micreta.app.core.net

import com.micreta.app.core.logging.EventLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Tiny GET-only HTTPS JSON helper.
 *
 * We deliberately avoid pulling in OkHttp / Retrofit for the v0.2.0 sprint —
 * a handful of calls per drive (weather + speed limit) don't justify another
 * dependency. HttpURLConnection on Android with a short connect/read timeout
 * is good enough.
 */
object HttpJson {

    private const val CONNECT_TIMEOUT_MS = 4_000
    private const val READ_TIMEOUT_MS = 6_000

    suspend fun get(url: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Micreta/0.2.0 (Android; +https://github.com/albert-c-a/micreta)")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                return@withContext Result.failure(RuntimeException("HTTP $code for $url"))
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            // Some endpoints (Overpass) return raw JSON objects, others
            // wrap responses in arrays. Caller handles arrays by overriding.
            Result.success(JSONObject(text))
        } catch (t: Throwable) {
            EventLogger.warn(TAG, "GET $url failed: ${t.message}")
            Result.failure(t)
        } finally {
            conn.disconnect()
        }
    }

    /** Raw GET that returns the body as a String (for endpoints whose root is an array). */
    suspend fun getText(url: String): Result<String> = withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Micreta/0.2.0 (Android; +https://github.com/albert-c-a/micreta)")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                return@withContext Result.failure(RuntimeException("HTTP $code for $url"))
            }
            Result.success(conn.inputStream.bufferedReader().use { it.readText() })
        } catch (t: Throwable) {
            EventLogger.warn(TAG, "GET $url failed: ${t.message}")
            Result.failure(t)
        } finally {
            conn.disconnect()
        }
    }

    private const val TAG = "Http"
    @Suppress("unused") private val TIMEUNIT_REF = TimeUnit.MILLISECONDS
}
