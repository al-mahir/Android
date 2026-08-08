package com.iti.data.payment.repository

import com.iti.data.payment.PaymentDataSource
import com.iti.data.payment.mapper.toDomain
import com.iti.domain.core.Result
import com.iti.domain.core.resultOf
import com.iti.domain.payment.model.CardBrand
import com.iti.domain.payment.model.PaymentIntention
import com.iti.domain.payment.model.PaymentMethodType
import com.iti.domain.payment.model.PaymentOutcome
import com.iti.domain.payment.model.WalletProvider
import com.iti.domain.payment.repository.PaymentRepository

class PaymentRepositoryImpl(
    private val dataSource: PaymentDataSource,
) : PaymentRepository {

    override suspend fun createIntention(packageId: String, method: PaymentMethodType): Result<PaymentIntention> =
        resultOf { dataSource.createIntention(packageId, method.name).toDomain() }

    override suspend fun confirmWalletPayment(
        intentionId: String,
        walletProvider: WalletProvider,
        walletNumber: String,
    ): Result<PaymentOutcome> = resultOf {
        dataSource.confirmWalletPayment(intentionId, walletProvider.name, walletNumber).toDomain()
    }

    override suspend fun confirmCardPayment(
        intentionId: String,
        cardBrand: CardBrand,
        cardNumber: String,
        expiry: String,
        cvv: String,
        cardholderName: String,
    ): Result<PaymentOutcome> = resultOf {
        dataSource.confirmCardPayment(intentionId, cardBrand.name, cardNumber, expiry, cvv, cardholderName).toDomain()
    }
}
