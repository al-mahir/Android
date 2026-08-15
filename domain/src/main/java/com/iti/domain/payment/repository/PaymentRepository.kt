package com.iti.domain.payment.repository

import com.iti.domain.core.Result
import com.iti.domain.payment.model.PaymentIntention
import com.iti.domain.payment.model.PaymentMethodType
import com.iti.domain.payment.model.PaymentOutcome

interface PaymentRepository {

    suspend fun createIntention(
        packageId: String,
        method: PaymentMethodType,
        idempotencyKey: String,
    ): Result<PaymentIntention>

    suspend fun getPaymentStatus(intentionId: String): Result<PaymentOutcome>
}
