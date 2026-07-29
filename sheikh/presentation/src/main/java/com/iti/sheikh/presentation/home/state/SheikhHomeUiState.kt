package com.iti.sheikh.presentation.home.state

import androidx.annotation.StringRes

data class SheikhHomeUiState(
    val isLoading: Boolean = true,
    @StringRes val errorMessageRes: Int? = null,
    val initials: String? = null,
    val avatarUrl: String? = null,
) {
    val hasError: Boolean get() = errorMessageRes != null
}
