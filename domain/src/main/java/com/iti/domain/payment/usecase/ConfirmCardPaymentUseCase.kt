package com.iti.domain.payment.usecase

import com.iti.domain.core.Result
import com.iti.domain.payment.model.CardBrand
import com.iti.domain.payment.model.PaymentOutcome
import com.iti.domain.payment.repository.PaymentRepository

class ConfirmCardPaymentUseCase(
    private val repository: PaymentRepository,
) {
    suspend operator fun invoke(
        intentionId: String,
        cardBrand: CardBrand,
        cardNumber: String,
        expiry: String,
        cvv: String,
        cardholderName: String,
    ): Result<PaymentOutcome> {
        require(intentionId.isNotBlank()) { "intentionId must not be blank" }
        return repository.confirmCardPayment(intentionId, cardBrand, cardNumber, expiry, cvv, cardholderName)
    }
}
