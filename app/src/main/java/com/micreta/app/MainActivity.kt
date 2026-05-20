package com.micreta.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.micreta.app.core.logging.EventLogger
import com.micreta.app.core.permissions.PermissionsManager
import com.micreta.app.navigation.MicretaNavHost
import com.micreta.app.ui.theme.MicretaTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        result.forEach { (perm, granted) ->
            EventLogger.info("Perm", "$perm = $granted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        EventLogger.info("UI", "MainActivity onCreate")

        setContent {
            MicretaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    LaunchedEffect(Unit) {
                        val missing = PermissionsManager.missing(this@MainActivity)
                        if (missing.isNotEmpty()) {
                            EventLogger.info("Perm", "Requesting: ${missing.joinToString()}")
                            permissionLauncher.launch(missing.toTypedArray())
                        }
                    }
                    MicretaNavHost()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventLogger.info("UI", "MainActivity onDestroy")
    }
}
