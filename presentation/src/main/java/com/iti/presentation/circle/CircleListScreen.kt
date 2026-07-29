package com.iti.presentation.circle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.components.placeholderscreens.EmptyDataScreen
import com.example.designsystem.components.placeholderscreens.NetworkErrorScreen
import com.example.designsystem.components.topbar.BackTitleTopBar
import com.example.designsystem.theme.Theme
import com.iti.presentation.R
import com.iti.presentation.circle.state.CircleListEffect
import com.iti.presentation.circle.state.CircleListIntent
import com.iti.presentation.circle.state.CircleListUiState
import com.iti.presentation.core.mvi.ObserveEffect
import org.koin.androidx.compose.koinViewModel

@Composable
fun CircleListScreen(
    onBack: () -> Unit,
    onNavigateToJoiningCircle: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CircleListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            is CircleListEffect.NavigateToJoiningCircle -> onNavigateToJoiningCircle(effect.circleId)
            CircleListEffect.NavigateBack -> onBack()
        }
    }

    CircleListContent(
        state = state,
        onBack = onBack,
        onSearchChanged = { viewModel.onIntent(CircleListIntent.SearchQueryChanged(it)) },
        onTagSelected = { viewModel.onIntent(CircleListIntent.TagSelected(it)) },
        onJoin = { viewModel.onIntent(CircleListIntent.JoinCircle(it)) },
        onRetry = { viewModel.onIntent(CircleListIntent.Retry) },
        modifier = modifier,
    )
}

@Composable
private fun CircleListContent(
    state: CircleListUiState,
    onBack: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onTagSelected: (String) -> Unit,
    onJoin: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround),
    ) {
        BackTitleTopBar(
            title = stringResource(R.string.circle_list_title),
            onBackClick = onBack,
        )

        when {
            state.isLoading -> CircleListSkeleton()
            state.isError -> NetworkErrorScreen(modifier = Modifier.fillMaxSize(), onRetry = onRetry)
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Spacer(modifier = Modifier.height(8.dp))
                            CircleSearchBar(query = state.searchQuery, onQueryChanged = onSearchChanged)
                            Spacer(modifier = Modifier.height(12.dp))
                            CircleTagRow(
                                tags = state.availableTags,
                                selected = state.selectedTag,
                                onSelected = onTagSelected,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    if (state.filteredCircles.isEmpty()) {
                        item {
                            EmptyDataScreen(modifier = Modifier.fillMaxWidth().padding(top = 48.dp))
                        }
                    } else {
                        items(state.filteredCircles, key = { it.id }) { circle ->
                            CircleCard(
                                circle = circle,
                                onJoin = { onJoin(circle.id) },
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
private fun CircleSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
        placeholder = {
            Text(
                text = stringResource(R.string.circle_search_hint),
                style = Theme.typography.body.medium,
                color = Theme.colors.secondaryFont,
            )
        },
        leadingIcon = {
            Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = Theme.colors.secondaryFont)
        },
        singleLine = true,
        textStyle = Theme.typography.body.medium.copy(color = Theme.colors.primaryFont),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Theme.colors.surface,
            unfocusedContainerColor = Theme.colors.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun CircleTagRow(
    tags: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tags) { tag ->
            val isSelected = tag == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) Theme.colors.primary else Theme.colors.surface)
                    .clickable { onSelected(tag) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tag,
                    style = Theme.typography.body.small,
                    color = if (isSelected) Theme.colors.onPrimary else Theme.colors.secondaryFont,
                )
            }
        }
    }
}

@Composable
private fun CircleListSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Theme.colors.surface),
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
