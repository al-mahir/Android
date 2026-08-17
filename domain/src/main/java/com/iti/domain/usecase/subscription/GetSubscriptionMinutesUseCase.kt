package com.iti.domain.usecase.subscription

import com.iti.domain.core.Result
import com.iti.domain.model.SubscriptionMinutes
import com.iti.domain.payment.repository.PaymentRepository

/** Loads the student's remaining meeting minutes. `null` data means "no subscription". */
class GetSubscriptionMinutesUseCase(
    private val repository: PaymentRepository,
) {
    suspend operator fun invoke(): Result<SubscriptionMinutes?> = repository.getSubscriptionMinutes()
}
