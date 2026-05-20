package com.micreta.app.core.voice

import com.micreta.app.domain.model.CustomCommand
import com.micreta.app.domain.model.VoiceCommand

/**
 * Pure rule-based parser. Order matters — more specific patterns first.
 *
 * Input is accent-folded ([VoiceText.fold]) before matching, so patterns are
 * written without accents ("musica", "diagnostico", "cancion") and match both
 * "música" and "musica" coming from STT.
 *
 * Returns [VoiceCommand.Unknown] when no rule matches; [resolve] adds the
 * multi-turn "treat-as-destination" fallback used after Micreta asks "¿a
 * dónde?".
 *
 * v0.2.1 changes:
 *  - Accent-insensitive matching (P1 normalization).
 *  - Expanded multimedia triggers (pon música / pon spotify / dale música…).
 *  - New FindCheapGasStation intent (P3).
 *  - [resolve]: always parse first; only fall back to a bare destination when
 *    the parse is Unknown *and* we were awaiting a destination (P0 routing fix,
 *    so "pon música" is never treated as a navigation destination).
 */
object CommandParser {

    // ---- Navigation -----------------------------------------------------

    private val navigatePatterns = listOf(
        Regex("""^(?:venga\s+)?(?:llevame)\s+(?:a|al|a la|a los|a las)\s+(.+)$"""),
        Regex("""^vamos\s+(?:a|al|a la|a los|a las)\s+(.+)$"""),
        Regex("""^(?:vamonos)\s+(?:a|al|a la|a los|a las)\s+(.+)$"""),
        Regex("""^(?:ir|navega|navegar|guiame)\s+(?:a|al|a la|a los|a las)\s+(.+)$"""),
        Regex("""^abre\s+waze(?:\s+(?:a|al|a la|hacia|hasta)\s+(.+))?$"""),
        Regex("""^(?:quiero|queremos)\s+ir\s+(?:a|al|a la|a los|a las)\s+(.+)$""")
    )

    private val homePatterns = listOf(
        Regex("""^llevame\s+a\s+casa$"""),
        Regex("""^vamos\s+a\s+casa$"""),
        Regex("""^a\s+casa$"""),
        Regex("""^volver\s+a\s+casa$""")
    )

    // P3 — cheap gas station search. Checked BEFORE lastFuel/navigate so
    // "llevame a una gasolinera barata" doesn't become a generic destination.
    private val gasStationPatterns = listOf(
        Regex("""^gasolinera\s+mas\s+barata.*$"""),
        Regex("""^(?:la\s+)?gasolinera\s+mas\s+barata.*$"""),
        Regex("""^gasolina\s+mas\s+barata.*$"""),
        Regex("""^gasolinera\s+barata.*$"""),
        Regex("""^buscar\s+gasolina$"""),
        Regex("""^buscar\s+(?:una\s+)?gasolinera.*$"""),
        Regex("""^busca\s+(?:una\s+)?gasolinera.*$"""),
        Regex("""^echar\s+gasolina$"""),
        Regex("""^repostar$"""),
        Regex("""^donde\s+(?:puedo\s+)?repostar.*$"""),
        Regex("""^llevame\s+a\s+una\s+gasolinera(?:\s+barata)?$""")
    )

    private val lastFuelPatterns = listOf(
        Regex("""^llevame\s+a\s+(?:la\s+)?(?:ultima\s+)?gasolinera$"""),
        Regex("""^vamos\s+a\s+(?:la\s+)?gasolinera$""")
    )

    private val parkingPatterns = listOf(
        Regex("""^donde\s+he\s+aparcado.*$"""),
        Regex("""^llevame\s+(?:al|a)\s+coche$"""),
        Regex("""^busca\s+(?:mi|el)\s+coche$""")
    )

    private val inversePatterns = listOf(
        Regex("""^volver$"""),
        Regex("""^vuelta$"""),
        Regex("""^volver\s+atras$"""),
        Regex("""^ruta\s+de\s+vuelta$""")
    )

    private val etaPatterns = listOf(
        Regex("""^avisa\s+(?:a\s+)?(.+?)\s+que\s+voy\s+(?:a|al|a la|a los|a las)\s+(.+)$"""),
        Regex("""^manda\s+eta\s+a\s+(.+?)\s+(?:hacia|para|a)\s+(.+)$""")
    )

    // ---- Music ----------------------------------------------------------

    private val playlistPatterns = listOf(
        Regex("""^pon\s+(?:mi\s+)?playlist\s+(.+)$"""),
        Regex("""^reproduce\s+(?:mi\s+)?playlist\s+(.+)$"""),
        Regex("""^pon\s+(?:la\s+)?lista\s+(.+)$""")
    )

    private val musicPlay = listOf(
        Regex("""^pon\s+musica.*$"""),
        Regex("""^reproduce\s+musica.*$"""),
        Regex("""^pon\s+algo\s+de\s+musica.*$"""),
        Regex("""^dale\s+(?:a\s+la\s+)?musica$"""),
        Regex("""^quiero\s+musica.*$"""),
        Regex("""^pon\s+spotify.*$"""),
        Regex("""^abre\s+spotify.*$"""),
        Regex("""^pon\s+musica\s+en\s+spotify.*$"""),
        Regex("""^musica$""")
    )
    private val musicPause = listOf(
        Regex("""^pausa(?:\s+la\s+musica)?$"""),
        Regex("""^para\s+(?:la\s+)?musica$""")
    )
    private val musicResume = listOf(
        Regex("""^reanuda(?:\s+la\s+musica)?$"""),
        Regex("""^continua(?:\s+la\s+musica)?$"""),
        Regex("""^sigue(?:\s+la\s+musica)?$""")
    )
    private val nextTrack = listOf(
        Regex("""^siguiente(?:\s+cancion)?$"""),
        Regex("""^pasa(?:\s+(?:de|la)\s+cancion)?$"""),
        Regex("""^cambia\s+(?:de\s+)?cancion$""")
    )
    private val prevTrack = listOf(
        Regex("""^cancion\s+anterior$"""),
        Regex("""^vuelve\s+atras$"""),
        Regex("""^anterior$""")
    )
    private val volumeUp = listOf(
        Regex("""^sube(?:\s+el)?\s+volumen$"""),
        Regex("""^mas\s+volumen$""")
    )
    private val volumeDown = listOf(
        Regex("""^baja(?:\s+el)?\s+volumen$"""),
        Regex("""^menos\s+volumen$""")
    )

    // ---- OBD / status ---------------------------------------------------

    private val statusPatterns = listOf(
        Regex("""^como\s+esta\s+el\s+coche$"""),
        Regex("""^diagnostico$"""),
        Regex("""^revisa\s+el\s+coche$"""),
        Regex("""^estado\s+del\s+coche$"""),
        Regex("""^lee\s+(?:el\s+)?coche$""")
    )

    private val tripReportPatterns = listOf(
        Regex("""^resumen\s+del\s+viaje$"""),
        Regex("""^como\s+ha\s+ido\s+el\s+viaje$"""),
        Regex("""^trip\s+report$""")
    )

    private val startMonitoringPatterns = listOf(
        Regex("""^monitoriza\s+el\s+coche$"""),
        Regex("""^vigila\s+el\s+coche$""")
    )

    private val stopMonitoringPatterns = listOf(
        Regex("""^para\s+(?:de\s+)?monitorizar$"""),
        Regex("""^deja\s+de\s+vigilar$""")
    )

    // ---- Productivity ---------------------------------------------------

    private val weatherPatterns = listOf(
        Regex("""^que\s+tiempo\s+hace.*$"""),
        Regex("""^tiempo\s+hoy$"""),
        Regex("""^meteorologia$""")
    )

    private val calendarPatterns = listOf(
        Regex("""^que\s+tengo\s+hoy.*$"""),
        Regex("""^agenda(?:\s+de\s+hoy)?$"""),
        Regex("""^proxima\s+reunion$""")
    )

    private val refuelPatterns = listOf(
        Regex("""^(?:apunta|registra)\s+(?:el\s+)?repostaje$"""),
        Regex("""^he\s+repostado$""")
    )

    // ---- Safety ---------------------------------------------------------

    private val sosPatterns = listOf(
        Regex("""^sos$"""),
        Regex("""^llama\s+(?:a\s+)?emergencias$"""),
        Regex("""^emergencias$"""),
        Regex("""^necesito\s+ayuda$""")
    )

    private val cancelSosPatterns = listOf(
        Regex("""^cancela$"""),
        Regex("""^para$"""),
        Regex("""^anula(?:\s+sos)?$""")
    )

    private val stopPatterns = listOf(
        Regex("""^(?:salir|sal|deja)\s+(?:del\s+)?modo\s+conduccion$"""),
        Regex("""^para\s+micreta$"""),
        Regex("""^apagate$"""),
        Regex("""^hasta\s+luego$""")
    )

    // ---- Yes / No -------------------------------------------------------

    private val affirmative = listOf(
        Regex("""^(?:si|claro|vale|de acuerdo|venga|ok|okey|okay|confirmado|por supuesto)$""")
    )
    private val negative = listOf(
        Regex("""^(?:no|nada|cancela|dejalo|nope)$""")
    )

    // ---- Entry points ---------------------------------------------------

    /** Normalize: fold accents, lowercase, trim trailing punctuation. */
    private fun normalize(raw: String): String =
        VoiceText.fold(raw).trim().trimEnd('.', ',', '!', '?', ';', ' ').trim()

    fun parse(raw: String, custom: List<CustomCommand> = emptyList()): VoiceCommand {
        val text = normalize(raw)

        // Custom commands first — let users override built-ins if they want to.
        for (c in custom) {
            if (!c.enabled) continue
            val phrase = VoiceText.fold(c.phrase)
            if (phrase.isNotEmpty() && (text == phrase || text.contains(phrase))) {
                return VoiceCommand.CustomMatch(c.id, raw)
            }
        }

        // Special destinations
        if (homePatterns.anyMatch(text)) return VoiceCommand.NavigateHome(raw)
        if (gasStationPatterns.anyMatch(text)) return VoiceCommand.FindCheapGasStation(raw)
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

        // Music — playlist before generic play.
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

    /**
     * Full routing: parse first; only if the parse is [VoiceCommand.Unknown]
     * *and* we were awaiting a destination do we treat the utterance as a bare
     * destination. This prevents "pon música" / "gasolinera más barata" from
     * being swallowed as navigation just because Micreta had asked "¿a dónde?".
     */
    fun resolve(
        raw: String,
        custom: List<CustomCommand> = emptyList(),
        awaitingDestination: Boolean = false
    ): VoiceCommand {
        val cmd = parse(raw, custom)
        if (cmd !is VoiceCommand.Unknown) return cmd
        if (awaitingDestination) {
            parseDestinationOnly(raw)?.takeIf { it.isNotBlank() }?.let {
                return VoiceCommand.NavigateTo(it, raw)
            }
        }
        return cmd
    }

    /** Helper for multi-turn: when we asked "¿a dónde?" we only need a destination phrase. */
    fun parseDestinationOnly(raw: String): String? {
        val text = normalize(raw)
        if (text.isBlank()) return null
        // Try the full nav parser first.
        for (pattern in navigatePatterns) {
            pattern.matchEntire(text)?.groupValues?.getOrNull(1)?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { return stripLeadingArticle(it) }
        }
        // Don't treat a recognised non-destination command as a destination.
        if (homePatterns.anyMatch(text) ||
            lastFuelPatterns.anyMatch(text) ||
            gasStationPatterns.anyMatch(text)
        ) return null
        // Otherwise treat the utterance as a bare destination ("al gimnasio" → "gimnasio").
        return stripLeadingArticle(text).takeIf { it.isNotBlank() }
    }

    private val leadingArticle = Regex("""^(?:a|al|a la|a los|a las|hacia|hasta|el|la|los|las|un|una)\s+""")
    private fun stripLeadingArticle(s: String): String = leadingArticle.replaceFirst(s, "").trim()

    private fun List<Regex>.anyMatch(text: String): Boolean = any { it.matches(text) }
}
