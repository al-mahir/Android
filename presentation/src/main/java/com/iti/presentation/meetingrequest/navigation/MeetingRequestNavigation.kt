package com.iti.presentation.meetingrequest.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.iti.presentation.meetingrequest.browse.SheikhBrowseScreen
import com.iti.presentation.meetingrequest.request.MeetingRequestScreen

fun EntryProviderScope<NavKey>.meetingRequestEntries(
    onNavigate: (NavKey) -> Unit,
    onNavigateToCall: (String, String, String, Int) -> Unit,
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    entry<MeetingRequestRoute.SheikhBrowseList> {
        SheikhBrowseScreen(
            onBack = onBack,
            onOpenRequest = { sheikhId -> onNavigate(MeetingRequestRoute.SendMeetingRequest(sheikhId)) },
        )
    }

    entry<MeetingRequestRoute.SendMeetingRequest> { route ->
        MeetingRequestScreen(
            sheikhId = route.sheikhId,
            onBack = onBack,
            onMeetingAccepted = onNavigateToCall,
            onShowMessage = onShowMessage,
        )
    }
}

