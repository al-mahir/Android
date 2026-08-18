package com.iti.domain.usecase.subscription

import com.iti.domain.core.Result
import com.iti.domain.model.SubscriptionPackage
import com.iti.domain.payment.repository.PaymentRepository

/**
 * Loads the purchasable package catalogue. One-shot rather than a [kotlinx.coroutines.flow.Flow]:
 * the catalogue is a plain REST GET with no push channel, so callers refresh explicitly.
 */
class GetSubscriptionPackagesUseCase(
    private val repository: PaymentRepository,
) {
    suspend operator fun invoke(): Result<List<SubscriptionPackage>> = repository.getPackages()
}
