package com.iti.data.payment.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
