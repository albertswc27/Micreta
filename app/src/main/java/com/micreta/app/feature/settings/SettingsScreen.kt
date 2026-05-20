package com.micreta.app.feature.settings

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.micreta.app.MicretaApp
import com.micreta.app.core.permissions.PermissionsManager
import com.micreta.app.domain.model.AppSettings
import com.micreta.app.domain.model.FavoritePlace
import com.micreta.app.domain.model.PersonalityProfile
import com.micreta.app.ui.components.MicretaCard
import com.micreta.app.ui.components.PrimaryActionButton
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun SettingsScreen(
    onOpenBluetoothSetup: () -> Unit,
    onOpenCustomCommands: () -> Unit,
    onOpenSystemHealth: () -> Unit,
    onOpenDebug: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val app = MicretaApp.get()
    val context = LocalContext.current
    val settings by app.container.settingsRepository.settings.collectAsStateWithLifecycle(AppSettings())
    val favorites by app.container.favoritesRepository.favorites.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    var locationPermissionDenied by remember { mutableStateOf(false) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.all { it }
        locationPermissionDenied = !granted
        if (granted) {
            scope.launch { app.container.settingsRepository.setActivateOnGpsSpeed(true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineMedium)

        MicretaCard(title = "Identidad") {
            EditableField("Nombre del asistente", settings.assistantName) {
                scope.launch { app.container.settingsRepository.setAssistantName(it) }
            }
            EditableField("Nombre del coche", settings.carName) {
                scope.launch { app.container.settingsRepository.setCarName(it) }
            }
            PersonalityPicker(settings.personality) {
                scope.launch { app.container.settingsRepository.setPersonality(it) }
            }
        }

        MicretaCard(title = "Coche conectado") {
            Text(
                text = settings.carBluetoothName?.let { "$it (${settings.carBluetoothMac})" }
                    ?: "Sin dispositivo configurado.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(6.dp))
            ToggleRow("Activar al conectar Bluetooth del coche", settings.activateOnBluetooth) {
                scope.launch { app.container.settingsRepository.setActivateOnBluetooth(it) }
            }
            ToggleRow("Activar al conectar el cargador", settings.activateOnCharging) {
                scope.launch { app.container.settingsRepository.setActivateOnCharging(it) }
            }
            ToggleRow("Activar por velocidad GPS (>15 km/h)", settings.activateOnGpsSpeed) {
                if (!it) {
                    locationPermissionDenied = false
                    scope.launch { app.container.settingsRepository.setActivateOnGpsSpeed(false) }
                } else {
                    val missing = PermissionsManager.missing(context, PermissionsManager.optionalLocation())
                    if (missing.isEmpty()) {
                        scope.launch { app.container.settingsRepository.setActivateOnGpsSpeed(true) }
                    } else {
                        locationPermissionLauncher.launch(missing.toTypedArray())
                    }
                }
            }
            if (locationPermissionDenied) {
                Text(
                    "Sin ubicación, la activación por velocidad GPS queda desactivada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            ToggleRow("Modo demo (mock OBD2)", settings.demoMode) {
                scope.launch { app.container.settingsRepository.setDemoMode(it) }
            }
            Spacer(Modifier.height(8.dp))
            PrimaryActionButton(
                label = "Configurar Bluetooth",
                onClick = onOpenBluetoothSetup,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                container = MaterialTheme.colorScheme.surfaceVariant,
                content = MaterialTheme.colorScheme.onSurface
            )
        }

        MicretaCard(title = "Música y audio") {
            MusicAppSelector(settings.musicAppPackage) { pkg ->
                scope.launch { app.container.settingsRepository.setMusicAppPackage(pkg) }
            }
            ToggleRow("Bajar música cuando Micreta habla (ducking)", settings.audioDuckingEnabled) {
                scope.launch { app.container.settingsRepository.setAudioDuckingEnabled(it) }
            }
            ToggleRow("Reanudar música al entrar al coche", settings.resumeLastMediaOnDrive) {
                scope.launch { app.container.settingsRepository.setResumeLastMediaOnDrive(it) }
            }
        }

        MicretaCard(title = "Seguridad") {
            ToggleRow("Aviso de exceso de velocidad", settings.speedLimitWarnEnabled) {
                scope.launch { app.container.settingsRepository.setSpeedLimitWarnEnabled(it) }
            }
            ToggleRow("No molestar estricto durante conducción", settings.strictDoNotDisturb) {
                scope.launch { app.container.settingsRepository.setStrictDoNotDisturb(it) }
            }
            EditableField("Teléfono SOS (por defecto 112)", settings.sosPhoneNumber) {
                scope.launch { app.container.settingsRepository.setSosPhoneNumber(it) }
            }
        }

        MicretaCard(title = "ETA a contacto") {
            EditableField("Nombre", settings.etaContactName) { n ->
                scope.launch { app.container.settingsRepository.setEtaContact(n, settings.etaContactPhone) }
            }
            EditableField("Teléfono (opcional)", settings.etaContactPhone) { p ->
                scope.launch { app.container.settingsRepository.setEtaContact(settings.etaContactName, p) }
            }
            Text(
                "Si dejas el teléfono vacío, al pedir ETA Micreta abre el selector " +
                "de Android para compartir por WhatsApp, Telegram o SMS.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        MicretaCard(title = "Otros modos") {
            ToggleRow("Modo nocturno automático (21:00–07:00)", settings.autoNightMode) {
                scope.launch { app.container.settingsRepository.setAutoNightMode(it) }
            }
            ToggleRow("Registrar viajes (GPS + acelerómetro)", settings.tripsEnabled) {
                scope.launch { app.container.settingsRepository.setTripsEnabled(it) }
            }
            ToggleRow("Comandos personalizados", settings.customCommandsEnabled) {
                scope.launch { app.container.settingsRepository.setCustomCommandsEnabled(it) }
            }
        }

        MicretaCard(title = "Favoritos") {
            favorites.forEach { fav ->
                FavoriteRow(fav,
                    onSave = { updated -> scope.launch { app.container.favoritesRepository.upsert(updated) } },
                    onDelete = { scope.launch { app.container.favoritesRepository.remove(fav.id) } }
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        app.container.favoritesRepository.upsert(
                            FavoritePlace(
                                id = UUID.randomUUID().toString(),
                                name = "Nuevo favorito",
                                address = "",
                                voiceAliases = emptyList()
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Añadir favorito")
            }
        }

        // Navigation to secondary screens
        PrimaryActionButton("Comandos personalizados", onClick = onOpenCustomCommands,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            container = MaterialTheme.colorScheme.surfaceVariant, content = MaterialTheme.colorScheme.onSurface)
        PrimaryActionButton("Salud del sistema", onClick = onOpenSystemHealth,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            container = MaterialTheme.colorScheme.surfaceVariant, content = MaterialTheme.colorScheme.onSurface)
        PrimaryActionButton("Debug", onClick = onOpenDebug,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            container = MaterialTheme.colorScheme.surfaceVariant, content = MaterialTheme.colorScheme.onSurface)
        PrimaryActionButton("Acerca de Micreta", onClick = onOpenAbout,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            container = MaterialTheme.colorScheme.surfaceVariant, content = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun EditableField(label: String, value: String, onSave: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onSave(it)
        },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun PersonalityPicker(current: PersonalityProfile, onPick: (PersonalityProfile) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (current) {
        PersonalityProfile.FRIENDLY -> "Amigable"
        PersonalityProfile.FORMAL -> "Formal"
        PersonalityProfile.PLAYFUL -> "Gamberra"
        PersonalityProfile.ROBOTIC -> "Robótica"
    }
    Box {
        Button(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
        ) { Text("Personalidad: $label") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PersonalityProfile.values().forEach { p ->
                DropdownMenuItem(text = { Text(p.spanishLabel()) }, onClick = { onPick(p); expanded = false })
            }
        }
    }
}

private fun PersonalityProfile.spanishLabel(): String = when (this) {
    PersonalityProfile.FRIENDLY -> "Amigable"
    PersonalityProfile.FORMAL -> "Formal"
    PersonalityProfile.PLAYFUL -> "Gamberra"
    PersonalityProfile.ROBOTIC -> "Robótica"
}

@Composable
private fun FavoriteRow(
    fav: FavoritePlace,
    onSave: (FavoritePlace) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember(fav.id) { mutableStateOf(fav.name) }
    var address by remember(fav.id) { mutableStateOf(fav.address) }
    var aliases by remember(fav.id) { mutableStateOf(fav.voiceAliases.joinToString(", ")) }
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; onSave(fav.copy(name = it)) },
                label = { Text("Nombre") },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
            }
        }
        OutlinedTextField(
            value = address,
            onValueChange = { address = it; onSave(fav.copy(address = it)) },
            label = { Text("Dirección") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = aliases,
            onValueChange = {
                aliases = it
                onSave(fav.copy(voiceAliases = it.split(",").map { s -> s.trim() }.filter { s -> s.isNotEmpty() }))
            },
            label = { Text("Alias de voz (separados por coma)") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MusicAppSelector(current: String?, onPick: (String?) -> Unit) {
    val candidates = listOf(
        "Sistema (predeterminado)" to null,
        "Spotify" to "com.spotify.music",
        "YouTube Music" to "com.google.android.apps.youtube.music",
        "Apple Music" to "com.apple.android.music",
        "Amazon Music" to "com.amazon.mp3"
    )
    var expanded by remember { mutableStateOf(false) }
    val label = candidates.firstOrNull { it.second == current }?.first ?: "Personalizado: $current"
    Box {
        Button(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
        ) { Text(label) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            candidates.forEach { (n, pkg) ->
                DropdownMenuItem(text = { Text(n) }, onClick = {
                    onPick(pkg)
                    expanded = false
                })
            }
        }
    }
}
