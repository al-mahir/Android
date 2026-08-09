package com.iti.data.payment

import com.iti.data.payment.dto.PaymentIntentionDto
import com.iti.data.payment.dto.PaymentOutcomeDto

interface PaymentDataSource {

    suspend fun createIntention(packageId: String, method: String): PaymentIntentionDto

    suspend fun confirmWalletPayment(
        intentionId: String,
        walletProvider: String,
        walletNumber: String,
    ): PaymentOutcomeDto

    suspend fun confirmCardPayment(
        intentionId: String,
        cardBrand: String,
        cardNumber: String,
        expiry: String,
        cvv: String,
        cardholderName: String,
    ): PaymentOutcomeDto
}
