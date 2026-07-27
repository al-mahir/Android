package com.iti.domain.usecase.subscription

import com.iti.domain.core.Result
import com.iti.domain.repository.AlmahirRepository

class RestorePurchasesUseCase(
    private val repository: AlmahirRepository,
) {
    suspend operator fun invoke(): Result<Boolean> = repository.restorePurchases()
}
