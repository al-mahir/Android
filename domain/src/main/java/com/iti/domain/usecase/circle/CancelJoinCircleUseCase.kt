package com.iti.domain.usecase.circle

import com.iti.domain.core.Result
import com.iti.domain.repository.CircleRepository

class CancelJoinCircleUseCase(
    private val repository: CircleRepository,
) {
    suspend operator fun invoke(circleId: String): Result<Unit> {
        require(circleId.isNotBlank()) { "circleId must not be blank" }
        return repository.cancelJoinCircle(circleId)
    }
}
