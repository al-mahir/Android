package com.iti.domain.payment.usecase

import com.iti.domain.core.Result
import com.iti.domain.payment.model.PaymentOutcome
import com.iti.domain.payment.model.WalletProvider
import com.iti.domain.payment.repository.PaymentRepository

class ConfirmWalletPaymentUseCase(
    private val repository: PaymentRepository,
) {
    suspend operator fun invoke(
        intentionId: String,
        walletProvider: WalletProvider,
        walletNumber: String,
    ): Result<PaymentOutcome> {
        require(intentionId.isNotBlank()) { "intentionId must not be blank" }
        return repository.confirmWalletPayment(intentionId, walletProvider, walletNumber)
    }
}
