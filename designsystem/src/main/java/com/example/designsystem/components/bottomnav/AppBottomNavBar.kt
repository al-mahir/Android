package com.example.designsystem.components.bottomnav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.designsystem.R

enum class AppBottomNavDestination {
    Home,
    Reservations,
    Profile,
}

@Composable
fun AppBottomNavBar(
    selectedDestination: AppBottomNavDestination,
    onDestinationSelected: (AppBottomNavDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val destinations = AppBottomNavDestination.entries
    BottomNavBar(
        tabs = appBottomNavTabs(),
        selectedIndex = destinations.indexOf(selectedDestination),
        onTabSelected = { index -> onDestinationSelected(destinations[index]) },
        modifier = modifier,
    )
}

@Composable
private fun appBottomNavTabs(): List<BottomNavTab> = listOf(
    BottomNavTab(
        title = stringResource(R.string.bottom_nav_home),
        icon = painterResource(R.drawable.ic_home_unselected_svg),
        selectedIcon = painterResource(R.drawable.ic_home_selected),
    ),
    BottomNavTab(
        title = stringResource(R.string.bottom_nav_reservations),
        icon = painterResource(R.drawable.ic_reservations_unselected),
        selectedIcon = painterResource(R.drawable.ic_reservations_selected_svg),
    ),
    BottomNavTab(
        title = stringResource(R.string.bottom_nav_profile),
        icon = painterResource(R.drawable.ic_profile_unselected),
        selectedIcon = painterResource(R.drawable.ic_profile_selected),
    ),
)
