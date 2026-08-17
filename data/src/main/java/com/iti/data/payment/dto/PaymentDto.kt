package com.iti.data.payment.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One entry of `GET /api/payment/packages`. The backend identifies a package by its [code] —
 * that is the value sent back as `packageId` when creating a payment intention.
 *
 * Everything except [code], [name] and the price is defaulted: the endpoint is public and may
 * grow fields, and a package that ships without a description or features must still render.
 */
@Serializable
data class SubscriptionPackageDto(
    @SerialName("code") val code: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("priceMinorUnits") val priceMinorUnits: Long,
    @SerialName("currencyCode") val currencyCode: String = "EGP",
    @SerialName("meetingMinutesAllowed") val meetingMinutesAllowed: Int = 0,
    @SerialName("durationDays") val durationDays: Int = 0,
    @SerialName("features") val features: List<String> = emptyList(),
)

/**
 * `data` payload of `GET /api/students/me/subscription-minutes`.
 *
 * Timestamps are ISO-8601 instants (e.g. `2026-08-17T19:46:57.384Z`); they are parsed into epoch
 * millis by the mapper so the domain stays free of a date library choice.
 */
@Serializable
data class SubscriptionMinutesDto(
    @SerialName("packageName") val packageName: String? = null,
    // Nullable, not defaulted: kotlinx applies a default only when the key is ABSENT, so an
    // explicit `"remainingMinutes": null` — which this endpoint really sends — would throw
    // JsonConvertException and fail the whole entitlement lookup.
    @SerialName("totalMinutes") val totalMinutes: Int? = null,
    @SerialName("remainingMinutes") val remainingMinutes: Int? = null,
    @SerialName("startedAt") val startedAt: String? = null,
    @SerialName("expiresAt") val expiresAt: String? = null,
)

@Serializable
data class CreateIntentionRequest(
    @SerialName("packageId") val packageId: String,
    @SerialName("method") val method: String,
    @SerialName("idempotencyKey") val idempotencyKey: String,
)

@Serializable
data class PaymentIntentionDto(
    @SerialName("intentionId") val intentionId: String,
    @SerialName("clientSecret") val clientSecret: String,
    @SerialName("publicKey") val publicKey: String,
    @SerialName("amountMinorUnits") val amountMinorUnits: Long,
    @SerialName("currencyCode") val currencyCode: String,
    @SerialName("expiresAt") val expiresAt: String? = null,
)

@Serializable
data class PaymentOutcomeDto(
    @SerialName("status") val status: String,
    @SerialName("transactionId") val transactionId: String? = null,
    @SerialName("failureReasonCode") val failureReasonCode: String? = null,
)
