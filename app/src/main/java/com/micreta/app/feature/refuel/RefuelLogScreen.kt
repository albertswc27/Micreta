package com.micreta.app.feature.refuel

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.micreta.app.domain.model.RefuelEntry
import com.micreta.app.ui.components.MicretaCard
import com.micreta.app.ui.components.PrimaryActionButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun RefuelLogScreen() {
    val app = MicretaApp.get()
    val entries by app.container.refuelRepository.entries.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    var litres by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var km by remember { mutableStateOf("") }
    var station by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Repostajes", style = MaterialTheme.typography.headlineMedium)
        MicretaCard(title = "Apuntar repostaje") {
            OutlinedTextField(value = litres, onValueChange = { litres = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                label = { Text("Litros") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = cost, onValueChange = { cost = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                label = { Text("Coste total (€)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = km, onValueChange = { km = it.filter { c -> c.isDigit() } },
                label = { Text("Cuentakilómetros") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = station, onValueChange = { station = it },
                label = { Text("Gasolinera (opcional)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            PrimaryActionButton(
                label = "Guardar repostaje",
                icon = Icons.Filled.LocalGasStation,
                onClick = {
                    val l = litres.replace(',', '.').toDoubleOrNull() ?: return@PrimaryActionButton
                    val c = cost.replace(',', '.').toDoubleOrNull() ?: return@PrimaryActionButton
                    val k = km.toIntOrNull() ?: return@PrimaryActionButton
                    scope.launch {
                        app.container.refuelRepository.add(
                            RefuelEntry(
                                id = UUID.randomUUID().toString(),
                                timestampMs = System.currentTimeMillis(),
                                odometerKm = k,
                                litres = l,
                                totalCostEur = c,
                                stationName = station
                            )
                        )
                        app.container.maintenanceRepository.setOdometer(k)
                        litres = ""; cost = ""; km = ""; station = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            )
        }

        // Realised consumption between consecutive fills (litres / Δkm * 100)
        val pairs = entries.windowed(2, 1, partialWindows = false).map { (newer, older) -> RefuelStretch(newer, older) }
        val avg = pairs.map { it.l100 }.filter { it.isFinite() && it > 0 }.let { if (it.isEmpty()) 0.0 else it.average() }
        if (pairs.isNotEmpty()) {
            MicretaCard(title = "Consumo real") {
                Text("${"%.2f".format(avg)} L/100 km de media", style = MaterialTheme.typography.titleLarge)
                Text("Calculado entre repostajes consecutivos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        val df = SimpleDateFormat("d MMM, HH:mm", Locale("es", "ES"))
        entries.forEach { e ->
            MicretaCard(title = "${df.format(Date(e.timestampMs))} · ${e.stationName.ifBlank { "Sin nombre" }}") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${"%.1f".format(e.litres)} L · ${"%.2f".format(e.totalCostEur)} € · ${e.odometerKm} km",
                            style = MaterialTheme.typography.bodyLarge)
                        Text("${"%.3f".format(e.pricePerLitre)} €/L", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = {
                        scope.launch { app.container.refuelRepository.remove(e.id) }
                    }) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar") }
                }
            }
        }
    }
}

private data class RefuelStretch(val newer: RefuelEntry, val older: RefuelEntry) {
    val deltaKm = (newer.odometerKm - older.odometerKm).coerceAtLeast(1)
    val l100 = newer.litres * 100.0 / deltaKm
}
