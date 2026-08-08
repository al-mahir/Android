package com.iti.data.payment.dto

data class PaymentIntentionDto(
    val intentionId: String,
    val clientSecret: String,
    val amountMinorUnits: Long,
    val currencyCode: String,
)

data class PaymentOutcomeDto(
    val transactionId: String,
    val status: String,
    val failureReasonCode: String? = null,
)
