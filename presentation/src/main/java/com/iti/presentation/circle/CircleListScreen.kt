package com.iti.presentation.circle

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.components.bottomsheet.AppBottomSheet
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.button.SecondaryButton
import com.example.designsystem.components.filter.FilterChips
import com.example.designsystem.components.loading.shimmer
import com.example.designsystem.components.placeholderscreens.EmptyDataScreen
import com.example.designsystem.components.placeholderscreens.EmptySearchScreen
import com.example.designsystem.components.placeholderscreens.NetworkErrorScreen
import com.example.designsystem.components.refresh.AppPullToRefreshBox
import com.example.designsystem.components.refresh.PullToRefreshPlaceholder
import com.example.designsystem.components.search.SearchBar
import com.example.designsystem.components.textfield.TextField
import com.example.designsystem.components.topbar.BackTitleTopBar
import com.example.designsystem.theme.Theme
import com.iti.meeting.domain.model.circle.CircleStatus
import com.iti.presentation.R
import com.iti.presentation.circle.state.CircleListEffect
import com.iti.presentation.circle.state.CircleListIntent
import com.iti.presentation.circle.state.CircleListUiState
import com.iti.presentation.core.mvi.ObserveEffect
import org.koin.androidx.compose.koinViewModel

/** Room left under the last card so the floating action button never covers it. */
private val ListBottomInset = 96.dp

@Composable
fun CircleListScreen(
    onBack: () -> Unit,
    onOpenCircle: (String) -> Unit,
    onOpenCreateCircle: () -> Unit,
    modifier: Modifier = Modifier,
    /** True while this screen is the top back-stack entry — flips false→true when the user
     * returns here (e.g. after leaving a circle), triggering a joined-circles refresh. */
    refreshKey: Boolean = true,
    viewModel: CircleListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(refreshKey) {
        if (refreshKey) viewModel.onIntent(CircleListIntent.Refresh)
    }

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            is CircleListEffect.OpenCircle -> onOpenCircle(effect.circleId)
            CircleListEffect.OpenCreateCircle -> onOpenCreateCircle()
            CircleListEffect.NavigateBack -> onBack()
            is CircleListEffect.ShowMessage ->
                Toast.makeText(context, effect.messageRes, Toast.LENGTH_SHORT).show()
        }
    }

    CircleListContent(
        state = state,
        onBack = onBack,
        onSearchChanged = { viewModel.onIntent(CircleListIntent.SearchQueryChanged(it)) },
        onStatusSelected = { viewModel.onIntent(CircleListIntent.StatusSelected(it)) },
        onCircleClick = { viewModel.onIntent(CircleListIntent.CircleClicked(it)) },
        onRetry = { viewModel.onIntent(CircleListIntent.Retry) },
        onPullToRefresh = { viewModel.onIntent(CircleListIntent.PullToRefresh) },
        onCreateCircle = { viewModel.onIntent(CircleListIntent.CreateCircleClicked) },
        onJoinPrivateClick = { viewModel.onIntent(CircleListIntent.JoinPrivateClicked) },
        onJoinCircleIdChanged = { viewModel.onIntent(CircleListIntent.JoinCircleIdChanged(it)) },
        onJoinPasswordChanged = { viewModel.onIntent(CircleListIntent.JoinPasswordChanged(it)) },
        onJoinTokenChanged = { viewModel.onIntent(CircleListIntent.JoinTokenChanged(it)) },
        onJoinModeChanged = { viewModel.onIntent(CircleListIntent.JoinModeChanged(it)) },
        onSubmitJoin = { viewModel.onIntent(CircleListIntent.SubmitJoinPrivate) },
        onSubmitJoinViaToken = { viewModel.onIntent(CircleListIntent.SubmitJoinViaToken) },
        onDismissJoinSheet = { viewModel.onIntent(CircleListIntent.DismissJoinPrivate) },
        modifier = modifier,
    )
}

@Composable
private fun CircleListContent(
    state: CircleListUiState,
    onBack: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onStatusSelected: (CircleStatus?) -> Unit,
    onCircleClick: (String) -> Unit,
    onRetry: () -> Unit,
    onPullToRefresh: () -> Unit,
    onCreateCircle: () -> Unit,
    onJoinPrivateClick: () -> Unit,
    onJoinCircleIdChanged: (String) -> Unit,
    onJoinPasswordChanged: (String) -> Unit,
    onJoinTokenChanged: (String) -> Unit,
    onJoinModeChanged: (Boolean) -> Unit,
    onSubmitJoin: () -> Unit,
    onSubmitJoinViaToken: () -> Unit,
    onDismissJoinSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // Drives both the header separator and the FAB collapse — the header is pinned, so it needs an
    // edge of its own once content slides underneath it.
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }
    val clearFilters = {
        onSearchChanged("")
        onStatusSelected(null)
    }

    // A new query or status rebuilds the list under the reader's finger; put them back at the top
    // so the first result is the one they see.
    LaunchedEffect(state.searchQuery, state.selectedStatus) {
        if (listState.firstVisibleItemIndex > 0) listState.animateScrollToItem(0)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Theme.colors.backGround,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateCircle,
                // Labelled while the list is at rest, shrinking to the icon once the user scrolls.
                expanded = !isScrolled,
                containerColor = Theme.colors.primary,
                contentColor = Theme.colors.onPrimary,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.create_circle_title),
                    )
                },
                text = {
                    BasicText(
                        text = stringResource(R.string.create_circle_title),
                        style = Theme.typography.body.medium.copy(
                            color = Theme.colors.onPrimary,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            BackTitleTopBar(
                title = stringResource(R.string.circle_list_title),
                onBackClick = onBack,
            )

            // Search and status chips are how the user drives the list, so they stay pinned
            // instead of scrolling away with the results they control.
            CircleListHeader(
                query = state.searchQuery,
                selectedStatus = state.selectedStatus,
                resultCount = state.filteredCircles.size,
                showResultCount = !state.isLoading && !state.isError,
                onSearchChanged = onSearchChanged,
                onStatusSelected = onStatusSelected,
                onJoinPrivateClick = onJoinPrivateClick,
                onClearFilters = clearFilters,
                isScrolled = isScrolled,
            )

            AppPullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = onPullToRefresh,
                // Nothing to refresh yet while the first load is still painting the skeleton.
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
            ) {
                when {
                    state.isLoading -> CircleListSkeleton()
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
                                bottom = ListBottomInset,
                            ),
                            verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
                        ) {
                            state.currentCircle?.let { current ->
                                item(key = "current-circle") {
                                    CurrentCircleCard(
                                        circle = current,
                                        onClick = { onCircleClick(current.id) },
                                    )
                                }
                            }

                            if (state.filteredCircles.isEmpty()) {
                                item(key = "empty") {
                                    val hasFilters =
                                        state.searchQuery.isNotBlank() || state.selectedStatus != null
                                    if (hasFilters) {
                                        EmptySearchScreen(
                                            modifier = Modifier.fillParentMaxSize(),
                                            actionButtonText = stringResource(R.string.filters_clear),
                                            onActionClick = clearFilters,
                                        )
                                    } else {
                                        EmptyDataScreen(
                                            modifier = Modifier.fillParentMaxSize(),
                                            actionButtonText = stringResource(R.string.create_circle_title),
                                            onActionClick = onCreateCircle,
                                        )
                                    }
                                }
                            } else {
                                items(state.filteredCircles, key = { it.id }) { circle ->
                                    CircleCard(
                                        circle = circle,
                                        isJoined = circle.id in state.joinedCircleIds,
                                        onClick = { onCircleClick(circle.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.joinSheetVisible) {
        JoinPrivateCircleSheet(
            state = state,
            onCircleIdChanged = onJoinCircleIdChanged,
            onPasswordChanged = onJoinPasswordChanged,
            onTokenChanged = onJoinTokenChanged,
            onModeChanged = onJoinModeChanged,
            onSubmit = onSubmitJoin,
            onSubmitViaToken = onSubmitJoinViaToken,
            onDismiss = onDismissJoinSheet,
        )
    }
}

/**
 * The pinned filter bar: search field, private-circle shortcut, status chips and the result
 * count. It sits between the top bar and the list and never scrolls, so the controls that drive
 * the list are always one tap away.
 */
@Composable
private fun CircleListHeader(
    query: String,
    selectedStatus: CircleStatus?,
    resultCount: Int,
    showResultCount: Boolean,
    onSearchChanged: (String) -> Unit,
    onStatusSelected: (CircleStatus?) -> Unit,
    onJoinPrivateClick: () -> Unit,
    onClearFilters: () -> Unit,
    isScrolled: Boolean,
    modifier: Modifier = Modifier,
) {
    val hasFilters = query.isNotBlank() || selectedStatus != null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Theme.colors.backGround),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Theme.spacing.medium,
                    end = Theme.spacing.medium,
                    top = Theme.spacing.small,
                ),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchBar(
                query = query,
                onQueryChange = onSearchChanged,
                hint = stringResource(R.string.circle_search_hint),
                onClear = { onSearchChanged("") },
                modifier = Modifier.weight(1f),
            )
            HeaderIconAction(
                icon = Icons.Outlined.Lock,
                contentDescription = stringResource(R.string.circle_join_private),
                onClick = onJoinPrivateClick,
            )
        }

        FilterChips(
            options = listOf(
                null to stringResource(R.string.circle_filter_all),
                CircleStatus.SCHEDULED to stringResource(R.string.circle_status_scheduled),
                CircleStatus.ONGOING to stringResource(R.string.circle_status_ongoing),
                CircleStatus.COMPLETED to stringResource(R.string.circle_status_completed),
                CircleStatus.CANCELLED to stringResource(R.string.circle_status_cancelled),
            ),
            selectedValue = selectedStatus,
            onValueSelected = onStatusSelected,
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
                    text = stringResource(R.string.circle_count, resultCount),
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
            label = "circle-header-divider",
        )
        HorizontalDivider(
            thickness = 1.dp,
            color = Theme.colors.surfaceVariant,
            modifier = Modifier.alpha(dividerAlpha),
        )
    }
}

/** Square, field-height action sitting beside the search bar. */
@Composable
private fun HeaderIconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .size(Theme.size.componentsNormalHeight)
            .clip(shape)
            .background(Theme.colors.primary.copy(alpha = 0.10f))
            .border(width = 1.dp, color = Theme.colors.primary.copy(alpha = 0.24f), shape = shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Theme.colors.primary,
            modifier = Modifier.size(Theme.size.iconMedium),
        )
    }
}

@Composable
private fun JoinPrivateCircleSheet(
    state: CircleListUiState,
    onCircleIdChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onTokenChanged: (String) -> Unit,
    onModeChanged: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onSubmitViaToken: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(onDismiss = onDismiss) {
        Spacer(modifier = Modifier.height(8.dp))
        BasicText(
            text = stringResource(R.string.circle_join_private_sheet_title),
            style = Theme.typography.body.large.copy(
                color = Theme.colors.primaryFont,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Mode toggle: By ID vs By Invite Link
        FilterChips(
            options = listOf(
                false to stringResource(R.string.circle_join_mode_by_id),
                true to stringResource(R.string.circle_join_via_link),
            ),
            selectedValue = state.joinByToken,
            onValueSelected = onModeChanged,
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (!state.joinByToken) {
            // ── By ID + password ──────────────────────────────────────────
            TextField(
                text = state.joinCircleId,
                onTextChange = onCircleIdChanged,
                hint = stringResource(R.string.circle_join_id_hint),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                isError = state.joinErrorRes == R.string.circle_join_id_required,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextField(
                text = state.joinPassword,
                onTextChange = onPasswordChanged,
                hint = stringResource(R.string.circle_password_hint),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            state.joinErrorRes?.let { errorRes ->
                Spacer(modifier = Modifier.height(8.dp))
                BasicText(
                    text = stringResource(errorRes),
                    style = Theme.typography.body.small.copy(color = Theme.colors.error),
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            PrimaryButton(
                caption = stringResource(R.string.circle_join_confirm),
                onClick = onSubmit,
                isLoading = state.isJoining,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            // ── By invite token / link ────────────────────────────────────
            TextField(
                text = state.joinToken,
                onTextChange = onTokenChanged,
                hint = stringResource(R.string.circle_join_token_hint),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                isError = state.joinErrorRes == R.string.circle_join_token_required,
                modifier = Modifier.fillMaxWidth(),
            )
            state.joinErrorRes?.let { errorRes ->
                Spacer(modifier = Modifier.height(8.dp))
                BasicText(
                    text = stringResource(errorRes),
                    style = Theme.typography.body.small.copy(color = Theme.colors.error),
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            PrimaryButton(
                caption = stringResource(R.string.circle_join_confirm),
                onClick = onSubmitViaToken,
                isLoading = state.isJoining,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        SecondaryButton(
            caption = stringResource(R.string.circle_join_cancel),
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CircleListSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        repeat(5) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .clip(Theme.shapes.large)
                    .shimmer(),
            )
        }
    }
}
