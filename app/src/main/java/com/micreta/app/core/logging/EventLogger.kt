package com.micreta.app.core.logging

import android.util.Log
import com.micreta.app.domain.model.DebugEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * App-wide event bus used by the Debug screen.
 *
 * Ring-buffer of the last [MAX_EVENTS] events. Survives the lifetime of the
 * process but is *not* persisted — debug events are ephemeral by design.
 *
 * Also forwards to logcat so they show up in `adb logcat` when probing in the car.
 */
object EventLogger {

    private const val MAX_EVENTS = 200

    private val _events = MutableStateFlow<List<DebugEvent>>(emptyList())
    val events: StateFlow<List<DebugEvent>> = _events

    fun info(tag: String, message: String) = log(DebugEvent(tag = tag, message = message, level = DebugEvent.Level.INFO))
    fun warn(tag: String, message: String) = log(DebugEvent(tag = tag, message = message, level = DebugEvent.Level.WARN))
    fun error(tag: String, message: String) = log(DebugEvent(tag = tag, message = message, level = DebugEvent.Level.ERROR))

    fun clear() {
        _events.value = emptyList()
    }

    private fun log(event: DebugEvent) {
        when (event.level) {
            DebugEvent.Level.INFO -> Log.i(event.tag, event.message)
            DebugEvent.Level.WARN -> Log.w(event.tag, event.message)
            DebugEvent.Level.ERROR -> Log.e(event.tag, event.message)
        }
        _events.update { current ->
            val next = current + event
            if (next.size > MAX_EVENTS) next.takeLast(MAX_EVENTS) else next
        }
    }
}
