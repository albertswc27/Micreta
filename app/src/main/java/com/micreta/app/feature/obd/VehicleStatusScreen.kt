package com.micreta.app.feature.obd

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.micreta.app.MicretaApp
import com.micreta.app.core.permissions.PermissionsManager
import com.micreta.app.data.obd.DtcDictionary
import com.micreta.app.domain.model.AppSettings
import com.micreta.app.domain.model.VehicleStatus
import com.micreta.app.ui.components.MicretaCard
import com.micreta.app.ui.components.PrimaryActionButton
import kotlinx.coroutines.launch

/**
 * Vehicle Status (E01 hardened).
 *
 * Visual contract:
 *  - A coloured banner at the top makes the data source obvious:
 *    blue "Mock / demo", green "OBD real conectado", grey "OBD desconectado".
 *  - Every metric shows units (rpm, km/h, ºC, %, V).
 *  - DTC codes are decorated with the human description from [DtcDictionary].
 *  - Three actions: lectura única (snapshot), monitorización continua, mock.
 *  - Continuous polling is controlled here; we do NOT auto-start it on
 *    entering the screen — that would defeat the on-demand requirement.
 */
@Composable
fun VehicleStatusScreen() {
    val app = MicretaApp.get()
    val context = LocalContext.current
    val status by app.container.obd.status.collectAsStateWithLifecycle()
    val connected by app.container.obd.isConnected.collectAsStateWithLifecycle()
    val continuous by app.container.obd.isContinuous.collectAsStateWithLifecycle()
    val settings by app.container.settingsRepository.settings.collectAsStateWithLifecycle(AppSettings())
    val scope = rememberCoroutineScope()
    var permissionDenied by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<ObdPermissionAction?>(null) }

    fun runSnapshot() {
        scope.launch {
            val mac = if (settings.demoMode) null else settings.obdBluetoothMac
            app.container.obd.snapshot(mac)
        }
    }

    fun runMonitoring() {
        if (settings.demoMode || settings.obdBluetoothMac.isNullOrBlank())
            app.container.obd.startMock()
        else
            app.container.obd.startContinuous(settings.obdBluetoothMac!!)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.all { it }
        permissionDenied = !granted
        if (granted) {
            when (pendingAction) {
                ObdPermissionAction.SNAPSHOT -> runSnapshot()
                ObdPermissionAction.MONITOR -> runMonitoring()
                null -> Unit
            }
        }
        pendingAction = null
    }

    fun runWithBluetoothPermission(action: ObdPermissionAction) {
        if (settings.demoMode) {
            when (action) {
                ObdPermissionAction.SNAPSHOT -> runSnapshot()
                ObdPermissionAction.MONITOR -> runMonitoring()
            }
            return
        }
        val missing = PermissionsManager.missing(context, PermissionsManager.optionalBluetooth())
        if (missing.isEmpty()) {
            when (action) {
                ObdPermissionAction.SNAPSHOT -> runSnapshot()
                ObdPermissionAction.MONITOR -> runMonitoring()
            }
        } else {
            pendingAction = action
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Estado del coche", style = MaterialTheme.typography.headlineMedium)

        SourceBanner(source = status.source, connected = connected, continuous = continuous, demoMode = settings.demoMode)
        if (permissionDenied) {
            MicretaCard(title = "Permiso Bluetooth", accent = MaterialTheme.colorScheme.error) {
                Text("No puedo conectar con el adaptador OBD sin permiso Bluetooth.")
            }
        }

        MicretaCard(title = "Motor") {
            Metric("Revoluciones", status.rpm?.let { "$it rpm" })
            Metric("Velocidad OBD", status.speedKmh?.let { "$it km/h" })
            Metric("Carga del motor", status.engineLoadPct?.let { "$it %" })
            Metric("Acelerador", status.throttlePct?.let { "$it %" })
        }

        MicretaCard(title = "Temperaturas") {
            Metric("Refrigerante", status.coolantTempC?.let { "$it ºC" },
                warn = status.coolantTempC?.let { it >= 100 } == true)
            Metric("Aire de admisión", status.intakeAirTempC?.let { "$it ºC" })
        }

        MicretaCard(title = "Combustible y batería") {
            Metric("Combustible", status.fuelLevelPct?.let { "$it %" },
                warn = status.fuelLevelPct?.let { it <= 15 } == true)
            Metric("Batería", status.batteryVoltage?.let { "%.2f V".format(it) },
                warn = status.batteryVoltage?.let { it <= 12.0 } == true)
            Metric("Autonomía estimada", status.estimatedRangeKm?.let { "~$it km" }, dim = true)
            Metric("Cuentakilómetros", status.odometerKm?.let { "$it km" })
        }

        MicretaCard(title = "Códigos de avería (DTC)") {
            if (status.dtcCodes.isEmpty()) {
                Text("Ninguno detectado.", style = MaterialTheme.typography.bodyLarge)
            } else {
                DtcDictionary.describeAll(status.dtcCodes).forEach { entry ->
                    val color = when (entry.severity) {
                        DtcDictionary.Severity.CRITICAL -> MaterialTheme.colorScheme.error
                        DtcDictionary.Severity.WARNING -> MaterialTheme.colorScheme.tertiary
                        DtcDictionary.Severity.INFO -> MaterialTheme.colorScheme.onSurface
                    }
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text("${entry.code}",
                            style = MaterialTheme.typography.titleLarge,
                            color = color)
                        Text(entry.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Actions
        if (continuous) {
            PrimaryActionButton(
                label = "Detener monitorización",
                icon = Icons.Filled.Stop,
                onClick = { app.container.obd.stop() },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                container = MaterialTheme.colorScheme.error,
                content = MaterialTheme.colorScheme.onError
            )
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryActionButton(
                    label = "Lectura única",
                    icon = Icons.Filled.Refresh,
                    onClick = { runWithBluetoothPermission(ObdPermissionAction.SNAPSHOT) },
                    modifier = Modifier.weight(1f).height(64.dp)
                )
                PrimaryActionButton(
                    label = "Monitorizar",
                    icon = Icons.Filled.PlayArrow,
                    onClick = { runWithBluetoothPermission(ObdPermissionAction.MONITOR) },
                    enabled = settings.demoMode || !settings.obdBluetoothMac.isNullOrBlank(),
                    modifier = Modifier.weight(1f).height(64.dp),
                    container = MaterialTheme.colorScheme.surfaceVariant,
                    content = MaterialTheme.colorScheme.onSurface
                )
            }
            if (!settings.demoMode && settings.obdBluetoothMac.isNullOrBlank()) {
                Text(
                    "No hay adaptador OBD configurado. Conecta uno desde Ajustes → Bluetooth, o activa el modo demo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private enum class ObdPermissionAction { SNAPSHOT, MONITOR }

@Composable
private fun SourceBanner(
    source: VehicleStatus.Source,
    connected: Boolean,
    continuous: Boolean,
    demoMode: Boolean
) {
    val (color, label) = when {
        continuous && source == VehicleStatus.Source.OBD_BLUETOOTH -> Color(0xFF1F3D2C) to "OBD real • monitorización continua"
        continuous && source == VehicleStatus.Source.MOCK -> Color(0xFF1F2E3D) to "Demo/mock • simulación en bucle"
        connected && source == VehicleStatus.Source.OBD_BLUETOOTH -> Color(0xFF1F3D2C) to "OBD real • última lectura"
        connected && source == VehicleStatus.Source.MOCK -> Color(0xFF1F2E3D) to "Demo/mock • última lectura"
        demoMode -> Color(0xFF1F2E3D) to "Demo activo • pulsa lectura para simular"
        else -> Color(0xFF2A2F39) to "OBD desconectado • pulsa lectura para conectar"
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .padding(12.dp)
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Metric(
    label: String,
    value: String?,
    warn: Boolean = false,
    dim: Boolean = false
) {
    val color = when {
        warn -> MaterialTheme.colorScheme.error
        dim -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value ?: "No disponible", style = MaterialTheme.typography.titleLarge, color = color)
    }
}
