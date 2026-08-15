package com.iti.data.payment.mapper

import com.iti.data.payment.dto.PaymentIntentionDto
import com.iti.data.payment.dto.PaymentOutcomeDto
import com.iti.domain.payment.model.PaymentIntention
import com.iti.domain.payment.model.PaymentOutcome
import com.iti.domain.payment.model.PaymentStatus

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
