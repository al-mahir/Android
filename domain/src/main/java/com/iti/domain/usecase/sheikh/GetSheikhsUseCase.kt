package com.iti.domain.usecase.sheikh

import com.iti.domain.model.Sheikh
import com.iti.domain.model.SheikhAvailability
import com.iti.domain.repository.AlmahirRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class GetSheikhsUseCase(
    private val repository: AlmahirRepository,
) {
    operator fun invoke(): Flow<List<Sheikh>> =
        repository.observeSheikhs().map { sheikhs ->
            sheikhs.sortedWith(
                compareBy<Sheikh> { it.availability.rank() }.thenByDescending { it.rating },
            )
        }

    private fun SheikhAvailability.rank(): Int = when (this) {
        SheikhAvailability.AVAILABLE -> 0
        SheikhAvailability.IN_SESSION -> 1
        SheikhAvailability.OFFLINE -> 2
    }
}
