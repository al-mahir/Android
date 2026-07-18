package com.iti.al_mahir.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.mushaf.presentation.MushafScreen

sealed interface AppRoute : NavKey {
    data object Mushaf : AppRoute
    data object Search : AppRoute
}

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val backStack = remember { mutableStateListOf<NavKey>(AppRoute.Search) }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<AppRoute.Mushaf> {
                MushafScreen()
            }
            entry<AppRoute.Search> {
                // Here we inject the ViewModel using Koin
                com.example.mushaf.presentation.search.MushafSearchScreen(
                    viewModel = org.koin.androidx.compose.koinViewModel()
                )
            }
        },
    )
}
