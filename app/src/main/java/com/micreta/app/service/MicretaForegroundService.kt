package com.micreta.app.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.micreta.app.MainActivity
import com.micreta.app.MicretaApp
import com.micreta.app.R
import com.micreta.app.core.bluetooth.CarDetectionEvents
import com.micreta.app.core.logging.EventLogger
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
        startForeground(NOTIFICATION_ID, buildNotification())
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
    }

    private fun stopDriving() {
        EventLogger.info(TAG, "Stopping driving mode.")
        val app = MicretaApp.get()
        speedLimitEventsJob?.cancel()
        speedLimitEventsJob = null
        motionEventsJob?.cancel()
        motionEventsJob = null
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
        _isRunning.value = false
    }

    companion object {
        const val ACTION_START = "com.micreta.app.action.START_DRIVING"
        const val ACTION_STOP = "com.micreta.app.action.STOP_DRIVING"
        /** Extra read by [MainActivity] to deep-link into a route (P2 notification actions). */
        const val EXTRA_OPEN_ROUTE = "com.micreta.app.extra.OPEN_ROUTE"
        const val ROUTE_VOICE = "voice"
        const val ROUTE_STATUS = "status"
        private const val NOTIFICATION_ID = 1001
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
