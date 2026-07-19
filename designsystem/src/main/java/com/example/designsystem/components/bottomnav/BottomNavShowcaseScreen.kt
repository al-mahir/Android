package com.example.designsystem.components.bottomnav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.Theme

/**
 * Interactive showcase that exercises every selection state of the app
 * bottom navigation. Useful when running the preview on a device to verify
 * tap behaviour, shadow, and theme integration end-to-end.
 */
@Composable
internal fun BottomNavShowcaseScreen(modifier: Modifier = Modifier) {
    var selected by remember { mutableStateOf(AppBottomNavDestination.Home) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = Theme.spacing.large),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
        ) {
            SectionLabel("Selected: ${selected.name}")
            Spacer(Modifier.height(Theme.spacing.small))

            SectionLabel("Each destination")
            AppBottomNavBar(
                selectedDestination = AppBottomNavDestination.Home,
                onDestinationSelected = {},
            )
            AppBottomNavBar(
                selectedDestination = AppBottomNavDestination.Mushaf,
                onDestinationSelected = {},
            )
            AppBottomNavBar(
                selectedDestination = AppBottomNavDestination.Profile,
                onDestinationSelected = {},
            )
        }

        AppBottomNavBar(
            selectedDestination = selected,
            onDestinationSelected = { selected = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    BasicText(
        text = text,
        style = Theme.typography.body.large.copy(color = Theme.colors.primary),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.medium),
    )
}
