package com.iti.al_mahir.sheikh.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
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
fun SheikhAppNavHost(modifier: Modifier = Modifier) {
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
            modifier = modifier,
        )
    }
}

@Composable
private fun SheikhAppNavHost(startDestination: NavKey, modifier: Modifier = Modifier) {
    val backStack = remember { mutableStateListOf(startDestination) }
    val context = LocalContext.current

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
                        availabilityPanel = {
                            SheikhAvailabilityPanel(
                                onMeetingAccepted = { circleId, token, channelName, uid -> 
                                    backStack.add(com.iti.meeting.presentation.navigation.MeetingRoute.Call(circleId, token, channelName, uid)) 
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
                    onNavigateToCall = { circleId, token, channelName, uid -> 
                        backStack.add(com.iti.meeting.presentation.navigation.MeetingRoute.Call(circleId, token, channelName, uid)) 
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


