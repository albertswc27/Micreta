package com.micreta.app

import android.content.Context
import com.micreta.app.core.activation.GpsSpeedActivationWatcher
import com.micreta.app.core.bluetooth.BluetoothScanner
import com.micreta.app.core.calendar.CalendarReader
import com.micreta.app.core.fuel.GasStationSearchService
import com.micreta.app.core.location.LocationService
import com.micreta.app.core.media.MediaControllerManager
import com.micreta.app.core.navigation.WazeNavigator
import com.micreta.app.core.safety.DoNotDisturbController
import com.micreta.app.core.safety.SosController
import com.micreta.app.core.safety.SpeedLimitWatcher
import com.micreta.app.core.sensors.MotionSensor
import com.micreta.app.core.traffic.SpeedLimitClient
import com.micreta.app.core.voice.TextToSpeechManager
import com.micreta.app.core.voice.VoiceRecognitionManager
import com.micreta.app.core.weather.WeatherClient
import com.micreta.app.data.obd.ObdRepository
import com.micreta.app.data.preferences.CustomCommandsRepository
import com.micreta.app.data.preferences.FavoritesRepository
import com.micreta.app.data.preferences.MaintenanceRepository
import com.micreta.app.data.preferences.ParkingMemoryRepository
import com.micreta.app.data.preferences.RefuelRepository
import com.micreta.app.data.preferences.SettingsRepository
import com.micreta.app.data.trip.TripRecorder
import com.micreta.app.data.trip.TripRepository
import com.micreta.app.domain.model.MicretaState
import com.micreta.app.domain.personality.MicretaPersonalityEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manual DI container. Holds singletons that should outlive any single
 * Activity / ViewModel: repositories, voice managers, the OBD layer, etc.
 *
 * Why no Hilt: the dependency graph here is shallow and we want fast builds
 * for the first APKs. If the project grows we can swap this object for Hilt
 * without touching call sites — they only see interfaces / concrete classes
 * they already construct themselves.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    // Persistence
    val settingsRepository by lazy { SettingsRepository(appContext) }
    val favoritesRepository by lazy { FavoritesRepository(appContext) }
    val tripRepository by lazy { TripRepository(appContext) }
    val parkingRepository by lazy { ParkingMemoryRepository(appContext) }
    val refuelRepository by lazy { RefuelRepository(appContext) }
    val maintenanceRepository by lazy { MaintenanceRepository(appContext) }
    val customCommandsRepository by lazy { CustomCommandsRepository(appContext) }

    // Voice + media
    val tts by lazy { TextToSpeechManager(appContext) }
    val voice by lazy { VoiceRecognitionManager(appContext) }
    val media by lazy { MediaControllerManager(appContext) }
    val waze by lazy { WazeNavigator(appContext) }

    // Sensors / location
    val locationService by lazy { LocationService(appContext) }
    val motionSensor by lazy { MotionSensor(appContext) }
    val speedLimitClient by lazy { SpeedLimitClient() }
    val speedLimitWatcher by lazy { SpeedLimitWatcher(locationService, speedLimitClient) }
    val gpsSpeedActivation by lazy { GpsSpeedActivationWatcher(locationService) }
    val weatherClient by lazy { WeatherClient() }
    val calendarReader by lazy { CalendarReader(appContext) }
    val gasStations by lazy { GasStationSearchService(appContext) }

    // Bluetooth / OBD
    val bluetoothScanner by lazy { BluetoothScanner(appContext) }
    val obd by lazy { ObdRepository(appContext) }

    // Safety
    val dnd by lazy { DoNotDisturbController(appContext) }
    val sos by lazy { SosController(appContext) }

    // Trip recorder uses location + motion + persistence.
    val tripRecorder by lazy {
        TripRecorder(
            locationService = locationService,
            motionSensor = motionSensor,
            tripRepo = tripRepository,
            parkingRepo = parkingRepository
        )
    }

    // Personality engine — held as singleton so its volatile profile field
    // can be updated whenever the user changes the preset.
    val personality by lazy { MicretaPersonalityEngine() }

    private val _micretaState = MutableStateFlow(MicretaState.SLEEPING)
    val micretaState: StateFlow<MicretaState> = _micretaState

    fun setState(state: MicretaState) { _micretaState.value = state }
}
