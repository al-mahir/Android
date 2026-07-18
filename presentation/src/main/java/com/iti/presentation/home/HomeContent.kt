package com.iti.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.designsystem.components.placeholderscreens.NetworkErrorScreen
import com.example.designsystem.theme.Theme
import com.iti.presentation.R
import com.iti.presentation.home.components.ActiveCircleRow
import com.iti.presentation.home.components.ContinueReadingCard
import com.iti.presentation.home.components.HomeHeader
import com.iti.presentation.home.components.HomeSearchBar
import com.iti.presentation.home.components.HomeSkeleton
import com.iti.presentation.home.components.SectionHeader
import com.iti.presentation.home.components.SheikhProfileCard
import com.iti.presentation.home.state.HomeUiState


@Composable
fun HomeContent(
    state: HomeUiState,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    onContinueReadingClick: () -> Unit,
    onSeeAllSheikhsClick: () -> Unit,
    onSeeAllCirclesClick: () -> Unit,
    onSheikhClick: (String) -> Unit,
    onJoinCircleClick: (String) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rootModifier = modifier
        .fillMaxSize()
        .background(Theme.colors.backGround)

    when {
        state.hasError -> NetworkErrorScreen(
            modifier = rootModifier,
            description = stringResource(state.errorMessageRes ?: R.string.home_error_generic),
            onRetry = onRetryClick,
        )

        state.isLoading && state.user == null -> HomeSkeleton(modifier = rootModifier)

        else -> LazyColumn(
            modifier = rootModifier,
            contentPadding = PaddingValues(
                start = Theme.spacing.medium,
                end = Theme.spacing.medium,
                top = Theme.spacing.medium,
                bottom = Theme.spacing.extraLarge,
            ),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
        ) {
            item(key = "header") {
                HomeHeader(
                    initials = state.user?.initials,
                    avatarUrl = state.user?.avatarUrl,
                    onProfileClick = onProfileClick,
                )
            }

            item(key = "search") {
                HomeSearchBar(onSearchClick = onSearchClick)
            }

            state.continueReading?.let { continueReading ->
                item(key = "continue-reading-header") {
                    SectionHeader(title = stringResource(R.string.home_section_continue_reading))
                }
                item(key = "continue-reading") {
                    ContinueReadingCard(
                        surahName = continueReading.surahName,
                        ayahNumber = continueReading.ayahNumber,
                        pageNumber = continueReading.pageNumber,
                        onClick = onContinueReadingClick,
                    )
                }
            }

            if (state.sheikhs.isNotEmpty()) {
                item(key = "sheikhs-header") {
                    SectionHeader(
                        title = stringResource(R.string.home_section_sheikhs),
                        actionLabel = stringResource(R.string.home_see_all),
                        onActionClick = onSeeAllSheikhsClick,
                    )
                }
                item(key = "sheikhs-row") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
                    ) {
                        items(items = state.sheikhs, key = { sheikh -> sheikh.id }) { sheikh ->
                            SheikhProfileCard(
                                sheikh = sheikh,
                                onClick = { onSheikhClick(sheikh.id) },
                            )
                        }
                    }
                }
            }

            if (state.circles.isNotEmpty()) {
                item(key = "circles-header") {
                    SectionHeader(
                        title = stringResource(R.string.home_section_circles),
                        actionLabel = stringResource(R.string.home_see_all),
                        onActionClick = onSeeAllCirclesClick,
                    )
                }
                items(items = state.circles, key = { circle -> circle.id }) { circle ->
                    ActiveCircleRow(
                        circle = circle,
                        isJoining = circle.id in state.joiningCircleIds,
                        onJoinClick = { onJoinCircleClick(circle.id) },
                    )
                }
            }
        }
    }
}
