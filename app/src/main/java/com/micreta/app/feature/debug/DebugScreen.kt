package com.micreta.app.feature.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.micreta.app.MicretaApp
import com.micreta.app.core.logging.CrashReporter
import com.micreta.app.core.logging.EventLogger
import com.micreta.app.domain.model.AppSettings
import com.micreta.app.domain.model.DebugEvent
import com.micreta.app.ui.components.MicretaCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DebugScreen() {
    val app = MicretaApp.get()
    val events by EventLogger.events.collectAsStateWithLifecycle()
    val ttsReady by app.container.tts.ready.collectAsStateWithLifecycle()
    val speaking by app.container.tts.speaking.collectAsStateWithLifecycle()
    val recAvailable by app.container.voice.available.collectAsStateWithLifecycle()
    val listening by app.container.voice.listening.collectAsStateWithLifecycle()
    val obdConnected by app.container.obd.isConnected.collectAsStateWithLifecycle()
    val obdStatus by app.container.obd.status.collectAsStateWithLifecycle()
    val settings by app.container.settingsRepository.settings.collectAsStateWithLifecycle(AppSettings())

    val listState = rememberLazyListState()
    LaunchedEffect(events.size) {
        if (events.isNotEmpty()) listState.animateScrollToItem(events.lastIndex)
    }

    val ctx = LocalContext.current
    var crash by remember { mutableStateOf(CrashReporter.read(ctx)) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Debug", style = MaterialTheme.typography.headlineMedium)

        crash?.let { text ->
            MicretaCard(title = "Último crash", accent = MaterialTheme.colorScheme.error) {
                Text(
                    text = if (text.length > 6000) text.take(6000) + "…" else text,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { CrashReporter.clear(ctx); crash = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                ) { Text("Limpiar crash") }
            }
        }

        MicretaCard(title = "Subsistemas") {
            StatusRow("TTS", if (ttsReady) "Listo" else "Inicializando")
            StatusRow("Hablando", if (speaking) "Sí" else "No")
            StatusRow("Reconocimiento de voz", if (recAvailable) "Disponible" else "No disponible")
            StatusRow("Escuchando", if (listening) "Sí" else "No")
            StatusRow("OBD conectado", if (obdConnected) "Sí" else "No")
            StatusRow("Origen OBD", obdStatus.source.name)
            StatusRow("Waze instalado", if (app.container.waze.isWazeInstalled()) "Sí" else "No")
            StatusRow("BT coche", settings.carBluetoothMac ?: "—")
            StatusRow("BT OBD", settings.obdBluetoothMac ?: "—")
            StatusRow("App música", settings.musicAppPackage ?: "—")
            StatusRow("Modo demo", if (settings.demoMode) "ON" else "OFF")
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { EventLogger.info("UI", "Manual TTS test"); app.container.tts.speak("Hola, soy Micreta. Esto es una prueba.") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
            ) { Text("Probar TTS") }
            Button(
                onClick = { EventLogger.clear() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
            ) { Text("Limpiar logs") }
        }

        Text("Eventos recientes", style = MaterialTheme.typography.titleLarge)
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(events) { ev -> EventRow(ev) }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

@Composable
private fun EventRow(event: DebugEvent) {
    val color = when (event.level) {
        DebugEvent.Level.INFO -> MaterialTheme.colorScheme.onSurface
        DebugEvent.Level.WARN -> MaterialTheme.colorScheme.tertiary
        DebugEvent.Level.ERROR -> MaterialTheme.colorScheme.error
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
    ) {
        Text(
            text = "${timeFormat.format(Date(event.timestampMs))} • ${event.tag}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(event.message, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}
