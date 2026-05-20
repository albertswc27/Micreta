package com.micreta.app.feature.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.micreta.app.MicretaApp
import com.micreta.app.domain.model.VoiceCommand
import com.micreta.app.ui.components.MicretaAvatar
import com.micreta.app.ui.components.MicretaCard
import com.micreta.app.ui.components.PrimaryActionButton

@Composable
fun VoiceCommandScreen(
    autoStart: Boolean = false
) {
    val ctx = LocalContext.current
    val vm: VoiceCommandViewModel = viewModel(factory = VoiceCommandViewModel.factory(ctx.applicationContext))
    val state by vm.uiState.collectAsStateWithLifecycle()
    val transcript by vm.transcript.collectAsStateWithLifecycle()
    val micretaState by MicretaApp.get().container.micretaState.collectAsStateWithLifecycle()

    LaunchedEffect(autoStart) {
        if (autoStart) vm.askWhereTo()
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
            else -> Unit
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
                onClick = { vm.askWhereTo() },
                modifier = Modifier.fillMaxWidth().height(72.dp)
            )
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
