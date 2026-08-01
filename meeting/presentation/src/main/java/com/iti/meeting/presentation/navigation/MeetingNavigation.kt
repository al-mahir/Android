package com.iti.meeting.presentation.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.iti.meeting.presentation.call.CallScreen

/**
 * Registers `:meeting-kit`'s destinations on the host app's Navigation 3 back stack, the same
 * pattern `:presentation`'s `authEntries(...)` uses. Circle entries land in Phase 2.
 *
 * @param onNavigate push a destination onto the host's back stack
 * @param onBack pop the current destination
 * @param onShowMessage surface a transient error/message to the user
 */
fun EntryProviderScope<NavKey>.meetingEntries(
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
) {
    entry<MeetingRoute.Call> { route ->
        CallScreen(
            requestId = route.requestId,
            token = route.token,
            channelName = route.channelName,
            userAccount = route.userAccount,
            remoteDisplayName = route.remoteDisplayName,
            onLeave = onBack,
        )
    }
}



