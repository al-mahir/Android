package com.iti.domain.usecase.user

import com.iti.domain.repository.AlmahirRepository


class DeleteAccountUseCase(
    private val repository: AlmahirRepository,
) {
    suspend operator fun invoke() = repository.deleteAccount()
}
