package com.iti.al_mahir.navigation

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import com.example.designsystem.components.dialog.ConfirmationDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.designsystem.components.bottomnav.AppBottomNavBar
import com.example.designsystem.components.bottomnav.AppBottomNavDestination
import com.example.mushaf.presentation.MushafScreen
import com.example.mushaf.presentation.download.navigation.DownloadsRoute
import com.example.mushaf.presentation.download.navigation.downloadsEntries
import com.example.mushaf.presentation.settings.MushafSettingsSection
import com.example.mushaf.presentation.settings.recite.navigation.ReciteSettingsRoute
import com.example.mushaf.presentation.settings.recite.navigation.reciteSettingsEntries
import com.iti.presentation.auth.navigation.AuthRoute
import com.iti.presentation.auth.navigation.authEntries
import com.iti.presentation.auth.session.SessionState
import com.iti.presentation.auth.session.SessionViewModel
import com.iti.meeting.presentation.call.session.CallForegroundService
import com.iti.meeting.presentation.call.session.CallSessionController
import com.iti.meeting.presentation.navigation.MeetingRoute
import com.iti.presentation.meetingrequest.navigation.MeetingRequestRoute
import com.iti.meeting.presentation.navigation.meetingEntries
import org.koin.compose.koinInject
import com.iti.presentation.circle.CircleListScreen
import com.iti.presentation.circle.InSessionScreen
import com.iti.presentation.circle.JoiningCircleScreen
import com.iti.presentation.home.HomeScreen
import com.iti.presentation.meetingrequest.navigation.meetingRequestEntries
import com.iti.presentation.profile.ProfileScreen
import com.iti.presentation.profile.navigation.ProfileRoute
import com.iti.presentation.profile.navigation.profileEntries
import com.iti.presentation.settings.SettingsScreen
import com.iti.presentation.settings.navigation.SettingsRoute
import com.iti.presentation.sheikh.SheikhDetailsScreen
import com.iti.presentation.sheikh.SheikhListScreen
import com.example.designsystem.theme.Theme
import com.iti.al_mahir.R
import org.koin.androidx.compose.koinViewModel

sealed interface AppRoute : NavKey {
    data object Home : AppRoute
    data class Mushaf(val startPage: Int? = null, val openInListenMode: Boolean = false) : AppRoute
    data object Profile : AppRoute
    data object Bookmarks : AppRoute
    data object Search : AppRoute
    data object SheikhList : AppRoute
    data class SheikhDetails(val sheikhId: String) : AppRoute
    data object CircleList : AppRoute
    data class JoiningCircle(val circleId: String) : AppRoute
    data class InSession(val circleId: String) : AppRoute
}

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    pendingAction: String? = null,
    onActionHandled: () -> Unit = {}
) {
    val sessionViewModel: SessionViewModel = koinViewModel()
    val session by sessionViewModel.state.collectAsStateWithLifecycle()

    when (val current = session) {
        SessionState.Resolving -> Unit

        else -> AppNavHost(
            startDestination = if (current == SessionState.Authenticated) {
                AppRoute.Home
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
private fun AppNavHost(
    startDestination: NavKey,
    pendingAction: String? = null,
    onActionHandled: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val backStack = remember { mutableStateListOf(startDestination) }
    val context = LocalContext.current
    val callController: CallSessionController = koinInject()

    fun isPhantomReplayOfEndedCall(requestId: String): Boolean {
        val session = callController.state.value
        return session.requestId == requestId && !session.isLive
    }

    fun openActiveCallIfLive() {
        val session = callController.state.value
        val requestId = session.requestId
        val channelName = session.channelName
        val userAccount = session.userAccount
        if (!session.isLive || requestId == null || channelName == null || userAccount == null) return
        val top = backStack.lastOrNull()
        if (top is MeetingRoute.Call && top.requestId == requestId) return
        backStack.add(
            MeetingRoute.Call(
                requestId = requestId,
                token = "",
                channelName = channelName,
                userAccount = userAccount,
                remoteDisplayName = session.remoteDisplayName,
            ),
        )
    }

    fun selectTab(destination: AppBottomNavDestination) {
        val root: NavKey = when (destination) {
            AppBottomNavDestination.Home -> AppRoute.Home
            AppBottomNavDestination.Mushaf -> AppRoute.Mushaf()
            AppBottomNavDestination.Bookmark -> AppRoute.Bookmarks
            AppBottomNavDestination.Profile -> AppRoute.Profile
        }
        backStack.clear()
        backStack.add(root)
    }

    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = backStack.size == 1) {
        if (backStack.lastOrNull() == AppRoute.Home) {
            showExitDialog = true
        } else {
            selectTab(AppBottomNavDestination.Home)
        }
    }

    if (showExitDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.exit_app_title),
            message = stringResource(R.string.exit_app_message),
            confirmLabel = stringResource(R.string.exit_app_confirm),
            dismissLabel = stringResource(R.string.exit_app_cancel),
            onConfirm = { (context as? Activity)?.finish() },
            onDismiss = { showExitDialog = false },
            confirmColor = Theme.colors.error,
            confirmContentColor = Theme.colors.onError
        )
    }

    LaunchedEffect(Unit) {
        openActiveCallIfLive()
    }

    LaunchedEffect(pendingAction) {
        when (pendingAction) {
            "ACTION_OPEN_MUSHAF_LISTEN" -> {
                backStack.removeAll { it is AppRoute.Mushaf }
                backStack.add(AppRoute.Mushaf(openInListenMode = true))
                onActionHandled()
            }

            CallForegroundService.ACTION_OPEN_ACTIVE_CALL -> {
                openActiveCallIfLive()
                onActionHandled()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        val showBanner =
            backStack.lastOrNull() !is AppRoute.Mushaf && backStack.lastOrNull() !is AppRoute.Search
        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
            if (showBanner) {
                val mainViewModel: com.iti.presentation.core.MainViewModel = koinViewModel()
                val banner by mainViewModel.banner.collectAsStateWithLifecycle()
                com.iti.presentation.core.components.OfflineBanner(banner = banner)
            }
            Box(modifier = Modifier.weight(1f)) {
                NavDisplay(
                    backStack = backStack,
                    modifier = Modifier.fillMaxSize(),
                    onBack = {
                        when {
                            backStack.lastOrNull() is AppRoute.Mushaf ->
                                selectTab(AppBottomNavDestination.Home)

                            backStack.size > 1 -> backStack.removeLastOrNull()
                        }
                    },
                    entryProvider = entryProvider {
                        authEntries(
                            onNavigate = { route -> backStack.add(route) },
                            onBack = { backStack.removeLastOrNull() },
                            onAuthenticated = {
                                backStack.clear()
                                backStack.add(AppRoute.Home)
                            },
                            onShowMessage = { message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            },
                        )

                        entry<AppRoute.Home> {
                            HomeScreen(
                                onOpenSearch = {
                                    backStack.removeAll { it == AppRoute.Search }
                                    backStack.add(AppRoute.Search)
                                },
                                onOpenProfile = { selectTab(AppBottomNavDestination.Profile) },
                                onOpenMushafAtPage = { page ->
                                    backStack.clear()
                                    backStack.add(AppRoute.Mushaf(startPage = page))
                                },
                                onOpenSheikh = { sheikhId ->
                                    backStack.add(AppRoute.SheikhDetails(sheikhId))
                                },
                                onOpenSheikhList = { backStack.add(AppRoute.SheikhList) },
                                onOpenCircleList = { backStack.add(AppRoute.CircleList) },
                                onOpenMeetingRequest = { sheikhId, sheikhName ->
                                    backStack.add(
                                        MeetingRequestRoute.SendMeetingRequest(
                                            sheikhId,
                                            sheikhName
                                        )
                                    )
                                },
                                onOpenActiveCall = { requestId, token, channelName, userAccount, remoteDisplayName ->
                                    backStack.add(
                                        MeetingRoute.Call(
                                            requestId = requestId,
                                            token = token,
                                            channelName = channelName,
                                            userAccount = userAccount,
                                            remoteDisplayName = remoteDisplayName
                                        )
                                    )
                                },
                            )
                        }

                        entry<AppRoute.Mushaf> { route ->
                            MushafScreen(
                                startPage = route.startPage,
                                openInListenMode = route.openInListenMode,
                                onBack = { selectTab(AppBottomNavDestination.Home) },
                                onOpenSettings = { backStack.add(SettingsRoute.Settings) },
                                onSearchClick = {
                                    backStack.removeAll { it == AppRoute.Search }
                                    backStack.add(AppRoute.Search)
                                },
                                onNavigateToSurahDownload = { reciterId ->
                                    backStack.add(DownloadsRoute.SurahDownload(reciterId))
                                },
                            )
                        }

                        entry<AppRoute.Search> {
                            com.example.mushaf.presentation.search.MushafSearchScreen(
                                viewModel = koinViewModel(),
                                onNavigateBack = { backStack.removeAt(backStack.lastIndex) },
                                onNavigateToMushaf = {
                                    backStack.removeAll { it is AppRoute.Mushaf }
                                    backStack.add(AppRoute.Mushaf())
                                }
                            )
                        }

                        entry<AppRoute.Bookmarks> {
                            com.iti.presentation.bookmark.BookmarkScreen(
                                onOpenMushafAtPage = { page ->
                                    backStack.clear()
                                    backStack.add(AppRoute.Mushaf(startPage = page))
                                },
                                onOpenSheikhDetails = { sheikhId ->
                                    backStack.add(AppRoute.SheikhDetails(sheikhId))
                                }
                            )
                        }

                        entry<AppRoute.Profile> {
                            val mainViewModel: com.iti.presentation.core.MainViewModel =
                                koinViewModel()
                            val isOnline by mainViewModel.isOnline.collectAsStateWithLifecycle()

                            val offlineMenus = setOf(
                                com.iti.presentation.profile.model.ProfileMenuType.SETTINGS,
                                com.iti.presentation.profile.model.ProfileMenuType.ABOUT,
                                com.iti.presentation.profile.model.ProfileMenuType.ATTRIBUTIONS,
                                com.iti.presentation.profile.model.ProfileMenuType.SHARE_APP
                            )

                            ProfileScreen(
                                visibleMenuItems = if (isOnline) com.iti.presentation.profile.model.ProfileMenuType.entries.toSet() else offlineMenus,
                                onOpenPremium = { backStack.add(ProfileRoute.Premium) },
                                onOpenMySubscription = { backStack.add(ProfileRoute.MySubscription) },
                                onOpenLegalDocument = { documentType ->
                                    backStack.add(ProfileRoute.StaticContent(documentType))
                                },
                                onSignedOut = {
                                    backStack.clear()
                                    backStack.add(AuthRoute.Login)
                                },
                                onOpenSettings = { backStack.add(SettingsRoute.Settings) },
                                onOpenSessions = { backStack.add(ProfileRoute.Sessions) },
                                onOpenAttributions = { backStack.add(ProfileRoute.Attributions) },
                            )
                        }

                        entry<AppRoute.SheikhList> {
                            SheikhListScreen(
                                onBack = { backStack.removeLastOrNull() },
                                onOpenSheikhDetails = { sheikhId ->
                                    backStack.add(AppRoute.SheikhDetails(sheikhId))
                                },
                            )
                        }

                        entry<AppRoute.SheikhDetails> { route ->
                            SheikhDetailsScreen(
                                sheikhId = route.sheikhId,
                                onBack = { backStack.removeLastOrNull() },
                                onNavigateToJoiningCircle = { circleId ->
                                    backStack.add(AppRoute.JoiningCircle(circleId))
                                },
                                onRequestMeeting = { sheikhId, sheikhName ->
                                    backStack.add(
                                        MeetingRequestRoute.SendMeetingRequest(
                                            sheikhId,
                                            sheikhName
                                        )
                                    )
                                },
                            )
                        }

                        entry<AppRoute.CircleList> {
                            CircleListScreen(
                                onBack = { backStack.removeLastOrNull() },
                                onNavigateToJoiningCircle = { circleId ->
                                    backStack.add(AppRoute.JoiningCircle(circleId))
                                },
                            )
                        }

                        entry<AppRoute.JoiningCircle> { route ->
                            JoiningCircleScreen(
                                circleId = route.circleId,
                                onBack = { backStack.removeLastOrNull() },
                                onNavigateToSession = { circleId ->
                                    backStack.removeLastOrNull()
                                    backStack.add(AppRoute.InSession(circleId))
                                },
                            )
                        }

                        entry<AppRoute.InSession> { route ->
                            InSessionScreen(
                                circleId = route.circleId,
                                onBack = { backStack.removeLastOrNull() },
                                onOpenMushaf = { backStack.add(AppRoute.Mushaf()) },
                            )
                        }

                        profileEntries(
                            onBack = { backStack.removeLastOrNull() },
                            onNavigateToCheckout = { packageId -> backStack.add(ProfileRoute.Checkout(packageId)) },
                        )

                        entry<SettingsRoute.Settings> {
                            SettingsScreen(
                                onBack = { backStack.removeLastOrNull() },
                                onShowMessage = { messageRes ->
                                    Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
                                },
                                mushafSection = {
                                    MushafSettingsSection(
                                        onOpenDownloads = { kind ->
                                            backStack.add(DownloadsRoute.Downloads(kind))
                                        },
                                        onOpenReciteSettings = {
                                            backStack.add(ReciteSettingsRoute.ReciteSettings)
                                        },
                                    )
                                },
                            )
                        }

                        downloadsEntries(
                            onBack = { backStack.removeLastOrNull() },
                            onNavigateToSurahList = { reciterId ->
                                backStack.add(com.example.mushaf.presentation.download.navigation.DownloadsRoute.SurahDownload(reciterId))
                            }
                        )

                        reciteSettingsEntries(onBack = { backStack.removeLastOrNull() })

                        meetingRequestEntries(
                            onNavigate = { route -> backStack.add(route) },
                            onNavigateToCall = { requestId, token, channelName, userAccount, remoteDisplayName ->
                                val top = backStack.lastOrNull()
                                val isCallAlreadyTop =
                                    top is MeetingRoute.Call && top.requestId == requestId
                                if (!isCallAlreadyTop && !isPhantomReplayOfEndedCall(requestId)) {
                                    backStack.add(
                                        MeetingRoute.Call(
                                            requestId,
                                            token,
                                            channelName,
                                            userAccount,
                                            remoteDisplayName
                                        )
                                    )
                                }
                            },
                            onBack = { backStack.removeLastOrNull() },
                            onShowMessage = { message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            },
                        )
                        meetingEntries(
                            onNavigate = { route -> backStack.add(route) },
                            onBack = {
                                backStack.removeLastOrNull()
                                while (backStack.lastOrNull() is MeetingRequestRoute) {
                                    backStack.removeLastOrNull()
                                }
                            },
                        )
                    },
                )
            }
        }

        val selectedTab = backStack.selectedDestination()
        if (selectedTab != null) {
            AppBottomNavBar(
                selectedDestination = selectedTab,
                onDestinationSelected = ::selectTab,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

private fun List<NavKey>.selectedDestination(): AppBottomNavDestination? =
    when (lastOrNull()) {
        AppRoute.Home -> AppBottomNavDestination.Home
        AppRoute.Bookmarks -> AppBottomNavDestination.Bookmark
        AppRoute.Profile -> AppBottomNavDestination.Profile
        else -> null
    }
