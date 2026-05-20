package com.micreta.app.core.voice

/**
 * Strips Micreta's own spoken prompts from a raw STT transcript before it
 * reaches the [CommandParser].
 *
 * Why: the SpeechRecognizer sometimes captures the tail of Micreta's TTS
 * greeting ("buenas tardes Albert, ¿a dónde vamos?") and prepends it to the
 * user's real command. This sanitizer removes the known prompt fragments so
 *   "buenas tardes albert a donde vamos pon musica" → "pon musica".
 *
 * It is deliberately conservative: it only removes whole-word matches of a
 * closed list of phrases Micreta actually says, and never returns an empty
 * string if that would discard the user's command.
 */
object TranscriptSanitizer {

    // Folded (accent-free, lowercase) fragments Micreta speaks in its prompts.
    // Sorted longest-first at use time so multi-word prompts are removed before
    // their sub-phrases.
    private val baseFragments = listOf(
        "a que destino te llevo",
        "dime un destino y arrancamos",
        "que andamos haciendo",
        "listos para rodar",
        "micreta operativa",
        "buenas tardes",
        "buenos dias",
        "buenas noches",
        "a donde vamos",
        "donde vamos",
        "cual es el destino",
        "destino requerido",
        "indica destino",
        "micreta",
        "hola"
    )

    /**
     * Returns the transcript with Micreta's prompt fragments removed.
     * [ownerName] is also stripped (it appears in greetings, e.g. "…Albert…").
     */
    fun clean(raw: String, ownerName: String = "Albert"): String {
        var text = VoiceText.fold(raw)
        if (text.isBlank()) return text

        val fragments = (baseFragments + VoiceText.fold(ownerName))
            .filter { it.isNotBlank() }
            .distinct()
            .sortedByDescending { it.length }

        for (frag in fragments) {
            val re = Regex("\\b" + Regex.escape(frag) + "\\b")
            text = re.replace(text, " ")
        }

        text = text
            .replace(Regex("[,;:]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        // Safety: never destroy the user's command entirely.
        return text.ifBlank { VoiceText.fold(raw) }
    }
}
