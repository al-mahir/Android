package com.iti.domain.usecase.subscription

import com.iti.domain.core.Result
import com.iti.domain.model.Subscription
import com.iti.domain.repository.AlmahirRepository
import kotlinx.coroutines.flow.Flow

class GetSubscriptionUseCase(
    private val repository: AlmahirRepository,
) {
    operator fun invoke(): Flow<Result<Subscription>> = repository.observeSubscription()
}
