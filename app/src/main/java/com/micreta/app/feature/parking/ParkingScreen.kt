package com.micreta.app.feature.parking

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.micreta.app.MicretaApp
import com.micreta.app.core.permissions.PermissionsManager
import com.micreta.app.core.share.ShareIntents
import com.micreta.app.domain.model.ParkingMemory
import com.micreta.app.ui.components.MicretaCard
import com.micreta.app.ui.components.PrimaryActionButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ParkingScreen() {
    val app = MicretaApp.get()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val parking by app.container.parkingRepository.parking.collectAsStateWithLifecycle(initialValue = null)
    var status by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    fun saveCurrentParking() {
        saving = true
        status = "Obteniendo tu ubicación…"
        scope.launch {
            val loc = app.container.locationService.awaitFix()
            if (loc == null) {
                status = "No he podido obtener tu ubicación. Comprueba el GPS y el permiso."
            } else {
                app.container.parkingRepository.save(
                    ParkingMemory(lat = loc.lat, lon = loc.lon, savedAtMs = System.currentTimeMillis())
                )
                status = "Aparcamiento guardado."
            }
            saving = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) saveCurrentParking()
        else status = "Necesito permiso de ubicación para guardar dónde aparcas."
    }

    fun onSaveClicked() {
        val missing = PermissionsManager.missing(ctx, PermissionsManager.optionalLocation())
        if (missing.isEmpty()) saveCurrentParking() else permissionLauncher.launch(missing.toTypedArray())
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Mi coche aparcado", style = MaterialTheme.typography.headlineMedium)

        PrimaryActionButton(
            label = if (saving) "Guardando…" else "Guardar dónde he aparcado",
            icon = Icons.Filled.LocationOn,
            onClick = { if (!saving) onSaveClicked() },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        )
        status?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        val p = parking
        if (p == null) {
            MicretaCard(title = "Sin ubicación guardada") {
                Text(
                    "Pulsa \"Guardar dónde he aparcado\" al salir del coche, o deja que " +
                    "Micreta lo guarde sola al desconectar el Bluetooth / salir del modo conducción.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            return@Column
        }
        val df = SimpleDateFormat("d MMM yyyy, HH:mm", Locale("es", "ES"))
        MicretaCard(title = "Guardado el ${df.format(Date(p.savedAtMs))}") {
            Text("Latitud: ${"%.5f".format(p.lat)}", style = MaterialTheme.typography.bodyLarge)
            Text("Longitud: ${"%.5f".format(p.lon)}", style = MaterialTheme.typography.bodyLarge)
            if (p.addressHint.isNotBlank())
                Text(p.addressHint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        PrimaryActionButton(
            label = "Abrir en mapa",
            icon = Icons.Filled.Map,
            onClick = { ShareIntents.openGeo(ctx, p.lat, p.lon, label = "Mi coche") },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        )
        PrimaryActionButton(
            label = "Compartir ubicación",
            icon = Icons.Filled.Share,
            onClick = {
                val link = "https://maps.google.com/?q=${p.lat},${p.lon}"
                ShareIntents.shareText(ctx, "He aparcado aquí: $link", subject = "Mi coche")
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            container = MaterialTheme.colorScheme.surfaceVariant,
            content = MaterialTheme.colorScheme.onSurface
        )
    }
}
