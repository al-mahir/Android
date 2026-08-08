package com.iti.presentation.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.designsystem.components.button.SecondaryButton
import com.example.designsystem.components.loading.ShimmerBox
import com.example.designsystem.components.placeholderscreens.NetworkErrorScreen
import com.example.designsystem.components.topbar.BackTitleTopBar
import com.example.designsystem.theme.Theme
import com.iti.presentation.R
import com.iti.presentation.subscription.components.PackageListItem
import com.iti.presentation.subscription.state.PackagesUiState

@Composable
fun PackagesContent(
    state: PackagesUiState,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onSelectPackageClick: (String) -> Unit,
    onStartFreeTrialClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround),
    ) {
        BackTitleTopBar(
            title = stringResource(R.string.packages_title),
            onBackClick = onBackClick,
        )

        when {
            state.hasError -> NetworkErrorScreen(
                modifier = Modifier.fillMaxSize(),
                description = stringResource(state.errorMessageRes ?: R.string.packages_error_generic),
                onRetry = onRetryClick,
            )

            state.isLoading && state.packages.isEmpty() -> PackagesSkeleton()

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = Theme.spacing.medium,
                    vertical = Theme.spacing.large,
                ),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
            ) {
                items(items = state.packages, key = { it.id }) { pkg ->
                    PackageListItem(
                        pkg = pkg,
                        isProcessing = state.processingPackageId == pkg.id,
                        onSelectClick = { onSelectPackageClick(pkg.id) },
                    )
                }

                item(key = "start-free-trial") {
                    SecondaryButton(
                        caption = stringResource(
                            if (state.isStartingTrial) R.string.packages_starting_trial
                            else R.string.packages_start_trial_cta
                        ),
                        onClick = onStartFreeTrialClick,
                        isLoading = state.isStartingTrial,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PackagesSkeleton(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.large),
    ) {
        repeat(3) {
            ShimmerBox(
                Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(Theme.shapes.large),
            )
        }
    }
}
