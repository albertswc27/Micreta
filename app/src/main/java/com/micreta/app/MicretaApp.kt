package com.micreta.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.micreta.app.core.logging.EventLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Application entry point.
 *
 * Startup duties:
 *  1. Create the [AppContainer] so it's ready before any UI is shown.
 *  2. Register the notification channel used by the foreground service.
 *  3. Seed default favorites + maintenance reminders the first time.
 *  4. Wire settings → personality / ducking / speed-limit so changes apply
 *     instantly without restart.
 */
class MicretaApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Capture uncaught crashes to a file so they can be read on the Debug
        // screen (useful when testing in the car without USB/logcat).
        com.micreta.app.core.logging.CrashReporter.install(this)
        container = AppContainer(this)

        createNotificationChannel()

        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        appScope.launch {
            container.favoritesRepository.seedDefaultsIfEmpty()
            container.maintenanceRepository.seedDefaultsIfEmpty()
            EventLogger.info("App", "Defaults seeded.")
        }

        // Live-bind user settings to the long-lived collaborators.
        appScope.launch {
            container.settingsRepository.settings.collect { s ->
                container.personality.ownerName = "Albert"
                container.personality.carName = s.carName.substringBefore(" ").ifBlank { "Micra" }
                container.personality.profile = s.personality
                container.tts.duckingEnabled = s.audioDuckingEnabled
                container.speedLimitWatcher.enabled = s.speedLimitWarnEnabled
                container.speedLimitWatcher.toleranceKmh = s.speedLimitToleranceKmh
                container.radarWatcher.enabled = s.radarWarnEnabled
                com.micreta.app.service.MicretaForegroundService.autoListenOnCarBluetoothEnabled =
                    s.autoListenOnCarBluetooth

                // A03 GPS-speed activation toggle reacts live.
                container.gpsSpeedActivation.enabled = s.activateOnGpsSpeed
                if (s.activateOnGpsSpeed) container.gpsSpeedActivation.start()
                else container.gpsSpeedActivation.stop()

                // A09 Night-mode hint: nothing to enforce at the OS level (Compose
                // theme already defaults to dark), but log so SystemHealth + Debug
                // surface the same fact across the app.
                if (s.autoNightMode && com.micreta.app.core.activation.NightModeController.isNightTime()) {
                    EventLogger.info("Night", "Auto-night active.")
                }
            }
        }

        // Auto-start driving mode when a configured trigger fires (BT car
        // connect, GPS sustained speed, charger connect if enabled).
        appScope.launch {
            com.micreta.app.core.bluetooth.CarDetectionEvents.trigger.collect { t ->
                if (t == null) return@collect
                val s = container.settingsRepository.settings.first()
                when (t) {
                    is com.micreta.app.core.bluetooth.CarDetectionEvents.Trigger.BluetoothConnected -> {
                        if (s.activateOnBluetooth) com.micreta.app.service.MicretaForegroundService.start(this@MicretaApp)
                    }
                    is com.micreta.app.core.bluetooth.CarDetectionEvents.Trigger.ChargingConnected -> {
                        if (s.activateOnCharging) com.micreta.app.service.MicretaForegroundService.start(this@MicretaApp)
                    }
                    is com.micreta.app.core.bluetooth.CarDetectionEvents.Trigger.GpsSpeed -> {
                        if (s.activateOnGpsSpeed) com.micreta.app.service.MicretaForegroundService.start(this@MicretaApp)
                    }
                    else -> { /* disconnection handled by the service itself */ }
                }
                com.micreta.app.core.bluetooth.CarDetectionEvents.reset()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java) ?: return
        val ch = NotificationChannel(
            CHANNEL_DRIVING,
            getString(R.string.notification_channel_driving),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_driving_desc)
            setShowBadge(false)
        }
        nm.createNotificationChannel(ch)
    }

    companion object {
        const val CHANNEL_DRIVING = "micreta_driving_channel"

        private lateinit var instance: MicretaApp
        fun get(): MicretaApp = instance
    }
}
