package com.micreta.app.domain.personality

import com.micreta.app.domain.model.MicretaState
import com.micreta.app.domain.model.ObdAlert
import com.micreta.app.domain.model.PersonalityProfile
import com.micreta.app.domain.model.TripSummary
import com.micreta.app.domain.model.WeatherSnapshot
import java.util.Calendar
import kotlin.random.Random

/**
 * Generates short, in-character phrases.
 *
 * v0.2.0 changes vs v0.1.0:
 *  - [profile] selectable (B08): FRIENDLY / FORMAL / PLAYFUL / ROBOTIC.
 *  - [contextualGreeting] uses time-of-day + optional weather (B04).
 *  - [tripSummary] formats end-of-trip stats spoken by TTS (E11).
 *  - [reaction] surfaces emotional reactions to events (J05).
 *
 * Personality goals:
 *  - friendly companion, not a corporate assistant
 *  - concise: every phrase should be readable / hearable in under 3 seconds
 *  - never invents data (delegates to caller for telemetry values)
 */
class MicretaPersonalityEngine(
    @Volatile var ownerName: String = "Albert",
    @Volatile var carName: String = "Micra",
    @Volatile var profile: PersonalityProfile = PersonalityProfile.FRIENDLY,
    @Volatile var lastWeather: WeatherSnapshot? = null
) {

    // ---- B04: contextual greeting ---------------------------------------

    fun contextualGreeting(): String {
        val timeBucket = currentTimeBucket()
        val w = lastWeather
        val core = when (profile) {
            PersonalityProfile.FRIENDLY -> when (timeBucket) {
                TimeBucket.MORNING -> "Buenos días, $ownerName."
                TimeBucket.MIDDAY -> "Hola, $ownerName."
                TimeBucket.AFTERNOON -> "Buenas tardes, $ownerName."
                TimeBucket.NIGHT -> "Buenas noches, $ownerName."
            }
            PersonalityProfile.FORMAL -> when (timeBucket) {
                TimeBucket.MORNING -> "Buenos días. ${ownerName}, todo listo."
                TimeBucket.MIDDAY, TimeBucket.AFTERNOON -> "Buenas tardes, $ownerName."
                TimeBucket.NIGHT -> "Buenas noches, $ownerName."
            }
            PersonalityProfile.PLAYFUL -> pick(
                "Eh, $ownerName, ¿qué andamos haciendo?",
                "$ownerName al volante, $carName al asfalto.",
                "Listos para rodar."
            )
            PersonalityProfile.ROBOTIC -> "$ownerName. Micreta operativa."
        }
        val weatherClause = w?.let { " Hoy ${it.spoken}." } ?: ""
        val ask = when (profile) {
            PersonalityProfile.FORMAL -> " ¿Cuál es el destino?"
            PersonalityProfile.ROBOTIC -> " Destino requerido."
            else -> " ¿A dónde vamos?"
        }
        return core + weatherClause + ask
    }

    fun greeting(): String = contextualGreeting()

    fun confirmDestination(destination: String): String = when (profile) {
        PersonalityProfile.FORMAL -> "De acuerdo. Marchando a $destination."
        PersonalityProfile.PLAYFUL -> pick(
            "Pues vamos a $destination.",
            "$destination, marchando."
        )
        PersonalityProfile.ROBOTIC -> "Ruta a $destination. Iniciando."
        PersonalityProfile.FRIENDLY -> pick(
            "Perfecto. Te llevo a $destination.",
            "Vamos para $destination.",
            "Rumbo a $destination."
        )
    }

    fun didNotUnderstand(): String = when (profile) {
        PersonalityProfile.FORMAL -> "Disculpa, no he entendido. ¿Puedes repetir?"
        PersonalityProfile.PLAYFUL -> pick("No te pillé. Otra vez.", "¿Eh? Repíteme.")
        PersonalityProfile.ROBOTIC -> "No reconocido. Repite."
        PersonalityProfile.FRIENDLY -> pick(
            "No lo he pillado bien. Repíteme.",
            "Perdona, ¿repites?"
        )
    }

    fun openingWaze(): String = pick("Abro Waze.", "Mapa en marcha.", "Activando navegación.")

    fun vehicleHealthy(): String = when (profile) {
        PersonalityProfile.FORMAL -> "El $carName se encuentra en buen estado."
        PersonalityProfile.PLAYFUL -> "El $carName va fino."
        PersonalityProfile.ROBOTIC -> "$carName: parámetros nominales."
        PersonalityProfile.FRIENDLY -> pick(
            "El $carName parece estar bien.",
            "Todo en orden con el $carName."
        )
    }

    fun listeningCue(): String = when (profile) {
        PersonalityProfile.FORMAL -> "Te escucho."
        PersonalityProfile.PLAYFUL -> "Cuéntame."
        PersonalityProfile.ROBOTIC -> "Escuchando."
        PersonalityProfile.FRIENDLY -> "Te escucho."
    }

    fun stopping(): String = when (profile) {
        PersonalityProfile.FORMAL -> "Hasta luego, $ownerName."
        PersonalityProfile.PLAYFUL -> pick("Ahí te quedas.", "Hasta la próxima.")
        PersonalityProfile.ROBOTIC -> "Modo conducción finalizado."
        PersonalityProfile.FRIENDLY -> pick(
            "Hasta luego, $ownerName.",
            "Apago el copiloto."
        )
    }

    // ---- B02: multi-turn helpers ----------------------------------------

    /** Used when the user starts a navigation flow without a destination. */
    fun askForDestination(): String = when (profile) {
        PersonalityProfile.FORMAL -> "¿A qué destino te llevo?"
        PersonalityProfile.PLAYFUL -> "¿Dónde vamos?"
        PersonalityProfile.ROBOTIC -> "Indica destino."
        PersonalityProfile.FRIENDLY -> "Dime un destino y arrancamos."
    }

    /** Asks for a yes/no confirmation. */
    fun confirmYesNo(question: String): String = "$question ¿Sí o no?"

    /** Disambiguates a favorite when multiple match. */
    fun disambiguateFavorites(options: List<String>): String {
        if (options.isEmpty()) return askForDestination()
        if (options.size == 1) return "¿Te refieres a ${options.first()}?"
        val list = options.dropLast(1).joinToString(", ") + " o " + options.last()
        return "Tengo varios: $list. ¿Cuál?"
    }

    // ---- Music ack ------------------------------------------------------

    fun musicCommandAck(action: MusicAction): String = when (action) {
        MusicAction.PLAY -> "Música."
        MusicAction.PAUSE -> "Pauso."
        MusicAction.RESUME -> "Sigo."
        MusicAction.NEXT -> "Siguiente."
        MusicAction.PREV -> "Anterior."
        MusicAction.VOL_UP -> "Más alto."
        MusicAction.VOL_DOWN -> "Bajo el volumen."
    }

    fun alertPhrase(alert: ObdAlert): String = alert.spokenMessage

    // ---- E11: spoken trip summary ---------------------------------------

    fun tripSummary(s: TripSummary): String {
        val km = "%.1f".format(s.distanceKm)
        val mins = s.durationMin
        val ecoLabel = when {
            s.ecoScore >= 85 -> "muy buena conducción"
            s.ecoScore >= 70 -> "buena conducción"
            s.ecoScore >= 50 -> "conducción mejorable"
            else -> "conducción brusca"
        }
        val frenazos = if (s.harshBrakings > 0) "${s.harshBrakings} ${plural(s.harshBrakings, "frenazo", "frenazos")}, " else ""
        val acel = if (s.harshAccelerations > 0) "${s.harshAccelerations} ${plural(s.harshAccelerations, "acelerón", "acelerones")}, " else ""
        val consumo = s.estimatedConsumptionL100?.let { "${"%.1f".format(it)} litros cada cien estimados, " } ?: ""
        return "Viaje completado. $km kilómetros en $mins minutos. ${frenazos}${acel}${consumo}eco-score ${s.ecoScore}, $ecoLabel."
    }

    // ---- J05: reactions -------------------------------------------------

    fun reaction(event: Reaction): String = when (event) {
        Reaction.HARSH_BRAKE -> pick("¡Qué frenazo!", "Cuidado con el freno.", "Brusco eso.")
        Reaction.HARSH_ACCEL -> pick("Con calma, $ownerName.", "Tranquilo en el acelerador.")
        Reaction.OVER_SPEED -> pick("Vas justo de velocidad.", "Ojo al límite.")
        Reaction.GOOD_TRIP -> pick("¡Buen viaje!", "Conducción suave.", "Eso es manejarlo.")
        Reaction.FUEL_LOW -> "Queda poco combustible, conviene repostar."
    }

    enum class Reaction { HARSH_BRAKE, HARSH_ACCEL, OVER_SPEED, GOOD_TRIP, FUEL_LOW }

    // ---- F01 spoken speed limit warning ---------------------------------

    fun overSpeedWarning(currentKmh: Int, limitKmh: Int): String = when (profile) {
        PersonalityProfile.FORMAL -> "Recuerda, el límite es $limitKmh."
        PersonalityProfile.PLAYFUL -> "Eh, vas a $currentKmh y el límite es $limitKmh."
        PersonalityProfile.ROBOTIC -> "Exceso. Límite $limitKmh."
        PersonalityProfile.FRIENDLY -> "Ojo, el límite es $limitKmh."
    }

    // ---- SOS ------------------------------------------------------------

    fun sosCountdown(seconds: Int): String = "Voy a llamar a emergencias en $seconds segundos. Di 'cancela' para parar."
    fun sosCalling(): String = "Llamando a emergencias."
    fun sosCancelled(): String = "Llamada cancelada."

    // ---- Labels ---------------------------------------------------------

    fun stateLabel(state: MicretaState): String = when (state) {
        MicretaState.SLEEPING -> "Dormida"
        MicretaState.DETECTING -> "Detectando coche"
        MicretaState.CONNECTED -> "Coche conectado"
        MicretaState.LISTENING -> "Escuchando"
        MicretaState.THINKING -> "Pensando"
        MicretaState.NAVIGATING -> "Navegando"
        MicretaState.ALERT -> "Alerta"
        MicretaState.HAPPY -> "Contenta"
        MicretaState.NEUTRAL -> "Lista"
        MicretaState.ERROR -> "Error"
    }

    enum class MusicAction { PLAY, PAUSE, RESUME, NEXT, PREV, VOL_UP, VOL_DOWN }

    // ---- internals ------------------------------------------------------

    private fun currentTimeBucket(): TimeBucket {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (h) {
            in 5..11 -> TimeBucket.MORNING
            in 12..14 -> TimeBucket.MIDDAY
            in 15..20 -> TimeBucket.AFTERNOON
            else -> TimeBucket.NIGHT
        }
    }

    private enum class TimeBucket { MORNING, MIDDAY, AFTERNOON, NIGHT }

    private fun pick(vararg options: String): String = options[Random.nextInt(options.size)]
    private fun plural(n: Int, sing: String, plur: String) = if (n == 1) sing else plur
}
