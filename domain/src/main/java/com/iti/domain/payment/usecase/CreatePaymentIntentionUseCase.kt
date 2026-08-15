package com.iti.domain.payment.usecase

import com.iti.domain.core.Result
import com.iti.domain.payment.model.PaymentIntention
import com.iti.domain.payment.model.PaymentMethodType
import com.iti.domain.payment.repository.PaymentRepository

class CreatePaymentIntentionUseCase(
    private val repository: PaymentRepository,
) {
    suspend operator fun invoke(
        packageId: String,
        method: PaymentMethodType,
        idempotencyKey: String,
    ): Result<PaymentIntention> {
        require(packageId.isNotBlank()) { "packageId must not be blank" }
        require(idempotencyKey.isNotBlank()) { "idempotencyKey must not be blank" }
        return repository.createIntention(packageId, method, idempotencyKey)
    }
}
