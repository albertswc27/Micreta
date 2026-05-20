package com.micreta.app.core.navigation

import android.content.Context
import android.content.pm.PackageManager
import com.micreta.app.core.logging.EventLogger
import com.micreta.app.domain.model.FavoritePlace

/**
 * Resolves a spoken destination to a [FavoritePlace] when possible, then
 * launches Waze with the right intent.
 *
 * Matching is intentionally forgiving: lowercased token match, plus a
 * "contains" check against name and aliases. We err on the side of *finding*
 * a favorite — the user can correct us if we picked the wrong one.
 */
class WazeNavigator(private val context: Context) {

    /** Try to find a favorite that matches the spoken destination. */
    fun matchFavorite(spoken: String, favorites: List<FavoritePlace>): FavoritePlace? {
        val needle = spoken.trim().lowercase()
        if (needle.isEmpty()) return null

        // Exact match against name or any alias.
        favorites.firstOrNull { place -> place.matchTokens.any { it == needle } }
            ?.let { return it }

        // Contained match — handles "vamos a la UAB de Bellaterra" → UAB.
        favorites.firstOrNull { place ->
            place.matchTokens.any { token ->
                token.length >= 2 && (needle.contains(token) || token.contains(needle))
            }
        }?.let { return it }

        return null
    }

    /**
     * Launches Waze. Returns true if an activity was started, false otherwise.
     *
     * The fallback chain: explicit Waze package → web fallback → giving up.
     */
    fun navigate(spoken: String, favorites: List<FavoritePlace>): Boolean {
        val favorite = matchFavorite(spoken, favorites)
        val intent = favorite?.let { NavigationIntentBuilder.forFavorite(it) }
            ?: NavigationIntentBuilder.forText(spoken)

        EventLogger.info(TAG, "Launching Waze for \"${favorite?.name ?: spoken}\" (favorite=${favorite != null})")

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            EventLogger.warn(TAG, "Waze app not available, trying web fallback. cause=${e.message}")
            try {
                context.startActivity(NavigationIntentBuilder.forTextWebFallback(spoken))
                true
            } catch (e2: Exception) {
                EventLogger.error(TAG, "Both Waze app and web fallback failed: ${e2.message}")
                false
            }
        }
    }

    fun isWazeInstalled(): Boolean = try {
        context.packageManager.getPackageInfo("com.waze", 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    companion object {
        private const val TAG = "Waze"
    }
}
