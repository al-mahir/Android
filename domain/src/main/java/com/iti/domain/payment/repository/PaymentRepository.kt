package com.iti.domain.payment.repository

import com.iti.domain.core.Result
import com.iti.domain.payment.model.CardBrand
import com.iti.domain.payment.model.PaymentIntention
import com.iti.domain.payment.model.PaymentMethodType
import com.iti.domain.payment.model.PaymentOutcome
import com.iti.domain.payment.model.WalletProvider

interface PaymentRepository {

    suspend fun createIntention(packageId: String, method: PaymentMethodType): Result<PaymentIntention>

    suspend fun confirmWalletPayment(
        intentionId: String,
        walletProvider: WalletProvider,
        walletNumber: String,
    ): Result<PaymentOutcome>

    suspend fun confirmCardPayment(
        intentionId: String,
        cardBrand: CardBrand,
        cardNumber: String,
        expiry: String,
        cvv: String,
        cardholderName: String,
    ): Result<PaymentOutcome>
}
