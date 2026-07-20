package com.example.mushaf.presentation.download.components

import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.example.mushaf.domain.model.LocalizedText


@Composable
fun LocalizedText.resolve(): String {
    val configuration = LocalConfiguration.current
    val language = configuration.locales[0].language
    return if (language == ARABIC_LANGUAGE) arabic else english
}


@Composable
fun rememberFormattedSize(sizeBytes: Long): String {
    val context = LocalContext.current
    return remember(context, sizeBytes) { Formatter.formatFileSize(context, sizeBytes) }
}

private const val ARABIC_LANGUAGE = "ar"
