package com.iti.sheikh.presentation.home.state

import androidx.annotation.StringRes
import com.iti.meeting.domain.model.ActiveCallRecord

data class SheikhHomeUiState(
    val isLoading: Boolean = true,
    val errorMessageRes: Int? = null,
    val initials: String? = null,
    val avatarUrl: String? = null,
    val activeCall: ActiveCallRecord? = null,
    val isOffline: Boolean = false,
) {
    val hasError: Boolean get() = errorMessageRes != null
}
