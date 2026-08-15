package com.iti.domain.payment.usecase

import com.iti.domain.core.Result
import com.iti.domain.payment.model.PaymentOutcome
import com.iti.domain.payment.repository.PaymentRepository

class GetPaymentStatusUseCase(
    private val repository: PaymentRepository,
) {
    suspend operator fun invoke(intentionId: String): Result<PaymentOutcome> {
        require(intentionId.isNotBlank()) { "intentionId must not be blank" }
        return repository.getPaymentStatus(intentionId)
    }
}
