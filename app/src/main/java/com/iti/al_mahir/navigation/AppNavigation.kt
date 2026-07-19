package com.iti.al_mahir.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.designsystem.components.bottomnav.AppBottomNavBar
import com.example.designsystem.components.bottomnav.AppBottomNavDestination
import com.example.mushaf.presentation.MushafScreen
import com.iti.presentation.auth.navigation.AuthRoute
import com.iti.presentation.auth.navigation.authEntries
import com.iti.presentation.home.HomeScreen
import com.iti.presentation.profile.ProfileScreen
import com.iti.presentation.profile.navigation.ProfileRoute
import com.iti.presentation.profile.navigation.profileEntries

sealed interface AppRoute : NavKey {
    data object Home : AppRoute

    data class Mushaf(val startPage: Int? = null) : AppRoute

    data object Profile : AppRoute
}

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val backStack = remember { mutableStateListOf<NavKey>(AuthRoute.Login) }
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.weight(1f),
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                authEntries(
                    onNavigate = { route -> backStack.add(route) },
                    onBack = { backStack.removeLastOrNull() },
                    // Clearing the stack is what makes sign-in irreversible: back from Home
                    // exits the app rather than returning to Login.
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
                        onOpenSearch = { },
                        onOpenProfile = { selectTab(AppBottomNavDestination.Profile) },
                        onOpenMushafAtPage = { page ->
                            backStack.clear()
                            backStack.add(AppRoute.Mushaf(startPage = page))
                        },
                        onOpenSheikh = { },
                        onOpenSheikhList = { },
                        onOpenCircleList = { },
                    )
                }
                entry<AppRoute.Mushaf> { route ->
                    MushafScreen(startPage = route.startPage)
                }
                entry<AppRoute.Profile> {
                    ProfileScreen(
                        onOpenPremium = { backStack.add(ProfileRoute.Premium) },
                        onOpenLegalDocument = { documentType ->
                            backStack.add(ProfileRoute.StaticContent(documentType))
                        },
                        // Logout and account deletion both end the session: clearing the stack
                        // is what makes sign-out irreversible, mirroring onAuthenticated above.
                        onSignedOut = {
                            backStack.clear()
                            backStack.add(AuthRoute.Login)
                        },
                    )
                }

                profileEntries(onBack = { backStack.removeLastOrNull() })
            },
        )

        // Auth is a full-screen flow with no tabs — the bar only appears once signed in.
        val selectedTab = backStack.selectedDestination()
        if (selectedTab != null) {
            AppBottomNavBar(
                selectedDestination = selectedTab,
                onDestinationSelected = ::selectTab,
            )
        }
    }
}


private fun List<NavKey>.selectedDestination(): AppBottomNavDestination? =
    when (lastOrNull()) {
        AppRoute.Home -> AppBottomNavDestination.Home
        AppRoute.Profile -> AppBottomNavDestination.Profile
        is AppRoute.Mushaf -> AppBottomNavDestination.Mushaf
        else -> null
    }