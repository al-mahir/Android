package com.iti.domain.usecase.sheikh

import com.iti.domain.core.Result
import com.iti.domain.model.SheikhAvailability
import com.iti.domain.repository.AlmahirSheikhRepository

class SetMyAvailabilityUseCase(
    private val repository: AlmahirSheikhRepository,
) {
    suspend operator fun invoke(availability: SheikhAvailability): Result<Unit> =
        repository.setMyAvailability(availability)
}
