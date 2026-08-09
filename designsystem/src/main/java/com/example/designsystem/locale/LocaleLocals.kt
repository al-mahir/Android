package com.example.core.designsystem.locale

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * The locale-aware composition locals of the composition that created it.
 *
 * Why this exists: `Popup`, `Dialog` and `ModalBottomSheet` each render into their own
 * `ComposeView`, and every ComposeView root re-provides [LocalContext], [LocalConfiguration] and
 * [LocalLayoutDirection] from its own owner view's context. That silently discards the localized
 * context `AlMahirTheme` installs at the app root, so `stringResource` inside a popup resolves
 * against the *system* locale rather than the language the user picked — an app in Arabic shows
 * stray English inside menus and sheets.
 *
 * Capture with [rememberLocaleLocals] outside the popup, then re-apply with [Provide] inside it:
 *
 * ```
 * val localeLocals = rememberLocaleLocals()
 * DropdownMenu(...) {
 *     localeLocals.Provide { /* stringResource here resolves in the app's locale */ }
 * }
 * ```
 */
@Immutable
class LocaleLocals internal constructor(
    private val context: Context,
    private val configuration: Configuration,
    private val layoutDirection: LayoutDirection,
) {
    @Composable
    fun Provide(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalContext provides context,
            LocalConfiguration provides configuration,
            LocalLayoutDirection provides layoutDirection,
            content = content,
        )
    }
}

/**
 * True when the UI is currently presenting in Arabic.
 *
 * For text that comes from *resources* you never need this — the resource system picks the locale
 * for you. It is for data-provided text that ships in both languages (surah names, reciter names),
 * where the caller has to choose. Reads the composition's locale, so it follows the in-app language
 * `AlMahirTheme` installs rather than the device setting.
 */
@Composable
fun isArabicLocale(): Boolean {
    val layoutDirection = LocalLayoutDirection.current
    val locale = LocalConfiguration.current.locales[0]
    return layoutDirection == LayoutDirection.Rtl || locale?.language == "ar"
}

/** Captures the calling composition's locale-aware locals. Call *outside* the popup. */
@Composable
fun rememberLocaleLocals(): LocaleLocals {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val layoutDirection = LocalLayoutDirection.current
    return remember(context, configuration, layoutDirection) {
        LocaleLocals(context, configuration, layoutDirection)
    }
}
