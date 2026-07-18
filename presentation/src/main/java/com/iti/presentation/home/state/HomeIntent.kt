package com.iti.presentation.home.state

/** Every user interaction the Home screen can dispatch. */
sealed interface HomeIntent {
    data object Retry : HomeIntent
    data object SearchClicked : HomeIntent
    data object ProfileClicked : HomeIntent
    data object ContinueReadingClicked : HomeIntent
    data object SeeAllSheikhsClicked : HomeIntent
    data object SeeAllCirclesClicked : HomeIntent
    data class SheikhClicked(val sheikhId: String) : HomeIntent
    data class JoinCircleClicked(val circleId: String) : HomeIntent
}
