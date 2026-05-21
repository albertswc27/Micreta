package com.micreta.app.feature.voice

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.micreta.app.MicretaApp
import com.micreta.app.core.permissions.PermissionsManager
import com.micreta.app.domain.model.GasStationOption
import com.micreta.app.domain.model.VoiceCommand
import com.micreta.app.ui.components.MicretaAvatar
import com.micreta.app.ui.components.MicretaCard
import com.micreta.app.ui.components.PrimaryActionButton
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun VoiceCommandScreen(
    autoStart: Boolean = false
) {
    val ctx = LocalContext.current
    val vm: VoiceCommandViewModel = viewModel(factory = VoiceCommandViewModel.factory(ctx.applicationContext))
    val state by vm.uiState.collectAsStateWithLifecycle()
    val transcript by vm.transcript.collectAsStateWithLifecycle()
    val micretaState by MicretaApp.get().container.micretaState.collectAsStateWithLifecycle()
    var permissionError by remember { mutableStateOf<String?>(null) }
    var pendingVoicePermission by remember { mutableStateOf<VoicePermissionRequest?>(null) }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            permissionError = null
            vm.askWhereTo()
        } else {
            permissionError = "Necesito permiso de micrófono para escucharte."
        }
    }

    val featurePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val request = pendingVoicePermission ?: return@rememberLauncherForActivityResult
        pendingVoicePermission = null
        val granted = result.values.all { it }
        vm.onPermissionResult(request, granted)
        if (!granted) {
            permissionError = when (request) {
                VoicePermissionRequest.CALENDAR -> "Necesito permiso de calendario para leer tu agenda."
                VoicePermissionRequest.LOCATION -> "Necesito permiso de ubicación para consultar contexto local."
            }
        } else {
            permissionError = null
        }
    }

    fun startVoiceWithPermission() {
        val missing = PermissionsManager.missing(ctx, PermissionsManager.optionalMicrophone())
        if (missing.isEmpty()) {
            permissionError = null
            vm.askWhereTo()
        } else {
            microphonePermissionLauncher.launch(missing.toTypedArray())
        }
    }

    LaunchedEffect(vm) {
        vm.permissionRequests.collect { request ->
            val perms = when (request) {
                VoicePermissionRequest.CALENDAR -> PermissionsManager.optionalCalendar()
                VoicePermissionRequest.LOCATION -> PermissionsManager.optionalLocation()
            }
            val missing = PermissionsManager.missing(ctx, perms)
            if (missing.isEmpty()) {
                vm.onPermissionResult(request, granted = true)
            } else {
                pendingVoicePermission = request
                featurePermissionLauncher.launch(missing.toTypedArray())
            }
        }
    }

    LaunchedEffect(autoStart) {
        if (autoStart) startVoiceWithPermission()
    }

    // Stop the recognizer when leaving the Voz screen so it never keeps
    // listening in the background or carries state into the next screen.
    DisposableEffect(Unit) {
        onDispose { vm.cancel() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Box(modifier = Modifier.height(220.dp), contentAlignment = Alignment.Center) {
            MicretaAvatar(state = micretaState, size = 200.dp)
        }
        Text(
            text = when (state) {
                VoiceUiState.Idle -> "Toca para hablar"
                VoiceUiState.Listening -> "Te escucho…"
                is VoiceUiState.Heard -> "Te he oído"
                is VoiceUiState.Done -> "Hecho"
                is VoiceUiState.Failed -> "Repíteme"
                is VoiceUiState.GasStations -> "Gasolineras"
            },
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(20.dp))

        if (transcript.isNotBlank() && state == VoiceUiState.Listening) {
            MicretaCard(title = "Detectando") {
                Text(transcript, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(12.dp))
        }

        when (val s = state) {
            is VoiceUiState.Heard -> MicretaCard(title = "Lo último que dijiste") {
                Text("\"${s.text}\"", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(6.dp))
                Text("→ ${describe(s.command)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            is VoiceUiState.Done -> MicretaCard(title = "Resultado") {
                Text(s.summary, style = MaterialTheme.typography.bodyLarge)
            }
            is VoiceUiState.Failed -> MicretaCard(title = "Error", accent = MaterialTheme.colorScheme.error) {
                Text(s.reason, style = MaterialTheme.typography.bodyLarge)
            }
            is VoiceUiState.GasStations -> GasStationResults(
                options = s.options,
                onNavigate = { vm.navigateToGasOption(it) },
                onOpenWaze = { vm.openWazeGasSearch(s.fallbackQuery ?: "gasolinera") }
            )
            else -> Unit
        }
        permissionError?.let { message ->
            Spacer(Modifier.height(12.dp))
            MicretaCard(title = "Permiso", accent = MaterialTheme.colorScheme.error) {
                Text(message, style = MaterialTheme.typography.bodyLarge)
            }
        }

        // "Qué puedes decir" — tappable suggestions when idle or after an error.
        if (state == VoiceUiState.Idle || state is VoiceUiState.Failed) {
            Spacer(Modifier.height(12.dp))
            MicretaCard(title = "Prueba a decir") {
                SuggestionChips(onPick = { vm.submitText(it) })
            }
        }

        Spacer(Modifier.weight(1f))

        if (state == VoiceUiState.Listening) {
            PrimaryActionButton(
                label = "Cancelar",
                icon = Icons.Filled.Stop,
                onClick = { vm.cancel() },
                modifier = Modifier.fillMaxWidth().height(72.dp),
                container = MaterialTheme.colorScheme.error,
                content = MaterialTheme.colorScheme.onError
            )
        } else {
            PrimaryActionButton(
                label = "Preguntar destino",
                icon = Icons.Filled.Mic,
                onClick = { startVoiceWithPermission() },
                modifier = Modifier.fillMaxWidth().height(72.dp)
            )
        }
    }
}

@Composable
private fun SuggestionChips(onPick: (String) -> Unit) {
    val suggestions = listOf("Pon música", "Llévame a casa", "Gasolinera cercana", "Diagnóstico")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        suggestions.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { phrase ->
                    AssistChip(
                        onClick = { onPick(phrase) },
                        label = { Text(phrase) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun GasStationResults(
    options: List<GasStationOption>,
    onNavigate: (GasStationOption) -> Unit,
    onOpenWaze: () -> Unit
) {
    if (options.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            options.forEach { opt ->
                MicretaCard(title = opt.name) {
                    Text(
                        text = "%.1f km".format(opt.distanceKm) + (opt.brand?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = opt.priceLabel ?: "Precio no disponible",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { onNavigate(opt) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Navigation, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Ir")
                    }
                }
            }
        }
    } else {
        MicretaCard(title = "Gasolineras") {
            Text(
                "No tengo precios todavía. Puedo abrir Waze buscando gasolineras cercanas.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(10.dp))
            Button(onClick = onOpenWaze, modifier = Modifier.fillMaxWidth()) {
                Text("Abrir Waze")
            }
        }
    }
}

private fun describe(command: VoiceCommand): String = when (command) {
    is VoiceCommand.NavigateTo -> "Navegar a \"${command.destination}\""
    is VoiceCommand.NavigateHome -> "Navegar a casa"
    is VoiceCommand.NavigateLastFuel -> "Navegar a la gasolinera"
    is VoiceCommand.NavigateLastParking -> "Llevarte al coche aparcado"
    is VoiceCommand.NavigateInverse -> "Ruta inversa de vuelta"
    is VoiceCommand.EtaToContact -> "Enviar ETA hacia \"${command.destination}\""
    is VoiceCommand.FindCheapGasStation -> "Buscar gasolinera barata"
    is VoiceCommand.PlayMusic -> "Reproducir música"
    is VoiceCommand.PauseMusic -> "Pausar música"
    is VoiceCommand.ResumeMusic -> "Reanudar música"
    is VoiceCommand.NextTrack -> "Siguiente canción"
    is VoiceCommand.PreviousTrack -> "Canción anterior"
    is VoiceCommand.VolumeUp -> "Subir volumen"
    is VoiceCommand.VolumeDown -> "Bajar volumen"
    is VoiceCommand.OpenPlaylist -> "Abrir playlist \"${command.name}\""
    is VoiceCommand.VehicleStatusQuery -> "Diagnóstico del coche"
    is VoiceCommand.TripReportRequest -> "Resumen del último viaje"
    is VoiceCommand.StartObdMonitoring -> "Iniciar monitorización OBD"
    is VoiceCommand.StopObdMonitoring -> "Parar monitorización OBD"
    is VoiceCommand.WeatherQuery -> "Consultar el tiempo"
    is VoiceCommand.CalendarQuery -> "Consultar la agenda"
    is VoiceCommand.AddRefuel -> "Apuntar un repostaje"
    is VoiceCommand.SosCall -> "Llamar a emergencias (countdown)"
    is VoiceCommand.CancelSos -> "Cancelar SOS"
    is VoiceCommand.StopDriving -> "Salir del modo conducción"
    is VoiceCommand.Affirmative -> "Sí"
    is VoiceCommand.Negative -> "No"
    is VoiceCommand.CustomMatch -> "Comando personalizado"
    is VoiceCommand.Unknown -> "No reconocido"
}
