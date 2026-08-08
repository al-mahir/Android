package com.iti.presentation.payment.checkout.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.text.NumberFormat
import java.util.Currency

@Composable
internal fun rememberFormattedWholePrice(minorUnits: Long, currencyCode: String): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(minorUnits, currencyCode, locale) {
        val format = NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance(currencyCode)
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        format.format(minorUnits / 100.0)
    }
}
