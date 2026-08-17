package com.iti.domain.payment.repository

import com.iti.domain.core.Result
import com.iti.domain.model.SubscriptionMinutes
import com.iti.domain.model.SubscriptionPackage
import com.iti.domain.payment.model.PaymentIntention
import com.iti.domain.payment.model.PaymentMethodType
import com.iti.domain.payment.model.PaymentOutcome

interface PaymentRepository {

    /** Public catalogue of active, purchasable packages. */
    suspend fun getPackages(): Result<List<SubscriptionPackage>>

    /** The signed-in student's entitlement; `Success(null)` when they have no subscription. */
    suspend fun getSubscriptionMinutes(): Result<SubscriptionMinutes?>

    suspend fun createIntention(
        packageId: String,
        method: PaymentMethodType,
        idempotencyKey: String,
    ): Result<PaymentIntention>

    suspend fun getPaymentStatus(intentionId: String): Result<PaymentOutcome>
}
