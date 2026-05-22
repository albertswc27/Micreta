package com.micreta.app.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.micreta.app.MicretaApp
import com.micreta.app.core.bluetooth.BluetoothDeviceInfo
import com.micreta.app.core.permissions.PermissionsManager
import com.micreta.app.domain.model.AppSettings
import com.micreta.app.ui.components.MicretaCard
import kotlinx.coroutines.launch

/**
 * Lets the user pick:
 *  - which paired device represents the car (triggers driving mode)
 *  - which paired device is the OBD2 ELM327 adapter
 *
 * We don't do BLE/classic discovery here — Android Settings does that better.
 * If a device isn't listed, the user pairs it from system Settings, then comes back.
 */
@Composable
fun BluetoothSetupScreen() {
    val app = MicretaApp.get()
    val context = LocalContext.current
    val settings by app.container.settingsRepository.settings.collectAsStateWithLifecycle(AppSettings())
    val scope = rememberCoroutineScope()
    var devices by remember { mutableStateOf<List<BluetoothDeviceInfo>>(emptyList()) }
    var permissionDenied by remember { mutableStateOf(false) }
    val bt = app.container.bluetoothScanner
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionDenied = !result.values.all { it }
        if (!permissionDenied) {
            devices = bt.pairedDevices()
        }
    }

    LaunchedEffect(Unit) {
        val missing = PermissionsManager.missing(context, PermissionsManager.optionalBluetooth())
        if (missing.isEmpty()) {
            devices = bt.pairedDevices()
        } else {
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
        Text("Bluetooth", style = MaterialTheme.typography.headlineMedium)

        when {
            permissionDenied -> MicretaCard(
                title = "Permiso Bluetooth",
                accent = MaterialTheme.colorScheme.error
            ) { Text("No puedo listar dispositivos emparejados sin permiso Bluetooth.") }

            !bt.isBluetoothSupported() ->
                MicretaCard(title = "No disponible") { Text("Este dispositivo no tiene Bluetooth.") }

            !bt.isBluetoothEnabled() -> MicretaCard(
                title = "Bluetooth apagado",
                accent = MaterialTheme.colorScheme.error
            ) { Text("Activa Bluetooth desde los ajustes del sistema y vuelve aquí.") }

            else -> {
                MicretaCard(title = "Mi coche (Micra K13)") {
                    Text(
                        settings.carBluetoothName?.let { "$it · ${settings.carBluetoothMac}" }
                            ?: "Sin seleccionar",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    DeviceList(devices) { dev ->
                        scope.launch { app.container.settingsRepository.setCarBluetooth(dev.address, dev.name) }
                    }
                }

                MicretaCard(title = "Adaptador OBD2") {
                    Text(
                        settings.obdBluetoothName?.let { "$it · ${settings.obdBluetoothMac}" }
                            ?: "Sin seleccionar",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    DeviceList(devices) { dev ->
                        scope.launch { app.container.settingsRepository.setObdBluetooth(dev.address, dev.name) }
                    }
                }

                MicretaCard(title = "Consejo") {
                    Text(
                        "Si no ves tu Micra o tu ELM327 aquí, empareja el dispositivo desde " +
                        "Ajustes → Bluetooth y vuelve a esta pantalla.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceList(
    devices: List<BluetoothDeviceInfo>,
    onPick: (BluetoothDeviceInfo) -> Unit
) {
    if (devices.isEmpty()) {
        Text("Sin dispositivos emparejados.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            devices.forEach { dev ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onPick(dev) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(dev.name ?: "Sin nombre", style = MaterialTheme.typography.titleLarge)
                        Text(dev.address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
