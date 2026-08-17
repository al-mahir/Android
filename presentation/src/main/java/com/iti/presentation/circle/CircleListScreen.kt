package com.iti.presentation.circle

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.components.bottomsheet.AppBottomSheet
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.button.SecondaryButton
import com.example.designsystem.components.filter.FilterChips
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
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Theme.colors.backGround,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateCircle,
                containerColor = Theme.colors.primary,
                contentColor = Theme.colors.onPrimary,
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            }
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
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            state.currentCircle?.let { current ->
                                item(key = "current-circle") {
                                    CurrentCircleCard(
                                        circle = current,
                                        onClick = { onCircleClick(current.id) },
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    )
                                }
                            }

                            item {
                                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    SearchBar(
                                        query = state.searchQuery,
                                        onQueryChange = onSearchChanged,
                                        hint = stringResource(R.string.circle_search_hint),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    SecondaryButton(
                                        caption = stringResource(R.string.circle_join_private),
                                        onClick = onJoinPrivateClick,
                                        iconPainter = androidx.compose.ui.graphics.vector.rememberVectorPainter(
                                            Icons.Outlined.Lock,
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    FilterChips(
                                        options = listOf(
                                            null to stringResource(R.string.circle_filter_all),
                                            CircleStatus.SCHEDULED to stringResource(R.string.circle_status_scheduled),
                                            CircleStatus.ONGOING to stringResource(R.string.circle_status_ongoing),
                                            CircleStatus.COMPLETED to stringResource(R.string.circle_status_completed),
                                            CircleStatus.CANCELLED to stringResource(R.string.circle_status_cancelled),
                                        ),
                                        selectedValue = state.selectedStatus,
                                        onValueSelected = onStatusSelected,
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    BasicText(
                                        text = stringResource(R.string.circle_count, state.filteredCircles.size),
                                        style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }

                            if (state.filteredCircles.isEmpty()) {
                                item {
                                    if (state.searchQuery.isBlank()) {
                                        EmptyDataScreen(modifier = Modifier.fillMaxWidth().padding(top = 48.dp))
                                    } else {
                                        EmptySearchScreen(modifier = Modifier.fillMaxWidth().padding(top = 48.dp))
                                    }
                                }
                            } else {
                                items(state.filteredCircles, key = { it.id }) { circle ->
                                    CircleCard(
                                        circle = circle,
                                        isJoined = circle.id in state.joinedCircleIds,
                                        onClick = { onCircleClick(circle.id) },
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    )
                                }
                            }

                            item { Spacer(modifier = Modifier.height(80.dp)) }
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
            style = Theme.typography.body.large.copy(color = Theme.colors.primaryFont),
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
