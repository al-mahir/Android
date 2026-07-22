package com.iti.domain.usecase.subscription

import com.iti.domain.repository.AlmahirRepository

class RestorePurchasesUseCase(
    private val repository: AlmahirRepository,
) {
    suspend operator fun invoke(): Boolean = repository.restorePurchases()
}
