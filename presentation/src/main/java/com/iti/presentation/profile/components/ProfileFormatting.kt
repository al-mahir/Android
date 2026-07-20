package com.iti.presentation.profile.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.text.DateFormat
import java.util.Date


@Composable
internal fun rememberFormattedDate(epochMillis: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(epochMillis, locale) {
        DateFormat.getDateInstance(DateFormat.SHORT, locale).format(Date(epochMillis))
    }
}
