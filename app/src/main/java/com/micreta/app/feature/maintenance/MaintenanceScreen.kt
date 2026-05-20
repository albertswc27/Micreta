package com.micreta.app.feature.maintenance

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.micreta.app.MicretaApp
import com.micreta.app.domain.model.MaintenanceTask
import com.micreta.app.ui.components.MicretaCard
import com.micreta.app.ui.components.PrimaryActionButton
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun MaintenanceScreen() {
    val app = MicretaApp.get()
    val tasks by app.container.maintenanceRepository.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val odometer by app.container.maintenanceRepository.odometerKm.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()

    var odoInput by remember(odometer) { mutableStateOf(odometer?.toString() ?: "") }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Mantenimiento", style = MaterialTheme.typography.headlineMedium)

        MicretaCard(title = "Cuentakilómetros manual") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = odoInput,
                    onValueChange = { v -> odoInput = v.filter { it.isDigit() } },
                    label = { Text("Km actuales") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.height(8.dp))
                IconButton(onClick = {
                    odoInput.toIntOrNull()?.let { km ->
                        scope.launch { app.container.maintenanceRepository.setOdometer(km) }
                    }
                }) { Icon(Icons.Filled.Check, contentDescription = "Guardar km") }
            }
            Text(
                "El Micra K13 rara vez expone el odómetro por OBD-II. Apúntalo " +
                "tú (cuando repostas, por ejemplo) y Micreta usará este valor.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        tasks.forEach { task ->
            MaintenanceRow(task, currentKm = odometer,
                onToggle = { enabled ->
                    scope.launch { app.container.maintenanceRepository.upsert(task.copy(enabled = enabled)) }
                },
                onReset = {
                    scope.launch {
                        app.container.maintenanceRepository.upsert(
                            task.copy(
                                baseOdometerKm = odometer ?: task.baseOdometerKm,
                                baseAtMs = System.currentTimeMillis()
                            )
                        )
                    }
                },
                onDelete = { scope.launch { app.container.maintenanceRepository.remove(task.id) } }
            )
        }

        PrimaryActionButton(
            label = "Añadir tarea",
            icon = Icons.Filled.Add,
            onClick = {
                scope.launch {
                    app.container.maintenanceRepository.upsert(
                        MaintenanceTask(
                            id = UUID.randomUUID().toString(),
                            title = "Nueva tarea",
                            kind = MaintenanceTask.Kind.OTHER,
                            intervalKm = 10_000,
                            intervalDays = null
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            container = MaterialTheme.colorScheme.surfaceVariant,
            content = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MaintenanceRow(
    task: MaintenanceTask,
    currentKm: Int?,
    onToggle: (Boolean) -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit
) {
    val due = task.isDue(currentKm)
    val color = if (due) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    MicretaCard(title = task.title, accent = if (due) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                val byKm = task.intervalKm?.let { "cada $it km" }
                val byDays = task.intervalDays?.let { "cada $it días" }
                val interval = listOfNotNull(byKm, byDays).joinToString(" y ")
                Text(interval, style = MaterialTheme.typography.bodyLarge, color = color)
                if (currentKm != null && task.intervalKm != null) {
                    val nextAt = task.baseOdometerKm + task.intervalKm
                    val remaining = nextAt - currentKm
                    Text(
                        if (remaining > 0) "Próximo a $nextAt km (faltan $remaining)" else "Vencido (a $nextAt km)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(checked = task.enabled, onCheckedChange = onToggle)
            IconButton(onClick = onReset) { Icon(Icons.Filled.Check, contentDescription = "Hecho · reset") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar") }
        }
    }
}
