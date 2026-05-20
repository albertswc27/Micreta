package com.micreta.app.domain.model

/**
 * Emotional / functional state of Micreta. Drives the avatar mood and the
 * tone of the phrases produced by [com.micreta.app.domain.personality.MicretaPersonalityEngine].
 */
enum class MicretaState {
    SLEEPING,
    DETECTING,
    CONNECTED,
    LISTENING,
    THINKING,
    NAVIGATING,
    ALERT,
    HAPPY,
    NEUTRAL,
    ERROR
}
