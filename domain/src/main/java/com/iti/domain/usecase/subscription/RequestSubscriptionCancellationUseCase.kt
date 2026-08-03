package com.iti.domain.usecase.subscription

import com.iti.domain.core.Result
import com.iti.domain.repository.AlmahirRepository

class RequestSubscriptionCancellationUseCase(
    private val repository: AlmahirRepository,
) {
    suspend operator fun invoke(message: String): Result<Unit> {
        require(message.isNotBlank()) { "message must not be blank" }
        return repository.requestSubscriptionCancellation(message)
    }
}
