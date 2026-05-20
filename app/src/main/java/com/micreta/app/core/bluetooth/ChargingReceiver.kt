package com.micreta.app.core.bluetooth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.micreta.app.core.logging.EventLogger
import com.micreta.app.data.preferences.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Secondary trigger: phone starts charging → likely plugged into the car USB.
 * Optional, off by default. Toggled via [com.micreta.app.domain.model.AppSettings.activateOnCharging].
 */
class ChargingReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val settings = SettingsRepository(appContext).settings.first()
                if (!settings.activateOnCharging) {
                    EventLogger.info(TAG, "Charging event $action - feature disabled, ignoring.")
                    return@launch
                }
                when (action) {
                    Intent.ACTION_POWER_CONNECTED -> {
                        EventLogger.info(TAG, "Charging connected -> driving mode hint.")
                        CarDetectionEvents.notify(CarDetectionEvents.Trigger.ChargingConnected)
                    }
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        EventLogger.info(TAG, "Charging disconnected.")
                        CarDetectionEvents.notify(CarDetectionEvents.Trigger.ChargingDisconnected)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "ChargingRecv"
    }
}
