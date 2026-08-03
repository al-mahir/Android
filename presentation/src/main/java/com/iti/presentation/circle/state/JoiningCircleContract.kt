package com.iti.presentation.circle.state

import androidx.annotation.StringRes
import com.iti.meeting.domain.model.circle.Circle

data class JoiningCircleUiState(
    val circle: Circle? = null,
    val isLoading: Boolean = true,
)

sealed interface JoiningCircleIntent {
    data object CancelRequest : JoiningCircleIntent
}

sealed interface JoiningCircleEffect {
    data object NavigateBack : JoiningCircleEffect
    data class NavigateToSession(val circleId: String) : JoiningCircleEffect
    data class ShowMessage(@StringRes val messageRes: Int) : JoiningCircleEffect
}
