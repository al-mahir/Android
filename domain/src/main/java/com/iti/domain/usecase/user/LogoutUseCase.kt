package com.iti.domain.usecase.user

import com.iti.domain.repository.AlmahirRepository

class LogoutUseCase(
    private val repository: AlmahirRepository,
) {
    suspend operator fun invoke() = repository.logout()
}
