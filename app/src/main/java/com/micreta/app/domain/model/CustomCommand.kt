package com.micreta.app.domain.model

/**
 * User-defined voice command (B10).
 *
 * The user types a phrase ("a la oficina", "música tranquila") and picks an
 * [action] from a closed set. [payload] is the action's argument
 * (destination name, package, URL, phrase to speak…).
 */
data class CustomCommand(
    val id: String,
    val phrase: String,
    val action: Action,
    val payload: String = "",
    val enabled: Boolean = true
) {
    enum class Action {
        NAVIGATE_TO,        // payload = address/alias
        OPEN_APP,           // payload = package name
        OPEN_URL,           // payload = http(s) URL
        SPEAK,              // payload = text Micreta will say
        DIAGNOSTIC,         // payload ignored
        STOP_DRIVING        // payload ignored
    }
}
