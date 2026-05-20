package com.micreta.app.feature.voice

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.micreta.app.MicretaApp
import com.micreta.app.core.logging.EventLogger
import com.micreta.app.core.share.ShareIntents
import com.micreta.app.core.voice.CommandParser
import com.micreta.app.data.obd.DtcDictionary
import com.micreta.app.domain.model.CustomCommand
import com.micreta.app.domain.model.MicretaState
import com.micreta.app.domain.model.VoiceCommand
import com.micreta.app.domain.personality.MicretaPersonalityEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Drives the conversational loop:
 *  - micreta greets / asks
 *  - SpeechRecognizer listens
 *  - parsed command is executed (Waze, media, status, weather, calendar, SOS…)
 *  - micreta speaks the result, optionally re-prompts (multi-turn)
 *
 * The view subscribes to [uiState] and [transcript] to render live state.
 */
class VoiceCommandViewModel : ViewModel() {

    private val app = MicretaApp.get()
    private val container = app.container
    private val tts = container.tts
    private val voice = container.voice
    private val media = container.media
    private val waze = container.waze
    private val personality: MicretaPersonalityEngine = container.personality

    private val _uiState = MutableStateFlow<VoiceUiState>(VoiceUiState.Idle)
    val uiState: StateFlow<VoiceUiState> = _uiState

    val transcript: StateFlow<String> = voice.partial

    /** Multi-turn dialogue context. When the previous turn asked for a destination
     *  we resume here on the next user utterance. */
    private val _pending = MutableStateFlow<PendingTurn>(PendingTurn.None)
    val pending: StateFlow<PendingTurn> = _pending

    private val _permissionRequests = MutableSharedFlow<VoicePermissionRequest>(extraBufferCapacity = 1)
    val permissionRequests: SharedFlow<VoicePermissionRequest> = _permissionRequests
    private var commandWaitingForPermission: VoiceCommand? = null

    init {
        viewModelScope.launch {
            voice.results.collect { text -> onTranscript(text) }
        }
        viewModelScope.launch {
            voice.errors.collect {
                _uiState.value = VoiceUiState.Failed("No te he entendido.")
                tts.speak(personality.didNotUnderstand())
                container.setState(MicretaState.NEUTRAL)
            }
        }
    }

    /** Starts the listen-once flow (with the greeting question). */
    fun askWhereTo() {
        if (_uiState.value is VoiceUiState.Listening) return
        container.setState(MicretaState.THINKING)
        tts.speak(personality.contextualGreeting())
        _pending.value = PendingTurn.AwaitingDestination
        startListening()
    }

    /** Starts listening without speaking first — used by the in-driving voice button. */
    fun listenNow() {
        if (_uiState.value is VoiceUiState.Listening) return
        startListening()
    }

    fun cancel() {
        voice.stop()
        _uiState.value = VoiceUiState.Idle
        _pending.value = PendingTurn.None
        commandWaitingForPermission = null
        container.setState(MicretaState.NEUTRAL)
    }

    fun onPermissionResult(request: VoicePermissionRequest, granted: Boolean) {
        val command = commandWaitingForPermission
        commandWaitingForPermission = null
        if (!granted || command == null) {
            _uiState.value = VoiceUiState.Failed(
                when (request) {
                    VoicePermissionRequest.CALENDAR -> "Falta permiso de calendario."
                    VoicePermissionRequest.LOCATION -> "Falta permiso de ubicación."
                }
            )
            return
        }
        viewModelScope.launch { execute(command) }
    }

    private fun startListening() {
        _uiState.value = VoiceUiState.Listening
        container.setState(MicretaState.LISTENING)
        voice.start()
    }

    private fun onTranscript(text: String) {
        EventLogger.info(TAG, "Heard: \"$text\"")
        container.setState(MicretaState.THINKING)
        viewModelScope.launch {
            // Multi-turn shortcut: if we asked for a destination just before,
            // try to parse a bare destination first.
            if (_pending.value == PendingTurn.AwaitingDestination) {
                val dest = CommandParser.parseDestinationOnly(text)
                _pending.value = PendingTurn.None
                if (!dest.isNullOrBlank()) {
                    val cmd = VoiceCommand.NavigateTo(dest, text)
                    _uiState.value = VoiceUiState.Heard(text, cmd)
                    execute(cmd)
                    return@launch
                }
            }
            val custom = container.customCommandsRepository.commands.first()
            val command = CommandParser.parse(text, custom)
            _uiState.value = VoiceUiState.Heard(text, command)
            execute(command)
        }
    }

    private suspend fun execute(command: VoiceCommand) {
        when (command) {
            is VoiceCommand.NavigateTo -> navigateToDestination(command.destination)
            is VoiceCommand.NavigateHome -> navigateToFavoriteAlias("casa")
            is VoiceCommand.NavigateLastFuel -> navigateToFavoriteAlias("gasolinera")
            is VoiceCommand.NavigateLastParking -> navigateToParking()
            is VoiceCommand.NavigateInverse -> {
                val parking = container.parkingRepository.parking.first()
                if (parking != null) {
                    ShareIntents.openGeo(app, parking.lat, parking.lon, label = "Coche")
                    tts.speak("Abro el mapa con la última ubicación de salida.")
                    container.setState(MicretaState.NAVIGATING)
                    _uiState.value = VoiceUiState.Done("Ruta inversa al coche.")
                } else {
                    tts.speak("No tengo guardada una ubicación de salida.")
                    _uiState.value = VoiceUiState.Failed("Sin punto de origen.")
                }
            }
            is VoiceCommand.EtaToContact -> sendEta(command.destination)
            is VoiceCommand.PlayMusic -> {
                val pkg = container.settingsRepository.settings.first().musicAppPackage
                media.launchMusicApp(pkg); media.play()
                tts.speak(personality.musicCommandAck(MicretaPersonalityEngine.MusicAction.PLAY))
                container.setState(MicretaState.HAPPY)
                _uiState.value = VoiceUiState.Done("Música.")
            }
            is VoiceCommand.PauseMusic -> {
                media.pause(); tts.speak(personality.musicCommandAck(MicretaPersonalityEngine.MusicAction.PAUSE))
                container.setState(MicretaState.NEUTRAL)
                _uiState.value = VoiceUiState.Done("Pausada.")
            }
            is VoiceCommand.ResumeMusic -> {
                media.play(); tts.speak(personality.musicCommandAck(MicretaPersonalityEngine.MusicAction.RESUME))
                _uiState.value = VoiceUiState.Done("Sigo.")
            }
            is VoiceCommand.NextTrack -> {
                media.next(); tts.speak(personality.musicCommandAck(MicretaPersonalityEngine.MusicAction.NEXT))
                _uiState.value = VoiceUiState.Done("Siguiente.")
            }
            is VoiceCommand.PreviousTrack -> {
                media.previous(); tts.speak(personality.musicCommandAck(MicretaPersonalityEngine.MusicAction.PREV))
                _uiState.value = VoiceUiState.Done("Anterior.")
            }
            is VoiceCommand.VolumeUp -> {
                media.volumeUp(); tts.speak(personality.musicCommandAck(MicretaPersonalityEngine.MusicAction.VOL_UP))
                _uiState.value = VoiceUiState.Done("Subo.")
            }
            is VoiceCommand.VolumeDown -> {
                media.volumeDown(); tts.speak(personality.musicCommandAck(MicretaPersonalityEngine.MusicAction.VOL_DOWN))
                _uiState.value = VoiceUiState.Done("Bajo.")
            }
            is VoiceCommand.OpenPlaylist -> openPlaylist(command.name)
            is VoiceCommand.VehicleStatusQuery -> readVehicleStatus()
            is VoiceCommand.TripReportRequest -> speakLastTripSummary()
            is VoiceCommand.StartObdMonitoring -> {
                val s = container.settingsRepository.settings.first()
                val mac = s.obdBluetoothMac
                if (s.demoMode || mac.isNullOrBlank()) container.obd.startMock()
                else container.obd.startContinuous(mac)
                tts.speak("Monitorizo el coche.")
                _uiState.value = VoiceUiState.Done("OBD continuo activo.")
            }
            is VoiceCommand.StopObdMonitoring -> {
                container.obd.stop()
                tts.speak("Dejo de monitorizar.")
                _uiState.value = VoiceUiState.Done("OBD detenido.")
            }
            is VoiceCommand.WeatherQuery -> {
                if (!container.locationService.hasPermission()) {
                    requestPermission(VoicePermissionRequest.LOCATION, command)
                    return
                }
                container.locationService.startUpdates()
                val loc = container.locationService.lastKnown()
                if (loc == null) {
                    tts.speak("No tengo tu ubicación todavía.")
                    _uiState.value = VoiceUiState.Failed("Sin GPS.")
                    return
                }
                val w = container.weatherClient.fetch(loc)
                if (w != null) {
                    personality.lastWeather = w
                    tts.speak("Ahora mismo ${w.spoken}.")
                    _uiState.value = VoiceUiState.Done("${w.temperatureC.toInt()} ºC")
                } else {
                    tts.speak("No he podido consultar el tiempo.")
                    _uiState.value = VoiceUiState.Failed("Sin red.")
                }
            }
            is VoiceCommand.CalendarQuery -> {
                if (!container.calendarReader.hasPermission()) {
                    requestPermission(VoicePermissionRequest.CALENDAR, command)
                    return
                }
                val events = container.calendarReader.upcomingEvents()
                if (events.isEmpty()) {
                    tts.speak("No tienes nada pendiente en las próximas horas.")
                } else {
                    val list = events.take(3).joinToString(", ") { e ->
                        val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(e.startMs))
                        "$time ${e.title}"
                    }
                    tts.speak("Tienes: $list.")
                }
                _uiState.value = VoiceUiState.Done("Resumen agenda.")
            }
            is VoiceCommand.AddRefuel -> {
                tts.speak("Abro el registro de repostaje.")
                _uiState.value = VoiceUiState.Done("Repostaje abierto.")
                // The screen owner observes UiState.OpenScreen — we use Done
                // with an explicit signal flag elsewhere if needed.
            }
            is VoiceCommand.SosCall -> {
                val phone = container.settingsRepository.settings.first().sosPhoneNumber
                container.sos.arm(phone)
                tts.speak(personality.sosCountdown(com.micreta.app.core.safety.SosController.DEFAULT_COUNTDOWN))
                container.setState(MicretaState.ALERT)
                _uiState.value = VoiceUiState.Done("SOS armado.")
            }
            is VoiceCommand.CancelSos -> {
                container.sos.cancel()
                tts.speak(personality.sosCancelled())
                container.setState(MicretaState.NEUTRAL)
                _uiState.value = VoiceUiState.Done("Cancelado.")
            }
            is VoiceCommand.StopDriving -> {
                tts.speak(personality.stopping())
                com.micreta.app.service.MicretaForegroundService.stop(app)
                _uiState.value = VoiceUiState.Done("Hasta luego.")
            }
            is VoiceCommand.Affirmative -> {
                tts.speak("De acuerdo.")
                _uiState.value = VoiceUiState.Done("Sí")
            }
            is VoiceCommand.Negative -> {
                tts.speak("Vale.")
                _uiState.value = VoiceUiState.Done("No")
            }
            is VoiceCommand.CustomMatch -> executeCustom(command.customId)
            is VoiceCommand.Unknown -> {
                tts.speak(personality.didNotUnderstand())
                container.setState(MicretaState.NEUTRAL)
                _uiState.value = VoiceUiState.Failed("No te he pillado.")
            }
        }
    }

    private suspend fun navigateToDestination(destination: String) {
        val favs = container.favoritesRepository.favorites.first()
        tts.speak(personality.confirmDestination(destination))
        val ok = waze.navigate(destination, favs)
        if (ok) {
            container.setState(MicretaState.NAVIGATING)
            _uiState.value = VoiceUiState.Done("Abriendo Waze hacia $destination.")
        } else {
            tts.speak("No he podido abrir Waze.")
            container.setState(MicretaState.ERROR)
            _uiState.value = VoiceUiState.Failed("Waze no disponible.")
        }
    }

    private suspend fun navigateToFavoriteAlias(alias: String) {
        val favs = container.favoritesRepository.favorites.first()
        val match = favs.firstOrNull { it.matchTokens.any { tok -> tok.contains(alias) } }
        if (match != null) {
            navigateToDestination(match.name)
        } else {
            tts.speak("No tengo un favorito con \"$alias\".")
            _uiState.value = VoiceUiState.Failed("Sin favorito.")
        }
    }

    private suspend fun navigateToParking() {
        val parking = container.parkingRepository.parking.first()
        if (parking == null) {
            tts.speak("No he guardado la última ubicación del coche.")
            _uiState.value = VoiceUiState.Failed("Sin parking memorizado.")
            return
        }
        ShareIntents.openGeo(app, parking.lat, parking.lon, label = "Coche aparcado")
        tts.speak("Te llevo al coche.")
        container.setState(MicretaState.NAVIGATING)
        _uiState.value = VoiceUiState.Done("Abriendo mapa al coche.")
    }

    private suspend fun sendEta(destination: String) {
        val s = container.settingsRepository.settings.first()
        val message = "Voy${if (destination.isNotBlank()) " a $destination" else ""}. — Micreta"
        if (s.etaContactPhone.isNotBlank()) {
            ShareIntents.smsTo(app, s.etaContactPhone, message)
        } else {
            ShareIntents.shareText(app, message, subject = "ETA")
        }
        tts.speak("ETA enviado.")
        _uiState.value = VoiceUiState.Done("ETA: $destination")
    }

    private suspend fun openPlaylist(name: String) {
        val s = container.settingsRepository.settings.first()
        val pkg = s.musicAppPackage
        media.launchMusicApp(pkg)
        tts.speak("Pongo la lista $name.")
        _uiState.value = VoiceUiState.Done("Playlist: $name")
    }

    private suspend fun readVehicleStatus() {
        val s = container.settingsRepository.settings.first()
        val mac = if (s.demoMode) null else s.obdBluetoothMac
        tts.speak("Voy a leer el coche.")
        val v = container.obd.snapshot(mac)
        val parts = buildList<String> {
            if (v.rpm != null) add("${v.rpm} revoluciones")
            if (v.coolantTempC != null) add("refrigerante a ${v.coolantTempC} grados")
            if (v.batteryVoltage != null) add("batería ${"%.1f".format(v.batteryVoltage)} voltios")
            if (v.fuelLevelPct != null) add("combustible al ${v.fuelLevelPct} por ciento")
        }
        if (v.dtcCodes.isEmpty() && parts.isEmpty()) {
            tts.speak("Aún no tengo datos del coche.")
        } else {
            val main = if (parts.isEmpty()) "" else parts.joinToString(", ") + ". "
            val dtc = if (v.dtcCodes.isNotEmpty()) {
                val first = DtcDictionary.lookup(v.dtcCodes.first()).description
                "${v.dtcCodes.size} ${if (v.dtcCodes.size == 1) "código" else "códigos"} detectado${if (v.dtcCodes.size == 1) "" else "s"}: el más relevante, $first."
            } else personality.vehicleHealthy()
            tts.speak(main + dtc)
        }
        container.setState(MicretaState.NEUTRAL)
        _uiState.value = VoiceUiState.Done("Diagnóstico realizado.")
    }

    private suspend fun speakLastTripSummary() {
        val list = container.tripRepository.trips.first()
        val last = list.firstOrNull()
        if (last == null) {
            tts.speak("Aún no tengo viajes guardados.")
            _uiState.value = VoiceUiState.Failed("Historial vacío.")
            return
        }
        tts.speak(personality.tripSummary(last))
        _uiState.value = VoiceUiState.Done("Último viaje narrado.")
    }

    private suspend fun executeCustom(id: String) {
        val cmd = container.customCommandsRepository.commands.first().firstOrNull { it.id == id }
        if (cmd == null) {
            tts.speak("Ese comando ya no existe.")
            return
        }
        when (cmd.action) {
            CustomCommand.Action.NAVIGATE_TO -> navigateToDestination(cmd.payload)
            CustomCommand.Action.OPEN_APP -> {
                val pm = app.packageManager
                val intent = pm.getLaunchIntentForPackage(cmd.payload)
                if (intent != null) {
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    app.startActivity(intent)
                    tts.speak("Abro la app.")
                } else tts.speak("Esa app no está instalada.")
            }
            CustomCommand.Action.OPEN_URL -> {
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(cmd.payload)
                ).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                runCatching { app.startActivity(intent) }
                    .onFailure { tts.speak("No he podido abrir el enlace.") }
            }
            CustomCommand.Action.SPEAK -> tts.speak(cmd.payload)
            CustomCommand.Action.DIAGNOSTIC -> readVehicleStatus()
            CustomCommand.Action.STOP_DRIVING -> {
                tts.speak(personality.stopping())
                com.micreta.app.service.MicretaForegroundService.stop(app)
            }
        }
        _uiState.value = VoiceUiState.Done("Comando ejecutado.")
    }

    private fun requestPermission(request: VoicePermissionRequest, command: VoiceCommand) {
        commandWaitingForPermission = command
        _permissionRequests.tryEmit(request)
        _uiState.value = VoiceUiState.Failed(
            when (request) {
                VoicePermissionRequest.CALENDAR -> "Falta permiso de calendario."
                VoicePermissionRequest.LOCATION -> "Falta permiso de ubicación."
            }
        )
    }

    companion object {
        private const val TAG = "VoiceVM"

        fun factory(@Suppress("UNUSED_PARAMETER") appContext: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    VoiceCommandViewModel() as T
            }
    }
}

enum class PendingTurn { None, AwaitingDestination }

enum class VoicePermissionRequest { CALENDAR, LOCATION }

sealed class VoiceUiState {
    data object Idle : VoiceUiState()
    data object Listening : VoiceUiState()
    data class Heard(val text: String, val command: VoiceCommand) : VoiceUiState()
    data class Done(val summary: String) : VoiceUiState()
    data class Failed(val reason: String) : VoiceUiState()
}
