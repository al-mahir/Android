package com.iti.al_mahir.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.designsystem.components.bottomnav.AppBottomNavBar
import com.example.designsystem.components.bottomnav.AppBottomNavDestination
import com.example.designsystem.components.placeholderscreens.EmptyDataScreen
import com.example.mushaf.presentation.MushafScreen
import com.iti.presentation.home.HomeScreen

sealed interface AppRoute : NavKey {
    data object Home : AppRoute

    data class Mushaf(val startPage: Int? = null) : AppRoute

    data object Profile : AppRoute
}

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val backStack = remember { mutableStateListOf<NavKey>(AppRoute.Home) }

    fun selectTab(destination: AppBottomNavDestination) {
        val root: NavKey = when (destination) {
            AppBottomNavDestination.Home -> AppRoute.Home
            AppBottomNavDestination.Mushaf -> AppRoute.Mushaf()
            AppBottomNavDestination.Profile -> AppRoute.Profile
        }
        backStack.clear()
        backStack.add(root)
    }

    Column(modifier = modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.weight(1f),
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<AppRoute.Home> {
                    HomeScreen(
                        onOpenSearch = {  },
                        onOpenProfile = { selectTab(AppBottomNavDestination.Profile) },
                        onOpenMushafAtPage = { page ->
                            backStack.clear()
                            backStack.add(AppRoute.Mushaf(startPage = page))
                        },
                        onOpenSheikh = { },
                        onOpenSheikhList = {  },
                        onOpenCircleList = { },
                    )
                }
                entry<AppRoute.Mushaf> { route ->
                    MushafScreen(startPage = route.startPage)
                }
                entry<AppRoute.Profile> {
                    EmptyDataScreen(modifier = Modifier.fillMaxSize())
                }
            },
        )

        AppBottomNavBar(
            selectedDestination = backStack.selectedDestination(),
            onDestinationSelected = ::selectTab,
        )
    }
}

private fun List<NavKey>.selectedDestination(): AppBottomNavDestination =
    when (lastOrNull()) {
        is AppRoute.Mushaf -> AppBottomNavDestination.Mushaf
        AppRoute.Profile -> AppBottomNavDestination.Profile
        else -> AppBottomNavDestination.Home
    }
