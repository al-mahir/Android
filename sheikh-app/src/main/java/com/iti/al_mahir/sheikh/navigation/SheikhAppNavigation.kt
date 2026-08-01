package com.iti.al_mahir.sheikh.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.designsystem.R as DesignSystemR
import com.example.designsystem.components.bottomnav.BottomNavBar
import com.example.designsystem.components.bottomnav.BottomNavTab
import com.iti.sheikh.presentation.availability.SheikhAvailabilityPanel
import com.iti.meeting.presentation.call.session.CallForegroundService
import com.iti.meeting.presentation.call.session.CallSessionController
import com.iti.meeting.presentation.navigation.MeetingRoute
import com.iti.meeting.presentation.navigation.meetingEntries
import com.iti.presentation.auth.navigation.AuthRoute
import com.iti.presentation.auth.navigation.authEntries
import com.iti.presentation.auth.session.SessionState
import com.iti.presentation.auth.session.SessionViewModel
import com.iti.presentation.profile.ProfileScreen
import com.iti.presentation.profile.model.ProfileMenuType
import com.iti.presentation.profile.navigation.ProfileRoute
import com.iti.presentation.profile.navigation.profileEntries
import com.iti.presentation.settings.navigation.SettingsRoute
import com.iti.presentation.settings.navigation.settingsEntries
import com.iti.sheikh.presentation.home.SheikhHomeScreen
import com.iti.presentation.meetingrequest.navigation.meetingRequestEntries
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Sheikh-app-exclusive routes. Reused screens (auth, profile, settings) keep their own
 * [NavKey]s from `:presentation` — these are the app-owned tab roots, the same way `:app`'s
 * `AppRoute.Home`/`AppRoute.Profile` own the student app's tab roots.
 */
sealed interface SheikhAppRoute : NavKey {
    data object Home : SheikhAppRoute
    data object Profile : SheikhAppRoute
}

/** Sheikh-only view of the shared Profile menu: no "Sessions" row (student session history). */
private val sheikhProfileMenuItems = ProfileMenuType.entries.toSet() - ProfileMenuType.SESSIONS

private enum class SheikhBottomNavDestination {
    Home,
    Profile,
}

@Composable
fun SheikhAppNavHost(
    modifier: Modifier = Modifier,
    pendingAction: String? = null,
    onActionHandled: () -> Unit = {},
) {
    val sessionViewModel: SessionViewModel = koinViewModel()
    val session by sessionViewModel.state.collectAsStateWithLifecycle()

    when (val current = session) {
        SessionState.Resolving -> Unit

        else -> SheikhAppNavHost(
            startDestination = if (current == SessionState.Authenticated) {
                SheikhAppRoute.Home
            } else {
                AuthRoute.Login
            },
            pendingAction = pendingAction,
            onActionHandled = onActionHandled,
            modifier = modifier,
        )
    }
}

@Composable
private fun SheikhAppNavHost(
    startDestination: NavKey,
    pendingAction: String? = null,
    onActionHandled: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val backStack = remember { mutableStateListOf(startDestination) }
    val context = LocalContext.current
    val callController: CallSessionController = koinInject()

    /** See the identical helper in `:app`'s `AppNavigation.kt` — same reopen decision tree, cases
     * 1 and 2 from docs/Meeting-Call-Lifecycle-Plan.md. */
    fun openActiveCallIfLive() {
        val callSession = callController.state.value
        val requestId = callSession.requestId
        val channelName = callSession.channelName
        val userAccount = callSession.userAccount
        if (!callSession.isLive || requestId == null || channelName == null || userAccount == null) return
        val top = backStack.lastOrNull()
        if (top is MeetingRoute.Call && top.requestId == requestId) return
        backStack.add(
            MeetingRoute.Call(
                requestId = requestId,
                token = "",
                channelName = channelName,
                userAccount = userAccount,
                remoteDisplayName = callSession.remoteDisplayName,
            ),
        )
    }

    LaunchedEffect(Unit) {
        openActiveCallIfLive()
    }

    LaunchedEffect(pendingAction) {
        if (pendingAction == CallForegroundService.ACTION_OPEN_ACTIVE_CALL) {
            openActiveCallIfLive()
            onActionHandled()
        }
    }

    fun selectTab(destination: SheikhBottomNavDestination) {
        val root: NavKey = when (destination) {
            SheikhBottomNavDestination.Home -> SheikhAppRoute.Home
            SheikhBottomNavDestination.Profile -> SheikhAppRoute.Profile
        }
        backStack.clear()
        backStack.add(root)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.fillMaxSize(),
            onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                authEntries(
                    onNavigate = { route -> backStack.add(route) },
                    onBack = { backStack.removeLastOrNull() },
                    onAuthenticated = {
                        backStack.clear()
                        backStack.add(SheikhAppRoute.Home)
                    },
                    onShowMessage = { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    },
                )

                entry<SheikhAppRoute.Home> {
                    SheikhHomeScreen(
                        onOpenProfile = { selectTab(SheikhBottomNavDestination.Profile) },
                        onOpenActiveCall = { requestId, token, channelName, userAccount, remoteDisplayName ->
                            backStack.add(MeetingRoute.Call(requestId = requestId, token = token, channelName = channelName, userAccount = userAccount, remoteDisplayName = remoteDisplayName))
                        },
                        availabilityPanel = {
                            SheikhAvailabilityPanel(
                                onMeetingAccepted = { requestId, token, channelName, userAccount, remoteDisplayName ->
                                    // Guards against stacking duplicate Call entries — e.g. the
                                    // Busy-state "Rejoin" fallback or the auto-navigate effect
                                    // both firing for the same accepted call in quick succession.
                                    val top = backStack.lastOrNull()
                                    android.util.Log.d("MeetingLifecycle", "SheikhAppNavigation: onMeetingAccepted requestId=$requestId, backstack top=$top, size=${backStack.size}")
                                    if (top !is com.iti.meeting.presentation.navigation.MeetingRoute.Call || top.requestId != requestId) {
                                        android.util.Log.d("MeetingLifecycle", "SheikhAppNavigation: pushing Call($requestId)")
                                        backStack.add(com.iti.meeting.presentation.navigation.MeetingRoute.Call(requestId, token, channelName, userAccount, remoteDisplayName))
                                    } else {
                                        android.util.Log.d("MeetingLifecycle", "SheikhAppNavigation: SKIPPED push, already on Call($requestId)")
                                    }
                                },
                            )
                        },
                    )
                }

                entry<SheikhAppRoute.Profile> {
                    ProfileScreen(
                        onOpenPremium = { backStack.add(ProfileRoute.Premium) },
                        onOpenLegalDocument = { documentType ->
                            backStack.add(ProfileRoute.StaticContent(documentType))
                        },
                        onSignedOut = {
                            backStack.clear()
                            backStack.add(AuthRoute.Login)
                        },
                        onOpenSettings = { backStack.add(SettingsRoute.Settings) },
                        onOpenAttributions = { backStack.add(ProfileRoute.Attributions) },
                        visibleMenuItems = sheikhProfileMenuItems,
                    )
                }

                profileEntries(onBack = { backStack.removeLastOrNull() })

                settingsEntries(onBack = { backStack.removeLastOrNull() })

                meetingRequestEntries(
                    onNavigate = { route -> backStack.add(route) },
                    onNavigateToCall = { requestId, token, channelName, userAccount, remoteDisplayName ->
                        backStack.add(com.iti.meeting.presentation.navigation.MeetingRoute.Call(requestId, token, channelName, userAccount, remoteDisplayName))
                    },
                    onBack = { backStack.removeLastOrNull() },
                    onShowMessage = { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    },
                )
                meetingEntries(
                    onNavigate = { route -> backStack.add(route) },
                    onBack = { backStack.removeLastOrNull() },
                )
            },
        )

        val selectedTab = backStack.selectedDestination()
        if (selectedTab != null) {
            BottomNavBar(
                tabs = sheikhBottomNavTabs(),
                selectedIndex = SheikhBottomNavDestination.entries.indexOf(selectedTab),
                onTabSelected = { index -> selectTab(SheikhBottomNavDestination.entries[index]) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun sheikhBottomNavTabs(): List<BottomNavTab> = listOf(
    BottomNavTab(
        title = stringResource(DesignSystemR.string.bottom_nav_home),
        icon = painterResource(DesignSystemR.drawable.ic_home_unselected),
        selectedIcon = painterResource(DesignSystemR.drawable.ic_home_selected),
    ),
    BottomNavTab(
        title = stringResource(DesignSystemR.string.bottom_nav_profile),
        icon = painterResource(DesignSystemR.drawable.ic_profile_unselected),
        selectedIcon = painterResource(DesignSystemR.drawable.ic_profile_selected),
    ),
)

private fun List<NavKey>.selectedDestination(): SheikhBottomNavDestination? =
    when (lastOrNull()) {
        SheikhAppRoute.Home -> SheikhBottomNavDestination.Home
        SheikhAppRoute.Profile -> SheikhBottomNavDestination.Profile
        else -> null
    }


