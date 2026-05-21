package com.micreta.app.service

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.micreta.app.MainActivity
import com.micreta.app.MicretaApp
import com.micreta.app.R
import com.micreta.app.core.bluetooth.CarDetectionEvents
import com.micreta.app.core.logging.EventLogger
import com.micreta.app.domain.model.GeoPoint
import com.micreta.app.domain.model.MicretaState
import com.micreta.app.domain.personality.MicretaPersonalityEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Holds driving mode alive in the background.
 *
 * Responsibilities:
 *  - foreground notification (driving icon + tap-to-open MainActivity)
 *  - kicks off OBD polling (real or mock, depending on settings)
 *  - listens to [CarDetectionEvents] so a disconnect can stop the service
 *  - exposes [isRunning] so the UI can show "modo conducción activo"
 *
 * Intentionally does NOT own the voice loop: voice recognition needs a UI
 * lifecycle (Activity) to behave well on Android. The Activity drives voice;
 * the service guarantees the rest of the stack stays warm.
 */
class MicretaForegroundService : LifecycleService() {

    private var speedLimitEventsJob: Job? = null
    private var motionEventsJob: Job? = null
    private var longDriveJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        EventLogger.info(TAG, "Service onCreate.")
        observeCarTriggers()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_START -> startDriving()
            ACTION_STOP -> stopDriving()
        }

        return START_STICKY
    }

    private fun startDriving() {
        if (_isRunning.value) {
            EventLogger.info(TAG, "Driving mode already running; ignoring duplicate start.")
            return
        }

        EventLogger.info(TAG, "Starting driving mode.")
        if (!startForegroundSafely()) {
            // On Android 14+ a typed FGS needs its runtime permission; if none is
            // granted, starting would throw. Abort cleanly instead of crashing.
            EventLogger.warn(TAG, "Foreground start not permitted (missing permission?). Aborting driving mode.")
            stopSelf()
            return
        }
        _isRunning.value = true

        val app = MicretaApp.get()
        app.container.setState(MicretaState.CONNECTED)
        com.micreta.app.core.bluetooth.BluetoothCarStateMachine.enterDrivingMode()

        // F06 strict DND while driving.
        runCatching { app.container.dnd.activate() }
            .onFailure { EventLogger.warn(TAG, "DND activate failed: ${it.message}") }

        // F01 speed-limit watcher.
        runCatching { app.container.speedLimitWatcher.start() }
            .onFailure { EventLogger.warn(TAG, "SpeedLimit watcher failed: ${it.message}") }

        // Bridge over-speed events into spoken alerts + trip counter.
        speedLimitEventsJob?.cancel()
        speedLimitEventsJob = lifecycleScope.launch {
            app.container.speedLimitWatcher.events.collect { ev ->
                // Belt-and-suspenders: honor the live setting so disabling the
                // warning in Ajustes always takes effect, even mid-drive.
                if (!app.container.settingsRepository.settings.first().speedLimitWarnEnabled) return@collect
                app.container.tripRecorder.registerOverSpeedEvent()
                val phrase = app.container.personality.overSpeedWarning(ev.currentKmh, ev.limitKmh)
                app.container.tts.speak(phrase)
            }
        }

        // Bridge harsh motion events into spoken reactions (J05).
        motionEventsJob?.cancel()
        motionEventsJob = lifecycleScope.launch {
            app.container.motionSensor.events.collect { ev ->
                val reaction = if (ev.severity == com.micreta.app.core.sensors.MotionSensor.Severity.EMERGENCY) {
                    com.micreta.app.domain.personality.MicretaPersonalityEngine.Reaction.HARSH_BRAKE
                } else com.micreta.app.domain.personality.MicretaPersonalityEngine.Reaction.HARSH_ACCEL
                app.container.tts.speak(app.container.personality.reaction(reaction))
            }
        }

        // OBD is on-demand only (v0.2.0 feedback). The service no longer
        // auto-starts polling. Telemetry is fetched via:
        //   - voice command "diagnóstico" → snapshot()
        //   - Vehicle Status screen "Conectar" button → startContinuous()
        // The session itself is tracked separately by the trip recorder
        // (GPS + accelerometer), which doesn't need the ELM327 to be live.

        // Greet the user with a context-aware salutation (B04).
        lifecycleScope.launch {
            app.container.tts.speak(app.container.personality.contextualGreeting())
        }

        // D03 Resume the last media app (if configured + setting enabled).
        lifecycleScope.launch {
            val s = app.container.settingsRepository.settings.first()
            if (s.resumeLastMediaOnDrive && !s.musicAppPackage.isNullOrBlank()) {
                kotlinx.coroutines.delay(2500) // wait for greeting + BT audio handshake
                app.container.media.launchMusicApp(s.musicAppPackage)
                app.container.media.play()
                EventLogger.info(TAG, "Auto-resumed media app=${s.musicAppPackage}.")
            }
        }

        // Start the trip recorder so distance / eco-score are captured
        // without OBD. Optional — feature flagged so we never block driving
        // mode on a sensor permission.
        lifecycleScope.launch {
            runCatching { app.container.tripRecorder.start() }
                .onFailure { EventLogger.warn(TAG, "Trip recorder failed to start: ${it.message}") }
        }

        // Refuel-on-arrival: if the previous trip ended at a fuel station, ask
        // now how much was refueled and open the refuel log.
        lifecycleScope.launch {
            val pending = runCatching { app.container.refuelRepository.takePending() }.getOrNull() ?: return@launch
            kotlinx.coroutines.delay(3_500) // let the greeting finish first
            val where = pending.stationName?.takeIf { it.isNotBlank() }?.let { "Repostaste en $it. " } ?: ""
            app.container.tts.speak("$where¿Cuánto has echado? Te abro el registro de repostajes.")
            postRefuelPrompt(pending.stationName)
        }

        // G02-G05: remind about any maintenance task due (ITV, seguro, aceite…).
        lifecycleScope.launch {
            kotlinx.coroutines.delay(6_000) // after greeting + refuel prompt
            val odo = runCatching { app.container.maintenanceRepository.odometerKm.first() }.getOrNull()
            val due = runCatching { app.container.maintenanceRepository.tasks.first().filter { it.isDue(odo) } }.getOrNull().orEmpty()
            if (due.isNotEmpty()) {
                val list = due.joinToString(", ") { it.title }
                app.container.tts.speak(
                    if (due.size == 1) "Recuerda: $list está pendiente." else "Recuerda: tienes pendientes $list."
                )
            }
        }

        // F08: suggest a break after 2 h of continuous driving, then hourly.
        longDriveJob?.cancel()
        longDriveJob = lifecycleScope.launch {
            kotlinx.coroutines.delay(2 * 60 * 60_000L)
            while (true) {
                app.container.tts.speak(
                    "Llevas más de dos horas conduciendo, ${app.container.personality.ownerName}. Cuando puedas, para a descansar."
                )
                kotlinx.coroutines.delay(60 * 60_000L)
            }
        }
    }

    private fun stopDriving() {
        EventLogger.info(TAG, "Stopping driving mode.")
        val app = MicretaApp.get()
        speedLimitEventsJob?.cancel()
        speedLimitEventsJob = null
        motionEventsJob?.cancel()
        motionEventsJob = null
        longDriveJob?.cancel()
        longDriveJob = null
        app.container.obd.stop()
        app.container.speedLimitWatcher.stop()
        runCatching { app.container.dnd.deactivate() }
        com.micreta.app.core.bluetooth.BluetoothCarStateMachine.exitDrivingMode()

        // Close the trip and speak its summary (E11).
        lifecycleScope.launch {
            val summary = runCatching { app.container.tripRecorder.stop() }.getOrNull()
            if (summary != null) {
                app.container.tts.speak(app.container.personality.tripSummary(summary))
            } else {
                app.container.tts.speak(app.container.personality.stopping())
            }

            // Refuel-on-arrival: if we stopped at a fuel station, remember it so
            // the next car start can ask how much was refueled.
            val endLat = summary?.endLat
            val endLon = summary?.endLon
            if (endLat != null && endLon != null) {
                runCatching {
                    val name = app.container.gasStations.nearestFuelName(GeoPoint(endLat, endLon))
                    if (name != null) {
                        app.container.refuelRepository.setPending(name)
                        EventLogger.info(TAG, "Trip ended at a fuel station -> refuel pending.")
                    }
                }
            }

            app.container.setState(MicretaState.SLEEPING)
            _isRunning.value = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun observeCarTriggers() {
        lifecycleScope.launch {
            CarDetectionEvents.trigger.collect { t ->
                when (t) {
                    is CarDetectionEvents.Trigger.BluetoothDisconnected -> {
                        if (_isRunning.value) stopDriving()
                    }
                    else -> { /* connection / manual triggers are handled by the Activity */ }
                }
            }
        }
    }

    private fun buildNotification(): Notification {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        fun activityPi(requestCode: Int, route: String?): PendingIntent {
            val i = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                if (route != null) putExtra(EXTRA_OPEN_ROUTE, route)
            }
            return PendingIntent.getActivity(this, requestCode, i, flags)
        }

        val contentPi = activityPi(0, null)
        val talkPi = activityPi(2, ROUTE_VOICE)   // P2 — "Hablar" opens voice with auto-listen
        val carPi = activityPi(3, ROUTE_STATUS)   // "Coche" opens vehicle status

        val stopIntent = Intent(this, MicretaForegroundService::class.java).apply { action = ACTION_STOP }
        val stopPi = PendingIntent.getService(this, 1, stopIntent, flags)

        val builder = NotificationCompat.Builder(this, MicretaApp.CHANNEL_DRIVING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setOngoing(true)
            .setContentIntent(contentPi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)

        // "Hablar" only when the user opted to be offered voice on connect (P2).
        if (autoListenOnCarBluetoothEnabled) {
            builder.addAction(0, getString(R.string.action_talk), talkPi)
        }
        builder.addAction(0, getString(R.string.action_stop), stopPi)
        builder.addAction(0, getString(R.string.action_car), carPi)
        return builder.build()
    }

    /**
     * Starts the foreground service with a service type matching the runtime
     * permissions we actually hold. On Android 14+ a `location`/`connectedDevice`
     * FGS started without the matching permission throws — this avoids the crash
     * (e.g. tapping "Activar Micreta" before granting location).
     */
    private fun startForegroundSafely(): Boolean {
        val type = computeForegroundType()
        return try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type)
            true
        } catch (e: Exception) {
            EventLogger.error(TAG, "startForeground failed (type=$type): ${e.message}")
            false
        }
    }

    private fun computeForegroundType(): Int {
        var type = 0
        val hasLocation =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasBluetooth =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            else true
        if (hasBluetooth) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        if (hasLocation) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        if (type == 0) type = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        return type
    }

    /** A tap-to-log notification asking how much fuel was added (refuel-on-arrival). */
    private fun postRefuelPrompt(stationName: String?) {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_OPEN_ROUTE, ROUTE_REFUEL)
        }
        val pi = PendingIntent.getActivity(this, 4, intent, flags)
        val text = stationName?.takeIf { it.isNotBlank() }?.let { "¿Cuánto has repostado en $it?" }
            ?: "¿Cuánto has repostado? Toca para apuntarlo."
        val notification = NotificationCompat.Builder(this, MicretaApp.CHANNEL_DRIVING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Apuntar repostaje")
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching { NotificationManagerCompat.from(this).notify(REFUEL_NOTIFICATION_ID, notification) }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        EventLogger.info(TAG, "Service onDestroy.")
        speedLimitEventsJob?.cancel()
        speedLimitEventsJob = null
        motionEventsJob?.cancel()
        motionEventsJob = null
        longDriveJob?.cancel()
        longDriveJob = null
        _isRunning.value = false
    }

    companion object {
        const val ACTION_START = "com.micreta.app.action.START_DRIVING"
        const val ACTION_STOP = "com.micreta.app.action.STOP_DRIVING"
        /** Extra read by [MainActivity] to deep-link into a route (P2 notification actions). */
        const val EXTRA_OPEN_ROUTE = "com.micreta.app.extra.OPEN_ROUTE"
        const val ROUTE_VOICE = "voice"
        const val ROUTE_STATUS = "status"
        const val ROUTE_REFUEL = "refuel"
        private const val NOTIFICATION_ID = 1001
        private const val REFUEL_NOTIFICATION_ID = 1002
        private const val TAG = "MicretaSvc"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning

        /** P2 — kept in sync from settings; gates the "Hablar" notification action. */
        @Volatile
        var autoListenOnCarBluetoothEnabled: Boolean = true

        fun start(context: Context) {
            val intent = Intent(context, MicretaForegroundService::class.java).apply { action = ACTION_START }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MicretaForegroundService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }
    }
}
