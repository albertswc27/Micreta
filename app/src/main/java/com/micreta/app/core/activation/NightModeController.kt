package com.micreta.app.core.activation

import java.util.Calendar

/**
 * Auto-night-mode decider (A09).
 *
 * Tells the UI whether the night palette should be applied right now. The
 * call is intentionally side-effect free — the Composable reads it on each
 * recomposition. Decision rule:
 *
 *  - Always night between 21:00 and 07:00 local time.
 *  - Force-on when the user has the feature enabled and the system is in
 *    dark mode (the Compose [androidx.compose.foundation.isSystemInDarkTheme]
 *    is already handled by [com.micreta.app.ui.theme.MicretaTheme]).
 *
 * Used by [com.micreta.app.ui.theme.MicretaTheme] which already defaults to
 * dark; this controller drives a *more subdued* variant (lower contrast,
 * lower TTS volume) when [isNightTime] is true.
 */
object NightModeController {

    fun isNightTime(now: Calendar = Calendar.getInstance()): Boolean {
        val h = now.get(Calendar.HOUR_OF_DAY)
        return h >= 21 || h < 7
    }

    /** Multiplier applied to TTS volume / brightness when night. */
    fun nightDimMultiplier(enabled: Boolean): Float =
        if (enabled && isNightTime()) 0.7f else 1.0f
}
