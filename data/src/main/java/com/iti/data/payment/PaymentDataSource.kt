package com.iti.data.payment

import com.iti.data.payment.dto.PaymentIntentionDto
import com.iti.data.payment.dto.PaymentOutcomeDto

interface PaymentDataSource {

    suspend fun createIntention(
        packageId: String,
        method: String,
        idempotencyKey: String,
    ): PaymentIntentionDto

    suspend fun getPaymentStatus(intentionId: String): PaymentOutcomeDto
}
