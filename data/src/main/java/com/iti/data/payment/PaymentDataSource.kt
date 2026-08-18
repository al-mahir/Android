package com.iti.data.payment

import com.iti.data.payment.dto.PaymentIntentionDto
import com.iti.data.payment.dto.PaymentOutcomeDto
import com.iti.data.payment.dto.SubscriptionMinutesDto
import com.iti.data.payment.dto.SubscriptionPackageDto

interface PaymentDataSource {

    suspend fun getPackages(): List<SubscriptionPackageDto>

    /** `null` when the student has no subscription on record. */
    suspend fun getSubscriptionMinutes(): SubscriptionMinutesDto?

    suspend fun createIntention(
        packageId: String,
        method: String,
        idempotencyKey: String,
    ): PaymentIntentionDto

    suspend fun getPaymentStatus(intentionId: String): PaymentOutcomeDto
}
