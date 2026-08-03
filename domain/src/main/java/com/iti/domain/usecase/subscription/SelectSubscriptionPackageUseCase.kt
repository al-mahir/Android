package com.iti.domain.usecase.subscription

import com.iti.domain.core.Result
import com.iti.domain.model.Subscription
import com.iti.domain.repository.AlmahirRepository

class SelectSubscriptionPackageUseCase(
    private val repository: AlmahirRepository,
) {
    suspend operator fun invoke(packageId: String): Result<Subscription> {
        require(packageId.isNotBlank()) { "packageId must not be blank" }
        return repository.selectSubscriptionPackage(packageId)
    }
}
