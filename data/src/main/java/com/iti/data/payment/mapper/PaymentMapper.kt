package com.iti.data.payment.mapper

import com.iti.data.payment.dto.PaymentIntentionDto
import com.iti.data.payment.dto.PaymentOutcomeDto
import com.iti.data.payment.dto.SubscriptionMinutesDto
import com.iti.data.payment.dto.SubscriptionPackageDto
import com.iti.domain.model.SubscriptionMinutes
import com.iti.domain.model.SubscriptionPackage
import com.iti.domain.payment.model.PaymentIntention
import com.iti.domain.payment.model.PaymentOutcome
import com.iti.domain.payment.model.PaymentStatus
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

internal fun SubscriptionPackageDto.toDomain(): SubscriptionPackage = SubscriptionPackage(
    code = code,
    name = name,
    description = description?.takeIf { it.isNotBlank() },
    // Despite its name the wire field carries whole EGP, so it is passed through undivided.
    priceAmount = priceMinorUnits,
    currencyCode = currencyCode,
    meetingMinutesAllowed = meetingMinutesAllowed,
    durationDays = durationDays,
    features = features,
)

internal fun SubscriptionMinutesDto.toDomain(): SubscriptionMinutes = SubscriptionMinutes(
    packageName = packageName?.takeIf { it.isNotBlank() },
    // A null balance means "none left" — confirmed against the live endpoint, which pairs
    // `"remainingMinutes": null` with a 409 INSUFFICIENT_MINUTES on the very next request.
    // Negative values are clamped so the progress bar can never render nonsense.
    totalMinutes = (totalMinutes ?: 0).coerceAtLeast(0),
    remainingMinutes = (remainingMinutes ?: 0).coerceAtLeast(0),
    startedAtEpochMillis = startedAt.toEpochMillisOrNull(),
    expiresAtEpochMillis = expiresAt.toEpochMillisOrNull(),
)

/**
 * Parses an ISO-8601 timestamp. Accepts both offset-bearing forms (`...Z`, `...+02:00`) and the
 * bare local form some endpoints return (`2026-08-17T19:46:57.384`), which is read as UTC.
 * An unparseable or absent value becomes `null` rather than throwing — a malformed date must not
 * fail the whole entitlement lookup.
 */
private fun String?.toEpochMillisOrNull(): Long? {
    val raw = this?.trim().orEmpty()
    if (raw.isEmpty()) return null
    return runCatching { Instant.parse(raw).toEpochMilli() }
        .recoverCatching { LocalDateTime.parse(raw).toInstant(ZoneOffset.UTC).toEpochMilli() }
        .getOrNull()
}

internal fun PaymentIntentionDto.toDomain(): PaymentIntention = PaymentIntention(
    intentionId = intentionId,
    clientSecret = clientSecret,
    publicKey = publicKey,
    amountMinorUnits = amountMinorUnits,
    currencyCode = currencyCode,
)

internal fun PaymentOutcomeDto.toDomain(): PaymentOutcome = PaymentOutcome(
    transactionId = transactionId,
    status = status.toPaymentStatus(),
    failureReasonCode = failureReasonCode,
)

private fun String.toPaymentStatus(): PaymentStatus = when (this) {
    "SUCCESS" -> PaymentStatus.SUCCESS
    "PENDING" -> PaymentStatus.PENDING
    else -> PaymentStatus.FAILED
}
