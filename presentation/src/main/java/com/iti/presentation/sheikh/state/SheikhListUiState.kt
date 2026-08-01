package com.iti.presentation.sheikh.state

import com.iti.domain.model.Sheikh
import com.iti.domain.model.SheikhAvailability

data class SheikhListUiState(
    val sheikhs: List<Sheikh> = emptyList(),
    val filteredSheikhs: List<Sheikh> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: SheikhFilter = SheikhFilter.ALL,
    val isLoading: Boolean = true,
    val isError: Boolean = false,
)

enum class SheikhFilter { ALL, AVAILABLE, BUSY }

fun SheikhFilter.matches(availability: SheikhAvailability): Boolean = when (this) {
    SheikhFilter.ALL -> true
    SheikhFilter.AVAILABLE -> availability == SheikhAvailability.AVAILABLE
    SheikhFilter.BUSY -> availability == SheikhAvailability.IN_SESSION
}
