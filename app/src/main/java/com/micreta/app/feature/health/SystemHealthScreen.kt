package com.micreta.app.feature.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.micreta.app.MicretaApp
import com.micreta.app.core.activation.NightModeController
import com.micreta.app.core.permissions.PermissionsManager
import com.micreta.app.domain.model.AppSettings
import com.micreta.app.ui.components.MicretaCard

/**
 * Pantalla "Salud del sistema" (L08): qué subsistemas están listos para
 * correr en este momento. Apoya el endurecimiento — el usuario ve de un
 * vistazo si falta un permiso, si la app musical no está instalada, etc.
 */
@Composable
fun SystemHealthScreen() {
    val app = MicretaApp.get()
    val ctx = LocalContext.current
    val settings by app.container.settingsRepository.settings.collectAsStateWithLifecycle(AppSettings())
    val ttsReady by app.container.tts.ready.collectAsStateWithLifecycle()
    val recAvailable by app.container.voice.available.collectAsStateWithLifecycle()
    val obdConnected by app.container.obd.isConnected.collectAsStateWithLifecycle()
    val obdSource by app.container.obd.source.collectAsStateWithLifecycle()
    val btState by com.micreta.app.core.bluetooth.BluetoothCarStateMachine.state.collectAsStateWithLifecycle()

    val pm = ctx.packageManager
    val musicInstalled = settings.musicAppPackage?.let { p ->
        runCatching { pm.getPackageInfo(p, 0) }.isSuccess
    } ?: false
    val wazeInstalled = app.container.waze.isWazeInstalled()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Salud del sistema", style = MaterialTheme.typography.headlineMedium)

        MicretaCard(title = "Permisos") {
            Status("Micrófono", PermissionsManager.hasMicrophone(ctx))
            Status("Bluetooth", PermissionsManager.hasBluetoothConnect(ctx))
            Status("Ubicación", PermissionsManager.hasFineLocation(ctx))
            Status("Calendario", PermissionsManager.hasCalendar(ctx))
            Status("Política de notificaciones (DND)", app.container.dnd.hasPolicyAccess())
        }
        MicretaCard(title = "Voz") {
            Status("TTS listo", ttsReady)
            Status("Reconocimiento de voz disponible", recAvailable)
        }
        MicretaCard(title = "Bluetooth coche") {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Estado", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(btState.name, style = MaterialTheme.typography.titleLarge)
            }
            Status("Coche configurado", !settings.carBluetoothMac.isNullOrBlank())
        }
        MicretaCard(title = "OBD") {
            Status("OBD configurado", !settings.obdBluetoothMac.isNullOrBlank())
            Status("OBD conectado", obdConnected)
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Origen", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(obdSource.name, style = MaterialTheme.typography.titleLarge)
            }
        }
        MicretaCard(title = "Apps externas") {
            Status("Waze instalado", wazeInstalled)
            Status("App musical configurada e instalada", musicInstalled)
        }
        MicretaCard(title = "Modos") {
            Status("Modo demo", settings.demoMode)
            Status("Activar al conectar BT del coche", settings.activateOnBluetooth)
            Status("Activar al cargar", settings.activateOnCharging)
            Status("Activar por velocidad GPS", settings.activateOnGpsSpeed)
            Status("Modo nocturno automático (config)", settings.autoNightMode)
            Status("Es horario nocturno ahora (21:00–07:00)", NightModeController.isNightTime())
            Status("Aviso de exceso de velocidad", settings.speedLimitWarnEnabled)
            Status("No molestar estricto", settings.strictDoNotDisturb)
            Status("Ducking de audio", settings.audioDuckingEnabled)
        }
    }
}

@Composable
private fun Status(label: String, ok: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            if (ok) "OK" else "—",
            color = if (ok) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.titleLarge
        )
    }
}
