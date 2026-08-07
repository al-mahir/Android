package com.iti.presentation.subscription.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Date

@Composable
internal fun rememberFormattedDate(epochMillis: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(epochMillis, locale) {
        DateFormat.getDateInstance(DateFormat.SHORT, locale).format(Date(epochMillis))
    }
}

@Composable
internal fun rememberFormattedWholePrice(minorUnits: Long, currencyCode: String): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(minorUnits, currencyCode, locale) {
        val format = NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance(currencyCode)
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }
        format.format(minorUnits / 100.0)
    }
}
