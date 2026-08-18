package com.iti.sheikh.presentation.circle

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.components.placeholderscreens.EmptyDataScreen
import com.example.designsystem.components.placeholderscreens.NetworkErrorScreen
import com.example.designsystem.components.topbar.BackTitleTopBar
import com.example.designsystem.theme.Theme
import com.iti.sheikh.presentation.R
import com.iti.sheikh.presentation.circle.components.SheikhCircleCard
import com.iti.sheikh.presentation.circle.state.SheikhCircleListEffect
import com.iti.sheikh.presentation.circle.state.SheikhCircleListIntent
import com.iti.sheikh.presentation.circle.state.SheikhCircleListUiState
import com.iti.sheikh.presentation.core.mvi.ObserveEffect
import org.koin.androidx.compose.koinViewModel

private val SheikhCircleListTopBarHeight = 64.dp

@Composable
fun SheikhCircleListScreen(
    onBack: () -> Unit,
    onOpenCircle: (String) -> Unit,
    onOpenCreateCircle: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SheikhCircleListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            is SheikhCircleListEffect.OpenCircle -> onOpenCircle(effect.circleId)
            SheikhCircleListEffect.OpenCreateCircle -> onOpenCreateCircle()
        }
    }

    SheikhCircleListContent(
        state = state,
        onBack = onBack,
        onCircleClick = { viewModel.onIntent(SheikhCircleListIntent.CircleClicked(it)) },
        onRetry = { viewModel.onIntent(SheikhCircleListIntent.Retry) },
        onCreateCircle = { viewModel.onIntent(SheikhCircleListIntent.CreateCircleClicked) },
        modifier = modifier,
    )
}

@Composable
private fun SheikhCircleListContent(
    state: SheikhCircleListUiState,
    onBack: () -> Unit,
    onCircleClick: (String) -> Unit,
    onRetry: () -> Unit,
    onCreateCircle: () -> Unit,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            BackTitleTopBar(
                title = stringResource(R.string.sheikh_circle_list_title),
                onBackClick = onBack,
                height = SheikhCircleListTopBarHeight,
                extendsUnderStatusBar = true,
                modifier = Modifier.align(Alignment.TopCenter),
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = SheikhCircleListTopBarHeight),
            ) {
                when {
                    state.isLoading -> SheikhCircleListSkeleton()
                    state.isError -> NetworkErrorScreen(modifier = Modifier.fillMaxSize(), onRetry = onRetry)
                    state.circles.isEmpty() -> EmptyDataScreen(
                        modifier = Modifier.fillMaxSize(),
                        title = stringResource(R.string.sheikh_circle_list_empty_title),
                        description = stringResource(R.string.sheikh_circle_list_empty_description),
                        actionButtonText = stringResource(R.string.sheikh_circle_list_create),
                        onActionClick = onCreateCircle,
                    )
                    else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.circles, key = { it.id }) { circle ->
                            SheikhCircleCard(
                                circle = circle,
                                onClick = { onCircleClick(circle.id) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheikhCircleListSkeleton(modifier: Modifier = Modifier) {
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
