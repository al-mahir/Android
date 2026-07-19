package com.iti.domain.usecase.circle

import com.iti.domain.repository.AlmahirRepository

class JoinStudyCircleUseCase(
    private val repository: AlmahirRepository,
) {
    suspend operator fun invoke(circleId: String) {
        require(circleId.isNotBlank()) { "circleId must not be blank" }
        repository.joinStudyCircle(circleId)
    }
}
