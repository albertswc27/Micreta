package com.micreta.app.core.navigation

import android.content.Intent
import android.net.Uri
import com.micreta.app.domain.model.FavoritePlace

/**
 * Builds intents to launch Waze with a destination.
 *
 * Two paths:
 *  1. We have an exact address (or favorite with an address) → use `ll=` + `q=`
 *     style URI, which makes Waze go directly into navigation.
 *  2. We only have a search string → use Waze's search URI, the user taps once.
 *
 * Waze URI scheme reference:
 *   waze://?q=<text>&navigate=yes
 *   https://waze.com/ul?q=<text>&navigate=yes
 *
 * We try the deep link first; the caller is expected to fall back to https:
 * scheme if Waze isn't installed (handled by [com.micreta.app.core.navigation.WazeNavigator]).
 */
object NavigationIntentBuilder {

    private const val WAZE_PACKAGE = "com.waze"

    fun forFavorite(place: FavoritePlace): Intent {
        val query = place.address.ifBlank { place.name }
        return forText(query)
    }

    fun forText(destination: String): Intent {
        val encoded = Uri.encode(destination.trim())
        val uri = Uri.parse("waze://?q=$encoded&navigate=yes")
        return Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(WAZE_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /** Fallback when the user doesn't have Waze installed — opens the web flow. */
    fun forTextWebFallback(destination: String): Intent {
        val encoded = Uri.encode(destination.trim())
        val uri = Uri.parse("https://waze.com/ul?q=$encoded&navigate=yes")
        return Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    // ---- Coordinate navigation (gas-station flow, P3) -------------------

    /** Navigate straight to a lat/lon. Optional [label] is shown as the query. */
    fun forCoordinates(lat: Double, lon: Double, label: String? = null): Intent {
        val q = label?.takeIf { it.isNotBlank() }?.let { "&q=" + Uri.encode(it) }.orEmpty()
        val uri = Uri.parse("waze://?ll=$lat,$lon&navigate=yes$q")
        return Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(WAZE_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun forCoordinatesWebFallback(lat: Double, lon: Double): Intent {
        val uri = Uri.parse("https://waze.com/ul?ll=$lat,$lon&navigate=yes")
        return Intent(Intent.ACTION_VIEW, uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    }

    /** Open Waze on a *search* (no navigate=yes) so the user can pick a result. */
    fun forNearbySearch(query: String): Intent {
        val encoded = Uri.encode(query.trim())
        val uri = Uri.parse("waze://?q=$encoded")
        return Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(WAZE_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun forNearbySearchWebFallback(query: String): Intent {
        val encoded = Uri.encode(query.trim())
        val uri = Uri.parse("https://waze.com/ul?q=$encoded")
        return Intent(Intent.ACTION_VIEW, uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    }
}
