package com.micreta.app.domain.model

/**
 * Result of parsing the user's spoken phrase.
 *
 * The raw transcript is always kept so the Debug screen can display what
 * Android actually heard, even when we fail to classify it.
 */
sealed class VoiceCommand {
    abstract val raw: String

    // Navigation
    data class NavigateTo(val destination: String, override val raw: String) : VoiceCommand()
    data class NavigateHome(override val raw: String) : VoiceCommand()                      // C12
    data class NavigateLastFuel(override val raw: String) : VoiceCommand()                  // C12
    data class NavigateLastParking(override val raw: String) : VoiceCommand()               // C09
    data class NavigateInverse(override val raw: String) : VoiceCommand()                   // C07
    data class EtaToContact(val destination: String, override val raw: String) : VoiceCommand() // C03
    data class FindCheapGasStation(override val raw: String) : VoiceCommand()               // C08 / P3

    // Multimedia
    data class PlayMusic(override val raw: String) : VoiceCommand()
    data class PauseMusic(override val raw: String) : VoiceCommand()
    data class ResumeMusic(override val raw: String) : VoiceCommand()
    data class NextTrack(override val raw: String) : VoiceCommand()
    data class PreviousTrack(override val raw: String) : VoiceCommand()
    data class VolumeUp(override val raw: String) : VoiceCommand()
    data class VolumeDown(override val raw: String) : VoiceCommand()
    data class OpenPlaylist(val name: String, override val raw: String) : VoiceCommand()    // D05

    // Vehicle / OBD (on-demand only)
    data class VehicleStatusQuery(override val raw: String) : VoiceCommand()                // E13 also returns DTCs
    data class TripReportRequest(override val raw: String) : VoiceCommand()                 // E11 spoken
    data class StartObdMonitoring(override val raw: String) : VoiceCommand()                // "monitoriza el coche"
    data class StopObdMonitoring(override val raw: String) : VoiceCommand()

    // Productivity / context
    data class WeatherQuery(override val raw: String) : VoiceCommand()                       // I03
    data class CalendarQuery(override val raw: String) : VoiceCommand()                      // I01
    data class AddRefuel(override val raw: String) : VoiceCommand()                          // G06 (opens screen)

    // Safety
    data class SosCall(override val raw: String) : VoiceCommand()                            // F11
    data class CancelSos(override val raw: String) : VoiceCommand()

    // Lifecycle
    data class StopDriving(override val raw: String) : VoiceCommand()
    data class Affirmative(override val raw: String) : VoiceCommand()                        // multi-turn yes
    data class Negative(override val raw: String) : VoiceCommand()                           // multi-turn no
    data class CustomMatch(val customId: String, override val raw: String) : VoiceCommand()  // B10

    data class Unknown(override val raw: String) : VoiceCommand()
}
