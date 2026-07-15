package com.example.core.designsystem.locale

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/**
 * Returns a [Context] whose resources resolve in the given [locale]
 * without mutating the global app configuration.
 *
 * Why a wrapper: callers further down the tree may invoke
 * [Context.createConfigurationContext] themselves; we need their
 * configuration to inherit the locale we just applied.
 */
fun Context.localizedContext(locale: Locale): Context {
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    config.setLayoutDirection(locale)
    val localized = createConfigurationContext(config)
    return object : ContextWrapper(this) {
        override fun getResources(): Resources = localized.resources
        override fun getAssets(): AssetManager = localized.assets
        override fun getSystemService(name: String): Any? = localized.getSystemService(name)
        override fun createConfigurationContext(overrideConfiguration: Configuration): Context =
            localized.createConfigurationContext(overrideConfiguration)
    }
}
