package com.iti.presentation.home.state

sealed interface HomeIntent {
    data object Retry : HomeIntent
    data object SearchClicked : HomeIntent
    data object ProfileClicked : HomeIntent
    data object ContinueReadingClicked : HomeIntent
    data object SeeAllSheikhsClicked : HomeIntent
    data object SeeAllCirclesClicked : HomeIntent
    data class SheikhClicked(val sheikhId: String) : HomeIntent
    data class CircleClicked(val circleId: String) : HomeIntent
    data object ViewPendingMeetingClicked : HomeIntent
    data object CancelPendingMeetingClicked : HomeIntent
    data object RejoinActiveCallClicked : HomeIntent
    data object DismissActiveCallClicked : HomeIntent
    data object StartExamClicked : HomeIntent
}
