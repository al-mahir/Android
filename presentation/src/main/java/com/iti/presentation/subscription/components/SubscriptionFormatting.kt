package com.iti.presentation.subscription.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.iti.presentation.R
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Date

/**
 * Billing period suffix for a package price. The backend sends the raw [durationDays], so a
 * 30-day package reads "/month" and a 365-day one "/year" instead of an unhelpful day count.
 * Returns an empty string when the backend omits the duration — the price then stands alone
 * rather than claiming a period we don't know.
 */
@Composable
internal fun billingPeriodLabel(durationDays: Int): String = when {
    durationDays <= 0 -> ""
    durationDays in MONTH_DAYS_RANGE -> stringResource(R.string.packages_price_period_month)
    durationDays in YEAR_DAYS_RANGE -> stringResource(R.string.packages_price_period_year)
    durationDays % DAYS_PER_WEEK == 0 ->
        pluralStringResource(R.plurals.packages_price_period_weeks, durationDays / DAYS_PER_WEEK, durationDays / DAYS_PER_WEEK)
    else -> pluralStringResource(R.plurals.packages_price_period_days, durationDays, durationDays)
}

/** Included live-session allowance, rendered in hours once it passes an hour. */
@Composable
internal fun meetingAllowanceLabel(minutes: Int): String? = when {
    minutes <= 0 -> null
    minutes % MINUTES_PER_HOUR == 0 ->
        pluralStringResource(R.plurals.packages_meeting_hours, minutes / MINUTES_PER_HOUR, minutes / MINUTES_PER_HOUR)
    else -> pluralStringResource(R.plurals.packages_meeting_minutes, minutes, minutes)
}

private val MONTH_DAYS_RANGE = 28..31
private val YEAR_DAYS_RANGE = 360..366
private const val DAYS_PER_WEEK = 7
private const val MINUTES_PER_HOUR = 60

@Composable
internal fun rememberFormattedDate(epochMillis: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(epochMillis, locale) {
        DateFormat.getDateInstance(DateFormat.SHORT, locale).format(Date(epochMillis))
    }
}

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
