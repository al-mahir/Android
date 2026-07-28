package com.iti.sheikh.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.designsystem.components.placeholderscreens.NetworkErrorScreen
import com.example.designsystem.theme.Theme
import com.iti.domain.model.SheikhAvailability
import com.iti.sheikh.presentation.R
import com.iti.sheikh.presentation.home.components.SheikhAvailabilityCard
import com.iti.sheikh.presentation.home.components.SheikhAvailabilityIllustration
import com.iti.sheikh.presentation.home.components.SheikhHomeHeader
import com.iti.sheikh.presentation.home.state.SheikhHomeUiState

@Composable
fun SheikhHomeContent(
    state: SheikhHomeUiState,
    onProfileClick: () -> Unit,
    onAvailabilityToggle: (Boolean) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rootModifier = modifier
        .fillMaxSize()
        .background(Theme.colors.backGround)

    val gutter = Modifier.padding(horizontal = Theme.spacing.medium)

    when {
        state.hasError -> NetworkErrorScreen(
            modifier = rootModifier,
            description = stringResource(state.errorMessageRes ?: R.string.sheikh_home_error_generic),
            onRetry = onRetryClick,
        )

        else -> Column(
            modifier = rootModifier,
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
        ) {
            SheikhHomeHeader(
                initials = state.initials,
                avatarUrl = state.avatarUrl,
                isAvailable = state.availability == SheikhAvailability.AVAILABLE,
                isAvailabilityToggleEnabled = state.isAvailabilityToggleEnabled,
                onAvailabilityToggle = onAvailabilityToggle,
                onProfileClick = onProfileClick,
                modifier = gutter.padding(top = Theme.spacing.medium, bottom = Theme.spacing.medium),
            )

            SheikhAvailabilityCard(
                availability = state.availability,
                modifier = gutter,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                SheikhAvailabilityIllustration(availability = state.availability)
            }
        }
    }
}
