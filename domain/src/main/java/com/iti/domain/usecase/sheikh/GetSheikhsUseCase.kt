package com.iti.domain.usecase.sheikh

import com.iti.domain.core.Result
import com.iti.domain.core.map
import com.iti.domain.model.Sheikh
import com.iti.domain.model.SheikhAvailability
import com.iti.domain.repository.SheikhRepository

class GetSheikhsUseCase(
    private val repository: SheikhRepository,
) {
    suspend operator fun invoke(): Result<List<Sheikh>> =
        repository.getSheikhs().map { sheikhs ->
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
