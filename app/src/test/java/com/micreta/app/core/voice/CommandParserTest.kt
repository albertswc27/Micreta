package com.micreta.app.core.voice

import com.micreta.app.domain.model.VoiceCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandParserTest {

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
}
