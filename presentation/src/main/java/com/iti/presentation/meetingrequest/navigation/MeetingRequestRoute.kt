package com.iti.presentation.meetingrequest.navigation

import androidx.navigation3.runtime.NavKey

sealed interface MeetingRequestRoute : NavKey {
    data object SheikhBrowseList : MeetingRequestRoute
    data class SendMeetingRequest(val sheikhId: String, val sheikhName: String? = null) : MeetingRequestRoute
}
