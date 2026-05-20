package com.micreta.app.domain.model

/**
 * Selectable personality preset.
 *
 * The phrase pool in [com.micreta.app.domain.personality.MicretaPersonalityEngine]
 * branches on this enum, so changing the user's preset instantly changes
 * Micreta's tone without restarting the app.
 */
enum class PersonalityProfile {
    /** Default. Warm, concise, friendly. */
    FRIENDLY,

    /** Formal, courteous, distant — for serious drivers or company cars. */
    FORMAL,

    /** Playful, joking, casual. Closer to a buddy than an assistant. */
    PLAYFUL,

    /** Robotic, terse, JARVIS-style. */
    ROBOTIC
}
