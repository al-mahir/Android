package com.iti.al_mahir.navigation

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
import com.iti.presentation.circle.CircleListScreen
import com.iti.presentation.circle.InSessionScreen
import com.iti.presentation.circle.JoiningCircleScreen
import com.iti.presentation.home.HomeScreen
import com.iti.presentation.profile.ProfileScreen
import com.iti.presentation.profile.navigation.ProfileRoute
import com.iti.presentation.profile.navigation.profileEntries
import com.iti.presentation.settings.SettingsScreen
import com.iti.presentation.settings.navigation.SettingsRoute
import com.iti.presentation.sheikh.SheikhDetailsScreen
import com.iti.presentation.sheikh.SheikhListScreen
import org.koin.androidx.compose.koinViewModel

sealed interface AppRoute : NavKey {
    data object Home : AppRoute
    data class Mushaf(val startPage: Int? = null) : AppRoute
    data object Profile : AppRoute
    data object Search : AppRoute
    data object SheikhList : AppRoute
    data class SheikhDetails(val sheikhId: String) : AppRoute
    data object CircleList : AppRoute
    data class JoiningCircle(val circleId: String) : AppRoute
    data class InSession(val circleId: String) : AppRoute
}

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
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
            modifier = modifier,
        )
    }
}

@Composable
private fun AppNavHost(startDestination: NavKey, modifier: Modifier = Modifier) {
    val backStack = remember { mutableStateListOf(startDestination) }
    val context = LocalContext.current

    fun selectTab(destination: AppBottomNavDestination) {
        val root: NavKey = when (destination) {
            AppBottomNavDestination.Home -> AppRoute.Home
            AppBottomNavDestination.Mushaf -> AppRoute.Mushaf()
            AppBottomNavDestination.Profile -> AppRoute.Profile
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
                        onOpenSheikh = { sheikhId -> backStack.add(AppRoute.SheikhDetails(sheikhId)) },
                        onOpenSheikhList = { backStack.add(AppRoute.SheikhList) },
                        onOpenCircleList = { backStack.add(AppRoute.CircleList) },
                    )
                }

                entry<AppRoute.Mushaf> { route ->
                    MushafScreen(
                        startPage = route.startPage,
                        onBack = { selectTab(AppBottomNavDestination.Home) },
                        onOpenSettings = { backStack.add(SettingsRoute.Settings) },
                        onSearchClick = {
                            backStack.removeAll { it == AppRoute.Search }
                            backStack.add(AppRoute.Search)
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

                entry<AppRoute.Profile> {
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

                profileEntries(onBack = { backStack.removeLastOrNull() })

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

                downloadsEntries(onBack = { backStack.removeLastOrNull() })

                reciteSettingsEntries(onBack = { backStack.removeLastOrNull() })
            },
        )

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
        AppRoute.Profile -> AppBottomNavDestination.Profile
        else -> null
    }