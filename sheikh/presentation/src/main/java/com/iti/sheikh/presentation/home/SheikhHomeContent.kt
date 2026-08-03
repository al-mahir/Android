package com.iti.sheikh.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.placeholderscreens.NetworkErrorScreen
import com.example.designsystem.theme.Theme
import com.iti.sheikh.presentation.R
import com.iti.sheikh.presentation.home.components.OngoingCallCard
import com.iti.sheikh.presentation.home.components.SheikhHomeHeader
import com.iti.sheikh.presentation.home.state.SheikhHomeUiState

@Composable
fun SheikhHomeContent(
    state: SheikhHomeUiState,
    onProfileClick: () -> Unit,
    onRetryClick: () -> Unit,
    onOpenCircles: () -> Unit,
    onRejoinActiveCallClick: () -> Unit = {},
    onDismissActiveCallClick: () -> Unit = {},
    availabilityPanel: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rootModifier = modifier
        .fillMaxSize()
        .background(Theme.colors.backGround)

    val gutter = Modifier.padding(horizontal = Theme.spacing.medium)

    when {
        state.isOffline -> NetworkErrorScreen(
            modifier = rootModifier,
            description = stringResource(R.string.sheikh_home_error_generic),
            onRetry = onRetryClick,
        )

        state.hasError && state.initials == null -> NetworkErrorScreen(
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
                onProfileClick = onProfileClick,
                modifier = gutter.padding(top = Theme.spacing.medium, bottom = Theme.spacing.medium),
            )

            state.activeCall?.let { active ->
                OngoingCallCard(
                    call = active,
                    onRejoin = onRejoinActiveCallClick,
                    onDismiss = onDismissActiveCallClick,
                    modifier = gutter,
                )
            }

            Column(modifier = gutter) {
                availabilityPanel()
            }
        }
    }
}
