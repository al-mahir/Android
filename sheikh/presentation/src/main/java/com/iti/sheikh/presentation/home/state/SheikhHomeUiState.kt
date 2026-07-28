package com.iti.sheikh.presentation.home.state

import androidx.annotation.StringRes
import com.iti.domain.model.SheikhAvailability

data class SheikhHomeUiState(
    val isLoading: Boolean = true,
    @StringRes val errorMessageRes: Int? = null,
    val initials: String? = null,
    val avatarUrl: String? = null,
    val availability: SheikhAvailability = SheikhAvailability.OFFLINE,
    val isUpdatingAvailability: Boolean = false,
) {
    val hasError: Boolean get() = errorMessageRes != null

    /** A sheikh can't manually toggle themselves out of a live session. */
    val isAvailabilityToggleEnabled: Boolean
        get() = !isUpdatingAvailability && availability != SheikhAvailability.IN_SESSION
}
