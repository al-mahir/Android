package com.iti.domain.payment.usecase

import com.iti.domain.core.Result
import com.iti.domain.model.Subscription
import com.iti.domain.usecase.subscription.SelectSubscriptionPackageUseCase


class ActivateSubscriptionAfterPaymentUseCase(
    private val selectSubscriptionPackage: SelectSubscriptionPackageUseCase,
) {
    suspend operator fun invoke(packageId: String): Result<Subscription> =
        selectSubscriptionPackage(packageId)
}
