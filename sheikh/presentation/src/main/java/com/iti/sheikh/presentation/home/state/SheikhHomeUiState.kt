package com.iti.sheikh.presentation.home.state

import androidx.annotation.StringRes
import com.iti.meeting.domain.model.ActiveCallRecord
import com.iti.meeting.domain.model.circle.Circle

data class SheikhHomeUiState(
    val isLoading: Boolean = true,
    val errorMessageRes: Int? = null,
    val initials: String? = null,
    val avatarUrl: String? = null,
    val activeCall: ActiveCallRecord? = null,
    val isOffline: Boolean = false,
    val myCircles: List<Circle> = emptyList(),
    val availableCircles: List<Circle> = emptyList(),
) {
    val hasError: Boolean get() = errorMessageRes != null
}
