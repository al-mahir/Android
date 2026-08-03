package com.iti.sheikh.presentation.circle.state

import androidx.annotation.StringRes
import com.iti.meeting.domain.model.circle.CircleType

data class SheikhCreateCircleUiState(
    val name: String = "",
    val type: CircleType = CircleType.PUBLIC,
    val requiresApproval: Boolean = false,
    val maxParticipants: String = "10",
    val password: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val isSubmitting: Boolean = false,
    @StringRes val errorMessageRes: Int? = null,
)

sealed interface SheikhCreateCircleIntent {
    data class NameChanged(val name: String) : SheikhCreateCircleIntent
    data class TypeSelected(val type: CircleType) : SheikhCreateCircleIntent
    data class RequiresApprovalChanged(val value: Boolean) : SheikhCreateCircleIntent
    data class MaxParticipantsChanged(val value: String) : SheikhCreateCircleIntent
    data class PasswordChanged(val password: String) : SheikhCreateCircleIntent
    data class StartDateChanged(val value: String) : SheikhCreateCircleIntent
    data class EndDateChanged(val value: String) : SheikhCreateCircleIntent
    data object Submit : SheikhCreateCircleIntent
}

sealed interface SheikhCreateCircleEffect {
    data class CircleCreated(val circleId: String) : SheikhCreateCircleEffect
    data class ShowMessage(@StringRes val messageRes: Int) : SheikhCreateCircleEffect
}
