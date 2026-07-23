package com.example.mushaf.presentation.search.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.designsystem.R as DesignSystemR
import com.example.mushaf.presentation.R
import com.example.designsystem.theme.Theme

enum class MushafDestination { HOME, MUSHAF, PROFILE }

@Composable
fun MushafBottomBar(
    selected: MushafDestination,
    onSelect: (MushafDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier, containerColor = Theme.colors.backGround) {
        NavigationBarItem(
            selected = selected == MushafDestination.HOME,
            onClick = { onSelect(MushafDestination.HOME) },
            icon = { Icon(painterResource(id = DesignSystemR.drawable.ic_home_unselected_svg), contentDescription = null) },
            label = { Text(stringResource(R.string.nav_home), style = Theme.typography.body.small) },
            colors = navBarColors()
        )
        NavigationBarItem(
            selected = selected == MushafDestination.MUSHAF,
            onClick = { onSelect(MushafDestination.MUSHAF) },
            icon = { Icon(painterResource(id = DesignSystemR.drawable.ic_reservations_unselected), contentDescription = null) },
            label = { Text(stringResource(R.string.nav_mushaf), style = Theme.typography.body.small) },
            colors = navBarColors()
        )
        NavigationBarItem(
            selected = selected == MushafDestination.PROFILE,
            onClick = { onSelect(MushafDestination.PROFILE) },
            icon = { Icon(painterResource(id = DesignSystemR.drawable.ic_profile_unselected), contentDescription = null) },
            label = { Text(stringResource(R.string.nav_profile), style = Theme.typography.body.small) },
            colors = navBarColors()
        )
    }
}

@Composable
private fun navBarColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Theme.colors.primary,
    selectedTextColor = Theme.colors.primary,
    unselectedIconColor = Theme.colors.hint,
    unselectedTextColor = Theme.colors.hint,
    indicatorColor = Color.Transparent
)
