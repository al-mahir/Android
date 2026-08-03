package com.iti.domain.usecase.subscription

import com.iti.domain.core.Result
import com.iti.domain.model.Subscription
import com.iti.domain.repository.AlmahirRepository

class StartFreeTrialUseCase(
    private val repository: AlmahirRepository,
) {
    suspend operator fun invoke(): Result<Subscription> = repository.startFreeTrial()
}
