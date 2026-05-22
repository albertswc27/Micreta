package com.micreta.app

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.micreta.app.core.logging.EventLogger
import com.micreta.app.navigation.MicretaNavHost
import com.micreta.app.service.MicretaForegroundService
import com.micreta.app.ui.theme.MicretaTheme
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Route requested by a notification action (P2). Read on launch and on
    // every new intent (the Activity is singleTask, so re-tapping a
    // notification re-delivers via onNewIntent rather than onCreate).
    private var requestedRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        EventLogger.info("UI", "MainActivity onCreate")
        requestedRoute = intent?.getStringExtra(MicretaForegroundService.EXTRA_OPEN_ROUTE)

        // Keep the screen awake while driving mode is active so the user can give
        // voice commands without the phone sleeping.
        lifecycleScope.launch {
            MicretaForegroundService.isRunning.collect { running ->
                if (running) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        // Wake word "Micra" — V1 runs ONLY while the app is in the foreground
        // (no background mic loop). Requires a Picovoice AccessKey + the setting
        // enabled; otherwise wakeWord.available is false and this is a no-op.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val container = MicretaApp.get().container
                if (!container.wakeWord.available) return@repeatOnLifecycle
                try {
                    // Listen for "Micra" only when enabled AND we're not already
                    // running the speech recognizer (avoids fighting for the mic).
                    combine(
                        container.settingsRepository.settings,
                        container.voice.listening
                    ) { s, listening -> s.wakeWordEnabled && !listening }
                        .collect { shouldListen ->
                            if (shouldListen) {
                                container.wakeWord.start {
                                    runOnUiThread { requestedRoute = MicretaForegroundService.ROUTE_VOICE }
                                }
                            } else {
                                container.wakeWord.stop()
                            }
                        }
                } finally {
                    container.wakeWord.stop() // stop when the app leaves the foreground
                }
            }
        }

        setContent {
            MicretaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    MicretaNavHost(
                        startRoute = requestedRoute,
                        onRouteConsumed = { requestedRoute = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(MicretaForegroundService.EXTRA_OPEN_ROUTE)?.let {
            EventLogger.info("UI", "MainActivity onNewIntent route=$it")
            requestedRoute = it
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventLogger.info("UI", "MainActivity onDestroy")
    }
}
