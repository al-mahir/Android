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
import com.example.designsystem.components.placeholderscreens.EmptyDataScreen
import com.example.mushaf.presentation.MushafScreen
import com.iti.presentation.auth.navigation.AuthRoute
import com.iti.presentation.auth.navigation.authEntries
import com.iti.presentation.home.HomeScreen

/** Destinations of the signed-in app. Auth destinations live in `AuthRoute` (`:presentation`). */
sealed interface AppRoute : NavKey {
    data object Home : AppRoute

    /** [startPage] is set when arriving from Home's "Continue Reading"; null resumes. */
    data class Mushaf(val startPage: Int? = null) : AppRoute

    data object Profile : AppRoute
}

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    // The app opens on Login; a successful sign-in swaps the stack for Home.
    val backStack = remember { mutableStateListOf<NavKey>(AuthRoute.Login) }
    val context = LocalContext.current

    // Switching tabs resets the stack to that tab's root. The three tabs are independent
    // entry points rather than a growing history.
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
                    // Placeholder until the Profile feature lands.
                    EmptyDataScreen(modifier = Modifier.fillMaxSize())
                }
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

/**
 * The tab owning the current top-of-stack entry, or null while the user is in the auth flow
 * (which shows no bottom bar).
 */
private fun List<NavKey>.selectedDestination(): AppBottomNavDestination? =
    when (lastOrNull()) {
        AppRoute.Home -> AppBottomNavDestination.Home
        is AppRoute.Mushaf -> AppBottomNavDestination.Mushaf
        AppRoute.Profile -> AppBottomNavDestination.Profile
        else -> null
    }
