package com.example.designsystem.theme

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.LayoutDirection
import com.example.core.designsystem.locale.localizedContext
import com.example.designsystem.color.ColorScheme
import com.example.designsystem.color.darkColors
import com.example.designsystem.color.lightColors
import com.example.designsystem.color.localSPColorScheme
import com.example.designsystem.dimensions.LocalSPShapes
import com.example.designsystem.dimensions.LocalSPSpacing
import com.example.designsystem.dimensions.SPShapes
import com.example.designsystem.dimensions.SPSpacing
import com.example.designsystem.typo.LocalSPFontFamily
import com.example.designsystem.typo.LocalSPTypography
import com.example.designsystem.typo.SPTextStyle
import com.example.designsystem.typo.arabicFontFamily
import com.example.designsystem.typo.defaultSPTypographyForLanguage
import com.example.designsystem.typo.spTypographyOf
import java.util.Locale

@Composable
fun AlMahirTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    locale: Locale = Locale.getDefault(),
    colors: ColorScheme = if (isDarkTheme) darkColors else lightColors,
    fontFamily: FontFamily? = null,
    typography: SPTextStyle = fontFamily
        ?.let(::spTypographyOf)
        ?: defaultSPTypographyForLanguage(locale.language),
    spacing: SPSpacing = SPSpacing(),
    shapes: SPShapes = SPShapes(),
    content: @Composable () -> Unit,
) {
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current

    val (localizedContext, localizedConfiguration) = remember(locale, baseContext) {
        val ctx = baseContext.localizedContext(locale)
        val cfg = Configuration(baseConfiguration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        ctx to cfg
    }

    val layoutDirection = remember(locale) {
        if (isRtlLocale(locale)) LayoutDirection.Rtl else LayoutDirection.Ltr
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedConfiguration,
        LocalLayoutDirection provides layoutDirection,
        localSPColorScheme provides colors,
        LocalSPTypography provides typography,
        LocalSPFontFamily provides (fontFamily ?: arabicFontFamily),
        LocalSPSpacing provides spacing,
        LocalSPShapes provides shapes,
        content = content,
    )
}

private fun isRtlLocale(locale: Locale): Boolean =
    when (locale.language) {
        "ar", "fa", "he", "iw", "ur" -> true
        else -> false
    }