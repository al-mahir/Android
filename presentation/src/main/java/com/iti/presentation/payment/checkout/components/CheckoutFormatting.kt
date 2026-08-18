package com.iti.presentation.payment.checkout.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.text.NumberFormat
import java.util.Currency

@Composable
internal fun rememberFormattedWholePrice(amount: Long, currencyCode: String): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(amount, currencyCode, locale) {
        val format = NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance(currencyCode)
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }
        format.format(amount)
    }
}
