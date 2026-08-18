package com.iti.data.payment.repository

import com.iti.data.core.error.toDomainError
import com.iti.data.payment.PaymentDataSource
import com.iti.data.payment.mapper.toDomain
import com.iti.domain.core.Result
import com.iti.domain.core.resultOf
import com.iti.domain.model.SubscriptionMinutes
import com.iti.domain.model.SubscriptionPackage
import com.iti.domain.payment.model.PaymentIntention
import com.iti.domain.payment.model.PaymentMethodType
import com.iti.domain.payment.model.PaymentOutcome
import com.iti.domain.payment.repository.PaymentRepository

class PaymentRepositoryImpl(
    private val dataSource: PaymentDataSource,
) : PaymentRepository {

    override suspend fun getPackages(): Result<List<SubscriptionPackage>> =
        resultOf(mapError = { it.toDomainError() }) {
            dataSource.getPackages().map { it.toDomain() }
        }

    override suspend fun getSubscriptionMinutes(): Result<SubscriptionMinutes?> =
        resultOf(mapError = { it.toDomainError() }) {
            dataSource.getSubscriptionMinutes()?.toDomain()
        }

    override suspend fun createIntention(
        packageId: String,
        method: PaymentMethodType,
        idempotencyKey: String,
    ): Result<PaymentIntention> = resultOf {
        dataSource.createIntention(packageId, method.name, idempotencyKey).toDomain()
    }

    override suspend fun getPaymentStatus(intentionId: String): Result<PaymentOutcome> = resultOf {
        dataSource.getPaymentStatus(intentionId).toDomain()
    }
}
