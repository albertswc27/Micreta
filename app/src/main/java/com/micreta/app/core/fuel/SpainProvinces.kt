package com.micreta.app.core.fuel

import com.micreta.app.core.voice.VoiceText

/**
 * Maps a Spanish province (or autonomous-community) name — as returned by
 * Android's Geocoder — to its INE 2-digit code, which the Ministry fuel-price
 * API uses as `IDProvincia`.
 *
 * Names are matched accent- and case-insensitively, and `ñ` is folded to `n`
 * so "A Coruña" matches "a coruna". A `contains` fallback handles community
 * names like "Comunidad de Madrid" → Madrid.
 */
object SpainProvinces {

    private val byName: Map<String, String> = mapOf(
        "alava" to "01", "araba" to "01",
        "albacete" to "02",
        "alicante" to "03", "alacant" to "03",
        "almeria" to "04",
        "avila" to "05",
        "badajoz" to "06",
        "baleares" to "07", "illes balears" to "07", "islas baleares" to "07", "balears" to "07",
        "barcelona" to "08",
        "burgos" to "09",
        "caceres" to "10",
        "cadiz" to "11",
        "castellon" to "12", "castello" to "12",
        "ciudad real" to "13",
        "cordoba" to "14",
        "a coruna" to "15", "la coruna" to "15", "coruna" to "15",
        "cuenca" to "16",
        "girona" to "17", "gerona" to "17",
        "granada" to "18",
        "guadalajara" to "19",
        "guipuzcoa" to "20", "gipuzkoa" to "20",
        "huelva" to "21",
        "huesca" to "22",
        "jaen" to "23",
        "leon" to "24",
        "lleida" to "25", "lerida" to "25",
        "la rioja" to "26", "rioja" to "26",
        "lugo" to "27",
        "madrid" to "28",
        "malaga" to "29",
        "murcia" to "30",
        "navarra" to "31", "nafarroa" to "31",
        "ourense" to "32", "orense" to "32",
        "asturias" to "33",
        "palencia" to "34",
        "las palmas" to "35",
        "pontevedra" to "36",
        "salamanca" to "37",
        "santa cruz de tenerife" to "38", "tenerife" to "38",
        "cantabria" to "39",
        "segovia" to "40",
        "sevilla" to "41",
        "soria" to "42",
        "tarragona" to "43",
        "teruel" to "44",
        "toledo" to "45",
        "valencia" to "46",
        "valladolid" to "47",
        "vizcaya" to "48", "bizkaia" to "48",
        "zamora" to "49",
        "zaragoza" to "50",
        "ceuta" to "51",
        "melilla" to "52"
    )

    fun idFor(name: String?): String? {
        if (name.isNullOrBlank()) return null
        val k = VoiceText.fold(name).replace('ñ', 'n').removePrefix("provincia de ").trim()
        byName[k]?.let { return it }
        return byName.entries.firstOrNull { k.contains(it.key) }?.value
    }
}
