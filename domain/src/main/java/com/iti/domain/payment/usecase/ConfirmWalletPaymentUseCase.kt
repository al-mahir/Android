package com.iti.domain.payment.usecase

import com.iti.domain.core.Result
import com.iti.domain.payment.model.PaymentOutcome
import com.iti.domain.payment.model.WalletProvider
import com.iti.domain.payment.repository.PaymentRepository


@Deprecated("Use GetPaymentStatusUseCase after the Paymob SDK callback instead.")
class ConfirmWalletPaymentUseCase(
    @Suppress("UNUSED_PARAMETER") private val repository: PaymentRepository,
) {
    @Deprecated("Use GetPaymentStatusUseCase after the Paymob SDK callback instead.")
    suspend operator fun invoke(
        intentionId: String,
        walletProvider: WalletProvider,
        walletNumber: String,
    ): Result<PaymentOutcome> {
        throw UnsupportedOperationException(
            "Wallet confirmation is handled by the Paymob SDK. Use GetPaymentStatusUseCase."
        )
    }
}
