package com.micreta.app.core.voice

import com.micreta.app.domain.model.VoiceCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandParserTest {

    /** Mirrors VoiceCommandViewModel.onTranscript routing (sanitize → resolve). */
    private fun route(input: String, awaiting: Boolean = true): VoiceCommand =
        CommandParser.resolve(TranscriptSanitizer.clean(input), emptyList(), awaiting)

    // ---- Existing baseline ---------------------------------------------

    @Test
    fun parsesNavigationDestination() {
        val command = CommandParser.parse("Llévame al taller")
        assertTrue(command is VoiceCommand.NavigateTo)
        assertEquals("taller", (command as VoiceCommand.NavigateTo).destination)
    }

    @Test
    fun parsesMusicCommands() {
        assertTrue(CommandParser.parse("pon música") is VoiceCommand.PlayMusic)
        assertTrue(CommandParser.parse("pausa la música") is VoiceCommand.PauseMusic)
        assertTrue(CommandParser.parse("siguiente canción") is VoiceCommand.NextTrack)
    }

    @Test
    fun parsesSosAndCancelCommands() {
        assertTrue(CommandParser.parse("llama a emergencias") is VoiceCommand.SosCall)
        assertTrue(CommandParser.parse("cancela") is VoiceCommand.CancelSos)
    }

    @Test
    fun parsesVehicleDiagnostic() {
        assertTrue(CommandParser.parse("diagnóstico") is VoiceCommand.VehicleStatusQuery)
        assertTrue(CommandParser.parse("revisa el coche") is VoiceCommand.VehicleStatusQuery)
    }

    @Test
    fun parsesAffirmativeAndNegative() {
        assertTrue(CommandParser.parse("sí") is VoiceCommand.Affirmative)
        assertTrue(CommandParser.parse("no") is VoiceCommand.Negative)
    }

    @Test
    fun fallsBackToUnknownWhenNoRuleMatches() {
        val command = CommandParser.parse("pon el modo nave espacial")
        assertTrue(command is VoiceCommand.Unknown)
    }

    // ---- The reported self-capture bug (P0) ----------------------------

    @Test
    fun selfCaptured_prompt_plus_playMusic_isPlayMusic() {
        val cmd = route("buenas tardes albert a dónde vamos pon música")
        assertTrue("expected PlayMusic, got $cmd", cmd is VoiceCommand.PlayMusic)
    }

    @Test
    fun selfCaptured_prompt_plus_gasStation_isFindCheapGasStation() {
        val cmd = route("buenas tardes albert a dónde vamos gasolinera más barata")
        assertTrue("expected FindCheapGasStation, got $cmd", cmd is VoiceCommand.FindCheapGasStation)
    }

    @Test
    fun selfCaptured_prompt_plus_destination_isCleanDestination() {
        val cmd = route("a dónde vamos al gimnasio")
        assertTrue("expected NavigateTo, got $cmd", cmd is VoiceCommand.NavigateTo)
        assertEquals("gimnasio", (cmd as VoiceCommand.NavigateTo).destination)
    }

    // ---- Bare commands & routing ---------------------------------------

    @Test
    fun bare_playMusic() {
        assertTrue(route("pon música", awaiting = false) is VoiceCommand.PlayMusic)
    }

    @Test
    fun bare_gasStation() {
        assertTrue(route("gasolinera más barata", awaiting = false) is VoiceCommand.FindCheapGasStation)
    }

    @Test
    fun playMusic_whileAwaitingDestination_doesNotBecomeNavigation() {
        val cmd = route("pon música", awaiting = true)
        assertTrue("expected PlayMusic, got $cmd", cmd is VoiceCommand.PlayMusic)
    }

    // ---- Accent normalization (P1) -------------------------------------

    @Test
    fun musica_variants_arePlayMusic() {
        assertTrue(CommandParser.parse("dale música") is VoiceCommand.PlayMusic)
        assertTrue(CommandParser.parse("pon spotify") is VoiceCommand.PlayMusic)
        assertTrue(CommandParser.parse("reproduce musica") is VoiceCommand.PlayMusic)
    }

    @Test
    fun diagnostico_withoutAccent_isVehicleStatus() {
        assertTrue(CommandParser.parse("diagnostico") is VoiceCommand.VehicleStatusQuery)
    }

    // ---- Gas search vs saved-favorite shortcut -------------------------

    @Test
    fun gasStation_doesNotShadow_lastFuelFavorite() {
        assertTrue(CommandParser.parse("vamos a la gasolinera") is VoiceCommand.NavigateLastFuel)
    }

    @Test
    fun gasStation_triggersForSearchPhrases() {
        assertTrue(CommandParser.parse("repostar") is VoiceCommand.FindCheapGasStation)
        assertTrue(CommandParser.parse("buscar gasolinera") is VoiceCommand.FindCheapGasStation)
    }
}
