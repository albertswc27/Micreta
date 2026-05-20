package com.micreta.app.domain.model

/**
 * A user-defined destination Micreta can navigate to by voice alias.
 *
 * @param id stable id (UUID string)
 * @param name human label, e.g. "UAB"
 * @param address textual address; passed to Waze as `q=` if present
 * @param voiceAliases alternative names the user might say
 */
data class FavoritePlace(
    val id: String,
    val name: String,
    val address: String,
    val voiceAliases: List<String> = emptyList()
) {
    /** All strings considered a match for this place (lowercased). */
    val matchTokens: List<String>
        get() = (listOf(name) + voiceAliases).map { it.trim().lowercase() }.filter { it.isNotEmpty() }
}
