package com.iti.presentation.home.state

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.iti.domain.model.AyahOfTheDay
import com.iti.domain.model.ReadingProgress
import com.iti.domain.model.Sheikh
import com.iti.domain.model.User
import com.iti.meeting.domain.model.ActiveCallRecord
import com.iti.meeting.domain.model.PendingMeetingRequest
import com.iti.meeting.domain.model.circle.Circle


@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    @StringRes val errorMessageRes: Int? = null,
    val user: User? = null,
    val readingProgress: ReadingProgress? = null,
    val ayahOfTheDay: AyahOfTheDay? = null,
    val sheikhs: List<Sheikh> = emptyList(),
    val myCircles: List<Circle> = emptyList(),
    val availableCircles: List<Circle> = emptyList(),
    val pendingMeetingRequest: PendingMeetingRequest? = null,
    val activeCall: ActiveCallRecord? = null,
    val isOffline: Boolean = false,
) {
    val hasError: Boolean get() = errorMessageRes != null
}
