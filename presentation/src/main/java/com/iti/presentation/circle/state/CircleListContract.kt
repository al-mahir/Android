package com.iti.presentation.circle.state

import androidx.annotation.StringRes
import com.iti.meeting.domain.model.circle.Circle
import com.iti.meeting.domain.model.circle.CircleStatus

data class CircleListUiState(
    val circles: List<Circle> = emptyList(),
    val myCircles: List<Circle> = emptyList(),
    val filteredCircles: List<Circle> = emptyList(),
    val searchQuery: String = "",
    val selectedStatus: CircleStatus? = null,
    val joinedCircleIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val joinSheetVisible: Boolean = false,
    val joinCircleId: String = "",
    val joinPassword: String = "",
    val isJoining: Boolean = false,
    @StringRes val joinErrorRes: Int? = null,
) {
    /** The circle the current user belongs to, surfaced at the top of the list. */
    val currentCircle: Circle?
        get() = myCircles.firstOrNull { it.status == CircleStatus.ONGOING } ?: myCircles.firstOrNull()
}

sealed interface CircleListIntent {
    data class SearchQueryChanged(val query: String) : CircleListIntent
    data class StatusSelected(val status: CircleStatus?) : CircleListIntent
    data class CircleClicked(val circleId: String) : CircleListIntent
    data object CreateCircleClicked : CircleListIntent
    data object JoinPrivateClicked : CircleListIntent
    data class JoinCircleIdChanged(val circleId: String) : CircleListIntent
    data class JoinPasswordChanged(val password: String) : CircleListIntent
    data object SubmitJoinPrivate : CircleListIntent
    data object DismissJoinPrivate : CircleListIntent
    data object Retry : CircleListIntent
}

sealed interface CircleListEffect {
    data class OpenCircle(val circleId: String) : CircleListEffect
    data object OpenCreateCircle : CircleListEffect
    data object NavigateBack : CircleListEffect
    data class ShowMessage(@StringRes val messageRes: Int) : CircleListEffect
}
