package com.iti.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.designsystem.components.bottomnav.bottomNavBarHeight
import com.example.designsystem.components.placeholderscreens.NetworkErrorScreen
import com.example.designsystem.components.section.SectionHeader
import com.example.designsystem.theme.Theme
import com.iti.presentation.R
import com.iti.presentation.home.components.ActiveCircleRow
import com.iti.presentation.home.components.AyahOfTheDayCard
import com.iti.presentation.home.components.ContinueReadingCard
import com.iti.presentation.home.components.HomeHeader
import com.iti.presentation.home.components.HomeSkeleton
import com.iti.presentation.home.components.OngoingCallCard
import com.iti.presentation.home.components.PendingMeetingRequestCard
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
    onViewPendingMeetingClick: () -> Unit = {},
    onCancelPendingMeetingClick: () -> Unit = {},
    onRejoinActiveCallClick: () -> Unit = {},
    onDismissActiveCallClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val rootModifier = modifier
        .fillMaxSize()
        .background(Theme.colors.backGround)

    val gutter = Modifier.padding(horizontal = Theme.spacing.medium)

    when {
        state.hasError -> NetworkErrorScreen(
            modifier = rootModifier,
            description = stringResource(state.errorMessageRes ?: R.string.home_error_generic),
            onRetry = onRetryClick,
        )

        state.isLoading && state.user == null -> HomeSkeleton(modifier = rootModifier)

        else -> Column(modifier = rootModifier) {
            HomeHeader(
                initials = state.user?.initials,
                avatarUrl = state.user?.avatarUrl,
                onProfileClick = onProfileClick,
                onSearchClick = onSearchClick,
                modifier = Modifier
                    .then(gutter)
                    .padding(top = Theme.spacing.medium, bottom = Theme.spacing.medium),
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = Theme.spacing.extraLarge + bottomNavBarHeight(),
                ),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
            ) {
                // ── Ongoing call (process died mid-call — rejoin prompt) ───────
                state.activeCall?.let { active ->
                    item(key = "active-call") {
                        OngoingCallCard(
                            call = active,
                            onRejoin = onRejoinActiveCallClick,
                            onDismiss = onDismissActiveCallClick,
                            modifier = gutter,
                        )
                    }
                }

                // ── Pending meeting request ──────────────────────────────────
                state.pendingMeetingRequest?.let { pending ->
                    item(key = "pending-meeting-request") {
                        PendingMeetingRequestCard(
                            request = pending,
                            onView = onViewPendingMeetingClick,
                            onCancel = onCancelPendingMeetingClick,
                            modifier = gutter,
                        )
                    }
                }

                // ── Continue reading ─────────────────────────────────────────
                state.readingProgress?.let { progress ->
                    item(key = "continue-reading") {
                        ContinueReadingCard(
                            progress = progress,
                            onClick = onContinueReadingClick,
                            modifier = gutter,
                        )
                    }
                }

                // ── Sheikhs ─────────────────────────────────────────────────
                if (state.sheikhs.isNotEmpty() && !state.isOffline) {
                    item(key = "sheikhs-header") {
                        SectionHeader(
                            title = stringResource(R.string.home_section_sheikhs),
                            actionLabel = stringResource(R.string.home_see_all),
                            onActionClick = onSeeAllSheikhsClick,
                            modifier = gutter,
                        )
                    }
                    item(key = "sheikhs-row") {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = Theme.spacing.medium),
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

                // ── Active circles ───────────────────────────────────────────
                if (state.circles.isNotEmpty() && !state.isOffline) {
                    item(key = "circles-header") {
                        SectionHeader(
                            title = stringResource(R.string.home_section_circles),
                            actionLabel = stringResource(R.string.home_see_all),
                            onActionClick = onSeeAllCirclesClick,
                            modifier = gutter,
                        )
                    }
                    items(items = state.circles, key = { circle -> circle.id }) { circle ->
                        ActiveCircleRow(
                            circle = circle,
                            isJoining = circle.id in state.joiningCircleIds,
                            onJoinClick = { onJoinCircleClick(circle.id) },
                            modifier = gutter,
                        )
                    }
                }

                // ── Ayah of the Day ──────────────────────────────────────────
                state.ayahOfTheDay?.let { ayah ->
                    item(key = "ayah-of-the-day") {
                        AyahOfTheDayCard(
                            ayah = ayah,
                            modifier = gutter,
                        )
                    }
                }
            }
        }
    }
}
