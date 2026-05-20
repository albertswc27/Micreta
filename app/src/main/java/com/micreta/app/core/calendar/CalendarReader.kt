package com.micreta.app.core.calendar

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.micreta.app.core.logging.EventLogger
import com.micreta.app.domain.model.CalendarEventInfo

/**
 * Calendar Provider reader (I01 Resumen del calendario).
 *
 * Reads upcoming events for the user's primary calendar(s). Read-only; we
 * never modify, create, or delete events.
 *
 * Returns at most [MAX_EVENTS] within the next [HORIZON_HOURS] hours so
 * the morning briefing doesn't ramble forever.
 */
class CalendarReader(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun upcomingEvents(
        horizonHours: Int = HORIZON_HOURS,
        max: Int = MAX_EVENTS
    ): List<CalendarEventInfo> {
        if (!hasPermission()) {
            EventLogger.warn(TAG, "READ_CALENDAR permission missing.")
            return emptyList()
        }
        val now = System.currentTimeMillis()
        val until = now + horizonHours * 3_600_000L

        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.ALL_DAY
        )

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now)
        ContentUris.appendId(builder, until)
        val uri = builder.build()

        val results = mutableListOf<CalendarEventInfo>()
        try {
            context.contentResolver.query(
                uri, projection,
                null, null,
                CalendarContract.Instances.BEGIN + " ASC"
            )?.use { cursor ->
                while (cursor.moveToNext() && results.size < max) {
                    val title = cursor.getString(0)?.takeIf { it.isNotBlank() } ?: continue
                    val begin = cursor.getLong(1)
                    val location = cursor.getString(2)?.takeIf { it.isNotBlank() }
                    val allDay = cursor.getInt(3) == 1
                    if (allDay) continue
                    results.add(CalendarEventInfo(title = title, startMs = begin, location = location))
                }
            }
        } catch (t: Throwable) {
            EventLogger.warn(TAG, "Calendar query failed: ${t.message}")
        }
        return results
    }

    companion object {
        private const val TAG = "Calendar"
        private const val HORIZON_HOURS = 12
        private const val MAX_EVENTS = 4
    }
}
