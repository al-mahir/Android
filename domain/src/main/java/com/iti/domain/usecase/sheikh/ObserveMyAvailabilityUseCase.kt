package com.iti.domain.usecase.sheikh

import com.iti.domain.core.Result
import com.iti.domain.model.SheikhAvailability
import com.iti.domain.repository.AlmahirSheikhRepository
import kotlinx.coroutines.flow.Flow

class ObserveMyAvailabilityUseCase(
    private val repository: AlmahirSheikhRepository,
) {
    operator fun invoke(): Flow<Result<SheikhAvailability>> = repository.observeMyAvailability()
}
