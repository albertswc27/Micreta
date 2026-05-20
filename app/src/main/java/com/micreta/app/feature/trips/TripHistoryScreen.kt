package com.micreta.app.feature.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.micreta.app.MicretaApp
import com.micreta.app.domain.model.TripSummary
import com.micreta.app.ui.components.MicretaCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TripHistoryScreen() {
    val app = MicretaApp.get()
    val trips by app.container.tripRepository.trips.collectAsStateWithLifecycle(initialValue = emptyList())

    // Single LazyColumn for the whole screen: avoids nesting a scrollable list
    // inside a Column (which crashed with an "infinity max height" measurement)
    // and makes the list scroll naturally.
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Historial de viajes", style = MaterialTheme.typography.headlineMedium)
        }
        if (trips.isEmpty()) {
            item {
                MicretaCard(title = "Sin viajes") {
                    Text(
                        "Aún no has cerrado ningún viaje. Activa el modo conducción " +
                        "y sal de él para que aparezca aquí.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            item {
                val totalKm = trips.sumOf { it.distanceKm }
                val avgEco = trips.sumOf { it.ecoScore.toLong() } / trips.size
                MicretaCard(title = "Resumen") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Stat("Viajes", "${trips.size}", Modifier.weight(1f))
                        Stat("Total km", "%.0f".format(totalKm), Modifier.weight(1f))
                        Stat("Eco medio", "$avgEco", Modifier.weight(1f))
                    }
                }
            }
            items(trips) { trip -> TripRow(trip) }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.displayLarge)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val dateFmt = SimpleDateFormat("EEE d MMM, HH:mm", Locale("es", "ES"))

@Composable
private fun TripRow(trip: TripSummary) {
    val ecoColor = when {
        trip.ecoScore >= 80 -> MaterialTheme.colorScheme.secondary
        trip.ecoScore >= 50 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    MicretaCard(title = dateFmt.format(Date(trip.startedAtMs)), accent = ecoColor) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("%.1f km · %d min · máx %d km/h".format(trip.distanceKm, trip.durationMin, trip.maxSpeedKmh),
                    style = MaterialTheme.typography.bodyLarge)
                val sub = buildList<String> {
                    if (trip.harshAccelerations > 0) add("${trip.harshAccelerations} acel")
                    if (trip.harshBrakings > 0) add("${trip.harshBrakings} fren")
                    if (trip.overSpeedEvents > 0) add("${trip.overSpeedEvents} excesos")
                    trip.estimatedConsumptionL100?.let { add("~${"%.1f".format(it)} L/100") }
                }
                if (sub.isNotEmpty()) Text(sub.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${trip.ecoScore}", style = MaterialTheme.typography.displayLarge, color = ecoColor)
        }
    }
}
