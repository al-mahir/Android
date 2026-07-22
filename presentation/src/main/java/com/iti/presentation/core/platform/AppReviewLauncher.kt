package com.iti.presentation.core.platform

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.core.net.toUri


interface AppReviewLauncher {
    fun requestReview(activity: Activity)
}


class StoreListingAppReviewLauncher(
    private val packageName: String,
) : AppReviewLauncher {

    override fun requestReview(activity: Activity) {
        val marketIntent = Intent(Intent.ACTION_VIEW, "$MARKET_URI$packageName".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            activity.startActivity(marketIntent)
        } catch (_: ActivityNotFoundException) {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, "$WEB_URI$packageName".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private companion object {
        const val MARKET_URI = "market://details?id="
        const val WEB_URI = "https://play.google.com/store/apps/details?id="
    }
}
