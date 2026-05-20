package com.micreta.app.core.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptSanitizerTest {

    @Test
    fun stripsGreetingAndPrompt_keepsCommand() {
        assertEquals(
            "pon musica",
            TranscriptSanitizer.clean("buenas tardes albert a dónde vamos pon música")
        )
    }

    @Test
    fun stripsGreetingAndPrompt_forGasStation() {
        assertEquals(
            "gasolinera mas barata",
            TranscriptSanitizer.clean("buenos días albert a dónde vamos gasolinera más barata")
        )
    }

    @Test
    fun keepsRealCommand_untouched() {
        assertEquals("pon musica", TranscriptSanitizer.clean("pon música"))
    }

    @Test
    fun neverReturnsEmpty_evenIfOnlyPrompt() {
        // "micreta" alone is a prompt word; sanitizing must not wipe everything.
        assertTrue(TranscriptSanitizer.clean("micreta").isNotBlank())
    }

    @Test
    fun doesNotEatCancelCommand() {
        // "para" is a real user command (cancel) — must survive sanitizing.
        assertEquals("para", TranscriptSanitizer.clean("para"))
    }
}
