package com.iti.meeting.presentation.navigation

import androidx.navigation3.runtime.NavKey

/**
 * `:meeting-kit`'s own destinations. [androidx.navigation3.runtime.NavKey] is a plain library
 * interface, so implementing it here does not couple this module to any host app's nav graph.
 */
sealed interface MeetingRoute : NavKey {
    data object CircleSearch : MeetingRoute
    data object CreateCircle : MeetingRoute
    data class CircleLobby(val circleId: String) : MeetingRoute
    data class CircleHost(val circleId: String) : MeetingRoute
    data class Call(
        val requestId: String,
        val token: String,
        val channelName: String,
        val userAccount: String,
    ) : MeetingRoute
}


