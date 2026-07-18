package com.iti.domain.usecase.home

import com.iti.domain.repository.AlmahirRepository

class JoinCircleUseCase(
    private val repository: AlmahirRepository,
) {
    suspend operator fun invoke(circleId: String) {
        require(circleId.isNotBlank()) { "circleId must not be blank" }
        repository.joinCircle(circleId)
    }
}
