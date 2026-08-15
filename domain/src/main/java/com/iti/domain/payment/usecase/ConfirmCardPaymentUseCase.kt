package com.iti.domain.payment.usecase

import com.iti.domain.core.Result
import com.iti.domain.payment.model.CardBrand
import com.iti.domain.payment.model.PaymentOutcome
import com.iti.domain.payment.repository.PaymentRepository

@Deprecated("Use GetPaymentStatusUseCase after the Paymob SDK callback instead.")
class ConfirmCardPaymentUseCase(
    @Suppress("UNUSED_PARAMETER") private val repository: PaymentRepository,
) {
    @Deprecated("Use GetPaymentStatusUseCase after the Paymob SDK callback instead.")
    suspend operator fun invoke(
        intentionId: String,
        cardBrand: CardBrand,
        cardNumber: String,
        expiry: String,
        cvv: String,
        cardholderName: String,
    ): Result<PaymentOutcome> {
        throw UnsupportedOperationException(
            "Card confirmation is handled by the Paymob SDK. Use GetPaymentStatusUseCase."
        )
    }
}
