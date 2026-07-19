package com.example.mushaf.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import com.example.designsystem.theme.Theme
import com.example.mushaf.presentation.components.MushafBottomBar
import com.example.mushaf.presentation.components.MushafErrorState
import com.example.mushaf.presentation.components.MushafLoading
import com.example.mushaf.presentation.components.MushafPageView
import com.example.mushaf.presentation.components.MushafTopBar
import com.example.mushaf.presentation.state.MushafIntent
import com.example.mushaf.presentation.state.PageLoadState
import org.koin.androidx.compose.koinViewModel

@Composable
fun MushafScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    viewModel: MushafViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val pagerState = rememberPagerState(
        initialPage = state.currentPage - 1,
        pageCount = { state.pageCount },
    )

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onIntent(MushafIntent.LoadPage(pagerState.currentPage + 1))
    }

    LaunchedEffect(state.currentPage) {
        val target = (state.currentPage - 1).coerceIn(0, state.pageCount - 1)
        if (pagerState.currentPage != target) {
            pagerState.scrollToPage(target)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 2,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            viewModel.onIntent(MushafIntent.ToggleBars)
                        }
                    },
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
                                areAyahsHidden = !state.areAyahsVisible,
                                revealedWordIds = state.revealedWordIds,
                            )

                        PageLoadState.Failed ->
                            MushafErrorState(onRetry = { viewModel.onIntent(MushafIntent.Retry) })

                        PageLoadState.Loading -> MushafLoading()
                    }
                }
            }
        }

        MushafTopBar(
            visible = state.areBarsVisible,
            surahName = state.page?.lines
                ?.firstOrNull { it.surahNumber != null }
                ?.let { SurahNameResolver.nameFor(it.surahNumber) }
                ?: "",
            juzNumber = 1,
            hizbNumber = 1,
            isBookmarked = false,
            onBack = onBack,
            onBookmark = {},
            onSettings = {},
            modifier = Modifier.align(Alignment.TopCenter),
        )

        MushafBottomBar(
            visible = state.areBarsVisible,
            mushafMode = state.mushafMode,
            areAyahsVisible = state.areAyahsVisible,
            isRecordingActive = state.isRecordingActive,
            onToggleAyahVisibility = { viewModel.onIntent(MushafIntent.ToggleAyahVisibility) },
            onRevealNextWord = { viewModel.onIntent(MushafIntent.RevealNextWord) },
            onRevealNextAyah = { viewModel.onIntent(MushafIntent.RevealNextAyah) },
            onModeSelected = { mode -> viewModel.onIntent(MushafIntent.SetMode(mode)) },
            onToggleRecording = { viewModel.onIntent(MushafIntent.ToggleRecording) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
