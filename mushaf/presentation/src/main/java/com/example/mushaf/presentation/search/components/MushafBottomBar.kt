package com.example.mushaf.presentation.search.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.example.designsystem.R
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
            icon = { 
                Icon(
                    painter = painterResource(id = R.drawable.ic_home_unselected_svg), // Replace with your actual icon
                    contentDescription = null 
                ) 
            },
            label = { Text("Home", style = Theme.typography.body.small) },
            colors = navBarColors()
        )
        NavigationBarItem(
            selected = selected == MushafDestination.MUSHAF,
            onClick = { onSelect(MushafDestination.MUSHAF) },
            icon = { 
                Icon(
                    painter = painterResource(id = R.drawable.ic_reservations_unselected), // Replace with actual Mushaf icon
                    contentDescription = null 
                ) 
            },
            label = { Text("Mushaf", style = Theme.typography.body.small) },
            colors = navBarColors()
        )
        NavigationBarItem(
            selected = selected == MushafDestination.PROFILE,
            onClick = { onSelect(MushafDestination.PROFILE) },
            icon = { 
                Icon(
                    painter = painterResource(id = R.drawable.ic_profile_unselected), // Replace with actual Profile icon
                    contentDescription = null 
                ) 
            },
            label = { Text("Profile", style = Theme.typography.body.small) },
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
