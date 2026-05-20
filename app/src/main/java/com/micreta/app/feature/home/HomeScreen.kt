package com.micreta.app.feature.home

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.micreta.app.MicretaApp
import com.micreta.app.domain.model.MicretaState
import com.micreta.app.service.MicretaForegroundService
import com.micreta.app.ui.components.MicretaAvatar
import com.micreta.app.ui.components.PrimaryActionButton

/**
 * Top-level companion view: avatar + state label + primary actions.
 *
 * Layout philosophy:
 *  - Driving-relevant actions are big, full-width and at the top
 *    (Activar Micreta, Preguntar destino).
 *  - Secondary entries (Trips, Maintenance, Parking, Refuel) live in a
 *    2-column grid below so they're discoverable without cluttering the
 *    in-car critical path.
 */
@Composable
fun HomeScreen(
    onStartVoice: () -> Unit,
    onOpenWaze: () -> Unit,
    onOpenVehicleStatus: () -> Unit,
    onOpenTrips: () -> Unit,
    onOpenMaintenance: () -> Unit,
    onOpenRefuel: () -> Unit,
    onOpenParking: () -> Unit
) {
    val app = MicretaApp.get()
    val state by app.container.micretaState.collectAsStateWithLifecycle()
    val running by MicretaForegroundService.isRunning.collectAsStateWithLifecycle()
    val settings by app.container.settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = com.micreta.app.domain.model.AppSettings()
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.height(240.dp), contentAlignment = Alignment.Center) {
            MicretaAvatar(state = state, size = 220.dp)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = settings.assistantName,
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = labelFor(state),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimaryActionButton(
                label = if (running) "Salir del modo conducción" else "Activar Micreta",
                icon = Icons.Filled.DirectionsCar,
                onClick = {
                    if (running) MicretaForegroundService.stop(app)
                    else MicretaForegroundService.start(app)
                },
                modifier = Modifier.fillMaxWidth()
            )
            PrimaryActionButton(
                label = "Preguntar destino",
                icon = Icons.Filled.Mic,
                onClick = onStartVoice,
                modifier = Modifier.fillMaxWidth(),
                container = MaterialTheme.colorScheme.secondary,
                content = MaterialTheme.colorScheme.onSecondary
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryActionButton(
                    label = "Waze",
                    icon = Icons.Filled.Map,
                    onClick = onOpenWaze,
                    modifier = Modifier.weight(1f),
                    container = MaterialTheme.colorScheme.surfaceVariant,
                    content = MaterialTheme.colorScheme.onSurface
                )
                PrimaryActionButton(
                    label = "Coche",
                    icon = Icons.Filled.Speed,
                    onClick = onOpenVehicleStatus,
                    modifier = Modifier.weight(1f),
                    container = MaterialTheme.colorScheme.surfaceVariant,
                    content = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryActionButton(
                    label = "Historial",
                    icon = Icons.Filled.Route,
                    onClick = onOpenTrips,
                    modifier = Modifier.weight(1f),
                    container = MaterialTheme.colorScheme.surfaceVariant,
                    content = MaterialTheme.colorScheme.onSurface
                )
                PrimaryActionButton(
                    label = "Aparcado",
                    icon = Icons.Filled.PinDrop,
                    onClick = onOpenParking,
                    modifier = Modifier.weight(1f),
                    container = MaterialTheme.colorScheme.surfaceVariant,
                    content = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryActionButton(
                    label = "Repostajes",
                    icon = Icons.Filled.LocalGasStation,
                    onClick = onOpenRefuel,
                    modifier = Modifier.weight(1f),
                    container = MaterialTheme.colorScheme.surfaceVariant,
                    content = MaterialTheme.colorScheme.onSurface
                )
                PrimaryActionButton(
                    label = "Mantenimiento",
                    icon = Icons.Filled.Build,
                    onClick = onOpenMaintenance,
                    modifier = Modifier.weight(1f),
                    container = MaterialTheme.colorScheme.surfaceVariant,
                    content = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun labelFor(state: MicretaState): String = when (state) {
    MicretaState.SLEEPING -> "Dormida"
    MicretaState.DETECTING -> "Detectando coche"
    MicretaState.CONNECTED -> "Coche conectado"
    MicretaState.LISTENING -> "Escuchando"
    MicretaState.THINKING -> "Pensando"
    MicretaState.NAVIGATING -> "Navegando"
    MicretaState.ALERT -> "Alerta"
    MicretaState.HAPPY -> "Contenta"
    MicretaState.NEUTRAL -> "Lista"
    MicretaState.ERROR -> "Error"
}
