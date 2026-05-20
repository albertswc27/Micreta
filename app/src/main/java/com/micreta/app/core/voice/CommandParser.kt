package com.micreta.app.core.voice

import com.micreta.app.domain.model.CustomCommand
import com.micreta.app.domain.model.VoiceCommand

/**
 * Pure rule-based parser. Order matters — more specific patterns first.
 *
 * Returns [VoiceCommand.Unknown] when no rule matches; the calling layer is
 * expected to ask the user to repeat rather than guess.
 *
 * v0.2.0 changes:
 *  - Closed intent table (hardening B01 from Drive feedback).
 *  - New voice commands: SOS, weather, calendar, trip report, parking memory,
 *    home/last-fuel destinations, refuel log, playlist, custom mappings.
 *  - Multi-turn helpers: parseAffirmative/Negative + parseDestinationOnly.
 */
object CommandParser {

    // ---- Navigation -----------------------------------------------------

    private val navigatePatterns = listOf(
        Regex("""^(?:venga\s+)?(?:llévame|llevame)\s+(?:a|al|a la|a los|a las)\s+(.+)$""", RegexOption.IGNORE_CASE),
        Regex("""^vamos\s+(?:a|al|a la|a los|a las)\s+(.+)$""", RegexOption.IGNORE_CASE),
        Regex("""^(?:vámonos|vamonos)\s+(?:a|al|a la|a los|a las)\s+(.+)$""", RegexOption.IGNORE_CASE),
        Regex("""^(?:ir|navega|navegar|guíame|guiame)\s+(?:a|al|a la|a los|a las)\s+(.+)$""", RegexOption.IGNORE_CASE),
        Regex("""^abre\s+waze(?:\s+(?:a|al|a la|hacia|hasta)\s+(.+))?$""", RegexOption.IGNORE_CASE),
        Regex("""^(?:quiero|queremos)\s+ir\s+(?:a|al|a la|a los|a las)\s+(.+)$""", RegexOption.IGNORE_CASE)
    )

    private val homePatterns = listOf(
        Regex("""^(?:llévame|llevame)\s+a\s+casa$""", RegexOption.IGNORE_CASE),
        Regex("""^vamos\s+a\s+casa$""", RegexOption.IGNORE_CASE),
        Regex("""^a\s+casa$""", RegexOption.IGNORE_CASE),
        Regex("""^volver\s+a\s+casa$""", RegexOption.IGNORE_CASE)
    )

    private val lastFuelPatterns = listOf(
        Regex("""^(?:llévame|llevame)\s+a\s+(?:la\s+)?(?:última\s+)?gasolinera$""", RegexOption.IGNORE_CASE),
        Regex("""^vamos\s+a\s+(?:la\s+)?gasolinera$""", RegexOption.IGNORE_CASE)
    )

    private val parkingPatterns = listOf(
        Regex("""^(?:dónde\s+he\s+aparcado|donde\s+he\s+aparcado).*$""", RegexOption.IGNORE_CASE),
        Regex("""^(?:llévame|llevame)\s+(?:al|a)\s+coche$""", RegexOption.IGNORE_CASE),
        Regex("""^busca\s+(?:mi|el)\s+coche$""", RegexOption.IGNORE_CASE)
    )

    private val inversePatterns = listOf(
        Regex("""^volver$""", RegexOption.IGNORE_CASE),
        Regex("""^vuelta$""", RegexOption.IGNORE_CASE),
        Regex("""^volver\s+atrás$""", RegexOption.IGNORE_CASE),
        Regex("""^ruta\s+de\s+vuelta$""", RegexOption.IGNORE_CASE)
    )

    private val etaPatterns = listOf(
        Regex("""^avisa\s+(?:a\s+)?(.+?)\s+que\s+voy\s+(?:a|al|a la|a los|a las)\s+(.+)$""", RegexOption.IGNORE_CASE),
        Regex("""^manda\s+eta\s+a\s+(.+?)\s+(?:hacia|para|a)\s+(.+)$""", RegexOption.IGNORE_CASE)
    )

    // ---- Music ----------------------------------------------------------

    private val playlistPatterns = listOf(
        Regex("""^pon\s+(?:mi\s+)?playlist\s+(.+)$""", RegexOption.IGNORE_CASE),
        Regex("""^reproduce\s+(?:mi\s+)?playlist\s+(.+)$""", RegexOption.IGNORE_CASE),
        Regex("""^pon\s+(?:la\s+)?lista\s+(.+)$""", RegexOption.IGNORE_CASE)
    )

    private val musicPlay = listOf(
        Regex("""^pon\s+música.*$""", RegexOption.IGNORE_CASE),
        Regex("""^reproduce\s+música.*$""", RegexOption.IGNORE_CASE),
        Regex("""^pon\s+algo\s+de\s+música.*$""", RegexOption.IGNORE_CASE),
        Regex("""^música$""", RegexOption.IGNORE_CASE)
    )
    private val musicPause = listOf(
        Regex("""^pausa(?:\s+la\s+música)?$""", RegexOption.IGNORE_CASE),
        Regex("""^para\s+(?:la\s+)?música$""", RegexOption.IGNORE_CASE)
    )
    private val musicResume = listOf(
        Regex("""^reanuda(?:\s+la\s+música)?$""", RegexOption.IGNORE_CASE),
        Regex("""^continúa(?:\s+la\s+música)?$""", RegexOption.IGNORE_CASE),
        Regex("""^sigue(?:\s+la\s+música)?$""", RegexOption.IGNORE_CASE)
    )
    private val nextTrack = listOf(
        Regex("""^siguiente(?:\s+canción)?$""", RegexOption.IGNORE_CASE),
        Regex("""^pasa(?:\s+(?:de|la)\s+canción)?$""", RegexOption.IGNORE_CASE),
        Regex("""^cambia\s+(?:de\s+)?canción$""", RegexOption.IGNORE_CASE)
    )
    private val prevTrack = listOf(
        Regex("""^canción\s+anterior$""", RegexOption.IGNORE_CASE),
        Regex("""^vuelve\s+atrás$""", RegexOption.IGNORE_CASE),
        Regex("""^anterior$""", RegexOption.IGNORE_CASE)
    )
    private val volumeUp = listOf(
        Regex("""^sube(?:\s+el)?\s+volumen$""", RegexOption.IGNORE_CASE),
        Regex("""^más\s+volumen$""", RegexOption.IGNORE_CASE)
    )
    private val volumeDown = listOf(
        Regex("""^baja(?:\s+el)?\s+volumen$""", RegexOption.IGNORE_CASE),
        Regex("""^menos\s+volumen$""", RegexOption.IGNORE_CASE)
    )

    // ---- OBD / status ---------------------------------------------------

    private val statusPatterns = listOf(
        Regex("""^cómo\s+está\s+el\s+coche$""", RegexOption.IGNORE_CASE),
        Regex("""^como\s+esta\s+el\s+coche$""", RegexOption.IGNORE_CASE),
        Regex("""^diagnóstico$""", RegexOption.IGNORE_CASE),
        Regex("""^diagnostico$""", RegexOption.IGNORE_CASE),
        Regex("""^revisa\s+el\s+coche$""", RegexOption.IGNORE_CASE),
        Regex("""^estado\s+del\s+coche$""", RegexOption.IGNORE_CASE),
        Regex("""^lee\s+(?:el\s+)?coche$""", RegexOption.IGNORE_CASE)
    )

    private val tripReportPatterns = listOf(
        Regex("""^resumen\s+del\s+viaje$""", RegexOption.IGNORE_CASE),
        Regex("""^cómo\s+ha\s+ido\s+el\s+viaje$""", RegexOption.IGNORE_CASE),
        Regex("""^como\s+ha\s+ido\s+el\s+viaje$""", RegexOption.IGNORE_CASE),
        Regex("""^trip\s+report$""", RegexOption.IGNORE_CASE)
    )

    private val startMonitoringPatterns = listOf(
        Regex("""^monitoriza\s+el\s+coche$""", RegexOption.IGNORE_CASE),
        Regex("""^vigila\s+el\s+coche$""", RegexOption.IGNORE_CASE)
    )

    private val stopMonitoringPatterns = listOf(
        Regex("""^para\s+(?:de\s+)?monitorizar$""", RegexOption.IGNORE_CASE),
        Regex("""^deja\s+de\s+vigilar$""", RegexOption.IGNORE_CASE)
    )

    // ---- Productivity ---------------------------------------------------

    private val weatherPatterns = listOf(
        Regex("""^(?:qué|que)\s+tiempo\s+hace.*$""", RegexOption.IGNORE_CASE),
        Regex("""^tiempo\s+hoy$""", RegexOption.IGNORE_CASE),
        Regex("""^meteorología$""", RegexOption.IGNORE_CASE),
        Regex("""^meteorologia$""", RegexOption.IGNORE_CASE)
    )

    private val calendarPatterns = listOf(
        Regex("""^(?:qué|que)\s+tengo\s+hoy.*$""", RegexOption.IGNORE_CASE),
        Regex("""^agenda(?:\s+de\s+hoy)?$""", RegexOption.IGNORE_CASE),
        Regex("""^próxima\s+reunión$""", RegexOption.IGNORE_CASE),
        Regex("""^proxima\s+reunion$""", RegexOption.IGNORE_CASE)
    )

    private val refuelPatterns = listOf(
        Regex("""^(?:apunta|registra)\s+(?:el\s+)?repostaje$""", RegexOption.IGNORE_CASE),
        Regex("""^he\s+repostado$""", RegexOption.IGNORE_CASE)
    )

    // ---- Safety ---------------------------------------------------------

    private val sosPatterns = listOf(
        Regex("""^sos$""", RegexOption.IGNORE_CASE),
        Regex("""^llama\s+(?:a\s+)?emergencias$""", RegexOption.IGNORE_CASE),
        Regex("""^emergencias$""", RegexOption.IGNORE_CASE),
        Regex("""^necesito\s+ayuda$""", RegexOption.IGNORE_CASE)
    )

    private val cancelSosPatterns = listOf(
        Regex("""^cancela$""", RegexOption.IGNORE_CASE),
        Regex("""^para$""", RegexOption.IGNORE_CASE),
        Regex("""^anula(?:\s+sos)?$""", RegexOption.IGNORE_CASE)
    )

    private val stopPatterns = listOf(
        Regex("""^(?:salir|sal|deja)\s+(?:del\s+)?modo\s+conducción$""", RegexOption.IGNORE_CASE),
        Regex("""^para\s+micreta$""", RegexOption.IGNORE_CASE),
        Regex("""^apágate$""", RegexOption.IGNORE_CASE),
        Regex("""^apagate$""", RegexOption.IGNORE_CASE),
        Regex("""^hasta\s+luego$""", RegexOption.IGNORE_CASE)
    )

    // ---- Yes / No -------------------------------------------------------

    private val affirmative = listOf(
        Regex("""^(?:sí|si|claro|vale|de acuerdo|venga|ok|okey|okay|confirmado|por supuesto)$""", RegexOption.IGNORE_CASE)
    )
    private val negative = listOf(
        Regex("""^(?:no|nada|cancela|déjalo|dejalo|nope)$""", RegexOption.IGNORE_CASE)
    )

    // ---- Entry point ----------------------------------------------------

    fun parse(raw: String, custom: List<CustomCommand> = emptyList()): VoiceCommand {
        val text = raw.trim().trimEnd('.', '!', '?').lowercase()

        // Custom commands first — let users override built-ins if they want to.
        for (c in custom) {
            if (!c.enabled) continue
            val phrase = c.phrase.trim().lowercase()
            if (phrase.isNotEmpty() && (text == phrase || text.contains(phrase))) {
                return VoiceCommand.CustomMatch(c.id, raw)
            }
        }

        // Special destinations
        if (homePatterns.anyMatch(text)) return VoiceCommand.NavigateHome(raw)
        if (lastFuelPatterns.anyMatch(text)) return VoiceCommand.NavigateLastFuel(raw)
        if (parkingPatterns.anyMatch(text)) return VoiceCommand.NavigateLastParking(raw)
        if (inversePatterns.anyMatch(text)) return VoiceCommand.NavigateInverse(raw)

        // ETA: avisa a Marta que voy al gym
        for (p in etaPatterns) {
            val m = p.matchEntire(text) ?: continue
            val dest = m.groupValues.getOrNull(2)?.trim().orEmpty()
            if (dest.isNotBlank()) return VoiceCommand.EtaToContact(dest, raw)
        }

        // Navigation
        for (pattern in navigatePatterns) {
            val m = pattern.matchEntire(text) ?: continue
            val destination = (m.groupValues.getOrNull(1) ?: "").trim()
            if (destination.isNotBlank()) {
                return VoiceCommand.NavigateTo(destination, raw)
            }
        }

        // Music
        for (pattern in playlistPatterns) {
            val m = pattern.matchEntire(text) ?: continue
            val name = m.groupValues.getOrNull(1)?.trim().orEmpty()
            if (name.isNotBlank()) return VoiceCommand.OpenPlaylist(name, raw)
        }
        if (musicPause.anyMatch(text)) return VoiceCommand.PauseMusic(raw)
        if (musicResume.anyMatch(text)) return VoiceCommand.ResumeMusic(raw)
        if (musicPlay.anyMatch(text)) return VoiceCommand.PlayMusic(raw)
        if (nextTrack.anyMatch(text)) return VoiceCommand.NextTrack(raw)
        if (prevTrack.anyMatch(text)) return VoiceCommand.PreviousTrack(raw)
        if (volumeUp.anyMatch(text)) return VoiceCommand.VolumeUp(raw)
        if (volumeDown.anyMatch(text)) return VoiceCommand.VolumeDown(raw)

        // Vehicle / OBD
        if (tripReportPatterns.anyMatch(text)) return VoiceCommand.TripReportRequest(raw)
        if (startMonitoringPatterns.anyMatch(text)) return VoiceCommand.StartObdMonitoring(raw)
        if (stopMonitoringPatterns.anyMatch(text)) return VoiceCommand.StopObdMonitoring(raw)
        if (statusPatterns.anyMatch(text)) return VoiceCommand.VehicleStatusQuery(raw)

        // Productivity
        if (weatherPatterns.anyMatch(text)) return VoiceCommand.WeatherQuery(raw)
        if (calendarPatterns.anyMatch(text)) return VoiceCommand.CalendarQuery(raw)
        if (refuelPatterns.anyMatch(text)) return VoiceCommand.AddRefuel(raw)

        // Safety
        if (cancelSosPatterns.anyMatch(text)) return VoiceCommand.CancelSos(raw)
        if (sosPatterns.anyMatch(text)) return VoiceCommand.SosCall(raw)

        if (stopPatterns.anyMatch(text)) return VoiceCommand.StopDriving(raw)

        // Yes / No
        if (affirmative.anyMatch(text)) return VoiceCommand.Affirmative(raw)
        if (negative.anyMatch(text)) return VoiceCommand.Negative(raw)

        return VoiceCommand.Unknown(raw)
    }

    /** Helper for multi-turn: when we asked "¿a dónde?" we only need a destination phrase. */
    fun parseDestinationOnly(raw: String): String? {
        val text = raw.trim().trimEnd('.', '!', '?').lowercase()
        // Try the full nav parser first.
        for (pattern in navigatePatterns) {
            val m = pattern.matchEntire(text) ?: continue
            m.groupValues.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
        }
        // Fall back: treat the whole utterance as a destination if it doesn't
        // look like a command.
        if (text.isBlank()) return null
        if (homePatterns.anyMatch(text) || lastFuelPatterns.anyMatch(text)) return null
        return text
    }

    private fun List<Regex>.anyMatch(text: String): Boolean = any { it.matches(text) }
}
