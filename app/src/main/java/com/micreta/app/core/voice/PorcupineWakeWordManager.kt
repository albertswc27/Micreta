package com.micreta.app.core.voice

import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import android.content.Context
import com.micreta.app.BuildConfig
import com.micreta.app.core.logging.EventLogger
import java.io.File

/**
 * Real "Micra" wake-word engine (Picovoice Porcupine v4, Spanish).
 *
 * Inert until a Picovoice AccessKey is provided in local.properties
 * (`picovoice.accessKey=...`) — [available] is false otherwise, so the UI keeps
 * the setting disabled. The keyword (.ppn) and Spanish model (.pv) ship in
 * assets/wakeword and are copied to internal storage on first use.
 *
 * V1 runs only while the app is in the foreground (no background mic loop).
 */
class PorcupineWakeWordManager(context: Context) : WakeWordManager {

    private val appContext = context.applicationContext
    private val accessKey: String = BuildConfig.PICOVOICE_ACCESS_KEY
    private var porcupine: PorcupineManager? = null

    override val available: Boolean get() = accessKey.isNotBlank()

    override fun start(onDetected: () -> Unit) {
        if (!available || porcupine != null) return
        try {
            val keywordPath = copyAsset("wakeword/Micra_es_android_v4_0_0.ppn")
            val modelPath = copyAsset("wakeword/porcupine_params_es.pv")
            porcupine = PorcupineManager.Builder()
                .setAccessKey(accessKey)
                .setKeywordPath(keywordPath)
                .setModelPath(modelPath)
                .setSensitivity(0.65f)
                .build(appContext, PorcupineManagerCallback { _ -> onDetected() })
            porcupine?.start()
            EventLogger.info(TAG, "Wake word 'Micra' listening.")
        } catch (e: Throwable) {
            EventLogger.error(TAG, "Wake word init failed: ${e.message}")
            runCatching { porcupine?.delete() }
            porcupine = null
        }
    }

    override fun stop() {
        runCatching { porcupine?.stop() }
        runCatching { porcupine?.delete() }
        porcupine = null
    }

    private fun copyAsset(assetPath: String): String {
        val out = File(appContext.filesDir, assetPath.substringAfterLast('/'))
        if (out.exists() && out.length() > 0L) return out.absolutePath // already extracted
        appContext.assets.open(assetPath).use { input -> out.outputStream().use { input.copyTo(it) } }
        return out.absolutePath
    }

    companion object {
        private const val TAG = "WakeWord"
    }
}
