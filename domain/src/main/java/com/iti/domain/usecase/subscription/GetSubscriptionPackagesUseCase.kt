package com.iti.domain.usecase.subscription

import com.iti.domain.core.Result
import com.iti.domain.model.SubscriptionPackage
import com.iti.domain.repository.AlmahirRepository
import kotlinx.coroutines.flow.Flow

class GetSubscriptionPackagesUseCase(
    private val repository: AlmahirRepository,
) {
    operator fun invoke(): Flow<Result<List<SubscriptionPackage>>> =
        repository.observeSubscriptionPackages()
}
