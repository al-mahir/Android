package com.iti.domain.usecase.sheikh

import com.iti.domain.core.Result
import com.iti.domain.model.Sheikh
import com.iti.domain.repository.SheikhRepository

class GetSheikhByIdUseCase(
    private val repository: SheikhRepository,
) {
    suspend operator fun invoke(sheikhId: String): Result<Sheikh?> =
        repository.getSheikhById(sheikhId)
}
