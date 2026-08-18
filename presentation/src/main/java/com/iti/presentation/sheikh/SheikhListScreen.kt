package com.iti.presentation.sheikh

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.components.avatar.InitialsAvatar
import com.example.designsystem.components.filter.FilterChips
import com.example.designsystem.components.loading.shimmer
import com.example.designsystem.components.placeholderscreens.EmptyDataScreen
import com.example.designsystem.components.placeholderscreens.EmptySearchScreen
import com.example.designsystem.components.placeholderscreens.NetworkErrorScreen
import com.example.designsystem.components.rating.RatingLabel
import com.example.designsystem.components.refresh.AppPullToRefreshBox
import com.example.designsystem.components.refresh.PullToRefreshPlaceholder
import com.example.designsystem.components.search.SearchBar
import com.example.designsystem.components.status.StatusLabel
import com.example.designsystem.components.topbar.BackTitleTopBar
import com.example.designsystem.theme.Theme
import com.iti.domain.model.Sheikh
import com.iti.domain.model.SheikhAvailability
import com.iti.presentation.R
import com.iti.presentation.core.mvi.ObserveEffect
import com.iti.presentation.sheikh.state.SheikhFilter
import com.iti.presentation.sheikh.state.SheikhListEffect
import com.iti.presentation.sheikh.state.SheikhListIntent
import com.iti.presentation.sheikh.state.SheikhListUiState
import org.koin.androidx.compose.koinViewModel

@Composable
fun SheikhListScreen(
    onBack: () -> Unit,
    onOpenSheikhDetails: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SheikhListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            is SheikhListEffect.NavigateToSheikhDetails -> onOpenSheikhDetails(effect.sheikhId)
            SheikhListEffect.NavigateBack -> onBack()
            is SheikhListEffect.ShowMessage ->
                Toast.makeText(context, effect.messageRes, Toast.LENGTH_SHORT).show()
        }
    }

    SheikhListContent(
        state = state,
        onBack = onBack,
        onSearchChanged = { viewModel.onIntent(SheikhListIntent.SearchQueryChanged(it)) },
        onFilterSelected = { viewModel.onIntent(SheikhListIntent.FilterSelected(it)) },
        onSheikhClick = { viewModel.onIntent(SheikhListIntent.SheikhClicked(it)) },
        onSheikhBookmarkClick = { viewModel.onIntent(SheikhListIntent.ToggleSheikhBookmark(it)) },
        onRetry = { viewModel.onIntent(SheikhListIntent.Retry) },
        onRefresh = { viewModel.onIntent(SheikhListIntent.Refresh) },
        modifier = modifier,
    )
}

@Composable
private fun SheikhListContent(
    state: SheikhListUiState,
    onBack: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onFilterSelected: (SheikhFilter) -> Unit,
    onSheikhClick: (String) -> Unit,
    onSheikhBookmarkClick: (String) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // The header is pinned, so it needs a separator of its own once cards slide beneath it.
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }
    val clearFilters = {
        onSearchChanged("")
        onFilterSelected(SheikhFilter.ALL)
    }

    // A new query or filter rebuilds the list under the reader's finger; put them back at the top.
    LaunchedEffect(state.searchQuery, state.selectedFilter) {
        if (listState.firstVisibleItemIndex > 0) listState.animateScrollToItem(0)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround),
    ) {
        BackTitleTopBar(
            title = stringResource(R.string.sheikh_list_title),
            onBackClick = onBack,
        )

        // Search and filters drive the list, so they stay pinned under the top bar instead of
        // scrolling away with the results they control.
        SheikhListHeader(
            query = state.searchQuery,
            selectedFilter = state.selectedFilter,
            availableCount = state.filteredSheikhs.count {
                it.availability == SheikhAvailability.AVAILABLE
            },
            showResultCount = !state.isLoading && !state.isError,
            onSearchChanged = onSearchChanged,
            onFilterSelected = onFilterSelected,
            onClearFilters = clearFilters,
            isScrolled = isScrolled,
        )

        AppPullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            // Nothing to refresh yet while the first load is still painting the skeleton.
            enabled = !state.isLoading,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
        ) {
            when {
                state.isLoading -> SheikhListSkeleton()
                // The error screen is pullable too — reaching for Retry is optional.
                state.isError -> PullToRefreshPlaceholder {
                    NetworkErrorScreen(onRetry = onRetry)
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = Theme.spacing.medium,
                            end = Theme.spacing.medium,
                            top = Theme.spacing.small,
                            bottom = Theme.spacing.large,
                        ),
                        verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
                    ) {
                        if (state.filteredSheikhs.isEmpty()) {
                            item(key = "empty") {
                                val hasFilters = state.searchQuery.isNotBlank() ||
                                    state.selectedFilter != SheikhFilter.ALL
                                if (hasFilters) {
                                    EmptySearchScreen(
                                        modifier = Modifier.fillParentMaxSize(),
                                        actionButtonText = stringResource(R.string.filters_clear),
                                        onActionClick = clearFilters,
                                    )
                                } else {
                                    EmptyDataScreen(modifier = Modifier.fillParentMaxSize())
                                }
                            }
                        } else {
                            items(state.filteredSheikhs, key = { it.id }) { sheikh ->
                                SheikhCard(
                                    sheikh = sheikh,
                                    onClick = { onSheikhClick(sheikh.id) },
                                    isBookmarked = sheikh.id in state.bookmarkedSheikhIds,
                                    onBookmarkClick = { onSheikhBookmarkClick(sheikh.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The pinned filter bar: search field, availability chips and the available-teacher count. It
 * sits between the top bar and the list and never scrolls away.
 */
@Composable
private fun SheikhListHeader(
    query: String,
    selectedFilter: SheikhFilter,
    availableCount: Int,
    showResultCount: Boolean,
    onSearchChanged: (String) -> Unit,
    onFilterSelected: (SheikhFilter) -> Unit,
    onClearFilters: () -> Unit,
    isScrolled: Boolean,
    modifier: Modifier = Modifier,
) {
    val hasFilters = query.isNotBlank() || selectedFilter != SheikhFilter.ALL

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Theme.colors.backGround),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        SearchBar(
            query = query,
            onQueryChange = onSearchChanged,
            hint = stringResource(R.string.sheikh_search_hint),
            onClear = { onSearchChanged("") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Theme.spacing.medium,
                    end = Theme.spacing.medium,
                    top = Theme.spacing.small,
                ),
        )

        FilterChips(
            options = listOf(
                SheikhFilter.ALL to stringResource(R.string.sheikh_filter_all),
                SheikhFilter.AVAILABLE to stringResource(R.string.sheikh_filter_available),
                SheikhFilter.BUSY to stringResource(R.string.sheikh_filter_busy),
            ),
            selectedValue = selectedFilter,
            onValueSelected = onFilterSelected,
            modifier = Modifier.padding(horizontal = Theme.spacing.medium),
        )

        if (showResultCount) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.extraSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = stringResource(R.string.sheikh_list_count, availableCount),
                    style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
                )
                if (hasFilters) {
                    BasicText(
                        text = stringResource(R.string.filters_clear),
                        style = Theme.typography.body.small.copy(color = Theme.colors.primary),
                        modifier = Modifier
                            .clip(Theme.shapes.small)
                            .clickable(role = Role.Button, onClick = onClearFilters)
                            .padding(horizontal = Theme.spacing.small, vertical = 2.dp),
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(Theme.spacing.extraSmall))
        }

        // Only drawn once content has slid under the header, so a list at rest stays seamless.
        val dividerAlpha by animateFloatAsState(
            targetValue = if (isScrolled) 1f else 0f,
            label = "sheikh-header-divider",
        )
        HorizontalDivider(
            thickness = 1.dp,
            color = Theme.colors.surfaceVariant,
            modifier = Modifier.alpha(dividerAlpha),
        )
    }
}

@Composable
private fun SheikhCard(
    sheikh: Sheikh,
    onClick: () -> Unit,
    isBookmarked: Boolean,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Theme.shapes.large)
            .background(Theme.colors.surface)
            .border(width = 1.dp, color = Theme.colors.surfaceVariant, shape = Theme.shapes.large)
            .clickable(onClick = onClick)
            .padding(Theme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        SheikhInitialsAvatar(
            initials = sheikh.initials,
            sheikhId = sheikh.id,
            avatarUrl = sheikh.avatarUrl,
            contentDescription = stringResource(R.string.sheikh_cd_avatar, sheikh.name),
            // The availability dot rides on the avatar so status reads at a glance, before
            // the eye reaches the label.
            statusColor = sheikh.availability.color(),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
        ) {
            BasicText(
                text = sheikh.name,
                style = Theme.typography.body.large.copy(
                    color = Theme.colors.primaryFont,
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            sheikh.specialization.takeIf { it.isNotBlank() }?.let { specialization ->
                BasicText(
                    text = specialization,
                    style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
            ) {
                RatingLabel(
                    rating = sheikh.rating,
                    contentDescription = stringResource(R.string.sheikh_stat_rating),
                )
                SheikhStatusChip(availability = sheikh.availability)
            }
        }

        IconButton(onClick = onBookmarkClick) {
            Icon(
                painter = painterResource(
                    if (isBookmarked) {
                        com.example.designsystem.R.drawable.ic_bookmark_filled
                    } else {
                        com.example.designsystem.R.drawable.ic_bookmark
                    },
                ),
                contentDescription = stringResource(R.string.sheikh_cd_favourite),
                tint = if (isBookmarked) Theme.colors.primary else Theme.colors.secondaryFont,
                modifier = Modifier.size(Theme.size.iconMedium),
            )
        }
    }
}

/**
 * Circular avatar with a stable, id-derived background so the same teacher always wears the same
 * colour. Falls back to initials whenever [avatarUrl] is missing or fails to load.
 *
 * @param statusColor when non-null, an availability dot is drawn on the avatar's outer corner.
 */
@Composable
internal fun SheikhInitialsAvatar(
    initials: String,
    sheikhId: String,
    modifier: Modifier = Modifier,
    size: Int = 48,
    avatarUrl: String? = null,
    contentDescription: String = "",
    statusColor: Color? = null,
) {
    val palette = listOf(
        Color(0xFF1B5E20), Color(0xFF0D47A1), Color(0xFF4A148C),
        Color(0xFF880E4F), Color(0xFF795548), Color(0xFF37474F),
    )
    val containerColor = palette[sheikhId.hashCode().and(0x7FFFFFFF) % palette.size]
    val avatarSize: Dp = size.dp

    Box(modifier = modifier.size(avatarSize)) {
        InitialsAvatar(
            initials = initials,
            contentDescription = contentDescription,
            imageUrl = avatarUrl,
            containerColor = containerColor,
            contentColor = Color.White,
            textStyle = if (size >= 72) Theme.typography.title else Theme.typography.body.medium,
            modifier = Modifier.size(avatarSize),
        )
        if (statusColor != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(Theme.size.iconSemiMedium)
                    .clip(CircleShape)
                    // A ring in the card colour lifts the dot off the avatar underneath it.
                    .background(Theme.colors.surface)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(statusColor),
            )
        }
    }
}

@Composable
internal fun SheikhStatusChip(
    availability: SheikhAvailability,
    modifier: Modifier = Modifier,
) {
    StatusLabel(
        text = availability.label(),
        color = availability.color(),
        modifier = modifier,
    )
}

@Composable
private fun SheikhAvailability.label(): String = when (this) {
    SheikhAvailability.AVAILABLE -> stringResource(R.string.sheikh_status_available)
    SheikhAvailability.IN_SESSION -> stringResource(R.string.sheikh_status_in_session)
    SheikhAvailability.OFFLINE -> stringResource(R.string.sheikh_status_offline)
}

@Composable
private fun SheikhAvailability.color(): Color = when (this) {
    SheikhAvailability.AVAILABLE -> Theme.colors.success
    SheikhAvailability.IN_SESSION -> Theme.colors.amber
    SheikhAvailability.OFFLINE -> Theme.colors.secondaryFont
}

@Composable
private fun SheikhListSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        repeat(6) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .clip(Theme.shapes.large)
                    .shimmer(),
            )
        }
    }
}
