package com.iti.presentation.sheikh

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.components.placeholderscreens.EmptyDataScreen
import com.example.designsystem.components.placeholderscreens.NetworkErrorScreen
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

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            is SheikhListEffect.NavigateToSheikhDetails -> onOpenSheikhDetails(effect.sheikhId)
            SheikhListEffect.NavigateBack -> onBack()
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround),
    ) {
        BackTitleTopBar(
            title = stringResource(R.string.sheikh_list_title),
            onBackClick = onBack,
        )

        when {
            state.isLoading -> SheikhListSkeleton()
            state.isError -> NetworkErrorScreen(
                modifier = Modifier.fillMaxSize(),
                onRetry = onRetry,
            )
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Spacer(modifier = Modifier.height(8.dp))
                            SheikhSearchBar(
                                query = state.searchQuery,
                                onQueryChanged = onSearchChanged,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            SheikhFilterRow(
                                selected = state.selectedFilter,
                                onSelected = onFilterSelected,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(
                                    R.string.sheikh_list_count,
                                    state.filteredSheikhs.count {
                                        it.availability == SheikhAvailability.AVAILABLE
                                    },
                                ),
                                style = Theme.typography.body.small,
                                color = Theme.colors.secondaryFont,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    if (state.filteredSheikhs.isEmpty()) {
                        item {
                            EmptyDataScreen(modifier = Modifier.fillMaxWidth().padding(top = 48.dp))
                        }
                    } else {
                        items(state.filteredSheikhs, key = { it.id }) { sheikh ->
                            SheikhCard(
                                sheikh = sheikh,
                                onClick = { onSheikhClick(sheikh.id) },
                                isBookmarked = sheikh.id in state.bookmarkedSheikhIds,
                                onBookmarkClick = { onSheikhBookmarkClick(sheikh.id) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SheikhSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        placeholder = {
            Text(
                text = stringResource(R.string.sheikh_search_hint),
                style = Theme.typography.body.medium,
                color = Theme.colors.secondaryFont,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = Theme.colors.secondaryFont,
            )
        },
        singleLine = true,
        textStyle = Theme.typography.body.medium.copy(color = Theme.colors.primaryFont),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Theme.colors.surface,
            unfocusedContainerColor = Theme.colors.surface,
            disabledContainerColor = Theme.colors.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun SheikhFilterRow(
    selected: SheikhFilter,
    onSelected: (SheikhFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filters = listOf(
        SheikhFilter.ALL to stringResource(R.string.sheikh_filter_all),
        SheikhFilter.AVAILABLE to stringResource(R.string.sheikh_filter_available),
        SheikhFilter.BUSY to stringResource(R.string.sheikh_filter_busy),
    )
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(filters) { (filter, label) ->
            val isSelected = filter == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) Theme.colors.primary else Theme.colors.surface,
                    )
                    .clickable { onSelected(filter) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = Theme.typography.body.small,
                    color = if (isSelected) Theme.colors.onPrimary else Theme.colors.secondaryFont,
                )
            }
        }
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
            .clip(RoundedCornerShape(16.dp))
            .background(Theme.colors.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SheikhInitialsAvatar(initials = sheikh.initials, sheikhId = sheikh.id)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = sheikh.name,
                style = Theme.typography.body.medium,
                color = Theme.colors.primaryFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = sheikh.rating.toString(),
                    style = Theme.typography.body.small,
                    color = Theme.colors.secondaryFont,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            SheikhStatusChip(availability = sheikh.availability)
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
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
internal fun SheikhInitialsAvatar(
    initials: String,
    sheikhId: String,
    modifier: Modifier = Modifier,
    size: Int = 48,
) {
    val colors = listOf(
        Color(0xFF1B5E20), Color(0xFF0D47A1), Color(0xFF4A148C),
        Color(0xFF880E4F), Color(0xFF795548), Color(0xFF37474F),
    )
    val color = colors[sheikhId.hashCode().and(0x7FFFFFFF) % colors.size]
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = Theme.typography.body.medium,
            color = Color.White,
        )
    }
}

@Composable
internal fun SheikhStatusChip(
    availability: SheikhAvailability,
    modifier: Modifier = Modifier,
) {
    val (label, color) = when (availability) {
        SheikhAvailability.AVAILABLE ->
            stringResource(R.string.sheikh_status_available) to Color(0xFF4CAF50)
        SheikhAvailability.IN_SESSION ->
            stringResource(R.string.sheikh_status_in_session) to Theme.colors.error
        SheikhAvailability.OFFLINE ->
            stringResource(R.string.sheikh_status_offline) to Theme.colors.secondaryFont
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color),
        )
        Text(text = label, style = Theme.typography.body.small, color = color)
    }
}

@Composable
private fun SheikhListSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        repeat(5) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Theme.colors.surface),
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
