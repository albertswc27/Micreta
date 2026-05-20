package com.micreta.app.core.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.micreta.app.core.logging.EventLogger

/**
 * Intent helpers for things we hand off to other apps:
 *  - ETA messages (C03)
 *  - Parking location share
 *  - Trip CSV / log file share (L02)
 *
 * We never write SMS ourselves — we hand the message to Android's share
 * sheet. That avoids the SMS Play Store review process and lets the user
 * pick WhatsApp, Telegram, SMS, etc. on the spot.
 */
object ShareIntents {

    fun shareText(context: Context, text: String, subject: String? = null) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            if (subject != null) putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(Intent.createChooser(send, subject ?: "Compartir").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (t: Throwable) {
            EventLogger.error("Share", "ACTION_SEND failed: ${t.message}")
        }
    }

    fun smsTo(context: Context, phone: String, body: String) {
        val uri = Uri.parse("smsto:$phone")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (t: Throwable) {
            // Fallback to plain text share if there's no SMS app.
            shareText(context, body, "Mensaje")
        }
    }

    fun openGeo(context: Context, lat: Double, lon: Double, label: String? = null) {
        val tag = label?.let { Uri.encode(it) }
        val uriStr = if (tag != null) "geo:$lat,$lon?q=$lat,$lon($tag)" else "geo:$lat,$lon?q=$lat,$lon"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (t: Throwable) {
            EventLogger.error("Share", "Geo intent failed: ${t.message}")
        }
    }
}
