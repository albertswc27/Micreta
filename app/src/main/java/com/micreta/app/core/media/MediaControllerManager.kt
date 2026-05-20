package com.micreta.app.core.media

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent
import com.micreta.app.core.logging.EventLogger

/**
 * Controls media playback via system-wide media key events.
 *
 * Works with whichever app currently holds the media session — usually the
 * last one that was playing. We don't bind to a specific player so the user
 * is free to use Spotify, YT Music, Apple Music, podcasts, etc.
 *
 * Volume control goes through [AudioManager] STREAM_MUSIC.
 */
class MediaControllerManager(private val context: Context) {

    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun play() = sendKey(KeyEvent.KEYCODE_MEDIA_PLAY)
    fun pause() = sendKey(KeyEvent.KEYCODE_MEDIA_PAUSE)
    fun playPause() = sendKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    fun next() = sendKey(KeyEvent.KEYCODE_MEDIA_NEXT)
    fun previous() = sendKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)

    fun volumeUp() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
    }

    fun volumeDown() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
    }

    /**
     * Launches the configured music app (e.g. Spotify) if installed. Falls
     * back to system MUSIC category intent so the user's default music app
     * opens regardless of brand.
     */
    fun launchMusicApp(packageName: String?): Boolean {
        val pm = context.packageManager
        if (!packageName.isNullOrBlank()) {
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                EventLogger.info(TAG, "Launched music app: $packageName")
                return true
            }
        }
        return try {
            val generic = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_MUSIC)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(generic)
            EventLogger.info(TAG, "Launched system music intent.")
            true
        } catch (e: Exception) {
            EventLogger.warn(TAG, "Could not launch music app: ${e.message}")
            false
        }
    }

    private fun sendKey(keyCode: Int) {
        val now = SystemClock.uptimeMillis()
        val down = KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0)
        val up = KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0)
        audioManager.dispatchMediaKeyEvent(down)
        audioManager.dispatchMediaKeyEvent(up)
        EventLogger.info(TAG, "Sent media key: ${KeyEvent.keyCodeToString(keyCode)}")
    }

    companion object {
        private const val TAG = "Media"
    }
}
