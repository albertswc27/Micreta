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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
                onSave = { updated -> scope.launch { app.container.maintenanceRepository.upsert(updated) } },
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

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryActionButton(
                label = "Por km",
                icon = Icons.Filled.Add,
                onClick = {
                    scope.launch {
                        app.container.maintenanceRepository.upsert(
                            MaintenanceTask(
                                id = UUID.randomUUID().toString(),
                                title = "Nueva tarea",
                                kind = MaintenanceTask.Kind.OTHER,
                                intervalKm = 10_000,
                                intervalDays = null,
                                baseOdometerKm = odometer ?: 0
                            )
                        )
                    }
                },
                modifier = Modifier.weight(1f).height(56.dp),
                container = MaterialTheme.colorScheme.surfaceVariant,
                content = MaterialTheme.colorScheme.onSurface
            )
            PrimaryActionButton(
                label = "Por fecha",
                icon = Icons.Filled.Add,
                onClick = {
                    scope.launch {
                        app.container.maintenanceRepository.upsert(
                            MaintenanceTask(
                                id = UUID.randomUUID().toString(),
                                title = "Nuevo recordatorio",
                                kind = MaintenanceTask.Kind.OTHER,
                                intervalKm = null,
                                intervalDays = 365,
                                baseAtMs = System.currentTimeMillis()
                            )
                        )
                    }
                },
                modifier = Modifier.weight(1f).height(56.dp),
                container = MaterialTheme.colorScheme.surfaceVariant,
                content = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MaintenanceRow(
    task: MaintenanceTask,
    currentKm: Int?,
    onSave: (MaintenanceTask) -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit
) {
    val due = task.isDue(currentKm)
    var title by remember(task.id) { mutableStateOf(task.title) }
    var km by remember(task.id) { mutableStateOf(task.intervalKm?.toString() ?: "") }
    var days by remember(task.id) { mutableStateOf(task.intervalDays?.toString() ?: "") }

    MicretaCard(title = task.title, accent = if (due) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it; onSave(task.copy(title = it)) },
            label = { Text("Nombre (ITV, seguro, neumáticos…)") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = km,
                onValueChange = { v -> km = v.filter { it.isDigit() }; onSave(task.copy(intervalKm = km.toIntOrNull())) },
                label = { Text("Cada km") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = days,
                onValueChange = { v -> days = v.filter { it.isDigit() }; onSave(task.copy(intervalDays = days.toIntOrNull())) },
                label = { Text("Cada días") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        if (currentKm != null && task.intervalKm != null) {
            val nextAt = task.baseOdometerKm + task.intervalKm
            val remaining = nextAt - currentKm
            Text(
                if (remaining > 0) "Próximo a $nextAt km (faltan $remaining)" else "Vencido (a $nextAt km)",
                style = MaterialTheme.typography.bodyMedium,
                color = if (remaining > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
            )
        }
        task.intervalDays?.let { d ->
            val dueAt = task.baseAtMs + d * 86_400_000L
            val daysLeft = (dueAt - System.currentTimeMillis()) / 86_400_000L
            val df = SimpleDateFormat("d MMM yyyy", Locale("es", "ES"))
            Text(
                if (daysLeft >= 0) "Próxima: ${df.format(Date(dueAt))} · faltan $daysLeft días"
                else "Vencida el ${df.format(Date(dueAt))}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (daysLeft >= 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Activa", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Switch(checked = task.enabled, onCheckedChange = { onSave(task.copy(enabled = it)) })
            IconButton(onClick = onReset) { Icon(Icons.Filled.Check, contentDescription = "Hecho · reiniciar") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar") }
        }
    }
}
