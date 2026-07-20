package com.iti.presentation.core.platform

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.core.net.toUri

/**
 * Unwraps the [Activity] behind a composition's context. Compose hands out a `ContextWrapper`
 * chain, so a plain cast fails; anything needing an Activity (in-app review) must walk it.
 */
fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/**
 * Thin wrappers over the platform intents a screen can fire.
 *
 * Every function returns `false` instead of throwing when no app can handle the intent, so the
 * caller can surface a localized message rather than crashing on a device without a browser.
 */
object ExternalActions {

    /** Opens [url] in the user's browser. */
    fun openUrl(context: Context, url: String): Boolean =
        context.startSafely(
            Intent(Intent.ACTION_VIEW, url.toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )

    /**
     * Fires the system share sheet.
     *
     * @param chooserTitle localized chooser heading — some OEM sheets still show it.
     */
    fun shareText(
        context: Context,
        text: String,
        chooserTitle: String,
    ): Boolean {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        return context.startSafely(
            Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun Context.startSafely(intent: Intent): Boolean = try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
