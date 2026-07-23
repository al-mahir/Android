package com.example.mushaf.presentation.recite

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale


@Composable
internal fun currentLocale(): Locale {
    val locales = LocalConfiguration.current.locales
    return if (locales.isEmpty) Locale.ROOT else locales[0]
}
