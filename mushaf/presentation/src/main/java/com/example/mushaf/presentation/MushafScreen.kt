package com.example.mushaf.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.snapshotFlow
import com.example.designsystem.theme.Theme
import com.example.mushaf.presentation.components.MushafChrome
import com.example.mushaf.presentation.components.MushafErrorState
import com.example.mushaf.presentation.components.MushafLoading
import com.example.mushaf.presentation.components.MushafPageView
import com.example.mushaf.presentation.state.MushafIntent
import com.example.mushaf.presentation.state.PageLoadState
import org.koin.androidx.compose.koinViewModel


@Composable
fun MushafScreen(
    modifier: Modifier = Modifier,
    viewModel: MushafViewModel = koinViewModel(),
    onNavigateSearch: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()

    val pagerState = rememberPagerState(
        initialPage = state.currentPage - 1,
        pageCount = { state.pageCount },
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .collect { page ->
                val requestedPage = page + 1
                if (requestedPage != viewModel.state.value.currentPage) {
                    viewModel.onIntent(MushafIntent.LoadPage(requestedPage))
                }
            }
    }

    LaunchedEffect(state.currentPage) {
        val target = (state.currentPage - 1).coerceIn(0, state.pageCount - 1)
        if (pagerState.currentPage != target) {
            pagerState.scrollToPage(target)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround),
    ) {
        MushafChrome(
            currentPage = state.currentPage,
            pageCount = state.pageCount,
            isTajweedEnabled = state.isTajweedEnabled,
            isFollowAlongActive = state.isFollowAlongActive,
            onToggleTajweed = { enabled -> viewModel.onIntent(MushafIntent.ToggleTajweed(enabled)) },
            onToggleFollowAlong = {
                viewModel.onIntent(
                    if (state.isFollowAlongActive) MushafIntent.StopFollowAlongPreview
                    else MushafIntent.StartFollowAlongPreview,
                )
            },
        )

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 2,
                modifier = Modifier.fillMaxSize(),
            ) { pageIndex ->
                val pageNumber = pageIndex + 1
                Box(modifier = Modifier.fillMaxSize().graphicsLayer()) {
                    when (val pageState = state.pageState(pageNumber)) {
                        is PageLoadState.Loaded ->
                            MushafPageView(
                                page = pageState.page,
                                mode = state.readingMode,
                                highlightedWordId = {
                                    if (pageNumber == state.currentPage) state.highlightedWordId
                                    else null
                                },
                                prefetchPages = if (pageNumber == state.currentPage) {
                                    listOfNotNull(
                                        state.pages[pageNumber - 2],
                                        state.pages[pageNumber - 1],
                                        state.pages[pageNumber + 1],
                                        state.pages[pageNumber + 2],
                                    )
                                } else {
                                    emptyList()
                                },
                            )

                        PageLoadState.Failed ->
                            MushafErrorState(onRetry = { viewModel.onIntent(MushafIntent.Retry) })

                        PageLoadState.Loading -> MushafLoading()
                    }
                }
            }
        }
    }
}
