package com.micreta.app.core.voice

/**
 * Shared text normalization for the voice pipeline.
 *
 * Spanish STT output is inconsistent with accents ("música" vs "musica"), so
 * we fold to a canonical accent-free, lowercase form before pattern matching.
 * `ñ` is intentionally preserved (it changes meaning); only the accented
 * vowels are folded.
 */
object VoiceText {

    private val whitespace = Regex("\\s+")

    /** Lowercase, fold accented vowels (á→a … ü→u, keeps ñ), collapse whitespace, trim. */
    fun fold(input: String): String {
        val sb = StringBuilder(input.length)
        for (c in input.lowercase()) {
            sb.append(
                when (c) {
                    'á', 'à', 'ä', 'â', 'ã' -> 'a'
                    'é', 'è', 'ë', 'ê' -> 'e'
                    'í', 'ì', 'ï', 'î' -> 'i'
                    'ó', 'ò', 'ö', 'ô', 'õ' -> 'o'
                    'ú', 'ù', 'ü', 'û' -> 'u'
                    else -> c
                }
            )
        }
        return sb.toString().replace(whitespace, " ").trim()
    }
}
