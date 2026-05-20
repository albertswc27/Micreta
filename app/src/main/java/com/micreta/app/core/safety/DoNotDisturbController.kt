package com.micreta.app.core.safety

import android.app.NotificationManager
import android.content.Context
import com.micreta.app.core.logging.EventLogger

/**
 * Strict "no molestar" while driving (F06).
 *
 * We don't force-enable the system DND policy (that requires
 * ACCESS_NOTIFICATION_POLICY which surfaces a Settings deep-link, a poor
 * UX on first launch). Instead we ask the OS whether DND is on and surface
 * a hint card in Settings if it isn't — driving mode still proceeds.
 *
 * If the user has granted ACCESS_NOTIFICATION_POLICY (Settings → Apps →
 * Special access), we set INTERRUPTION_FILTER_PRIORITY for the duration
 * of the drive and restore the previous filter on exit.
 */
class DoNotDisturbController(context: Context) {

    private val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var previousFilter: Int? = null

    fun hasPolicyAccess(): Boolean = nm.isNotificationPolicyAccessGranted

    fun activate() {
        if (!hasPolicyAccess()) {
            EventLogger.info(TAG, "DND policy not granted — soft-mode only.")
            return
        }
        previousFilter = nm.currentInterruptionFilter
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        EventLogger.info(TAG, "Strict DND activated (priority-only).")
    }

    fun deactivate() {
        if (!hasPolicyAccess()) return
        val prev = previousFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL
        nm.setInterruptionFilter(prev)
        previousFilter = null
        EventLogger.info(TAG, "Strict DND restored to filter=$prev.")
    }

    companion object {
        private const val TAG = "DND"
    }
}
