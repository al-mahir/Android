package com.example.mushaf.presentation.download.components

import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.mushaf.domain.model.LocalizedText
import com.example.mushaf.presentation.R


@Composable
fun LocalizedText.resolve(): String {
    val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
    val configLocale = LocalConfiguration.current.locales[0]
    val isArabic = layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl || 
                   configLocale?.language == ARABIC_LANGUAGE
    return if (isArabic) arabic else english
}

@Composable
fun rememberFormattedSize(sizeBytes: Long): String {
    val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
    val configLocale = LocalConfiguration.current.locales[0]
    val isArabic = layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl || 
                   configLocale?.language == ARABIC_LANGUAGE
    val locale = if (isArabic) java.util.Locale("ar") else (configLocale ?: java.util.Locale.ENGLISH)
    
    val sizeInGb = sizeBytes / (1024f * 1024f * 1024f)
    val sizeInMb = sizeBytes / (1024f * 1024f)
    
    val (valueToFormat, stringResId) = if (sizeBytes >= 1024 * 1024 * 1024L) {
        sizeInGb to R.string.downloads_size_gb
    } else {
        sizeInMb to R.string.downloads_size_mb
    }
    
    val formattedNumber = remember(valueToFormat, locale) {
        java.text.NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 }.format(valueToFormat)
    }
    return stringResource(stringResId, formattedNumber)
}

private const val ARABIC_LANGUAGE = "ar"
