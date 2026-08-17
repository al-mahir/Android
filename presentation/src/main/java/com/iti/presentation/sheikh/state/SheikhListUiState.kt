package com.iti.presentation.sheikh.state

import com.iti.domain.model.Sheikh
import com.iti.domain.model.SheikhAvailability

data class SheikhListUiState(
    val sheikhs: List<Sheikh> = emptyList(),
    val filteredSheikhs: List<Sheikh> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: SheikhFilter = SheikhFilter.ALL,
    val isLoading: Boolean = true,
    /** A user-initiated swipe-to-refresh is in flight. Distinct from [isLoading]: the list stays
     * on screen and only the pull indicator spins. */
    val isRefreshing: Boolean = false,
    val isError: Boolean = false,
    val bookmarkedSheikhIds: Set<String> = emptySet(),
)

enum class SheikhFilter { ALL, AVAILABLE, BUSY }

fun SheikhFilter.matches(availability: SheikhAvailability): Boolean = when (this) {
    SheikhFilter.ALL -> true
    SheikhFilter.AVAILABLE -> availability == SheikhAvailability.AVAILABLE
    SheikhFilter.BUSY -> availability == SheikhAvailability.IN_SESSION
}
