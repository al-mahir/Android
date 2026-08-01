package com.iti.sheikh.presentation.home.state

sealed interface SheikhHomeIntent {
    data object ProfileClicked : SheikhHomeIntent
    data object Retry : SheikhHomeIntent
    data object RejoinActiveCallClicked : SheikhHomeIntent
    data object DismissActiveCallClicked : SheikhHomeIntent
}
